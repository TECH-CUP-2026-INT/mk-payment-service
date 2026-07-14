package co.edu.escuelaing.techcup.payment.service.ports;

import co.edu.escuelaing.techcup.payment.service.Payer;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentGatewayPort {

    PseTransactionResult createPseTransaction(String idempotencyKey, BigDecimal amount, String financialInstitution,
            Payer payer, String notificationUrl);

    PaymentStatusResult getPaymentStatus(String mpPaymentId);

    List<PaymentMethodInfo> getAvailablePaymentMethods();
}
