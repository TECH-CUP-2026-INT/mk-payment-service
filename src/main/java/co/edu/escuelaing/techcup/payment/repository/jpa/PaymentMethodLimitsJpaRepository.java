package co.edu.escuelaing.techcup.payment.repository.jpa;

import co.edu.escuelaing.techcup.payment.entity.PaymentMethodLimitsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodLimitsJpaRepository extends JpaRepository<PaymentMethodLimitsEntity, String> {
}
