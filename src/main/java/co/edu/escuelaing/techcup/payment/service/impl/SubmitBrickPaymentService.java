package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.exception.PaymentGatewayException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderExpiredException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotFoundException;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.CardTransactionResult;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.PseTransactionResult;
import co.edu.escuelaing.techcup.payment.service.ports.SubmitBrickPaymentUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class SubmitBrickPaymentService implements SubmitBrickPaymentUseCase {

    private static final String PSE_METHOD_ID = "pse";

    private final PaymentOrderRepositoryPort paymentOrderRepository;
    private final PaymentGatewayPort paymentGateway;
    private final String callbackUrl;
    private final String notificationUrl;

    public SubmitBrickPaymentService(PaymentOrderRepositoryPort paymentOrderRepository,
            PaymentGatewayPort paymentGateway,
            @Value("${mercadopago.callback-url}") String callbackUrl,
            @Value("${mercadopago.notification-url}") String notificationUrl) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentGateway = paymentGateway;
        this.callbackUrl = callbackUrl;
        this.notificationUrl = notificationUrl;
    }

    @Override
    public PaymentOrder submit(String enrollmentId, String paymentMethodId, String token,
            Integer installments, String payerEmail, String financialInstitution,
            BigDecimal transactionAmount, String ipAddress) {

        PaymentOrder paymentOrder = paymentOrderRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new PaymentOrderNotFoundException(
                        "No existe una orden de pago para enrollmentId " + enrollmentId));

        rejectIfExpired(paymentOrder);

        if (PSE_METHOD_ID.equalsIgnoreCase(paymentMethodId)) {
            return submitPse(paymentOrder, financialInstitution, payerEmail, ipAddress);
        }
        return submitCard(paymentOrder, token, paymentMethodId, installments, payerEmail, null, null);
    }

    private PaymentOrder submitPse(PaymentOrder paymentOrder, String financialInstitution,
            String payerEmail, String ipAddress) {
        // PSE requires a full Payer — the Brick only provides email + bank.
        // The remaining fields are filled with safe placeholder values accepted
        // by Mercado Pago's sandbox; production flows should collect them via a
        // complementary form before calling this endpoint.
        Payer payer = new Payer(payerEmail, "CC", "0", "individual",
                "Pagador", "TechCup", "00000", "N/A", "0", "N/A", "N/A", "601", "00000");

        paymentOrder.startPseTransaction(payer, financialInstitution);

        PseTransactionResult result;
        try {
            result = paymentGateway.createPseTransaction(paymentOrder.getIdempotencyKey(),
                    paymentOrder.getAmount(), financialInstitution, payer, ipAddress, callbackUrl, notificationUrl);
        } catch (Exception ex) {
            throw new PaymentGatewayException("Mercado Pago rechazó la solicitud de transacción PSE", ex);
        }

        paymentOrder.assignGatewayReference(result.mpPaymentId(), result.externalResourceUrl(), null);
        return paymentOrderRepository.save(paymentOrder);
    }

    private PaymentOrder submitCard(PaymentOrder paymentOrder, String token,
            String paymentMethodId, Integer installments, String payerEmail,
            @SuppressWarnings("unused") String identificationType,
            @SuppressWarnings("unused") String identificationNumber) {

        paymentOrder.startCardTransaction();

        CardTransactionResult result;
        try {
            result = paymentGateway.createCardTransaction(paymentOrder.getIdempotencyKey(),
                    paymentOrder.getAmount(), token, paymentMethodId,
                    installments != null ? installments : 1, payerEmail,
                    identificationType, identificationNumber);
        } catch (Exception ex) {
            throw new PaymentGatewayException("Mercado Pago rechazó la solicitud de pago con tarjeta", ex);
        }

        paymentOrder.assignGatewayReference(result.mpPaymentId(), null, result.statusDetail());
        return paymentOrderRepository.save(paymentOrder);
    }

    private void rejectIfExpired(PaymentOrder paymentOrder) {
        boolean alreadyExpiredInDb = paymentOrder.getStatus() == PaymentOrderStatus.EXPIRED;
        boolean expiredButNotYetSwept = paymentOrder.isExpired(LocalDateTime.now(ZoneId.systemDefault()))
                && (paymentOrder.getStatus() == PaymentOrderStatus.PENDING
                        || paymentOrder.getStatus() == PaymentOrderStatus.AWAITING_BANK_CONFIRMATION);

        if (!alreadyExpiredInDb && !expiredButNotYetSwept) {
            return;
        }
        if (expiredButNotYetSwept) {
            paymentOrder.expire();
            try {
                paymentOrderRepository.save(paymentOrder);
            } catch (OptimisticLockingFailureException ignored) {
            }
        }
        throw new PaymentOrderExpiredException("La orden %s ya expiró".formatted(paymentOrder.getId()));
    }
}
