package co.edu.escuelaing.techcup.payment.service.ports;

import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepositoryPort {

    PaymentOrder save(PaymentOrder paymentOrder);

    boolean existsByEnrollmentId(String enrollmentId);

    Optional<PaymentOrder> findByEnrollmentId(String enrollmentId);

    Optional<PaymentOrder> findByMpPaymentId(String mpPaymentId);

    List<PaymentOrder> findByStatusInAndExpiresAtBefore(List<PaymentOrderStatus> statuses, LocalDateTime before);
}
