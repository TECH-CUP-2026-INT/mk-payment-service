package co.edu.escuelaing.techcup.payment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_method_limits")
@Getter
@Setter
@NoArgsConstructor
public class PaymentMethodLimitsEntity {

    @Id
    private String paymentMethodId;

    private BigDecimal minAllowedAmount;

    private BigDecimal maxAllowedAmount;

    private String status;

    private LocalDateTime lastSyncedAt;
}
