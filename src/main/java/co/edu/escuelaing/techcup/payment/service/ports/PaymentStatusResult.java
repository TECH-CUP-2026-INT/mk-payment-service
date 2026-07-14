package co.edu.escuelaing.techcup.payment.service.ports;

/**
 * status carries Mercado Pago's own raw status string (e.g. "approved",
 * "rejected", "pending", "in_process") - callers decide how to interpret it.
 */
public record PaymentStatusResult(String mpPaymentId, String status) {
}
