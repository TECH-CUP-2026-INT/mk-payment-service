package co.edu.escuelaing.techcup.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of submitting a payment via the Payment Brick.")
public record SubmitBrickPaymentResponse(

        @Schema(description = "Payment order status after the transaction was accepted.", example = "AWAITING_BANK_CONFIRMATION")
        String status,

        @Schema(description = "PSE redirect URL (only present for PSE payments).",
                example = "https://www.mercadopago.com/pse/ticket/1234567890")
        String externalResourceUrl) {
}
