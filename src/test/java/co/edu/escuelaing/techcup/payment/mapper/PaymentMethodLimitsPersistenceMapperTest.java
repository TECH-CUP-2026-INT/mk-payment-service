package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.entity.PaymentMethodLimitsEntity;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodLimitsPersistenceMapperTest {

    @Test
    @DisplayName("toEntity copia todos los campos del dominio")
    void mapsDomainToEntity() {
        LocalDateTime syncedAt = LocalDateTime.now();
        PaymentMethodLimits domain = new PaymentMethodLimits("pse", new BigDecimal("10000"),
                new BigDecimal("500000"), "active", syncedAt);

        PaymentMethodLimitsEntity entity = PaymentMethodLimitsPersistenceMapper.toEntity(domain);

        assertThat(entity.getPaymentMethodId()).isEqualTo("pse");
        assertThat(entity.getMinAllowedAmount()).isEqualByComparingTo("10000");
        assertThat(entity.getMaxAllowedAmount()).isEqualByComparingTo("500000");
        assertThat(entity.getStatus()).isEqualTo("active");
        assertThat(entity.getLastSyncedAt()).isEqualTo(syncedAt);
    }

    @Test
    @DisplayName("toDomain reconstruye el record a partir de la entidad")
    void mapsEntityToDomain() {
        LocalDateTime syncedAt = LocalDateTime.now();
        PaymentMethodLimitsEntity entity = new PaymentMethodLimitsEntity();
        entity.setPaymentMethodId("pse");
        entity.setMinAllowedAmount(new BigDecimal("10000"));
        entity.setMaxAllowedAmount(new BigDecimal("500000"));
        entity.setStatus("active");
        entity.setLastSyncedAt(syncedAt);

        PaymentMethodLimits domain = PaymentMethodLimitsPersistenceMapper.toDomain(entity);

        assertThat(domain).isEqualTo(new PaymentMethodLimits("pse", new BigDecimal("10000"),
                new BigDecimal("500000"), "active", syncedAt));
    }
}
