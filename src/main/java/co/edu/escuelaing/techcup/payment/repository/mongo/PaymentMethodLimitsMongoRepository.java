package co.edu.escuelaing.techcup.payment.repository.mongo;

import co.edu.escuelaing.techcup.payment.document.PaymentMethodLimitsDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentMethodLimitsMongoRepository extends MongoRepository<PaymentMethodLimitsDocument, String> {
}
