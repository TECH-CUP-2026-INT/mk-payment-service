package co.edu.escuelaing.techcup.payment.service.ports;

import co.edu.escuelaing.techcup.payment.service.PaymentOrder;

import java.math.BigDecimal;

public interface SubmitBrickPaymentUseCase {

    PaymentOrder submit(String enrollmentId, String paymentMethodId, String token,
            Integer installments, String payerEmail, String financialInstitution,
            BigDecimal transactionAmount, String ipAddress,
            String identificationType, String identificationNumber);
}
