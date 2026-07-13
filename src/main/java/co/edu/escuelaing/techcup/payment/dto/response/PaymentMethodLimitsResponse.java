package co.edu.escuelaing.techcup.payment.dto.response;

import java.math.BigDecimal;

public record PaymentMethodLimitsResponse(boolean valid, BigDecimal minAllowedAmount, BigDecimal maxAllowedAmount) {
}
