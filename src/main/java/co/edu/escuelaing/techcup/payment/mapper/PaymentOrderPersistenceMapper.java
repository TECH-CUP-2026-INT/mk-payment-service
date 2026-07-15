package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.entity.PaymentOrderEntity;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PaymentOrderPersistenceMapper {

    @Mapping(source = "id", target = "paymentOrderId")
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    @Mapping(source = "payer.email", target = "payerEmail")
    @Mapping(source = "payer.identificationType", target = "payerIdType")
    @Mapping(source = "payer.identificationNumber", target = "payerIdNumber")
    @Mapping(source = "payer.entityType", target = "payerEntityType")
    @Mapping(source = "version", target = "version", qualifiedByName = "longToInt")
    PaymentOrderEntity toEntity(PaymentOrder domain);

    default PaymentOrder toDomain(PaymentOrderEntity entity) {
        if (entity == null) {
            return null;
        }
        return PaymentOrder.reconstruct(
                entity.getPaymentOrderId(),
                entity.getEnrollmentId(),
                entity.getTeamId(),
                entity.getTournamentId(),
                entity.getAmount(),
                stringToStatus(entity.getStatus()),
                entity.getMpPaymentId(),
                entity.getIdempotencyKey(),
                entity.getExternalResourceUrl(),
                entityToPayer(entity),
                entity.getExpiresAt(),
                intToLong(entity.getVersion()));
    }

    @Named("statusToString")
    default String statusToString(PaymentOrderStatus status) {
        return status.name();
    }

    @Named("stringToStatus")
    default PaymentOrderStatus stringToStatus(String status) {
        return PaymentOrderStatus.valueOf(status);
    }

    @Named("longToInt")
    default Integer longToInt(Long version) {
        return version != null ? version.intValue() : null;
    }

    @Named("intToLong")
    default Long intToLong(Integer version) {
        return version != null ? version.longValue() : null;
    }

    @Named("entityToPayer")
    default Payer entityToPayer(PaymentOrderEntity entity) {
        if (entity.getPayerEmail() == null) return null;
        return new Payer(entity.getPayerEmail(), entity.getPayerIdType(),
                entity.getPayerIdNumber(), entity.getPayerEntityType());
    }
}
