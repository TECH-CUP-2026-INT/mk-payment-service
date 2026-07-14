package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.dto.response.CreatePaymentOrderResponse;
import co.edu.escuelaing.techcup.payment.dto.response.PaymentOrderStatusResponse;
import co.edu.escuelaing.techcup.payment.dto.response.SubmitPseTransactionResponse;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;

public final class PaymentOrderRestMapper {

    private PaymentOrderRestMapper() {
    }

    public static CreatePaymentOrderResponse toCreateResponse(PaymentOrder paymentOrder) {
        return new CreatePaymentOrderResponse(
                paymentOrder.getId(), paymentOrder.getStatus().name(), paymentOrder.getExpiresAt());
    }

    public static SubmitPseTransactionResponse toSubmitPseResponse(PaymentOrder paymentOrder) {
        return new SubmitPseTransactionResponse(paymentOrder.getStatus().name(), paymentOrder.getExternalResourceUrl());
    }

    /**
     * EXPIRED is an internal-only status: the public contract only knows
     * PENDING/AWAITING_BANK_CONFIRMATION/APPROVED/REJECTED, so it's mapped to
     * REJECTED here. The domain and database keep EXPIRED for auditing.
     */
    public static PaymentOrderStatusResponse toStatusResponse(PaymentOrder paymentOrder) {
        PaymentOrderStatus status = paymentOrder.getStatus() == PaymentOrderStatus.EXPIRED
                ? PaymentOrderStatus.REJECTED
                : paymentOrder.getStatus();
        return new PaymentOrderStatusResponse(status.name());
    }
}
