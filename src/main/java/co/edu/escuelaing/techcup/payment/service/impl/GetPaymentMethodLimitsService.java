package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.exception.PaymentMethodLimitsNotFoundException;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.ports.GetPaymentMethodLimitsUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodLimitsRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class GetPaymentMethodLimitsService implements GetPaymentMethodLimitsUseCase {

    private static final String PSE_PAYMENT_METHOD_ID = "pse";

    private final PaymentMethodLimitsRepositoryPort paymentMethodLimitsRepository;

    public GetPaymentMethodLimitsService(PaymentMethodLimitsRepositoryPort paymentMethodLimitsRepository) {
        this.paymentMethodLimitsRepository = paymentMethodLimitsRepository;
    }

    @Override
    public PaymentMethodLimits getPseLimits() {
        return paymentMethodLimitsRepository.findById(PSE_PAYMENT_METHOD_ID)
                .orElseThrow(() -> new PaymentMethodLimitsNotFoundException(
                        "No hay límites cacheados para PSE, aún no se ha ejecutado la sincronización"));
    }
}
