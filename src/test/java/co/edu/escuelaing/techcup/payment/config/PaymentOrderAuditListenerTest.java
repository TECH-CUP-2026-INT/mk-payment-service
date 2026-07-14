package co.edu.escuelaing.techcup.payment.config;

import co.edu.escuelaing.techcup.payment.entity.PaymentOrderEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOrderAuditListenerTest {

    private final PaymentOrderAuditListener listener = new PaymentOrderAuditListener();

    @Test
    @DisplayName("onPrePersist fija createdAt y updatedAt cuando la entidad es nueva")
    void setsCreatedAndUpdatedAtOnNewEntity() {
        PaymentOrderEntity entity = new PaymentOrderEntity();

        listener.onPrePersist(entity);

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isEqualTo(entity.getUpdatedAt());
    }

    @Test
    @DisplayName("onPrePersist no sobrescribe createdAt si ya tiene valor")
    void preservesExistingCreatedAt() {
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(1);
        PaymentOrderEntity entity = new PaymentOrderEntity();
        entity.setCreatedAt(originalCreatedAt);

        listener.onPrePersist(entity);

        assertThat(entity.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onPreUpdate refresca solo updatedAt, sin tocar createdAt")
    void refreshesUpdatedAtOnly() {
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(1);
        PaymentOrderEntity entity = new PaymentOrderEntity();
        entity.setCreatedAt(originalCreatedAt);

        listener.onPreUpdate(entity);

        assertThat(entity.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(entity.getUpdatedAt()).isNotNull();
    }
}
