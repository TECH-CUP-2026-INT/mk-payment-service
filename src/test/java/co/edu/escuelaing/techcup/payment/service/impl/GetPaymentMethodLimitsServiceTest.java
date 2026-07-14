package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.exception.PaymentMethodLimitsNotFoundException;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodLimitsRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetPaymentMethodLimitsServiceTest {

    private PaymentMethodLimitsRepositoryPort paymentMethodLimitsRepository;
    private GetPaymentMethodLimitsService service;

    @BeforeEach
    void setUp() {
        paymentMethodLimitsRepository = mock(PaymentMethodLimitsRepositoryPort.class);
        service = new GetPaymentMethodLimitsService(paymentMethodLimitsRepository);
    }

    @Nested
    @DisplayName("camino feliz")
    class HappyPath {

        @Test
        @DisplayName("devuelve los límites cacheados para pse")
        void returnsLimits() {
            PaymentMethodLimits limits = new PaymentMethodLimits(
                    "pse", new BigDecimal("10000"), new BigDecimal("500000"), "active", LocalDateTime.now());
            when(paymentMethodLimitsRepository.findById("pse")).thenReturn(Optional.of(limits));

            assertThat(service.getPseLimits()).isEqualTo(limits);
        }
    }

    @Nested
    @DisplayName("PaymentMethodLimitsNotFoundException")
    class NotFound {

        @Test
        @DisplayName("se lanza cuando aún no se ha sincronizado ningún límite para pse")
        void throwsWhenNotSynced() {
            when(paymentMethodLimitsRepository.findById("pse")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPseLimits())
                    .isInstanceOf(PaymentMethodLimitsNotFoundException.class);
        }
    }
}
