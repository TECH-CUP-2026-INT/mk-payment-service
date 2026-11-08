package co.edu.escuelaing.techcup.payment.service.ports;

import co.edu.escuelaing.techcup.payment.service.Payer;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentGatewayPort {

    PseTransactionResult createPseTransaction(String idempotencyKey, BigDecimal amount,
            String financialInstitution, Payer payer, String ipAddress, String callbackUrl, String notificationUrl);

    CardTransactionResult createCardTransaction(String idempotencyKey, BigDecimal amount,
            String token, String paymentMethodId, Integer installments, String payerEmail,
            String identificationType, String identificationNumber);

    PaymentStatusResult getPaymentStatus(String mpPaymentId);

    List<PaymentMethodInfo> getAvailablePaymentMethods();
}
