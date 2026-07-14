package co.edu.escuelaing.techcup.payment.scheduler;

import co.edu.escuelaing.techcup.payment.service.ports.SyncPaymentMethodsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SyncPaymentMethodsJobTest {

    @Test
    @DisplayName("run delega en SyncPaymentMethodsUseCase.sync")
    void delegatesToUseCase() {
        SyncPaymentMethodsUseCase useCase = mock(SyncPaymentMethodsUseCase.class);
        SyncPaymentMethodsJob job = new SyncPaymentMethodsJob(useCase);

        job.run();

        verify(useCase).sync();
    }
}
