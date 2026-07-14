package co.edu.escuelaing.techcup.payment.exception;

public class PaymentOrderNotPendingException extends RuntimeException {

    public PaymentOrderNotPendingException(String message) {
        super(message);
    }
}
