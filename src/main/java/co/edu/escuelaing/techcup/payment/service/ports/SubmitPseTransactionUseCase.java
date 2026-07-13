package co.edu.escuelaing.techcup.payment.service.ports;

import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;

public interface SubmitPseTransactionUseCase {

    PaymentOrder submit(String enrollmentId, Payer payer, String financialInstitution);
}
