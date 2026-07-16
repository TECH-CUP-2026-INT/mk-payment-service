package co.edu.escuelaing.techcup.payment.controller.impl;

import co.edu.escuelaing.techcup.payment.dto.request.CreatePaymentOrderRequest;
import co.edu.escuelaing.techcup.payment.dto.request.PaymentWebhookRequest;
import co.edu.escuelaing.techcup.payment.dto.request.SubmitPseTransactionRequest;
import co.edu.escuelaing.techcup.payment.mapper.PaymentOrderRestMapper;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.ports.CreatePaymentOrderUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.GetPaymentOrderStatusUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.ProcessPaymentWebhookUseCase;
import co.edu.escuelaing.techcup.payment.service.ports.SubmitPseTransactionUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentOrderControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private CreatePaymentOrderUseCase createPaymentOrderUseCase;
    private SubmitPseTransactionUseCase submitPseTransactionUseCase;
    private ProcessPaymentWebhookUseCase processPaymentWebhookUseCase;
    private GetPaymentOrderStatusUseCase getPaymentOrderStatusUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createPaymentOrderUseCase = mock(CreatePaymentOrderUseCase.class);
        submitPseTransactionUseCase = mock(SubmitPseTransactionUseCase.class);
        processPaymentWebhookUseCase = mock(ProcessPaymentWebhookUseCase.class);
        getPaymentOrderStatusUseCase = mock(GetPaymentOrderStatusUseCase.class);
        PaymentOrderRestMapper mapper = Mappers.getMapper(PaymentOrderRestMapper.class);
        PaymentOrderController controller = new PaymentOrderController(
                createPaymentOrderUseCase, submitPseTransactionUseCase,
                processPaymentWebhookUseCase, getPaymentOrderStatusUseCase, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static PaymentOrder anOrder() {
        return PaymentOrder.reconstruct(UUID.randomUUID(), "enr-1", "team-1", "tournament-1",
                new BigDecimal("50000"), PaymentOrderStatus.PENDING, null, UUID.randomUUID().toString(),
                null, null, LocalDateTime.now().plusMinutes(60), null);
    }

    @Test
    @DisplayName("POST /payment-orders crea la orden y devuelve 201")
    void createsOrder() throws Exception {
        when(createPaymentOrderUseCase.create("enr-1", "team-1", "tournament-1", new BigDecimal("50000")))
                .thenReturn(anOrder());
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(
                "enr-1", "team-1", "tournament-1", new BigDecimal("50000"));

        mockMvc.perform(post("/payment-orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private static SubmitPseTransactionRequest aPseRequest(String entityType) {
        return new SubmitPseTransactionRequest("1007", "payer@test.com", "CC", "123456", entityType,
                "Juan", "Pérez", "11001", "Calle 1", "123", "Centro", "Bogotá", "601", "12345");
    }

    @Test
    @DisplayName("POST /payment-orders/{enrollmentId}/pse envía la transacción y devuelve 200")
    void submitsPseTransaction() throws Exception {
        when(submitPseTransactionUseCase.submit(eq("enr-1"), any(Payer.class), eq("1007"), any()))
                .thenReturn(anOrder());
        SubmitPseTransactionRequest request = aPseRequest("individual");

        mockMvc.perform(post("/payment-orders/enr-1/pse")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /payment-orders/{enrollmentId}/pse usa el primer IP de X-Forwarded-For como ipAddress")
    void submitsPseTransactionHonoringForwardedFor() throws Exception {
        when(submitPseTransactionUseCase.submit(eq("enr-1"), any(Payer.class), eq("1007"), eq("203.0.113.5")))
                .thenReturn(anOrder());
        SubmitPseTransactionRequest request = aPseRequest("individual");

        mockMvc.perform(post("/payment-orders/enr-1/pse")
                        .header("X-Forwarded-For", "203.0.113.5, 10.0.0.1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(submitPseTransactionUseCase).submit(eq("enr-1"), any(Payer.class), eq("1007"), eq("203.0.113.5"));
    }

    @Test
    @DisplayName("POST /payment-orders/{enrollmentId}/pse rechaza entityType fuera del catálogo permitido")
    void rejectsInvalidEntityType() throws Exception {
        SubmitPseTransactionRequest request = aPseRequest("company");

        mockMvc.perform(post("/payment-orders/enr-1/pse")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(submitPseTransactionUseCase, never()).submit(any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /payment-orders/{enrollmentId}/pse rechaza entityType en blanco")
    void rejectsBlankEntityType() throws Exception {
        SubmitPseTransactionRequest request = aPseRequest(" ");

        mockMvc.perform(post("/payment-orders/enr-1/pse")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(submitPseTransactionUseCase, never()).submit(any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /payment-orders/webhook procesa la notificación cuando trae data.id")
    void processesWebhookWithId() throws Exception {
        PaymentWebhookRequest request = new PaymentWebhookRequest("payment.updated", "payment",
                new PaymentWebhookRequest.WebhookData("mp-1"));

        mockMvc.perform(post("/payment-orders/webhook")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(processPaymentWebhookUseCase).process("mp-1");
    }

    @Test
    @DisplayName("POST /payment-orders/webhook ignora la notificación cuando no trae data.id")
    void ignoresWebhookWithoutId() throws Exception {
        PaymentWebhookRequest request = new PaymentWebhookRequest("payment.updated", "payment", null);

        mockMvc.perform(post("/payment-orders/webhook")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(processPaymentWebhookUseCase, never()).process(any());
    }

    @Test
    @DisplayName("GET /payment-orders/{enrollmentId} devuelve el estado de la orden")
    void getsStatus() throws Exception {
        when(getPaymentOrderStatusUseCase.getByEnrollmentId("enr-1")).thenReturn(anOrder());

        mockMvc.perform(get("/payment-orders/enr-1"))
                .andExpect(status().isOk());
    }
}
