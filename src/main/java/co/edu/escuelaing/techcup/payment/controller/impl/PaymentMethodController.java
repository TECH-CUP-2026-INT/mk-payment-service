package co.edu.escuelaing.techcup.payment.controller.impl;

import co.edu.escuelaing.techcup.payment.dto.response.PaymentMethodLimitsResponse;
import co.edu.escuelaing.techcup.payment.mapper.PaymentMethodLimitsRestMapper;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.ports.GetPaymentMethodLimitsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/payment-methods")
public class PaymentMethodController {

    private final GetPaymentMethodLimitsUseCase getPaymentMethodLimitsUseCase;
    private final PaymentMethodLimitsRestMapper mapper;

    public PaymentMethodController(GetPaymentMethodLimitsUseCase getPaymentMethodLimitsUseCase,
            PaymentMethodLimitsRestMapper mapper) {
        this.getPaymentMethodLimitsUseCase = getPaymentMethodLimitsUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/limits")
    public ResponseEntity<PaymentMethodLimitsResponse> getLimits(@RequestParam BigDecimal amount) {
        PaymentMethodLimits limits = getPaymentMethodLimitsUseCase.getPseLimits();
        return ResponseEntity.ok(mapper.toLimitsResponse(limits, amount));
    }
}
