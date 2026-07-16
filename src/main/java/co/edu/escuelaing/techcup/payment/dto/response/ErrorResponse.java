package co.edu.escuelaing.techcup.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic error body returned for domain-level failures (4xx/5xx).")
public record ErrorResponse(
        @Schema(description = "Human-readable explanation of what went wrong.",
                example = "No existe una orden de pago para enrollmentId enr-12345")
        String message) {
}
