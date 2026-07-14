package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.exception.PaymentGatewayException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderExpiredException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotFoundException;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.PseTransactionResult;
import co.edu.escuelaing.techcup.payment.service.ports.SubmitPseTransactionUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SubmitPseTransactionService implements SubmitPseTransactionUseCase {

    private final PaymentOrderRepositoryPort paymentOrderRepository;
    private final PaymentGatewayPort paymentGateway;
    private final String notificationUrl;

    public SubmitPseTransactionService(PaymentOrderRepositoryPort paymentOrderRepository,
            PaymentGatewayPort paymentGateway,
            @Value("${mercadopago.notification-url}") String notificationUrl) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentGateway = paymentGateway;
        this.notificationUrl = notificationUrl;
    }

    @Override
    public PaymentOrder submit(String enrollmentId, Payer payer, String financialInstitution) {
        PaymentOrder paymentOrder = paymentOrderRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new PaymentOrderNotFoundException(
                        "No existe una orden de pago para enrollmentId " + enrollmentId));

        rejectIfExpired(paymentOrder);

        paymentOrder.startPseTransaction(payer, financialInstitution);

        PseTransactionResult result;
        try {
            result = paymentGateway.createPseTransaction(paymentOrder.getIdempotencyKey(), paymentOrder.getAmount(),
                    financialInstitution, payer, notificationUrl);
        } catch (Exception ex) {
            throw new PaymentGatewayException("Mercado Pago rechazó la solicitud de transacción PSE", ex);
        }

        paymentOrder.assignGatewayReference(result.mpPaymentId(), result.externalResourceUrl());
        return paymentOrderRepository.save(paymentOrder);
    }

    /**
     * Checked before attempting the state transition so 410 (expired) can be
     * told apart from 409 (wrong state). If the 5-minute ExpireTransactionJob
     * hasn't swept this order yet, the expiration is applied and persisted here
     * too - otherwise GetPaymentOrderStatus would keep reporting a stale status
     * to Tournament Service until the next cron tick.
     */
    private void rejectIfExpired(PaymentOrder paymentOrder) {
        boolean alreadyExpiredInDb = paymentOrder.getStatus() == PaymentOrderStatus.EXPIRED;
        boolean expiredButNotYetSwept = paymentOrder.isExpired(LocalDateTime.now())
                && (paymentOrder.getStatus() == PaymentOrderStatus.PENDING
                        || paymentOrder.getStatus() == PaymentOrderStatus.AWAITING_BANK_CONFIRMATION);

        if (!alreadyExpiredInDb && !expiredButNotYetSwept) {
            return;
        }
        if (expiredButNotYetSwept) {
            paymentOrder.expire();
            try {
                paymentOrderRepository.save(paymentOrder);
            } catch (OptimisticLockingFailureException ex) {
                // Another writer (ExpireTransactionJob or the webhook) already
                // resolved this order first - the 410 response below still holds.
            }
        }
        throw new PaymentOrderExpiredException("La orden %s ya expiró".formatted(paymentOrder.getId()));
    }
}
