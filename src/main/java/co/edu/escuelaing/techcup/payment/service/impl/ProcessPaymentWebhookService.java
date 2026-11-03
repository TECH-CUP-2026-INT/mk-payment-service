package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotAwaitingBankConfirmationException;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentStatusResult;
import co.edu.escuelaing.techcup.payment.service.ports.ProcessPaymentWebhookUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Mercado Pago retries webhook deliveries on any non-2xx response, so every
 * path here must end in a normal return - never propagate an exception.
 */
@Service
public class ProcessPaymentWebhookService implements ProcessPaymentWebhookUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentWebhookService.class);
    private static final String MP_STATUS_APPROVED = "approved";
    private static final String MP_STATUS_REJECTED = "rejected";
    private static final String MP_STATUS_CANCELLED = "cancelled";

    private final PaymentOrderRepositoryPort paymentOrderRepository;
    private final PaymentGatewayPort paymentGateway;

    public ProcessPaymentWebhookService(PaymentOrderRepositoryPort paymentOrderRepository,
            PaymentGatewayPort paymentGateway) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentGateway = paymentGateway;
    }

    @Override
    public void process(String mpPaymentId) {
        PaymentStatusResult statusResult;
        try {
            // The real status always comes from here, never from the webhook body.
            statusResult = paymentGateway.getPaymentStatus(mpPaymentId);
        } catch (Exception ex) {
            log.warn("No se pudo consultar el estado del pago {} en Mercado Pago, se ignora la notificación",
                    mpPaymentId, ex);
            return;
        }

        Optional<PaymentOrder> maybeOrder = paymentOrderRepository.findByMpPaymentId(mpPaymentId);
        if (maybeOrder.isEmpty()) {
            log.warn("No se encontró una orden de pago para mpPaymentId {}, se ignora la notificación", mpPaymentId);
            return;
        }
        PaymentOrder paymentOrder = maybeOrder.get();

        boolean approved = MP_STATUS_APPROVED.equals(statusResult.status());
        boolean rejected = MP_STATUS_REJECTED.equals(statusResult.status()) || MP_STATUS_CANCELLED.equals(statusResult.status());
        if (!approved && !rejected) {
            log.debug("Estado {} de Mercado Pago para la orden {} no requiere transición", statusResult.status(),
                    paymentOrder.getId());
            return;
        }

        try {
            if (approved) {
                paymentOrder.approve(mpPaymentId);
            } else {
                paymentOrder.reject();
            }
        } catch (PaymentOrderNotAwaitingBankConfirmationException ex) {
            log.info("La orden {} ya no está en AWAITING_BANK_CONFIRMATION, se ignora la notificación duplicada",
                    paymentOrder.getId());
            return;
        }

        try {
            paymentOrderRepository.save(paymentOrder);
        } catch (OptimisticLockingFailureException ex) {
            log.info("Conflicto de versión al resolver la orden {}, otro proceso ya la actualizó primero",
                    paymentOrder.getId());
        }
    }
}
