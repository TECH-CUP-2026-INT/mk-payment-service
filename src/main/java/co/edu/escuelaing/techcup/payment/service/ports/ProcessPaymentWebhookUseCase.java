package co.edu.escuelaing.techcup.payment.service.ports;

public interface ProcessPaymentWebhookUseCase {

    void process(String mpPaymentId);
}
