package co.edu.escuelaing.techcup.payment.repository.adapter;

import co.edu.escuelaing.techcup.payment.document.PaymentOrderDocument;
import co.edu.escuelaing.techcup.payment.mapper.PaymentOrderPersistenceMapper;
import co.edu.escuelaing.techcup.payment.repository.mongo.PaymentOrderMongoRepository;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class PaymentOrderRepositoryAdapter implements PaymentOrderRepositoryPort {

    private final PaymentOrderMongoRepository mongoRepository;

    public PaymentOrderRepositoryAdapter(PaymentOrderMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public PaymentOrder save(PaymentOrder paymentOrder) {
        PaymentOrderDocument saved = mongoRepository.save(PaymentOrderPersistenceMapper.toEntity(paymentOrder));
        return PaymentOrderPersistenceMapper.toDomain(saved);
    }

    @Override
    public boolean existsByEnrollmentId(String enrollmentId) {
        return mongoRepository.existsByEnrollmentId(enrollmentId);
    }

    @Override
    public Optional<PaymentOrder> findByEnrollmentId(String enrollmentId) {
        return mongoRepository.findByEnrollmentId(enrollmentId).map(PaymentOrderPersistenceMapper::toDomain);
    }

    @Override
    public Optional<PaymentOrder> findByMpPaymentId(String mpPaymentId) {
        return mongoRepository.findByMpPaymentId(mpPaymentId).map(PaymentOrderPersistenceMapper::toDomain);
    }

    @Override
    public List<PaymentOrder> findByStatusInAndExpiresAtBefore(List<PaymentOrderStatus> statuses, LocalDateTime before) {
        List<String> statusNames = statuses.stream().map(Enum::name).toList();
        return mongoRepository.findByStatusInAndExpiresAtBefore(statusNames, before).stream()
                .map(PaymentOrderPersistenceMapper::toDomain)
                .toList();
    }
}
