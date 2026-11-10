package co.edu.escuelaing.techcup.payment.service.ports;

import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;

public interface SubmitBrickPaymentUseCase {

    /**
     * @param payer     full payer data, required only for PSE (null for card payments).
     *                  Mercado Pago requires the complete payer since 31/12/2024.
     * @param cardPayer payer data for card payments (null for PSE). All fields are
     *                  optional; only the ones provided are sent to Mercado Pago.
     */
    PaymentOrder submit(String enrollmentId, String paymentMethodId, String token,
            Integer installments, String payerEmail, String financialInstitution,
            String identificationType, String identificationNumber, Payer payer,
            CardPayer cardPayer, String ipAddress);
}
