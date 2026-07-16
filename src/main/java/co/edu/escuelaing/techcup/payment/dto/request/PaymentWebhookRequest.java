package co.edu.escuelaing.techcup.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shape of Mercado Pago's own webhook payload. Any status-like field Mercado
 * Pago might include here is deliberately not modeled: the real status is
 * always fetched via PaymentGatewayPort.getPaymentStatus(paymentId), never
 * trusted from this body.
 */
@Schema(description = """
        Notification sent by Mercado Pago whenever a payment's status changes. Only \
        data.id is used; the actual status is always re-fetched from Mercado Pago's \
        API rather than trusted from this payload.""")
public record PaymentWebhookRequest(
        @Schema(description = "Mercado Pago event type.", example = "payment.updated") String action,
        @Schema(description = "Type of resource the notification refers to.", example = "payment") String type,
        WebhookData data) {

    @Schema(description = "Reference to the Mercado Pago resource that changed.")
    public record WebhookData(
            @Schema(description = "Mercado Pago's own payment id.", example = "1234567890") String id) {
    }
}
