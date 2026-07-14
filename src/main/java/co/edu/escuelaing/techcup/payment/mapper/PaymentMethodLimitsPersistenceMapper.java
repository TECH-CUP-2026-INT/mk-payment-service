package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.entity.PaymentMethodLimitsEntity;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;

public final class PaymentMethodLimitsPersistenceMapper {

    private PaymentMethodLimitsPersistenceMapper() {
    }

    public static PaymentMethodLimitsEntity toEntity(PaymentMethodLimits domain) {
        PaymentMethodLimitsEntity entity = new PaymentMethodLimitsEntity();
        entity.setPaymentMethodId(domain.paymentMethodId());
        entity.setMinAllowedAmount(domain.minAllowedAmount());
        entity.setMaxAllowedAmount(domain.maxAllowedAmount());
        entity.setStatus(domain.status());
        entity.setLastSyncedAt(domain.lastSyncedAt());
        return entity;
    }

    public static PaymentMethodLimits toDomain(PaymentMethodLimitsEntity entity) {
        return new PaymentMethodLimits(
                entity.getPaymentMethodId(),
                entity.getMinAllowedAmount(),
                entity.getMaxAllowedAmount(),
                entity.getStatus(),
                entity.getLastSyncedAt());
    }
}
