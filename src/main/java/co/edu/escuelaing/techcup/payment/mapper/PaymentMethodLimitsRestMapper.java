package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.dto.response.PaymentMethodLimitsResponse;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface PaymentMethodLimitsRestMapper {

    @Mapping(target = "valid", expression = "java(limits.isWithinRange(amount))")
    PaymentMethodLimitsResponse toLimitsResponse(PaymentMethodLimits limits, BigDecimal amount);
}
