package co.edu.escuelaing.techcup.payment.controller.impl;

import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.ports.GetPaymentMethodLimitsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentMethodControllerTest {

    private GetPaymentMethodLimitsUseCase getPaymentMethodLimitsUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getPaymentMethodLimitsUseCase = mock(GetPaymentMethodLimitsUseCase.class);
        PaymentMethodController controller = new PaymentMethodController(getPaymentMethodLimitsUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /payment-methods/limits indica si el monto está dentro de los límites de PSE")
    void getsLimits() throws Exception {
        when(getPaymentMethodLimitsUseCase.getPseLimits()).thenReturn(new PaymentMethodLimits(
                "pse", new BigDecimal("10000"), new BigDecimal("500000"), "active", LocalDateTime.now()));

        mockMvc.perform(get("/payment-methods/limits").param("amount", "50000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("GET /payment-methods/limits marca inválido un monto fuera de rango")
    void flagsAmountOutsideRange() throws Exception {
        when(getPaymentMethodLimitsUseCase.getPseLimits()).thenReturn(new PaymentMethodLimits(
                "pse", new BigDecimal("10000"), new BigDecimal("500000"), "active", LocalDateTime.now()));

        mockMvc.perform(get("/payment-methods/limits").param("amount", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
}
