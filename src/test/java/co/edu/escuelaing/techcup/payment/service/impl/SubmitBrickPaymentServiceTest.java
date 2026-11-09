package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.CardTransactionResult;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubmitBrickPaymentServiceTest {

    private static final String NOTIFICATION_URL = "https://backend.test/payment-orders/webhook";

    private PaymentOrderRepositoryPort paymentOrderRepository;
    private PaymentGatewayPort paymentGateway;
    private SubmitBrickPaymentService service;

    @BeforeEach
    void setUp() {
        paymentOrderRepository = mock(PaymentOrderRepositoryPort.class);
        paymentGateway = mock(PaymentGatewayPort.class);
        service = new SubmitBrickPaymentService(paymentOrderRepository, paymentGateway,
                "https://frontend.test/checkout/pse-return", NOTIFICATION_URL);
    }

    private static PaymentOrder aPendingOrder() {
        return PaymentOrder.builder()
                .paymentOrderId(UUID.randomUUID())
                .enrollmentId("enr-1")
                .teamId("team-1")
                .tournamentId("tournament-1")
                .amount(new BigDecimal("50000"))
                .status(PaymentOrderStatus.PENDING)
                .idempotencyKey("idem-1")
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .build();
    }

    private void givenPendingOrder() {
        when(paymentOrderRepository.findByEnrollmentId("enr-1")).thenReturn(Optional.of(aPendingOrder()));
        when(paymentOrderRepository.save(any(PaymentOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenMercadoPagoReplies(CardTransactionResult result) {
        when(paymentGateway.createCardTransaction(any(), any(), eq("tok-visa"), eq("visa"), eq(1),
                eq("payer@test.com"), eq("CC"), eq("12345678"), eq(NOTIFICATION_URL)))
                .thenReturn(result);
    }

    private PaymentOrder submitCard() {
        return service.submit("enr-1", "visa", "tok-visa", 1, "payer@test.com",
                null, "CC", "12345678", null, "127.0.0.1");
    }

    @Test
    @DisplayName("una tarjeta aprobada resuelve la orden como APPROVED y conserva el statusDetail")
    void approvesCardWhenMercadoPagoApproves() {
        givenPendingOrder();
        givenMercadoPagoReplies(new CardTransactionResult("1328689408", "approved", "accredited"));

        PaymentOrder result = submitCard();

        assertThat(result.getStatus()).isEqualTo(PaymentOrderStatus.APPROVED);
        assertThat(result.getMpPaymentId()).isEqualTo("1328689408");
        assertThat(result.getGatewayStatusDetail()).isEqualTo("accredited");
        assertThat(result.getExternalResourceUrl()).isNull();
    }

    @Test
    @DisplayName("una tarjeta rechazada resuelve la orden como REJECTED")
    void rejectsCardWhenMercadoPagoRejects() {
        givenPendingOrder();
        givenMercadoPagoReplies(new CardTransactionResult("999", "rejected", "cc_rejected_insufficient_amount"));

        PaymentOrder result = submitCard();

        assertThat(result.getStatus()).isEqualTo(PaymentOrderStatus.REJECTED);
        assertThat(result.getGatewayStatusDetail()).isEqualTo("cc_rejected_insufficient_amount");
    }

    @Test
    @DisplayName("una tarjeta pendiente deja la orden en AWAITING_BANK_CONFIRMATION hasta el webhook")
    void keepsAwaitingConfirmationWhileCardIsPending() {
        givenPendingOrder();
        givenMercadoPagoReplies(new CardTransactionResult("123", "in_process", "pending_contingency"));

        PaymentOrder result = submitCard();

        assertThat(result.getStatus()).isEqualTo(PaymentOrderStatus.AWAITING_BANK_CONFIRMATION);
        assertThat(result.getGatewayStatusDetail()).isEqualTo("pending_contingency");
    }
}
