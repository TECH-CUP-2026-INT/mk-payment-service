package co.edu.escuelaing.techcup.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "PSE amount limits currently configured in Mercado Pago, and whether the queried amount falls within them.")
public record PaymentMethodLimitsResponse(
        @Schema(description = "True when the queried amount is within [minAllowedAmount, maxAllowedAmount].",
                example = "true")
        boolean valid,

        @Schema(description = "Minimum amount Mercado Pago allows for a PSE payment.", example = "10000.00")
        BigDecimal minAllowedAmount,

        @Schema(description = "Maximum amount Mercado Pago allows for a PSE payment.", example = "500000.00")
        BigDecimal maxAllowedAmount) {
}
