package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.exception.AmountOutOfRangeException;
import co.edu.escuelaing.techcup.payment.exception.DuplicateEnrollmentOrderException;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodId;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.CreatePaymentOrderUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodLimitsRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.PreferenceResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CreatePaymentOrderService implements CreatePaymentOrderUseCase {

    private final PaymentOrderRepositoryPort paymentOrderRepository;
    private final PaymentMethodLimitsRepositoryPort paymentMethodLimitsRepository;
    private final PaymentGatewayPort paymentGateway;

    public CreatePaymentOrderService(PaymentOrderRepositoryPort paymentOrderRepository,
            PaymentMethodLimitsRepositoryPort paymentMethodLimitsRepository,
            PaymentGatewayPort paymentGateway) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentMethodLimitsRepository = paymentMethodLimitsRepository;
        this.paymentGateway = paymentGateway;
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

        String idempotencyKey = java.util.UUID.randomUUID().toString();
        PreferenceResult preference = paymentGateway.createPreference(
                idempotencyKey,
                "Inscripcion torneo " + tournamentId,
                amount,
                null,
                null);

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .paymentOrderId(java.util.UUID.randomUUID())
                .enrollmentId(enrollmentId)
                .teamId(teamId)
                .tournamentId(tournamentId)
                .amount(amount)
                .status(PaymentOrderStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .preferenceId(preference.preferenceId())
                .expiresAt(java.time.LocalDateTime.now(java.time.ZoneId.systemDefault()).plusMinutes(60))
                .build();
        return paymentOrderRepository.save(paymentOrder);
    }
}
