package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotFoundException;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetPaymentOrderStatusServiceTest {

    private PaymentOrderRepositoryPort paymentOrderRepository;
    private GetPaymentOrderStatusService service;

    @BeforeEach
    void setUp() {
        paymentOrderRepository = mock(PaymentOrderRepositoryPort.class);
        service = new GetPaymentOrderStatusService(paymentOrderRepository);
    }

    @Nested
    @DisplayName("camino feliz")
    class HappyPath {

        @Test
        @DisplayName("devuelve la orden encontrada por enrollmentId")
        void returnsOrder() {
            PaymentOrder order = PaymentOrder.builder()
                    .paymentOrderId(UUID.randomUUID())
                    .enrollmentId("enr-1")
                    .teamId("team-1")
                    .tournamentId("tournament-1")
                    .amount(new BigDecimal("50000"))
                    .status(PaymentOrderStatus.APPROVED)
                    .mpPaymentId("mp-1")
                    .idempotencyKey(UUID.randomUUID().toString())
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .version(0L)
                    .build();
            when(paymentOrderRepository.findByEnrollmentId("enr-1")).thenReturn(Optional.of(order));

            assertThat(service.getByEnrollmentId("enr-1")).isEqualTo(order);
        }
    }

    @Nested
    @DisplayName("PaymentOrderNotFoundException")
    class NotFound {

        @Test
        @DisplayName("se lanza cuando no existe una orden para el enrollmentId")
        void throwsWhenNotFound() {
            when(paymentOrderRepository.findByEnrollmentId("enr-x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByEnrollmentId("enr-x"))
                    .isInstanceOf(PaymentOrderNotFoundException.class);
        }
    }
}
