package co.edu.escuelaing.techcup.payment.exception;

public class PaymentOrderNotAwaitingBankConfirmationException extends RuntimeException {

    public PaymentOrderNotAwaitingBankConfirmationException(String message) {
        super(message);
    }
}
