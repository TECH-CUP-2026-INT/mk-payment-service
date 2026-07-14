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

    public PaymentMethodLimitsRepositoryAdapter(PaymentMethodLimitsJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<PaymentMethodLimits> findById(String paymentMethodId) {
        return jpaRepository.findById(paymentMethodId).map(PaymentMethodLimitsPersistenceMapper::toDomain);
    }

    @Override
    public PaymentMethodLimits save(PaymentMethodLimits limits) {
        PaymentMethodLimitsEntity saved = jpaRepository.save(PaymentMethodLimitsPersistenceMapper.toEntity(limits));
        return PaymentMethodLimitsPersistenceMapper.toDomain(saved);
    }
}
