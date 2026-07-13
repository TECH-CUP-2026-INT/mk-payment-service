package co.edu.escuelaing.techcup.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_orders")
@Getter
@Setter
@NoArgsConstructor
public class PaymentOrderEntity {

    @Id
    @Column(name = "payment_order_id")
    private UUID paymentOrderId;

    @Column(name = "enrollment_id", nullable = false, unique = true)
    private String enrollmentId;

    @Column(name = "team_id", nullable = false)
    private String teamId;

    @Column(name = "tournament_id", nullable = false)
    private String tournamentId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "mp_payment_id")
    private String mpPaymentId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "external_resource_url")
    private String externalResourceUrl;

    @Column(name = "payer_email")
    private String payerEmail;

    @Column(name = "payer_id_type")
    private String payerIdType;

    @Column(name = "payer_id_number")
    private String payerIdNumber;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
