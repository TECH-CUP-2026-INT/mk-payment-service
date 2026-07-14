package co.edu.escuelaing.techcup.payment.service;

/**
 * EXPIRED is internal only: it is never serialized as-is by the public API,
 * it is mapped to REJECTED in the response DTO while staying EXPIRED in the
 * domain and database for auditing.
 */
public enum PaymentOrderStatus {
    PENDING,
    AWAITING_BANK_CONFIRMATION,
    APPROVED,
    REJECTED,
    EXPIRED
}
