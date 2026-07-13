package co.edu.escuelaing.techcup.payment.exception;

public class PaymentOrderNotFoundException extends RuntimeException {

    public PaymentOrderNotFoundException(String message) {
        super(message);
    }
}
