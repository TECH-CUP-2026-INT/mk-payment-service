package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.dto.response.PaymentOrderStatusResponse;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOrderRestMapperTest {

    private final PaymentOrderRestMapper mapper = Mappers.getMapper(PaymentOrderRestMapper.class);

    @Test
    @DisplayName("EXPIRED se mapea a REJECTED en la respuesta pública, el dominio conserva EXPIRED")
    void mapsExpiredToRejected() {
        PaymentOrder expiredOrder = PaymentOrder.builder()
                .paymentOrderId(UUID.randomUUID())
                .enrollmentId("enr-1")
                .teamId("team-1")
                .tournamentId("tournament-1")
                .amount(new BigDecimal("50000"))
                .status(PaymentOrderStatus.EXPIRED)
                .idempotencyKey(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .version(0L)
                .build();

        PaymentOrderStatusResponse response = mapper.toStatusResponse(expiredOrder);

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(expiredOrder.getStatus()).isEqualTo(PaymentOrderStatus.EXPIRED);
    }

    @Test
    @DisplayName("los demás estados se serializan tal cual")
    void mapsOtherStatusesAsIs() {
        PaymentOrder approvedOrder = PaymentOrder.builder()
                .paymentOrderId(UUID.randomUUID())
                .enrollmentId("enr-1")
                .teamId("team-1")
                .tournamentId("tournament-1")
                .amount(new BigDecimal("50000"))
                .status(PaymentOrderStatus.APPROVED)
                .mpPaymentId("mp-1")
                .idempotencyKey(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusMinutes(1))
                .version(0L)
                .build();

        assertThat(mapper.toStatusResponse(approvedOrder).status()).isEqualTo("APPROVED");
    }
}
