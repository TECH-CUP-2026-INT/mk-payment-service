package co.edu.escuelaing.techcup.payment.service.impl;

import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.ExpireTransactionUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ExpireTransactionService implements ExpireTransactionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExpireTransactionService.class);

    private final PaymentOrderRepositoryPort paymentOrderRepository;

    public ExpireTransactionService(PaymentOrderRepositoryPort paymentOrderRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
    }

    @Override
    public void expireDueOrders() {
        List<PaymentOrder> dueOrders = paymentOrderRepository.findByStatusInAndExpiresAtBefore(
                List.of(PaymentOrderStatus.PENDING, PaymentOrderStatus.AWAITING_BANK_CONFIRMATION),
                LocalDateTime.now(ZoneId.systemDefault()));

        for (PaymentOrder paymentOrder : dueOrders) {
            paymentOrder.expire();
            try {
                paymentOrderRepository.save(paymentOrder);
            } catch (OptimisticLockingFailureException ex) {
                // Another writer (the webhook) already resolved this order first -
                // one conflicting order must not abort the rest of the batch.
                log.info("Conflicto de versión al expirar la orden {}, otro proceso ya la actualizó primero",
                        paymentOrder.getId());
            }
        }
    }
}
