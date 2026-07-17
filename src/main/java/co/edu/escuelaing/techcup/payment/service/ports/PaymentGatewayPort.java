package co.edu.escuelaing.techcup.payment.service.ports;

import co.edu.escuelaing.techcup.payment.service.Payer;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentGatewayPort {

    PreferenceResult createPreference(String idempotencyKey, String description, BigDecimal amount,
            String notificationUrl, String callbackUrl);

    PseTransactionResult createPseTransaction(String idempotencyKey, BigDecimal amount,
            String financialInstitution, Payer payer, String ipAddress, String callbackUrl, String notificationUrl);

    PaymentStatusResult getPaymentStatus(String mpPaymentId);

    List<PaymentMethodInfo> getAvailablePaymentMethods();
}
