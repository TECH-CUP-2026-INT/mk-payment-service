package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.entity.PaymentOrderEntity;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;

public final class PaymentOrderPersistenceMapper {

    private PaymentOrderPersistenceMapper() {
    }

    public static PaymentOrderEntity toEntity(PaymentOrder domain) {
        PaymentOrderEntity entity = new PaymentOrderEntity();
        entity.setPaymentOrderId(domain.getId());
        entity.setEnrollmentId(domain.getEnrollmentId());
        entity.setTeamId(domain.getTeamId());
        entity.setTournamentId(domain.getTournamentId());
        entity.setAmount(domain.getAmount());
        entity.setStatus(domain.getStatus().name());
        entity.setMpPaymentId(domain.getMpPaymentId());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setExternalResourceUrl(domain.getExternalResourceUrl());

        Payer payer = domain.getPayer();
        entity.setPayerEmail(payer != null ? payer.email() : null);
        entity.setPayerIdType(payer != null ? payer.identificationType() : null);
        entity.setPayerIdNumber(payer != null ? payer.identificationNumber() : null);
        entity.setPayerEntityType(payer != null ? payer.entityType() : null);

        entity.setExpiresAt(domain.getExpiresAt());
        entity.setVersion(domain.getVersion() != null ? domain.getVersion().intValue() : null);
        return entity;
    }

    public static PaymentOrder toDomain(PaymentOrderEntity entity) {
        Payer payer = entity.getPayerEmail() != null
                ? new Payer(entity.getPayerEmail(), entity.getPayerIdType(), entity.getPayerIdNumber(),
                        entity.getPayerEntityType())
                : null;

        return PaymentOrder.reconstruct(
                entity.getPaymentOrderId(),
                entity.getEnrollmentId(),
                entity.getTeamId(),
                entity.getTournamentId(),
                entity.getAmount(),
                PaymentOrderStatus.valueOf(entity.getStatus()),
                entity.getMpPaymentId(),
                entity.getIdempotencyKey(),
                entity.getExternalResourceUrl(),
                payer,
                entity.getExpiresAt(),
                entity.getVersion() != null ? entity.getVersion().longValue() : null);
    }
}
