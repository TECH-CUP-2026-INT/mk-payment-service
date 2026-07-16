package co.edu.escuelaing.techcup.payment.repository.adapter;

import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.ports.PseTransactionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoGatewayAdapterTest {

    private static final Payer PAYER = new Payer("payer@test.com", "CC", "123456789", "individual",
            "Juan", "Pérez", "11001", "Calle 1", "123", "Centro", "Bogotá", "601", "12345");

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("https://api.mercadopago.com");
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private MercadoPagoGatewayAdapter buildAdapter() {
        return new MercadoPagoGatewayAdapter(builder.build(), "test-token");
    }

    @Test
    @DisplayName("createPseTransaction envía el payer completo, additional_info.ip_address y callback_url/notification_url distintos")
    void sendsFullPayerAndDistinctUrls() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments"))
                .andExpect(jsonPath("$.payment_method_id").value("pse"))
                .andExpect(jsonPath("$.transaction_amount").value(50000))
                .andExpect(jsonPath("$.payer.email").value("payer@test.com"))
                .andExpect(jsonPath("$.payer.entity_type").value("individual"))
                .andExpect(jsonPath("$.payer.identification.type").value("CC"))
                .andExpect(jsonPath("$.payer.identification.number").value("123456789"))
                .andExpect(jsonPath("$.payer.first_name").value("Juan"))
                .andExpect(jsonPath("$.payer.last_name").value("Pérez"))
                .andExpect(jsonPath("$.payer.address.zip_code").value("11001"))
                .andExpect(jsonPath("$.payer.address.street_name").value("Calle 1"))
                .andExpect(jsonPath("$.payer.address.street_number").value("123"))
                .andExpect(jsonPath("$.payer.address.neighborhood").value("Centro"))
                .andExpect(jsonPath("$.payer.address.city").value("Bogotá"))
                .andExpect(jsonPath("$.payer.phone.area_code").value("601"))
                .andExpect(jsonPath("$.payer.phone.number").value("12345"))
                .andExpect(jsonPath("$.transaction_details.financial_institution").value("1007"))
                .andExpect(jsonPath("$.additional_info.ip_address").value("200.10.20.30"))
                .andExpect(jsonPath("$.callback_url").value("https://frontend.test/checkout/pse-return"))
                .andExpect(jsonPath("$.notification_url").value("https://backend.test/payment-orders/webhook"))
                .andRespond(withSuccess("""
                        {"id": 123, "status": "pending", "transaction_details": {"external_resource_url": "https://mp.test/ticket/1"}}
                        """, MediaType.APPLICATION_JSON));

        MercadoPagoGatewayAdapter adapter = buildAdapter();

        PseTransactionResult result = adapter.createPseTransaction("idem-1", new BigDecimal("50000"), "1007", PAYER,
                "200.10.20.30", "https://frontend.test/checkout/pse-return", "https://backend.test/payment-orders/webhook");

        assertThat(result.mpPaymentId()).isEqualTo("123");
        assertThat(result.status()).isEqualTo("pending");
        server.verify();
    }

    @Test
    @DisplayName("callback_url y notification_url viajan como valores distintos, no el mismo dos veces")
    void callbackUrlAndNotificationUrlAreNotConflated() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments"))
                .andExpect(jsonPath("$.callback_url").value("https://frontend.test/checkout/pse-return"))
                .andExpect(jsonPath("$.notification_url").value("https://backend.test/payment-orders/webhook"))
                .andRespond(withSuccess("""
                        {"id": 456, "status": "pending", "transaction_details": {"external_resource_url": "https://mp.test/ticket/2"}}
                        """, MediaType.APPLICATION_JSON));

        MercadoPagoGatewayAdapter adapter = buildAdapter();

        adapter.createPseTransaction("idem-2", new BigDecimal("10000"), "1007", PAYER, "200.10.20.30",
                "https://frontend.test/checkout/pse-return", "https://backend.test/payment-orders/webhook");

        server.verify();
    }
}
