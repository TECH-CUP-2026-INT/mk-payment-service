package co.edu.escuelaing.techcup.payment.service.ports;

import co.edu.escuelaing.techcup.payment.service.PaymentOrder;

public interface GetPaymentOrderStatusUseCase {

    PaymentOrder getByEnrollmentId(String enrollmentId);
}
