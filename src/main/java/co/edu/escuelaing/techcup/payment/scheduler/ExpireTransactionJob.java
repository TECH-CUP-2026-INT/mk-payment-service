package co.edu.escuelaing.techcup.payment.scheduler;

import co.edu.escuelaing.techcup.payment.service.ports.ExpireTransactionUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpireTransactionJob {

    private static final long FIVE_MINUTES_MILLIS = 300_000L;

    private final ExpireTransactionUseCase expireTransactionUseCase;

    public ExpireTransactionJob(ExpireTransactionUseCase expireTransactionUseCase) {
        this.expireTransactionUseCase = expireTransactionUseCase;
    }

    @Scheduled(fixedRate = FIVE_MINUTES_MILLIS)
    public void run() {
        expireTransactionUseCase.expireDueOrders();
    }
}
