package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.exception.PaymentGatewayException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderExpiredException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotFoundException;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.CardPayer;
import co.edu.escuelaing.techcup.payment.service.ports.CardTransactionResult;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.PseTransactionResult;
import co.edu.escuelaing.techcup.payment.service.ports.SubmitBrickPaymentUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

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
            String identificationType, String identificationNumber, Payer payer,
            CardPayer cardPayer, String ipAddress) {

        PaymentOrder paymentOrder = paymentOrderRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new PaymentOrderNotFoundException(
                        "No existe una orden de pago para enrollmentId " + enrollmentId));

        rejectIfExpired(paymentOrder);

        if (PSE_METHOD_ID.equalsIgnoreCase(paymentMethodId)) {
            return submitPse(paymentOrder, financialInstitution, payer, ipAddress);
        }
        return submitCard(paymentOrder, token, paymentMethodId, installments, payerEmail,
                identificationType, identificationNumber, cardPayer);
    }

    private PaymentOrder submitPse(PaymentOrder paymentOrder, String financialInstitution,
            Payer payer, String ipAddress) {
        if (payer == null) {
            throw new IllegalArgumentException(
                    "Los datos del pagador (documento, nombre, dirección y teléfono) son obligatorios para PSE");
        }

        paymentOrder.startPseTransaction(payer, financialInstitution);

        PseTransactionResult result;
        try {
            result = paymentGateway.createPseTransaction(paymentOrder.getIdempotencyKey(),
                    paymentOrder.getAmount(), financialInstitution, payer, ipAddress, callbackUrl,
                    notificationUrl, paymentOrder.getEnrollmentId());
        } catch (Exception ex) {
            throw new PaymentGatewayException("Mercado Pago rechazó la solicitud de transacción PSE", ex);
        }

        paymentOrder.assignGatewayReference(result.mpPaymentId(), result.externalResourceUrl(), null);
        return paymentOrderRepository.save(paymentOrder);
    }

    private PaymentOrder submitCard(PaymentOrder paymentOrder, String token,
            String paymentMethodId, Integer installments, String payerEmail,
            String identificationType, String identificationNumber, CardPayer cardPayer) {

        paymentOrder.startCardTransaction();

        CardPayer effectivePayer = cardPayer != null ? cardPayer
                : new CardPayer(payerEmail, identificationType, identificationNumber,
                        null, null, null, null, null, null, null, null, null, null);

        CardTransactionResult result;
        try {
            result = paymentGateway.createCardTransaction(paymentOrder.getIdempotencyKey(),
                    paymentOrder.getAmount(), token, paymentMethodId,
                    installments != null ? installments : 1, effectivePayer,
                    notificationUrl, paymentOrder.getEnrollmentId());
        } catch (Exception ex) {
            throw new PaymentGatewayException("Mercado Pago rechazó la solicitud de pago con tarjeta", ex);
        }

        paymentOrder.assignGatewayReference(result.mpPaymentId(), null, result.statusDetail());
        resolveCardStatus(paymentOrder, result.status());
        return paymentOrderRepository.save(paymentOrder);
    }

    /**
     * Maps Mercado Pago's card status onto the domain. Card flows do not need a
     * bank confirmation, so an approved/rejected card resolves the order
     * immediately; anything that is still pending keeps AWAITING_BANK_CONFIRMATION
     * until the webhook reports the final status.
     */
    private void resolveCardStatus(PaymentOrder paymentOrder, String mpStatus) {
        if (mpStatus == null) {
            return;
        }
        switch (mpStatus.toLowerCase(Locale.ROOT)) {
            case "approved", "processed" -> paymentOrder.approve(paymentOrder.getMpPaymentId());
            case "rejected", "cancelled" -> paymentOrder.reject();
            default -> {
                // pending / in_process / authorized: leave the order awaiting confirmation.
            }
        }
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
