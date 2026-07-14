package co.edu.escuelaing.techcup.payment.service.ports;

import java.math.BigDecimal;

public record PaymentMethodInfo(String id, String status, BigDecimal minAllowedAmount, BigDecimal maxAllowedAmount) {
}
