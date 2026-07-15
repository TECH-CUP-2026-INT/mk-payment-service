package co.edu.escuelaing.techcup.payment.controller.impl;

import co.edu.escuelaing.techcup.payment.dto.request.CreatePaymentOrderRequest;
import co.edu.escuelaing.techcup.payment.dto.request.PaymentWebhookRequest;
import co.edu.escuelaing.techcup.payment.dto.request.SubmitPseTransactionRequest;
import co.edu.escuelaing.techcup.payment.dto.response.CreatePaymentOrderResponse;
import co.edu.escuelaing.techcup.payment.dto.response.PaymentOrderStatusResponse;
import co.edu.escuelaing.techcup.payment.dto.response.SubmitPseTransactionResponse;
import co.edu.escuelaing.techcup.payment.mapper.PaymentOrderRestMapper;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.ports.CreatePaymentOrderUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.GetPaymentOrderStatusUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.ProcessPaymentWebhookUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.SubmitPseTransactionUseCase;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment-orders")
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

    @PostMapping
    public ResponseEntity<CreatePaymentOrderResponse> create(@Valid @RequestBody CreatePaymentOrderRequest request) {
        PaymentOrder paymentOrder = createPaymentOrderUseCase.create(
                request.enrollmentId(), request.teamId(), request.tournamentId(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toCreateResponse(paymentOrder));
    }

    @PostMapping("/{enrollmentId}/pse")
    public ResponseEntity<SubmitPseTransactionResponse> submitPse(@PathVariable String enrollmentId,
            @Valid @RequestBody SubmitPseTransactionRequest request) {
        PaymentOrder paymentOrder = submitPseTransactionUseCase.submit(
                enrollmentId, mapper.toPayer(request), request.financialInstitution());
        return ResponseEntity.ok(mapper.toSubmitPseResponse(paymentOrder));
    }

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

    @GetMapping("/{enrollmentId}")
    public ResponseEntity<PaymentOrderStatusResponse> getStatus(@PathVariable String enrollmentId) {
        PaymentOrder paymentOrder = getPaymentOrderStatusUseCase.getByEnrollmentId(enrollmentId);
        return ResponseEntity.ok(mapper.toStatusResponse(paymentOrder));
    }
}
