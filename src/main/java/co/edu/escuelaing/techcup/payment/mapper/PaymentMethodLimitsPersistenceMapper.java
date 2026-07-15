package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.entity.PaymentMethodLimitsEntity;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMethodLimitsPersistenceMapper {

    PaymentMethodLimitsEntity toEntity(PaymentMethodLimits domain);

    PaymentMethodLimits toDomain(PaymentMethodLimitsEntity entity);
}
