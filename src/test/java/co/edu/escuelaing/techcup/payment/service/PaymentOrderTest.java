package co.edu.escuelaing.techcup.payment.service;

import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotAwaitingBankConfirmationException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotPendingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class PaymentOrderTest {

    private static final Payer VALID_PAYER = new Payer("pagador@correo.com", "CC", "123456789");

    private PaymentOrder pendingOrder() {
        return PaymentOrder.create("enr-1", "team-1", "tournament-1", new BigDecimal("50000"));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("crea la orden en estado PENDING con expiración a 60 minutos y una idempotencyKey propia")
        void createsPendingOrder() {
            LocalDateTime before = LocalDateTime.now();
            PaymentOrder order = pendingOrder();
            LocalDateTime after = LocalDateTime.now();

            assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.PENDING);
            assertThat(order.getId()).isNotNull();
            assertThat(order.getIdempotencyKey()).isNotBlank();
            assertThat(order.getExpiresAt()).isAfter(before.plusMinutes(59)).isBefore(after.plusMinutes(61));
        }

        @Test
        @DisplayName("rechaza un monto menor o igual a cero")
        void rejectsNonPositiveAmount() {
            assertThatThrownBy(() -> PaymentOrder.create("enr-1", "team-1", "tournament-1", BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("startPseTransaction")
    class StartPseTransaction {

        @Test
        @DisplayName("transiciona a AWAITING_BANK_CONFIRMATION y guarda el pagador cuando la orden está PENDING")
        void transitionsWhenPending() {
            PaymentOrder order = pendingOrder();
            order.startPseTransaction(VALID_PAYER, "1007");

            assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.AWAITING_BANK_CONFIRMATION);
            assertThat(order.getPayer()).isEqualTo(VALID_PAYER);
        }

        @Test
        @DisplayName("lanza PaymentOrderNotPendingException si la orden no está PENDING")
        void rejectsWhenNotPending() {
            PaymentOrder order = pendingOrder();
            order.startPseTransaction(VALID_PAYER, "1007");

            assertThatThrownBy(() -> order.startPseTransaction(VALID_PAYER, "1007"))
                    .isInstanceOf(PaymentOrderNotPendingException.class);
        }

        @Test
        @DisplayName("rechaza una institución financiera en blanco")
        void rejectsBlankFinancialInstitution() {
            PaymentOrder order = pendingOrder();
            assertThatThrownBy(() -> order.startPseTransaction(VALID_PAYER, " "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("approve / reject")
    class ApproveReject {

        @Test
        @DisplayName("approve transiciona a APPROVED y registra el mpPaymentId cuando está AWAITING_BANK_CONFIRMATION")
        void approvesWhenAwaitingConfirmation() {
            PaymentOrder order = pendingOrder();
            order.startPseTransaction(VALID_PAYER, "1007");

            order.approve("mp-123");

            assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.APPROVED);
            assertThat(order.getMpPaymentId()).isEqualTo("mp-123");
        }

        @Test
        @DisplayName("reject transiciona a REJECTED cuando está AWAITING_BANK_CONFIRMATION")
        void rejectsWhenAwaitingConfirmation() {
            PaymentOrder order = pendingOrder();
            order.startPseTransaction(VALID_PAYER, "1007");

            order.reject();

            assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.REJECTED);
        }

        @Test
        @DisplayName("approve lanza PaymentOrderNotAwaitingBankConfirmationException si la orden sigue PENDING")
        void approveRejectsWhenPending() {
            PaymentOrder order = pendingOrder();
            assertThatThrownBy(() -> order.approve("mp-123"))
                    .isInstanceOf(PaymentOrderNotAwaitingBankConfirmationException.class);
        }

        @Test
        @DisplayName("reject lanza PaymentOrderNotAwaitingBankConfirmationException si la orden sigue PENDING")
        void rejectRejectsWhenPending() {
            PaymentOrder order = pendingOrder();
            assertThatThrownBy(order::reject)
                    .isInstanceOf(PaymentOrderNotAwaitingBankConfirmationException.class);
        }
    }

    @Nested
    @DisplayName("expire")
    class Expire {

        @Test
        @DisplayName("transiciona a EXPIRED desde PENDING")
        void expiresFromPending() {
            PaymentOrder order = pendingOrder();
            order.expire();
            assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.EXPIRED);
        }

        @Test
        @DisplayName("es idempotente: llamarlo dos veces no lanza excepción y deja la orden en EXPIRED")
        void expireIsIdempotent() {
            PaymentOrder order = pendingOrder();
            order.expire();

            assertThatNoException().isThrownBy(order::expire);
            assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.EXPIRED);
        }

        @Test
        @DisplayName("no hace nada si la orden ya está en un estado final distinto de EXPIRED")
        void doesNothingOnFinalState() {
            PaymentOrder order = pendingOrder();
            order.startPseTransaction(VALID_PAYER, "1007");
            order.approve("mp-123");

            order.expire();

            assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.APPROVED);
        }
    }

    @Test
    @DisplayName("reconstruct reconstruye la orden con los mismos datos persistidos")
    void reconstructRebuildsFromPersistedData() {
        UUID id = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        PaymentOrder order = PaymentOrder.reconstruct(id, "enr-1", "team-1", "tournament-1", new BigDecimal("50000"),
                PaymentOrderStatus.AWAITING_BANK_CONFIRMATION, "mp-1", "idem-1", "https://mp.test/1", VALID_PAYER,
                expiresAt, 3L);

        assertThat(order.getId()).isEqualTo(id);
        assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.AWAITING_BANK_CONFIRMATION);
        assertThat(order.getMpPaymentId()).isEqualTo("mp-1");
        assertThat(order.getVersion()).isEqualTo(3L);
    }
}
