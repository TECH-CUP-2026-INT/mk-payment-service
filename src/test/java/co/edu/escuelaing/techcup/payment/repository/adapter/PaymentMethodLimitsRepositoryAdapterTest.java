package co.edu.escuelaing.techcup.payment.repository.adapter;

import co.edu.escuelaing.techcup.payment.entity.PaymentMethodLimitsEntity;
import co.edu.escuelaing.techcup.payment.mapper.PaymentMethodLimitsPersistenceMapper;
import co.edu.escuelaing.techcup.payment.repository.jpa.PaymentMethodLimitsJpaRepository;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentMethodLimitsRepositoryAdapterTest {

    private final PaymentMethodLimitsPersistenceMapper mapper =
            Mappers.getMapper(PaymentMethodLimitsPersistenceMapper.class);

    private PaymentMethodLimitsJpaRepository jpaRepository;
    private PaymentMethodLimitsRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpaRepository = mock(PaymentMethodLimitsJpaRepository.class);
        adapter = new PaymentMethodLimitsRepositoryAdapter(jpaRepository, mapper);
    }

    private static PaymentMethodLimits someLimits() {
        return new PaymentMethodLimits("pse", new BigDecimal("10000"), new BigDecimal("500000"),
                "active", LocalDateTime.now());
    }

    @Test
    @DisplayName("findById mapea el resultado a dominio cuando existe")
    void findsById() {
        when(jpaRepository.findById("pse"))
                .thenReturn(Optional.of(mapper.toEntity(someLimits())));

        Optional<PaymentMethodLimits> result = adapter.findById("pse");

        assertThat(result).isPresent();
        assertThat(result.get().paymentMethodId()).isEqualTo("pse");
    }

    @Test
    @DisplayName("findById devuelve empty cuando no existe")
    void findsByIdEmpty() {
        when(jpaRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThat(adapter.findById("unknown")).isEmpty();
    }

    @Test
    @DisplayName("save delega en el repositorio JPA y devuelve el dominio reconstruido")
    void savesLimits() {
        PaymentMethodLimits limits = someLimits();
        PaymentMethodLimitsEntity persisted = mapper.toEntity(limits);
        when(jpaRepository.save(any(PaymentMethodLimitsEntity.class))).thenReturn(persisted);

        PaymentMethodLimits saved = adapter.save(limits);

        assertThat(saved.paymentMethodId()).isEqualTo("pse");
        verify(jpaRepository).save(any(PaymentMethodLimitsEntity.class));
    }
}
