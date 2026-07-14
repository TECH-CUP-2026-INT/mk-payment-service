package co.edu.escuelaing.techcup.payment.service.ports;

import co.edu.escuelaing.techcup.payment.service.PaymentOrder;

import java.math.BigDecimal;

public interface CreatePaymentOrderUseCase {

    PaymentOrder create(String enrollmentId, String teamId, String tournamentId, BigDecimal amount);
}
