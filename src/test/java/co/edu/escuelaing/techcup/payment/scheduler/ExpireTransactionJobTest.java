package co.edu.escuelaing.techcup.payment.scheduler;

import co.edu.escuelaing.techcup.payment.service.ports.ExpireTransactionUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExpireTransactionJobTest {

    @Test
    @DisplayName("run delega en ExpireTransactionUseCase.expireDueOrders")
    void delegatesToUseCase() {
        ExpireTransactionUseCase useCase = mock(ExpireTransactionUseCase.class);
        ExpireTransactionJob job = new ExpireTransactionJob(useCase);

        job.run();

        verify(useCase).expireDueOrders();
    }
}
