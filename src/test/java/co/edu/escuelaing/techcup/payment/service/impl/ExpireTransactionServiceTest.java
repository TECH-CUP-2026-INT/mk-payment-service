package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpireTransactionServiceTest {

    private PaymentOrderRepositoryPort paymentOrderRepository;
    private ExpireTransactionService service;

    @BeforeEach
    void setUp() {
        paymentOrderRepository = mock(PaymentOrderRepositoryPort.class);
        service = new ExpireTransactionService(paymentOrderRepository);
    }

    private PaymentOrder dueOrder(PaymentOrderStatus status) {
        return PaymentOrder.reconstruct(UUID.randomUUID(), "enr-" + UUID.randomUUID(), "team-1", "tournament-1",
                new BigDecimal("50000"), status, null, UUID.randomUUID().toString(), null, null,
                LocalDateTime.now().minusMinutes(1), 0L);
    }

    @Nested
    @DisplayName("camino feliz")
    class HappyPath {

        @Test
        @DisplayName("expira y guarda cada orden PENDING o AWAITING_BANK_CONFIRMATION vencida")
        void expiresDueOrders() {
            PaymentOrder pending = dueOrder(PaymentOrderStatus.PENDING);
            PaymentOrder awaiting = dueOrder(PaymentOrderStatus.AWAITING_BANK_CONFIRMATION);
            when(paymentOrderRepository.findByStatusInAndExpiresAtBefore(any(), any()))
                    .thenReturn(List.of(pending, awaiting));
            when(paymentOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.expireDueOrders();

            assertThat(pending.getStatus()).isEqualTo(PaymentOrderStatus.EXPIRED);
            assertThat(awaiting.getStatus()).isEqualTo(PaymentOrderStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("conflictos de optimistic locking")
    class OptimisticLocking {

        @Test
        @DisplayName("un conflicto en una orden no aborta el resto del lote")
        void oneConflictDoesNotAbortBatch() {
            PaymentOrder conflicting = dueOrder(PaymentOrderStatus.PENDING);
            PaymentOrder healthy = dueOrder(PaymentOrderStatus.PENDING);
            when(paymentOrderRepository.findByStatusInAndExpiresAtBefore(any(), any()))
                    .thenReturn(List.of(conflicting, healthy));
            when(paymentOrderRepository.save(conflicting))
                    .thenThrow(new ObjectOptimisticLockingFailureException(PaymentOrder.class, conflicting.getId()));
            when(paymentOrderRepository.save(healthy)).thenAnswer(invocation -> invocation.getArgument(0));

            assertThatNoException().isThrownBy(() -> service.expireDueOrders());

            assertThat(healthy.getStatus()).isEqualTo(PaymentOrderStatus.EXPIRED);
        }
    }
}
