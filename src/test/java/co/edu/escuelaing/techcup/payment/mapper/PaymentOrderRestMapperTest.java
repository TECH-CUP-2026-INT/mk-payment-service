package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.dto.response.PaymentOrderStatusResponse;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOrderRestMapperTest {

    @Test
    @DisplayName("EXPIRED se mapea a REJECTED en la respuesta pública, el dominio conserva EXPIRED")
    void mapsExpiredToRejected() {
        PaymentOrder expiredOrder = PaymentOrder.reconstruct(UUID.randomUUID(), "enr-1", "team-1", "tournament-1",
                new BigDecimal("50000"), PaymentOrderStatus.EXPIRED, null, UUID.randomUUID().toString(),
                null, null, LocalDateTime.now().minusMinutes(1), 0L);

        PaymentOrderStatusResponse response = PaymentOrderRestMapper.toStatusResponse(expiredOrder);

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(expiredOrder.getStatus()).isEqualTo(PaymentOrderStatus.EXPIRED);
    }

    @Test
    @DisplayName("los demás estados se serializan tal cual")
    void mapsOtherStatusesAsIs() {
        PaymentOrder approvedOrder = PaymentOrder.reconstruct(UUID.randomUUID(), "enr-1", "team-1", "tournament-1",
                new BigDecimal("50000"), PaymentOrderStatus.APPROVED, "mp-1", UUID.randomUUID().toString(),
                null, null, LocalDateTime.now().plusMinutes(1), 0L);

        assertThat(PaymentOrderRestMapper.toStatusResponse(approvedOrder).status()).isEqualTo("APPROVED");
    }
}
