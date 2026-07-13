package co.edu.escuelaing.techcup.payment.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreatePaymentOrderResponse(UUID paymentOrderId, String status, LocalDateTime expiresAt) {
}
