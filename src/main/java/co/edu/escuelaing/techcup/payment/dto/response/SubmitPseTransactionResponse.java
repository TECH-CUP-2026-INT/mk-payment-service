package co.edu.escuelaing.techcup.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of submitting a PSE transaction to Mercado Pago.")
public record SubmitPseTransactionResponse(
        @Schema(description = "Payment order status after the transaction was accepted by Mercado Pago.",
                example = "AWAITING_BANK_CONFIRMATION")
        String status,

        @Schema(description = "URL the payer's browser must be redirected to in order to complete the bank authentication.",
                example = "https://www.mercadopago.com/pse/ticket/1234567890")
        String externalResourceUrl) {
}
