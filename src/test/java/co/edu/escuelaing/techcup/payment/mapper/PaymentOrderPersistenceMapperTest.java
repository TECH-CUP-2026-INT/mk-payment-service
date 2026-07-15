package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.entity.PaymentOrderEntity;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOrderPersistenceMapperTest {

    private final PaymentOrderPersistenceMapper mapper = Mappers.getMapper(PaymentOrderPersistenceMapper.class);

    @Test
    @DisplayName("toEntity copia todos los campos del dominio, incluyendo el pagador aplanado")
    void mapsDomainToEntityWithPayer() {
        UUID id = UUID.randomUUID();
        Payer payer = new Payer("payer@test.com", "CC", "123456", "individual");
        PaymentOrder order = PaymentOrder.reconstruct(id, "enr-1", "team-1", "tournament-1",
                new BigDecimal("50000"), PaymentOrderStatus.AWAITING_BANK_CONFIRMATION, "mp-1",
                "idem-1", "https://mp.test/ticket", payer, LocalDateTime.now().plusMinutes(30), 3L);

        PaymentOrderEntity entity = mapper.toEntity(order);

        assertThat(entity.getPaymentOrderId()).isEqualTo(id);
        assertThat(entity.getEnrollmentId()).isEqualTo("enr-1");
        assertThat(entity.getTeamId()).isEqualTo("team-1");
        assertThat(entity.getTournamentId()).isEqualTo("tournament-1");
        assertThat(entity.getAmount()).isEqualByComparingTo("50000");
        assertThat(entity.getStatus()).isEqualTo("AWAITING_BANK_CONFIRMATION");
        assertThat(entity.getMpPaymentId()).isEqualTo("mp-1");
        assertThat(entity.getIdempotencyKey()).isEqualTo("idem-1");
        assertThat(entity.getExternalResourceUrl()).isEqualTo("https://mp.test/ticket");
        assertThat(entity.getPayerEmail()).isEqualTo("payer@test.com");
        assertThat(entity.getPayerIdType()).isEqualTo("CC");
        assertThat(entity.getPayerIdNumber()).isEqualTo("123456");
        assertThat(entity.getPayerEntityType()).isEqualTo("individual");
        assertThat(entity.getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("toEntity deja los campos del pagador y la versión en null cuando la orden aún no los tiene")
    void mapsDomainToEntityWithoutPayer() {
        PaymentOrder order = PaymentOrder.reconstruct(UUID.randomUUID(), "enr-2", "team-2", "tournament-2",
                new BigDecimal("30000"), PaymentOrderStatus.PENDING, null, "idem-2", null, null,
                LocalDateTime.now().plusMinutes(60), null);

        PaymentOrderEntity entity = mapper.toEntity(order);

        assertThat(entity.getPayerEmail()).isNull();
        assertThat(entity.getPayerIdType()).isNull();
        assertThat(entity.getPayerIdNumber()).isNull();
        assertThat(entity.getPayerEntityType()).isNull();
        assertThat(entity.getVersion()).isNull();
    }

    @Test
    @DisplayName("toDomain reconstruye el pagador a partir de los campos aplanados de la entidad")
    void mapsEntityToDomainWithPayer() {
        UUID id = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        PaymentOrderEntity entity = new PaymentOrderEntity();
        entity.setPaymentOrderId(id);
        entity.setEnrollmentId("enr-1");
        entity.setTeamId("team-1");
        entity.setTournamentId("tournament-1");
        entity.setAmount(new BigDecimal("50000"));
        entity.setStatus("APPROVED");
        entity.setMpPaymentId("mp-1");
        entity.setIdempotencyKey("idem-1");
        entity.setExternalResourceUrl("https://mp.test/ticket");
        entity.setPayerEmail("payer@test.com");
        entity.setPayerIdType("CC");
        entity.setPayerIdNumber("123456");
        entity.setPayerEntityType("individual");
        entity.setExpiresAt(expiresAt);
        entity.setVersion(5);

        PaymentOrder order = mapper.toDomain(entity);

        assertThat(order.getId()).isEqualTo(id);
        assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.APPROVED);
        assertThat(order.getPayer()).isEqualTo(new Payer("payer@test.com", "CC", "123456", "individual"));
        assertThat(order.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(order.getVersion()).isEqualTo(5L);
    }

    @Test
    @DisplayName("toDomain deja el pagador y la versión en null cuando la entidad no los tiene")
    void mapsEntityToDomainWithoutPayer() {
        PaymentOrderEntity entity = new PaymentOrderEntity();
        entity.setPaymentOrderId(UUID.randomUUID());
        entity.setEnrollmentId("enr-2");
        entity.setTeamId("team-2");
        entity.setTournamentId("tournament-2");
        entity.setAmount(new BigDecimal("30000"));
        entity.setStatus("PENDING");
        entity.setIdempotencyKey("idem-2");
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(60));

        PaymentOrder order = mapper.toDomain(entity);

        assertThat(order.getPayer()).isNull();
        assertThat(order.getVersion()).isNull();
    }
}
