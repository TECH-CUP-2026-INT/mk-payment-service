package co.edu.escuelaing.techcup.payment.repository.adapter;

import co.edu.escuelaing.techcup.payment.entity.PaymentOrderEntity;
import co.edu.escuelaing.techcup.payment.mapper.PaymentOrderPersistenceMapper;
import co.edu.escuelaing.techcup.payment.repository.jpa.PaymentOrderJpaRepository;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentOrderRepositoryAdapterTest {

    private PaymentOrderJpaRepository jpaRepository;
    private PaymentOrderRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpaRepository = mock(PaymentOrderJpaRepository.class);
        adapter = new PaymentOrderRepositoryAdapter(jpaRepository);
    }

    private static PaymentOrder anOrder() {
        return PaymentOrder.reconstruct(UUID.randomUUID(), "enr-1", "team-1", "tournament-1",
                new BigDecimal("50000"), PaymentOrderStatus.PENDING, null, UUID.randomUUID().toString(),
                null, null, LocalDateTime.now().plusMinutes(60), null);
    }

    @Test
    @DisplayName("save delega en el repositorio JPA y devuelve el dominio reconstruido")
    void savesOrder() {
        PaymentOrder order = anOrder();
        PaymentOrderEntity persistedEntity = PaymentOrderPersistenceMapper.toEntity(order);
        when(jpaRepository.save(any(PaymentOrderEntity.class))).thenReturn(persistedEntity);

        PaymentOrder saved = adapter.save(order);

        assertThat(saved.getEnrollmentId()).isEqualTo("enr-1");
        verify(jpaRepository).save(any(PaymentOrderEntity.class));
    }

    @Test
    @DisplayName("existsByEnrollmentId delega en el repositorio JPA")
    void checksExistsByEnrollmentId() {
        when(jpaRepository.existsByEnrollmentId("enr-1")).thenReturn(true);

        assertThat(adapter.existsByEnrollmentId("enr-1")).isTrue();
    }

    @Test
    @DisplayName("findByEnrollmentId mapea el resultado a dominio cuando existe")
    void findsByEnrollmentId() {
        PaymentOrder order = anOrder();
        when(jpaRepository.findByEnrollmentId("enr-1"))
                .thenReturn(Optional.of(PaymentOrderPersistenceMapper.toEntity(order)));

        Optional<PaymentOrder> result = adapter.findByEnrollmentId("enr-1");

        assertThat(result).isPresent();
        assertThat(result.get().getEnrollmentId()).isEqualTo("enr-1");
    }

    @Test
    @DisplayName("findByEnrollmentId devuelve empty cuando no existe")
    void findsByEnrollmentIdEmpty() {
        when(jpaRepository.findByEnrollmentId("enr-x")).thenReturn(Optional.empty());

        assertThat(adapter.findByEnrollmentId("enr-x")).isEmpty();
    }

    @Test
    @DisplayName("findByMpPaymentId mapea el resultado a dominio cuando existe")
    void findsByMpPaymentId() {
        PaymentOrder order = anOrder();
        when(jpaRepository.findByMpPaymentId("mp-1"))
                .thenReturn(Optional.of(PaymentOrderPersistenceMapper.toEntity(order)));

        Optional<PaymentOrder> result = adapter.findByMpPaymentId("mp-1");

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("findByStatusInAndExpiresAtBefore traduce los enums a nombres y mapea la lista resultante")
    void findsByStatusInAndExpiresAtBefore() {
        LocalDateTime before = LocalDateTime.now();
        PaymentOrderEntity entity = PaymentOrderPersistenceMapper.toEntity(anOrder());
        when(jpaRepository.findByStatusInAndExpiresAtBefore(
                List.of("PENDING", "AWAITING_BANK_CONFIRMATION"), before))
                .thenReturn(List.of(entity));

        List<PaymentOrder> result = adapter.findByStatusInAndExpiresAtBefore(
                List.of(PaymentOrderStatus.PENDING, PaymentOrderStatus.AWAITING_BANK_CONFIRMATION), before);

        assertThat(result).hasSize(1);
        verify(jpaRepository).findByStatusInAndExpiresAtBefore(
                eq(List.of("PENDING", "AWAITING_BANK_CONFIRMATION")), eq(before));
    }
}
