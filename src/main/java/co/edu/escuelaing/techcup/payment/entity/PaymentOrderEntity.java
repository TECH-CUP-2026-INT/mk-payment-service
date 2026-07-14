package co.edu.escuelaing.techcup.payment.entity;

import co.edu.escuelaing.techcup.payment.config.PaymentOrderAuditListener;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_payment_orders_enrollment_id", columnNames = "enrollmentId"),
                @UniqueConstraint(name = "uq_payment_orders_idempotency_key", columnNames = "idempotencyKey")
        },
        indexes = {
                @Index(name = "idx_payment_orders_expiration", columnList = "status, expiresAt")
        })
@EntityListeners(PaymentOrderAuditListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PaymentOrderEntity {

    @Id
    private UUID paymentOrderId;

    private String enrollmentId;

    private String teamId;

    private String tournamentId;

    private BigDecimal amount;

    private String status;

    private String mpPaymentId;

    private String idempotencyKey;

    private String externalResourceUrl;

    private String payerEmail;

    private String payerIdType;

    private String payerIdNumber;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Version
    private Integer version;
}
