package co.edu.escuelaing.techcup.payment.exception;

public class PaymentMethodLimitsNotFoundException extends RuntimeException {

    public PaymentMethodLimitsNotFoundException(String message) {
        super(message);
    }
}
