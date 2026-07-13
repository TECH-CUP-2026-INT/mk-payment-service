package co.edu.escuelaing.techcup.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentMethodLimits(
        String paymentMethodId,
        BigDecimal minAllowedAmount,
        BigDecimal maxAllowedAmount,
        String status,
        LocalDateTime lastSyncedAt) {

    public boolean isWithinRange(BigDecimal amount) {
        return amount.compareTo(minAllowedAmount) >= 0 && amount.compareTo(maxAllowedAmount) <= 0;
    }
}
