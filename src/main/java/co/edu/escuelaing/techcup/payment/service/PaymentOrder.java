package co.edu.escuelaing.techcup.payment.service;

import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotAwaitingBankConfirmationException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotPendingException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public final class PaymentOrder extends AggregateRoot {

    private static final long DEFAULT_EXPIRATION_MINUTES = 60;
    private static final String NOT_AWAITING_BANK_CONFIRMATION_MESSAGE =
            "La orden %s no está en estado AWAITING_BANK_CONFIRMATION (actual: %s)";

    private final String enrollmentId;
    private final String teamId;
    private final String tournamentId;
    private final BigDecimal amount;
    private final String idempotencyKey;
    private final LocalDateTime expiresAt;

    private PaymentOrderStatus status;
    private String mpPaymentId;
    private String externalResourceUrl;
    private Payer payer;
    private Long version;

    private PaymentOrder(Builder builder) {
        super(builder.paymentOrderId);
        this.enrollmentId = requireNonBlank(builder.enrollmentId, "enrollmentId");
        this.teamId = requireNonBlank(builder.teamId, "teamId");
        this.tournamentId = requireNonBlank(builder.tournamentId, "tournamentId");
        this.amount = requirePositive(builder.amount);
        this.status = builder.status;
        this.mpPaymentId = builder.mpPaymentId;
        this.idempotencyKey = requireNonBlank(builder.idempotencyKey, "idempotencyKey");
        this.externalResourceUrl = builder.externalResourceUrl;
        this.payer = builder.payer;
        this.expiresAt = builder.expiresAt;
        this.version = builder.version;
    }

    public static PaymentOrder create(String enrollmentId, String teamId, String tournamentId, BigDecimal amount) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        return builder()
                .paymentOrderId(UUID.randomUUID())
                .enrollmentId(enrollmentId)
                .teamId(teamId)
                .tournamentId(tournamentId)
                .amount(amount)
                .status(PaymentOrderStatus.PENDING)
                .idempotencyKey(UUID.randomUUID().toString())
                .expiresAt(now.plusMinutes(DEFAULT_EXPIRATION_MINUTES))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void startPseTransaction(Payer payer, String financialInstitution) {
        if (status != PaymentOrderStatus.PENDING) {
            throw new PaymentOrderNotPendingException(
                    "La orden %s no está en estado PENDING (actual: %s)".formatted(getId(), status));
        }
        if (payer == null) {
            throw new IllegalArgumentException("El pagador es obligatorio para iniciar una transacción PSE");
        }
        if (financialInstitution == null || financialInstitution.isBlank()) {
            throw new IllegalArgumentException("La institución financiera es obligatoria para iniciar una transacción PSE");
        }
        this.payer = payer;
        this.status = PaymentOrderStatus.AWAITING_BANK_CONFIRMATION;
    }

    /**
     * Attaches Mercado Pago's own reference for this transaction once the PSE
     * request has been accepted. Kept separate from startPseTransaction because
     * this data only exists after the gateway call succeeds, and the webhook
     * flow (TC-PAY-03) needs mpPaymentId already indexed to look the order up.
     */
    public void assignGatewayReference(String mpPaymentId, String externalResourceUrl) {
        if (status != PaymentOrderStatus.AWAITING_BANK_CONFIRMATION) {
            throw new PaymentOrderNotAwaitingBankConfirmationException(
                    NOT_AWAITING_BANK_CONFIRMATION_MESSAGE.formatted(getId(), status));
        }
        this.mpPaymentId = mpPaymentId;
        this.externalResourceUrl = externalResourceUrl;
    }

    public void approve(String mpPaymentId) {
        if (status != PaymentOrderStatus.AWAITING_BANK_CONFIRMATION) {
            throw new PaymentOrderNotAwaitingBankConfirmationException(
                    NOT_AWAITING_BANK_CONFIRMATION_MESSAGE.formatted(getId(), status));
        }
        this.mpPaymentId = mpPaymentId;
        this.status = PaymentOrderStatus.APPROVED;
    }

    public void reject() {
        if (status != PaymentOrderStatus.AWAITING_BANK_CONFIRMATION) {
            throw new PaymentOrderNotAwaitingBankConfirmationException(
                    NOT_AWAITING_BANK_CONFIRMATION_MESSAGE.formatted(getId(), status));
        }
        this.status = PaymentOrderStatus.REJECTED;
    }

    /**
     * Idempotent: expiring an order that's already in a final state is a no-op,
     * not an error, since both the webhook and the expiration job may race to
     * resolve the same order.
     */
    public void expire() {
        if (status == PaymentOrderStatus.PENDING || status == PaymentOrderStatus.AWAITING_BANK_CONFIRMATION) {
            this.status = PaymentOrderStatus.EXPIRED;
        }
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getTournamentId() {
        return tournamentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentOrderStatus getStatus() {
        return status;
    }

    public String getMpPaymentId() {
        return mpPaymentId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getExternalResourceUrl() {
        return externalResourceUrl;
    }

    public Payer getPayer() {
        return payer;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("%s es obligatorio".formatted(fieldName));
        }
        return value;
    }

    private static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        return amount;
    }

    public static final class Builder {
        private UUID paymentOrderId;
        private String enrollmentId;
        private String teamId;
        private String tournamentId;
        private BigDecimal amount;
        private PaymentOrderStatus status;
        private String mpPaymentId;
        private String idempotencyKey;
        private String externalResourceUrl;
        private Payer payer;
        private LocalDateTime expiresAt;
        private Long version;

        private Builder() {
        }

        public Builder paymentOrderId(UUID paymentOrderId) {
            this.paymentOrderId = paymentOrderId;
            return this;
        }

        public Builder enrollmentId(String enrollmentId) {
            this.enrollmentId = enrollmentId;
            return this;
        }

        public Builder teamId(String teamId) {
            this.teamId = teamId;
            return this;
        }

        public Builder tournamentId(String tournamentId) {
            this.tournamentId = tournamentId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder status(PaymentOrderStatus status) {
            this.status = status;
            return this;
        }

        public Builder mpPaymentId(String mpPaymentId) {
            this.mpPaymentId = mpPaymentId;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder externalResourceUrl(String externalResourceUrl) {
            this.externalResourceUrl = externalResourceUrl;
            return this;
        }

        public Builder payer(Payer payer) {
            this.payer = payer;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public PaymentOrder build() {
            return new PaymentOrder(this);
        }
    }
}
