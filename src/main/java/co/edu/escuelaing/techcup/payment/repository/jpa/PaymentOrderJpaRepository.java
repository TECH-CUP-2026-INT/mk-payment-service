package co.edu.escuelaing.techcup.payment.repository.jpa;

import co.edu.escuelaing.techcup.payment.entity.PaymentOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderJpaRepository extends JpaRepository<PaymentOrderEntity, UUID> {

    boolean existsByEnrollmentId(String enrollmentId);

    Optional<PaymentOrderEntity> findByEnrollmentId(String enrollmentId);

    Optional<PaymentOrderEntity> findByMpPaymentId(String mpPaymentId);

    List<PaymentOrderEntity> findByStatusInAndExpiresAtBefore(List<String> statuses, LocalDateTime expiresAt);
}
