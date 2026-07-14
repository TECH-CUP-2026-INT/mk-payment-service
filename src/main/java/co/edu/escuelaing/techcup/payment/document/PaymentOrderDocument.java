package co.edu.escuelaing.techcup.payment.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "payment_orders")
@CompoundIndex(name = "idx_payment_orders_expiration", def = "{'status': 1, 'expiresAt': 1}")
@Getter
@Setter
@NoArgsConstructor
public class PaymentOrderDocument {

    @Id
    private UUID paymentOrderId;

    @Indexed(unique = true)
    private String enrollmentId;

    private String teamId;

    private String tournamentId;

    private BigDecimal amount;

    private String status;

    @Indexed
    private String mpPaymentId;

    @Indexed(unique = true)
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
