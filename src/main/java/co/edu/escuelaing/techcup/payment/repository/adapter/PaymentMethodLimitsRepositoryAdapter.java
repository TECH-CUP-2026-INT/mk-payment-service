package co.edu.escuelaing.techcup.payment.repository.adapter;

import co.edu.escuelaing.techcup.payment.document.PaymentMethodLimitsDocument;
import co.edu.escuelaing.techcup.payment.mapper.PaymentMethodLimitsPersistenceMapper;
import co.edu.escuelaing.techcup.payment.repository.mongo.PaymentMethodLimitsMongoRepository;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodLimitsRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentMethodLimitsRepositoryAdapter implements PaymentMethodLimitsRepositoryPort {

    private final PaymentMethodLimitsMongoRepository mongoRepository;

    public PaymentMethodLimitsRepositoryAdapter(PaymentMethodLimitsMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public Optional<PaymentMethodLimits> findById(String paymentMethodId) {
        return mongoRepository.findById(paymentMethodId).map(PaymentMethodLimitsPersistenceMapper::toDomain);
    }

    @Override
    public PaymentMethodLimits save(PaymentMethodLimits limits) {
        PaymentMethodLimitsDocument saved = mongoRepository.save(PaymentMethodLimitsPersistenceMapper.toEntity(limits));
        return PaymentMethodLimitsPersistenceMapper.toDomain(saved);
    }
}
