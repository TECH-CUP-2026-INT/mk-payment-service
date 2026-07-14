package co.edu.escuelaing.techcup.payment.exception;

public class PaymentOrderExpiredException extends RuntimeException {

    public PaymentOrderExpiredException(String message) {
        super(message);
    }
}
