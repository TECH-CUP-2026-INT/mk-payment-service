package co.edu.escuelaing.techcup.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePaymentOrderRequest(
        @NotBlank(message = "enrollmentId es obligatorio") String enrollmentId,
        @NotBlank(message = "teamId es obligatorio") String teamId,
        @NotBlank(message = "tournamentId es obligatorio") String tournamentId,
        @NotNull(message = "amount es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "amount debe ser mayor que cero")
        BigDecimal amount) {
}
