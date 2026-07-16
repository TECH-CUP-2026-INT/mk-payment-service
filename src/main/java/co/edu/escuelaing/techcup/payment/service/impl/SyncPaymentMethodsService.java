package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.service.PaymentMethodId;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodInfo;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodLimitsRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.SyncPaymentMethodsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class SyncPaymentMethodsService implements SyncPaymentMethodsUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyncPaymentMethodsService.class);

    private final PaymentGatewayPort paymentGateway;
    private final PaymentMethodLimitsRepositoryPort paymentMethodLimitsRepository;

    public SyncPaymentMethodsService(PaymentGatewayPort paymentGateway,
            PaymentMethodLimitsRepositoryPort paymentMethodLimitsRepository) {
        this.paymentGateway = paymentGateway;
        this.paymentMethodLimitsRepository = paymentMethodLimitsRepository;
    }

    @Override
    public void sync() {
        List<PaymentMethodInfo> methods = paymentGateway.getAvailablePaymentMethods();
        methods.stream()
                .filter(method -> PaymentMethodId.PSE.equals(method.id()))
                .findFirst()
                .ifPresentOrElse(this::upsertLimits,
                        () -> log.warn("Mercado Pago no devolvió información del método de pago 'pse' en la sincronización"));
    }

    private void upsertLimits(PaymentMethodInfo pse) {
        paymentMethodLimitsRepository.save(new PaymentMethodLimits(
                pse.id(), pse.minAllowedAmount(), pse.maxAllowedAmount(), pse.status(), LocalDateTime.now(ZoneId.systemDefault())));
    }
}
