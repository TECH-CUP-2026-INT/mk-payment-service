package co.edu.escuelaing.techcup.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Newly created payment order, in PENDING status.")
public record CreatePaymentOrderResponse(
        @Schema(description = "Generated identifier of the payment order.",
                example = "b3f1c2a0-1234-4a5b-9c0d-abc123456789")
        UUID paymentOrderId,

        @Schema(description = "Payment order status right after creation.", example = "PENDING")
        String status,

        @Schema(description = "Instant after which this order can no longer be paid.",
                example = "2026-07-13T21:00:00")
        LocalDateTime expiresAt) {
}
