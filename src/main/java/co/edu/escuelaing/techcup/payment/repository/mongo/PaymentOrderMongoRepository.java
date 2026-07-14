package co.edu.escuelaing.techcup.payment.repository.mongo;

import co.edu.escuelaing.techcup.payment.document.PaymentOrderDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderMongoRepository extends MongoRepository<PaymentOrderDocument, UUID> {

    boolean existsByEnrollmentId(String enrollmentId);

    Optional<PaymentOrderDocument> findByEnrollmentId(String enrollmentId);

    Optional<PaymentOrderDocument> findByMpPaymentId(String mpPaymentId);

    List<PaymentOrderDocument> findByStatusInAndExpiresAtBefore(List<String> statuses, LocalDateTime expiresAt);
}
