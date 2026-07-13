package co.edu.escuelaing.techcup.payment.service.ports;

public record PseTransactionResult(String mpPaymentId, String status, String externalResourceUrl) {
}
