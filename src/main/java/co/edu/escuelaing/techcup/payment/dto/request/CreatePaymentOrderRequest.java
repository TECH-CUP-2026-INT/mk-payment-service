package co.edu.escuelaing.techcup.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request to open a new PENDING payment order for a tournament enrollment.")
public record CreatePaymentOrderRequest(
        @Schema(description = "Opaque enrollment identifier provided by mk-tournament-service. One payment order per enrollmentId.",
                example = "enr-12345")
        @NotBlank(message = "enrollmentId es obligatorio") String enrollmentId,

        @Schema(description = "Identifier of the team paying the enrollment fee.", example = "team-001")
        @NotBlank(message = "teamId es obligatorio") String teamId,

        @Schema(description = "Identifier of the tournament being enrolled into.", example = "torneo-2026")
        @NotBlank(message = "tournamentId es obligatorio") String tournamentId,

        @Schema(description = "Amount to charge, in COP. Must be greater than zero and within the PSE limits returned by GET /payment-methods/limits.",
                example = "50000.00")
        @NotNull(message = "amount es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "amount debe ser mayor que cero")
        BigDecimal amount) {
}
