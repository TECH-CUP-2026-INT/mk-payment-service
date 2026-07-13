package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotFoundException;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.ports.GetPaymentOrderStatusUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class GetPaymentOrderStatusService implements GetPaymentOrderStatusUseCase {

    private final PaymentOrderRepositoryPort paymentOrderRepository;

    public GetPaymentOrderStatusService(PaymentOrderRepositoryPort paymentOrderRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
    }

    @Override
    public PaymentOrder getByEnrollmentId(String enrollmentId) {
        return paymentOrderRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new PaymentOrderNotFoundException(
                        "No existe una orden de pago para enrollmentId " + enrollmentId));
    }
}
