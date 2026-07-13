package co.edu.escuelaing.techcup.payment.dto.response;

/**
 * The "status" field name is a consumer contract, not a free choice:
 * mk-tournament-service's PaymentServiceClientAdapter deserializes this
 * response with `record PaymentOrderResponse(PaymentOrderStatus status) {}`.
 * Renaming this field would deserialize silently to null there (that
 * adapter never throws), not to a visible error.
 */
public record PaymentOrderStatusResponse(String status) {
}
