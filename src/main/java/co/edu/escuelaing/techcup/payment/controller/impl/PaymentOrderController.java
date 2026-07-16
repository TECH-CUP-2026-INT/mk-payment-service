package co.edu.escuelaing.techcup.payment.controller.impl;

import co.edu.escuelaing.techcup.payment.dto.request.CreatePaymentOrderRequest;
import co.edu.escuelaing.techcup.payment.dto.request.PaymentWebhookRequest;
import co.edu.escuelaing.techcup.payment.dto.request.SubmitPseTransactionRequest;
import co.edu.escuelaing.techcup.payment.dto.response.CreatePaymentOrderResponse;
import co.edu.escuelaing.techcup.payment.dto.response.ErrorResponse;
import co.edu.escuelaing.techcup.payment.dto.response.PaymentOrderStatusResponse;
import co.edu.escuelaing.techcup.payment.dto.response.SubmitPseTransactionResponse;
import co.edu.escuelaing.techcup.payment.mapper.PaymentOrderRestMapper;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.ports.CreatePaymentOrderUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.GetPaymentOrderStatusUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.ProcessPaymentWebhookUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.SubmitPseTransactionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment-orders")
@Tag(name = "Payment Orders", description = "Create payment orders, submit PSE transactions, receive Mercado Pago webhooks, and query payment order status.")
public class PaymentOrderController {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrderController.class);

    private final CreatePaymentOrderUseCase createPaymentOrderUseCase;
    private final SubmitPseTransactionUseCase submitPseTransactionUseCase;
    private final ProcessPaymentWebhookUseCase processPaymentWebhookUseCase;
    private final GetPaymentOrderStatusUseCase getPaymentOrderStatusUseCase;
    private final PaymentOrderRestMapper mapper;

    public PaymentOrderController(CreatePaymentOrderUseCase createPaymentOrderUseCase,
            SubmitPseTransactionUseCase submitPseTransactionUseCase,
            ProcessPaymentWebhookUseCase processPaymentWebhookUseCase,
            GetPaymentOrderStatusUseCase getPaymentOrderStatusUseCase,
            PaymentOrderRestMapper mapper) {
        this.createPaymentOrderUseCase = createPaymentOrderUseCase;
        this.submitPseTransactionUseCase = submitPseTransactionUseCase;
        this.processPaymentWebhookUseCase = processPaymentWebhookUseCase;
        this.getPaymentOrderStatusUseCase = getPaymentOrderStatusUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Create a payment order",
            description = "Opens a new payment order in PENDING status for a tournament enrollment. "
                    + "There can only be one order per enrollmentId.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment order created.",
                    content = @Content(schema = @Schema(implementation = CreatePaymentOrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid body (missing fields, amount <= 0).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "A payment order already exists for this enrollmentId.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Amount is outside the range Mercado Pago allows for PSE.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CreatePaymentOrderResponse> create(@Valid @RequestBody CreatePaymentOrderRequest request) {
        PaymentOrder paymentOrder = createPaymentOrderUseCase.create(
                request.enrollmentId(), request.teamId(), request.tournamentId(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toCreateResponse(paymentOrder));
    }

    @Operation(summary = "Submit a PSE transaction",
            description = "Called from the frontend once the Mercado Pago Payment Brick's onSubmit callback fires "
                    + "for PSE, combined with payer details collected in a manual form (the Brick alone does not "
                    + "collect payer name/address/phone). Creates the actual PSE payment in Mercado Pago and moves "
                    + "the order to AWAITING_BANK_CONFIRMATION.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction accepted by Mercado Pago; redirect the payer to externalResourceUrl.",
                    content = @Content(schema = @Schema(implementation = SubmitPseTransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid body (missing/malformed payer fields).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "No payment order exists for this enrollmentId.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The payment order is not in PENDING status.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "The payment order already expired.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Mercado Pago rejected the request; the order stays PENDING for retry.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{enrollmentId}/pse")
    public ResponseEntity<SubmitPseTransactionResponse> submitPse(
            @Parameter(description = "enrollmentId of the pending order to submit PSE payment for.", example = "enr-12345")
            @PathVariable String enrollmentId,
            @Valid @RequestBody SubmitPseTransactionRequest request, HttpServletRequest httpRequest) {
        PaymentOrder paymentOrder = submitPseTransactionUseCase.submit(
                enrollmentId, mapper.toPayer(request), request.financialInstitution(), resolveClientIp(httpRequest));
        return ResponseEntity.ok(mapper.toSubmitPseResponse(paymentOrder));
    }

    /**
     * X-Forwarded-For may carry a comma-separated proxy chain; the first entry
     * is the original client. Falls back to the socket address when the
     * service is reached directly (e.g. local/dev, no reverse proxy in front).
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Operation(summary = "Receive a Mercado Pago webhook notification",
            description = "Called by Mercado Pago whenever a payment's status changes. Only data.id is used; the "
                    + "real status is always re-fetched from Mercado Pago's API, never trusted from this body. "
                    + "Always returns 204, even if data.id does not match any order, since Mercado Pago retries "
                    + "on any error status.")
    @ApiResponse(responseCode = "204", description = "Notification processed (or ignored) - Mercado Pago should not retry.")
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody PaymentWebhookRequest request) {
        String mpPaymentId = request.data() != null ? request.data().id() : null;
        if (mpPaymentId == null || mpPaymentId.isBlank()) {
            log.warn("Notificación de Mercado Pago sin data.id, se ignora");
            return ResponseEntity.noContent().build();
        }
        processPaymentWebhookUseCase.process(mpPaymentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a payment order's status",
            description = "Used by the frontend's Status Screen Brick (and by mk-tournament-service) to poll the "
                    + "current status of a payment order. EXPIRED is reported as REJECTED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current status of the payment order.",
                    content = @Content(schema = @Schema(implementation = PaymentOrderStatusResponse.class))),
            @ApiResponse(responseCode = "404", description = "No payment order exists for this enrollmentId.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{enrollmentId}")
    public ResponseEntity<PaymentOrderStatusResponse> getStatus(
            @Parameter(description = "enrollmentId whose payment order status is queried.", example = "enr-12345")
            @PathVariable String enrollmentId) {
        PaymentOrder paymentOrder = getPaymentOrderStatusUseCase.getByEnrollmentId(enrollmentId);
        return ResponseEntity.ok(mapper.toStatusResponse(paymentOrder));
    }
}
