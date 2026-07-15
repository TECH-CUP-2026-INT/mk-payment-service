package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.exception.PaymentGatewayException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderExpiredException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotFoundException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotPendingException;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.PseTransactionResult;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmitPseTransactionServiceTest {

    private static final Payer PAYER = new Payer("pagador@correo.com", "CC", "123456789", "individual");
    private static final String NOTIFICATION_URL = "https://example-test-tunnel.invalid/payment-orders/webhook";

    private PaymentOrderRepositoryPort paymentOrderRepository;
    private PaymentGatewayPort paymentGateway;
    private SubmitPseTransactionService service;

    @BeforeEach
    void setUp() {
        paymentOrderRepository = mock(PaymentOrderRepositoryPort.class);
        paymentGateway = mock(PaymentGatewayPort.class);
        service = new SubmitPseTransactionService(paymentOrderRepository, paymentGateway, NOTIFICATION_URL);
        when(paymentOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private PaymentOrder orderWith(PaymentOrderStatus status, LocalDateTime expiresAt) {
        return PaymentOrder.reconstruct(UUID.randomUUID(), "enr-1", "team-1", "tournament-1",
                new BigDecimal("50000"), status, null, UUID.randomUUID().toString(), null, null, expiresAt, 0L);
    }

    @Nested
    @DisplayName("camino feliz")
    class HappyPath {

        @Test
        @DisplayName("transiciona a AWAITING_BANK_CONFIRMATION y guarda la referencia de Mercado Pago")
        void submitsSuccessfully() {
            PaymentOrder order = orderWith(PaymentOrderStatus.PENDING, LocalDateTime.now().plusMinutes(60));
            when(paymentOrderRepository.findByEnrollmentId("enr-1")).thenReturn(Optional.of(order));
            when(paymentGateway.createPseTransaction(any(), any(), any(), any(), any()))
                    .thenReturn(new PseTransactionResult("mp-1", "pending", "https://mp.test/ticket/1"));

            PaymentOrder result = service.submit("enr-1", PAYER, "1007");

            assertThat(result.getStatus()).isEqualTo(PaymentOrderStatus.AWAITING_BANK_CONFIRMATION);
            assertThat(result.getMpPaymentId()).isEqualTo("mp-1");
            assertThat(result.getExternalResourceUrl()).isEqualTo("https://mp.test/ticket/1");
            verify(paymentOrderRepository).save(any());
        }
    }

    @Nested
    @DisplayName("PaymentOrderNotFoundException")
    class NotFound {

        @Test
        @DisplayName("se lanza cuando no existe una orden para el enrollmentId")
        void rejectsWhenNoOrder() {
            when(paymentOrderRepository.findByEnrollmentId("enr-x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submit("enr-x", PAYER, "1007"))
                    .isInstanceOf(PaymentOrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("PaymentOrderNotPendingException")
    class NotPending {

        @Test
        @DisplayName("se lanza cuando la orden ya está AWAITING_BANK_CONFIRMATION y no ha expirado")
        void rejectsWhenAlreadyAwaiting() {
            PaymentOrder order = orderWith(PaymentOrderStatus.AWAITING_BANK_CONFIRMATION, LocalDateTime.now().plusMinutes(60));
            when(paymentOrderRepository.findByEnrollmentId("enr-1")).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.submit("enr-1", PAYER, "1007"))
                    .isInstanceOf(PaymentOrderNotPendingException.class);
        }
    }

    @Nested
    @DisplayName("PaymentOrderExpiredException")
    class Expired {

        @Test
        @DisplayName("se lanza y persiste la expiración cuando el cron todavía no barrió la orden vencida")
        void expiresAndPersistsWhenNotYetSwept() {
            PaymentOrder order = orderWith(PaymentOrderStatus.PENDING, LocalDateTime.now().minusMinutes(1));
            when(paymentOrderRepository.findByEnrollmentId("enr-1")).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.submit("enr-1", PAYER, "1007"))
                    .isInstanceOf(PaymentOrderExpiredException.class);

            assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.EXPIRED);
            verify(paymentOrderRepository, times(1)).save(order);
        }

        @Test
        @DisplayName("se lanza sin volver a guardar cuando la orden ya estaba EXPIRED")
        void rejectsWhenAlreadyExpired() {
            PaymentOrder order = orderWith(PaymentOrderStatus.EXPIRED, LocalDateTime.now().minusMinutes(1));
            when(paymentOrderRepository.findByEnrollmentId("enr-1")).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.submit("enr-1", PAYER, "1007"))
                    .isInstanceOf(PaymentOrderExpiredException.class);
            verify(paymentOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("sigue respondiendo 410 aunque el guardado de la expiración choque por optimistic locking")
        void stillThrowsWhenSaveConflicts() {
            PaymentOrder order = orderWith(PaymentOrderStatus.PENDING, LocalDateTime.now().minusMinutes(1));
            when(paymentOrderRepository.findByEnrollmentId("enr-1")).thenReturn(Optional.of(order));
            when(paymentOrderRepository.save(order)).thenThrow(new OptimisticLockingFailureException("optimistic lock conflict"));

            assertThatThrownBy(() -> service.submit("enr-1", PAYER, "1007"))
                    .isInstanceOf(PaymentOrderExpiredException.class);
        }
    }

    @Nested
    @DisplayName("PaymentGatewayException")
    class GatewayFailure {

        @Test
        @DisplayName("se lanza y la orden no se persiste (queda en PENDING para reintento) si Mercado Pago rechaza la solicitud")
        void wrapsGatewayFailure() {
            PaymentOrder order = orderWith(PaymentOrderStatus.PENDING, LocalDateTime.now().plusMinutes(60));
            when(paymentOrderRepository.findByEnrollmentId("enr-1")).thenReturn(Optional.of(order));
            when(paymentGateway.createPseTransaction(any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Mercado Pago no disponible"));

            assertThatThrownBy(() -> service.submit("enr-1", PAYER, "1007"))
                    .isInstanceOf(PaymentGatewayException.class);

            verify(paymentOrderRepository, never()).save(any());
        }
    }
}
