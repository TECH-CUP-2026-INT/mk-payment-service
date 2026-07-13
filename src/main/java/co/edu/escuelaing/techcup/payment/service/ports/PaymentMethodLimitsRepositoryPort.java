package co.edu.escuelaing.techcup.payment.service.ports;

import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;

import java.util.Optional;

public interface PaymentMethodLimitsRepositoryPort {

    Optional<PaymentMethodLimits> findById(String paymentMethodId);

    PaymentMethodLimits save(PaymentMethodLimits limits);
}
