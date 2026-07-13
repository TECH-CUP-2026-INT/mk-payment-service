package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodInfo;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodLimitsRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncPaymentMethodsServiceTest {

    private PaymentGatewayPort paymentGateway;
    private PaymentMethodLimitsRepositoryPort paymentMethodLimitsRepository;
    private SyncPaymentMethodsService service;

    @BeforeEach
    void setUp() {
        paymentGateway = mock(PaymentGatewayPort.class);
        paymentMethodLimitsRepository = mock(PaymentMethodLimitsRepositoryPort.class);
        service = new SyncPaymentMethodsService(paymentGateway, paymentMethodLimitsRepository);
    }

    @Nested
    @DisplayName("camino feliz")
    class HappyPath {

        @Test
        @DisplayName("filtra por id=pse y hace upsert de sus límites")
        void upsertsPseLimits() {
            when(paymentGateway.getAvailablePaymentMethods()).thenReturn(List.of(
                    new PaymentMethodInfo("credit_card", "active", new BigDecimal("1000"), new BigDecimal("9000000")),
                    new PaymentMethodInfo("pse", "active", new BigDecimal("10000"), new BigDecimal("500000"))));

            service.sync();

            ArgumentCaptor<PaymentMethodLimits> captor = ArgumentCaptor.forClass(PaymentMethodLimits.class);
            verify(paymentMethodLimitsRepository).save(captor.capture());
            assertThat(captor.getValue().paymentMethodId()).isEqualTo("pse");
            assertThat(captor.getValue().minAllowedAmount()).isEqualTo(new BigDecimal("10000"));
            assertThat(captor.getValue().maxAllowedAmount()).isEqualTo(new BigDecimal("500000"));
        }
    }

    @Nested
    @DisplayName("pse ausente en la respuesta de Mercado Pago")
    class PseMissing {

        @Test
        @DisplayName("no hace upsert si Mercado Pago no devuelve el método pse")
        void doesNotUpsertWhenPseMissing() {
            when(paymentGateway.getAvailablePaymentMethods()).thenReturn(List.of(
                    new PaymentMethodInfo("credit_card", "active", new BigDecimal("1000"), new BigDecimal("9000000"))));

            service.sync();

            verify(paymentMethodLimitsRepository, never()).save(any());
        }
    }
}
