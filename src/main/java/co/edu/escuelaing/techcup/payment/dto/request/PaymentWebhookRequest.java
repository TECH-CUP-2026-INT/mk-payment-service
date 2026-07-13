package co.edu.escuelaing.techcup.payment.dto.request;

/**
 * Shape of Mercado Pago's own webhook payload. Any status-like field Mercado
 * Pago might include here is deliberately not modeled: the real status is
 * always fetched via PaymentGatewayPort.getPaymentStatus(paymentId), never
 * trusted from this body.
 */
public record PaymentWebhookRequest(String action, String type, WebhookData data) {

    public record WebhookData(String id) {
    }
}
