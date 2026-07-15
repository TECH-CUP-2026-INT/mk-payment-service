package co.edu.escuelaing.techcup.payment.repository.adapter;

import co.edu.escuelaing.techcup.payment.entity.PaymentMethodLimitsEntity;
import co.edu.escuelaing.techcup.payment.mapper.PaymentMethodLimitsPersistenceMapper;
import co.edu.escuelaing.techcup.payment.repository.jpa.PaymentMethodLimitsJpaRepository;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodLimitsRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentMethodLimitsRepositoryAdapter implements PaymentMethodLimitsRepositoryPort {

    private final PaymentMethodLimitsJpaRepository jpaRepository;
    private final PaymentMethodLimitsPersistenceMapper mapper;

    public PaymentMethodLimitsRepositoryAdapter(PaymentMethodLimitsJpaRepository jpaRepository,
            PaymentMethodLimitsPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<PaymentMethodLimits> findById(String paymentMethodId) {
        return jpaRepository.findById(paymentMethodId).map(mapper::toDomain);
    }

    @Override
    public PaymentMethodLimits save(PaymentMethodLimits limits) {
        PaymentMethodLimitsEntity saved = jpaRepository.save(mapper.toEntity(limits));
        return mapper.toDomain(saved);
    }
}
