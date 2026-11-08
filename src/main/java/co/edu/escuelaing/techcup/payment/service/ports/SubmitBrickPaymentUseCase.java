package co.edu.escuelaing.techcup.payment.service.ports;

import co.edu.escuelaing.techcup.payment.service.PaymentOrder;

public interface SubmitBrickPaymentUseCase {

    PaymentOrder submit(String enrollmentId, String paymentMethodId, String token,
            Integer installments, String payerEmail, String financialInstitution,
            String identificationType, String identificationNumber, String ipAddress);
}
