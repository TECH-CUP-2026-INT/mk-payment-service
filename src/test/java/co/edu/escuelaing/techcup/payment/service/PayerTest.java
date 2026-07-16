package co.edu.escuelaing.techcup.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayerTest {

    private static Payer validPayer(String entityType) {
        return new Payer("payer@test.com", "CC", "123456789", entityType,
                "Juan", "Pérez", "11001", "Calle 1", "123", "Centro", "Bogotá", "601", "12345");
    }

    @ParameterizedTest
    @ValueSource(strings = {"individual", "association"})
    @DisplayName("acepta entityType 'individual' o 'association'")
    void acceptsValidEntityType(String entityType) {
        Payer payer = validPayer(entityType);

        assertThat(payer.entityType()).isEqualTo(entityType);
    }

    @Test
    @DisplayName("rechaza entityType nulo")
    void rejectsNullEntityType() {
        assertThatThrownBy(() -> validPayer(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityType");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "company", "INDIVIDUAL"})
    @DisplayName("rechaza entityType vacío o fuera del catálogo permitido")
    void rejectsInvalidEntityType(String entityType) {
        assertThatThrownBy(() -> validPayer(entityType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityType");
    }

    @Nested
    @DisplayName("campos obligatorios adicionales del pagador (requeridos por Mercado Pago para PSE)")
    class RequiredPayerDetails {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("rechaza firstName vacío o nulo")
        void rejectsBlankFirstName(String firstName) {
            assertRejected(v -> new Payer(v.email(), v.identificationType(), v.identificationNumber(), v.entityType(),
                    firstName, v.lastName(), v.addressZipCode(), v.addressStreetName(), v.addressStreetNumber(),
                    v.addressNeighborhood(), v.addressCity(), v.phoneAreaCode(), v.phoneNumber()), "nombre");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("rechaza lastName vacío o nulo")
        void rejectsBlankLastName(String lastName) {
            assertRejected(v -> new Payer(v.email(), v.identificationType(), v.identificationNumber(), v.entityType(),
                    v.firstName(), lastName, v.addressZipCode(), v.addressStreetName(), v.addressStreetNumber(),
                    v.addressNeighborhood(), v.addressCity(), v.phoneAreaCode(), v.phoneNumber()), "apellido");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("rechaza addressZipCode vacío o nulo")
        void rejectsBlankAddressZipCode(String zipCode) {
            assertRejected(v -> new Payer(v.email(), v.identificationType(), v.identificationNumber(), v.entityType(),
                    v.firstName(), v.lastName(), zipCode, v.addressStreetName(), v.addressStreetNumber(),
                    v.addressNeighborhood(), v.addressCity(), v.phoneAreaCode(), v.phoneNumber()), "código postal");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("rechaza addressStreetName vacío o nulo")
        void rejectsBlankAddressStreetName(String streetName) {
            assertRejected(v -> new Payer(v.email(), v.identificationType(), v.identificationNumber(), v.entityType(),
                    v.firstName(), v.lastName(), v.addressZipCode(), streetName, v.addressStreetNumber(),
                    v.addressNeighborhood(), v.addressCity(), v.phoneAreaCode(), v.phoneNumber()), "calle");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("rechaza addressStreetNumber vacío o nulo")
        void rejectsBlankAddressStreetNumber(String streetNumber) {
            assertRejected(v -> new Payer(v.email(), v.identificationType(), v.identificationNumber(), v.entityType(),
                    v.firstName(), v.lastName(), v.addressZipCode(), v.addressStreetName(), streetNumber,
                    v.addressNeighborhood(), v.addressCity(), v.phoneAreaCode(), v.phoneNumber()), "número de la calle");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("rechaza addressNeighborhood vacío o nulo")
        void rejectsBlankAddressNeighborhood(String neighborhood) {
            assertRejected(v -> new Payer(v.email(), v.identificationType(), v.identificationNumber(), v.entityType(),
                    v.firstName(), v.lastName(), v.addressZipCode(), v.addressStreetName(), v.addressStreetNumber(),
                    neighborhood, v.addressCity(), v.phoneAreaCode(), v.phoneNumber()), "barrio");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("rechaza addressCity vacío o nulo")
        void rejectsBlankAddressCity(String city) {
            assertRejected(v -> new Payer(v.email(), v.identificationType(), v.identificationNumber(), v.entityType(),
                    v.firstName(), v.lastName(), v.addressZipCode(), v.addressStreetName(), v.addressStreetNumber(),
                    v.addressNeighborhood(), city, v.phoneAreaCode(), v.phoneNumber()), "ciudad");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("rechaza phoneAreaCode vacío o nulo")
        void rejectsBlankPhoneAreaCode(String areaCode) {
            assertRejected(v -> new Payer(v.email(), v.identificationType(), v.identificationNumber(), v.entityType(),
                    v.firstName(), v.lastName(), v.addressZipCode(), v.addressStreetName(), v.addressStreetNumber(),
                    v.addressNeighborhood(), v.addressCity(), areaCode, v.phoneNumber()), "indicativo telefónico");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("rechaza phoneNumber vacío o nulo")
        void rejectsBlankPhoneNumber(String phoneNumber) {
            assertRejected(v -> new Payer(v.email(), v.identificationType(), v.identificationNumber(), v.entityType(),
                    v.firstName(), v.lastName(), v.addressZipCode(), v.addressStreetName(), v.addressStreetNumber(),
                    v.addressNeighborhood(), v.addressCity(), v.phoneAreaCode(), phoneNumber), "número telefónico");
        }

        private void assertRejected(Function<Payer, Payer> construct, String expectedMessagePart) {
            Payer valid = validPayer("individual");
            assertThatThrownBy(() -> construct.apply(valid))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(expectedMessagePart);
        }
    }
}
