package co.edu.escuelaing.techcup.payment.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payment_method_limits")
@Getter
@Setter
@NoArgsConstructor
public class PaymentMethodLimitsDocument {

    @Id
    private String paymentMethodId;

    private BigDecimal minAllowedAmount;

    private BigDecimal maxAllowedAmount;

    private String status;

    private LocalDateTime lastSyncedAt;
}
