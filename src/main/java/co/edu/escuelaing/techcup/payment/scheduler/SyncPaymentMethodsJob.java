package co.edu.escuelaing.techcup.payment.scheduler;

import co.edu.escuelaing.techcup.payment.service.ports.SyncPaymentMethodsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncPaymentMethodsJob {

    private final SyncPaymentMethodsUseCase syncPaymentMethodsUseCase;

    public SyncPaymentMethodsJob(SyncPaymentMethodsUseCase syncPaymentMethodsUseCase) {
        this.syncPaymentMethodsUseCase = syncPaymentMethodsUseCase;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void run() {
        syncPaymentMethodsUseCase.sync();
    }
}
