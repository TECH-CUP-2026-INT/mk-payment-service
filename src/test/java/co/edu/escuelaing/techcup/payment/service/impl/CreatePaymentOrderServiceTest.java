package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.exception.AmountOutOfRangeException;
import co.edu.escuelaing.techcup.payment.exception.DuplicateEnrollmentOrderException;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodLimitsRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreatePaymentOrderServiceTest {

    private static final PaymentMethodLimits PSE_LIMITS = new PaymentMethodLimits(
            "pse", new BigDecimal("10000"), new BigDecimal("500000"), "active", LocalDateTime.now());

    private PaymentOrderRepositoryPort paymentOrderRepository;
    private PaymentMethodLimitsRepositoryPort paymentMethodLimitsRepository;
    private CreatePaymentOrderService service;

    @BeforeEach
    void setUp() {
        paymentOrderRepository = mock(PaymentOrderRepositoryPort.class);
        paymentMethodLimitsRepository = mock(PaymentMethodLimitsRepositoryPort.class);
        service = new CreatePaymentOrderService(paymentOrderRepository, paymentMethodLimitsRepository);
        when(paymentOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("camino feliz")
    class HappyPath {

        @Test
        @DisplayName("crea y persiste la orden cuando el monto está dentro de los límites y no hay duplicado")
        void createsOrder() {
            when(paymentMethodLimitsRepository.findById("pse")).thenReturn(Optional.of(PSE_LIMITS));
            when(paymentOrderRepository.existsByEnrollmentId("enr-1")).thenReturn(false);

            PaymentOrder order = service.create("enr-1", "team-1", "tournament-1", new BigDecimal("50000"));

            assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.PENDING);
            verify(paymentOrderRepository).save(any());
        }
    }

    @Nested
    @DisplayName("AmountOutOfRangeException")
    class AmountOutOfRange {

        @Test
        @DisplayName("se lanza cuando el monto excede el máximo permitido")
        void rejectsAboveMax() {
            when(paymentMethodLimitsRepository.findById("pse")).thenReturn(Optional.of(PSE_LIMITS));

            assertThatThrownBy(() -> service.create("enr-1", "team-1", "tournament-1", new BigDecimal("999999")))
                    .isInstanceOf(AmountOutOfRangeException.class);
            verify(paymentOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("se lanza cuando el monto está por debajo del mínimo permitido")
        void rejectsBelowMin() {
            when(paymentMethodLimitsRepository.findById("pse")).thenReturn(Optional.of(PSE_LIMITS));

            assertThatThrownBy(() -> service.create("enr-1", "team-1", "tournament-1", new BigDecimal("100")))
                    .isInstanceOf(AmountOutOfRangeException.class);
            verify(paymentOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("se lanza cuando no hay límites cacheados para PSE")
        void rejectsWhenNoLimitsCached() {
            when(paymentMethodLimitsRepository.findById("pse")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create("enr-1", "team-1", "tournament-1", new BigDecimal("50000")))
                    .isInstanceOf(AmountOutOfRangeException.class);
            verify(paymentOrderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("DuplicateEnrollmentOrderException")
    class Duplicate {

        @Test
        @DisplayName("se lanza cuando ya existe una orden para el enrollmentId")
        void rejectsDuplicate() {
            when(paymentMethodLimitsRepository.findById("pse")).thenReturn(Optional.of(PSE_LIMITS));
            when(paymentOrderRepository.existsByEnrollmentId("enr-1")).thenReturn(true);

            assertThatThrownBy(() -> service.create("enr-1", "team-1", "tournament-1", new BigDecimal("50000")))
                    .isInstanceOf(DuplicateEnrollmentOrderException.class);
            verify(paymentOrderRepository, never()).save(any());
        }
    }
}
