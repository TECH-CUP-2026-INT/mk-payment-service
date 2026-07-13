package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.dto.response.PaymentMethodLimitsResponse;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;

import java.math.BigDecimal;

public final class PaymentMethodLimitsRestMapper {

    private PaymentMethodLimitsRestMapper() {
    }

    public static PaymentMethodLimitsResponse toLimitsResponse(PaymentMethodLimits limits, BigDecimal amount) {
        return new PaymentMethodLimitsResponse(limits.isWithinRange(amount), limits.minAllowedAmount(), limits.maxAllowedAmount());
    }
}
