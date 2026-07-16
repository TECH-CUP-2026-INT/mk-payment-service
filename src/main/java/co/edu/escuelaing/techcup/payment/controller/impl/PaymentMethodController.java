package co.edu.escuelaing.techcup.payment.controller.impl;

import co.edu.escuelaing.techcup.payment.dto.response.ErrorResponse;
import co.edu.escuelaing.techcup.payment.dto.response.PaymentMethodLimitsResponse;
import co.edu.escuelaing.techcup.payment.mapper.PaymentMethodLimitsRestMapper;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.ports.GetPaymentMethodLimitsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/payment-methods")
@Tag(name = "Payment Methods", description = "Query PSE amount limits synced from Mercado Pago.")
public class PaymentMethodController {

    private final GetPaymentMethodLimitsUseCase getPaymentMethodLimitsUseCase;
    private final PaymentMethodLimitsRestMapper mapper;

    public PaymentMethodController(GetPaymentMethodLimitsUseCase getPaymentMethodLimitsUseCase,
            PaymentMethodLimitsRestMapper mapper) {
        this.getPaymentMethodLimitsUseCase = getPaymentMethodLimitsUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Get PSE amount limits",
            description = "Returns the min/max amount Mercado Pago currently allows for PSE (synced daily by "
                    + "SyncPaymentMethodsJob) and whether the given amount falls within them.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PSE limits and validity of the queried amount.",
                    content = @Content(schema = @Schema(implementation = PaymentMethodLimitsResponse.class))),
            @ApiResponse(responseCode = "404", description = "PSE limits have not been synced yet.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/limits")
    public ResponseEntity<PaymentMethodLimitsResponse> getLimits(
            @Parameter(description = "Amount to validate against the current PSE limits, in COP.", example = "50000.00")
            @RequestParam BigDecimal amount) {
        PaymentMethodLimits limits = getPaymentMethodLimitsUseCase.getPseLimits();
        return ResponseEntity.ok(mapper.toLimitsResponse(limits, amount));
    }
}
