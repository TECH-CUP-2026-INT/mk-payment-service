package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.exception.AmountOutOfRangeException;
import co.edu.escuelaing.techcup.payment.exception.DuplicateEnrollmentOrderException;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodId;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.ports.CreatePaymentOrderUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodLimitsRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CreatePaymentOrderService implements CreatePaymentOrderUseCase {

    private final PaymentOrderRepositoryPort paymentOrderRepository;
    private final PaymentMethodLimitsRepositoryPort paymentMethodLimitsRepository;

    public CreatePaymentOrderService(PaymentOrderRepositoryPort paymentOrderRepository,
            PaymentMethodLimitsRepositoryPort paymentMethodLimitsRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentMethodLimitsRepository = paymentMethodLimitsRepository;
    }

    @Override
    public PaymentOrder create(String enrollmentId, String teamId, String tournamentId, BigDecimal amount) {
        PaymentMethodLimits limits = paymentMethodLimitsRepository.findById(PaymentMethodId.PSE)
                .orElseThrow(() -> new AmountOutOfRangeException(
                        "No hay límites de monto cacheados para PSE, no se puede validar el monto"));
        if (!limits.isWithinRange(amount)) {
            throw new AmountOutOfRangeException(
                    "El monto %s está fuera del rango permitido [%s, %s]"
                            .formatted(amount, limits.minAllowedAmount(), limits.maxAllowedAmount()));
        }
        if (paymentOrderRepository.existsByEnrollmentId(enrollmentId)) {
            throw new DuplicateEnrollmentOrderException(
                    "Ya existe una orden de pago para enrollmentId " + enrollmentId);
        }
        PaymentOrder paymentOrder = PaymentOrder.create(enrollmentId, teamId, tournamentId, amount);
        return paymentOrderRepository.save(paymentOrder);
    }
}
