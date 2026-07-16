package co.edu.escuelaing.techcup.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The "status" field name is a consumer contract, not a free choice:
 * mk-tournament-service's PaymentServiceClientAdapter deserializes this
 * response with `record PaymentOrderResponse(PaymentOrderStatus status) {}`.
 * Renaming this field would deserialize silently to null there (that
 * adapter never throws), not to a visible error.
 */
@Schema(description = "Current status of a payment order. EXPIRED is never returned here - it is reported as REJECTED.")
public record PaymentOrderStatusResponse(
        @Schema(description = "One of PENDING, AWAITING_BANK_CONFIRMATION, APPROVED, REJECTED.",
                example = "APPROVED")
        String status) {
}
