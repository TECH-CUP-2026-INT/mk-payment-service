package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentStatusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessPaymentWebhookServiceTest {

    private PaymentOrderRepositoryPort paymentOrderRepository;
    private PaymentGatewayPort paymentGateway;
    private ProcessPaymentWebhookService service;

    @BeforeEach
    void setUp() {
        paymentOrderRepository = mock(PaymentOrderRepositoryPort.class);
        paymentGateway = mock(PaymentGatewayPort.class);
        service = new ProcessPaymentWebhookService(paymentOrderRepository, paymentGateway);
        when(paymentOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private PaymentOrder awaitingOrder(String mpPaymentId) {
        return PaymentOrder.builder()
                .paymentOrderId(UUID.randomUUID())
                .enrollmentId("enr-1")
                .teamId("team-1")
                .tournamentId("tournament-1")
                .amount(new BigDecimal("50000"))
                .status(PaymentOrderStatus.AWAITING_BANK_CONFIRMATION)
                .mpPaymentId(mpPaymentId)
                .idempotencyKey(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .version(0L)
                .build();
    }

    @Nested
    @DisplayName("la transición solo depende de PaymentGatewayPort.getPaymentStatus, nunca del body del webhook")
    class TrustsOnlyGatewayStatus {

        @Test
        @DisplayName("aprueba la orden cuando Mercado Pago reporta status=approved, sin recibir ningún status del body")
        void approvesBasedOnGatewayStatusOnly() {
            PaymentOrder order = awaitingOrder("mp-1");
            when(paymentOrderRepository.findByMpPaymentId("mp-1")).thenReturn(Optional.of(order));
            when(paymentGateway.getPaymentStatus("mp-1")).thenReturn(new PaymentStatusResult("mp-1", "approved"));

            // process() only ever receives the paymentId - there is no parameter
            // through which a caller-supplied "status" could reach this method,
            // so the only way this test can pass is if the transition came from
            // the mocked gateway response, never from a webhook body value.
            service.process("mp-1");

            assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.APPROVED);
            verify(paymentOrderRepository).save(order);
        }

        @Test
        @DisplayName("rechaza la orden cuando Mercado Pago reporta status=rejected")
        void rejectsBasedOnGatewayStatusOnly() {
            PaymentOrder order = awaitingOrder("mp-2");
            when(paymentOrderRepository.findByMpPaymentId("mp-2")).thenReturn(Optional.of(order));
            when(paymentGateway.getPaymentStatus("mp-2")).thenReturn(new PaymentStatusResult("mp-2", "rejected"));

            service.process("mp-2");

            assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.REJECTED);
            verify(paymentOrderRepository).save(order);
        }

        @Test
        @DisplayName("no transiciona ni guarda cuando Mercado Pago todavía reporta pending/in_process")
        void doesNothingWhileStillPending() {
            PaymentOrder order = awaitingOrder("mp-3");
            when(paymentOrderRepository.findByMpPaymentId("mp-3")).thenReturn(Optional.of(order));
            when(paymentGateway.getPaymentStatus("mp-3")).thenReturn(new PaymentStatusResult("mp-3", "in_process"));

            service.process("mp-3");

            assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.AWAITING_BANK_CONFIRMATION);
            verify(paymentOrderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("resiliencia: toda ruta termina en un retorno normal, nunca propaga una excepción")
    class Resilience {

        @Test
        @DisplayName("no falla y no guarda si Mercado Pago no puede responder el estado del pago")
        void doesNotFailWhenGatewayFails() {
            when(paymentGateway.getPaymentStatus("mp-4")).thenThrow(new RuntimeException("Mercado Pago no disponible"));

            assertThatNoException().isThrownBy(() -> service.process("mp-4"));
            verify(paymentOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("no falla y no guarda si no existe una orden para el mpPaymentId recibido")
        void doesNotFailWhenOrderNotFound() {
            when(paymentGateway.getPaymentStatus("mp-5")).thenReturn(new PaymentStatusResult("mp-5", "approved"));
            when(paymentOrderRepository.findByMpPaymentId("mp-5")).thenReturn(Optional.empty());

            assertThatNoException().isThrownBy(() -> service.process("mp-5"));
            verify(paymentOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("no falla si la orden ya no está AWAITING_BANK_CONFIRMATION (notificación duplicada)")
        void doesNotFailOnDuplicateNotification() {
            PaymentOrder order = PaymentOrder.builder()
                    .paymentOrderId(UUID.randomUUID())
                    .enrollmentId("enr-1")
                    .teamId("team-1")
                    .tournamentId("tournament-1")
                    .amount(new BigDecimal("50000"))
                    .status(PaymentOrderStatus.APPROVED)
                    .mpPaymentId("mp-6")
                    .idempotencyKey(UUID.randomUUID().toString())
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .version(0L)
                    .build();
            when(paymentOrderRepository.findByMpPaymentId("mp-6")).thenReturn(Optional.of(order));
            when(paymentGateway.getPaymentStatus("mp-6")).thenReturn(new PaymentStatusResult("mp-6", "approved"));

            assertThatNoException().isThrownBy(() -> service.process("mp-6"));
            verify(paymentOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("un conflicto de optimistic locking al guardar es un no-op silencioso: quien escribió primero gana")
        void silentlyIgnoresOptimisticLockConflict() {
            PaymentOrder order = awaitingOrder("mp-7");
            when(paymentOrderRepository.findByMpPaymentId("mp-7")).thenReturn(Optional.of(order));
            when(paymentGateway.getPaymentStatus("mp-7")).thenReturn(new PaymentStatusResult("mp-7", "approved"));
            when(paymentOrderRepository.save(order))
                    .thenThrow(new OptimisticLockingFailureException("optimistic lock conflict"));

            assertThatNoException().isThrownBy(() -> service.process("mp-7"));
        }
    }
}
