package co.edu.escuelaing.techcup.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayerTest {

    @ParameterizedTest
    @ValueSource(strings = {"individual", "association"})
    @DisplayName("acepta entityType 'individual' o 'association'")
    void acceptsValidEntityType(String entityType) {
        Payer payer = new Payer("payer@test.com", "CC", "123456789", entityType);

        assertThat(payer.entityType()).isEqualTo(entityType);
    }

    @Test
    @DisplayName("rechaza entityType nulo")
    void rejectsNullEntityType() {
        assertThatThrownBy(() -> new Payer("payer@test.com", "CC", "123456789", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityType");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "company", "INDIVIDUAL"})
    @DisplayName("rechaza entityType vacío o fuera del catálogo permitido")
    void rejectsInvalidEntityType(String entityType) {
        assertThatThrownBy(() -> new Payer("payer@test.com", "CC", "123456789", entityType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityType");
    }
}
