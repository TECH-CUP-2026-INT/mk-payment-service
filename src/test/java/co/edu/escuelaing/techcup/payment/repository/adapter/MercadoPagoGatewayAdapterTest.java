package co.edu.escuelaing.techcup.payment.repository.adapter;

import co.edu.escuelaing.techcup.payment.exception.PaymentGatewayException;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodInfo;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentStatusResult;
import co.edu.escuelaing.techcup.payment.service.ports.PseTransactionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

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

    @Test
    @DisplayName("createPseTransaction sin transaction_details deja externalResourceUrl en null")
    void createPseTransactionWithoutTransactionDetails() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments"))
                .andRespond(withSuccess("""
                        {"id": 789, "status": "pending"}
                        """, MediaType.APPLICATION_JSON));

        MercadoPagoGatewayAdapter adapter = buildAdapter();

        PseTransactionResult result = adapter.createPseTransaction("idem-3", new BigDecimal("30000"), "1007", PAYER,
                "200.10.20.30", "https://frontend.test/checkout/pse-return", "https://backend.test/payment-orders/webhook");

        assertThat(result.mpPaymentId()).isEqualTo("789");
        assertThat(result.status()).isEqualTo("pending");
        assertThat(result.externalResourceUrl()).isNull();
        server.verify();
    }

    @Test
    @DisplayName("createPseTransaction envuelve una respuesta de error de Mercado Pago en PaymentGatewayException")
    void createPseTransactionWrapsErrorResponse() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"message\": \"invalid payer\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        MercadoPagoGatewayAdapter adapter = buildAdapter();

        assertThatThrownBy(() -> adapter.createPseTransaction("idem-4", new BigDecimal("10000"), "1007", PAYER,
                "200.10.20.30", "https://frontend.test/checkout/pse-return", "https://backend.test/payment-orders/webhook"))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("No se pudo crear la transacción PSE en Mercado Pago");
        server.verify();
    }

    @Test
    @DisplayName("createPseTransaction envuelve un error inesperado (JSON inválido) en PaymentGatewayException")
    void createPseTransactionWrapsUnexpectedError() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        MercadoPagoGatewayAdapter adapter = buildAdapter();

        assertThatThrownBy(() -> adapter.createPseTransaction("idem-5", new BigDecimal("10000"), "1007", PAYER,
                "200.10.20.30", "https://frontend.test/checkout/pse-return", "https://backend.test/payment-orders/webhook"))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("No se pudo crear la transacción PSE en Mercado Pago");
        server.verify();
    }

    @Test
    @DisplayName("getPaymentStatus retorna el estado consultado a Mercado Pago")
    void getPaymentStatusReturnsStatus() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/123"))
                .andRespond(withSuccess("""
                        {"id": 123, "status": "approved"}
                        """, MediaType.APPLICATION_JSON));

        MercadoPagoGatewayAdapter adapter = buildAdapter();

        PaymentStatusResult result = adapter.getPaymentStatus("123");

        assertThat(result.mpPaymentId()).isEqualTo("123");
        assertThat(result.status()).isEqualTo("approved");
        server.verify();
    }

    @Test
    @DisplayName("getPaymentStatus envuelve una respuesta de error de Mercado Pago en PaymentGatewayException")
    void getPaymentStatusWrapsErrorResponse() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/999"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .body("{\"message\": \"not found\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        MercadoPagoGatewayAdapter adapter = buildAdapter();

        assertThatThrownBy(() -> adapter.getPaymentStatus("999"))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("No se pudo consultar el estado del pago 999 en Mercado Pago");
        server.verify();
    }

    @Test
    @DisplayName("getPaymentStatus envuelve un error inesperado en PaymentGatewayException")
    void getPaymentStatusWrapsUnexpectedError() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/123"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        MercadoPagoGatewayAdapter adapter = buildAdapter();

        assertThatThrownBy(() -> adapter.getPaymentStatus("123"))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("No se pudo consultar el estado del pago 123 en Mercado Pago");
        server.verify();
    }

    @Test
    @DisplayName("getAvailablePaymentMethods mapea la lista de medios de pago de Mercado Pago")
    void getAvailablePaymentMethodsReturnsMappedList() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payment_methods"))
                .andRespond(withSuccess("""
                        [{"id": "pse", "status": "active", "min_allowed_amount": 1000, "max_allowed_amount": 5000000}]
                        """, MediaType.APPLICATION_JSON));

        MercadoPagoGatewayAdapter adapter = buildAdapter();

        List<PaymentMethodInfo> result = adapter.getAvailablePaymentMethods();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("pse");
        assertThat(result.get(0).status()).isEqualTo("active");
        assertThat(result.get(0).minAllowedAmount()).isEqualByComparingTo("1000");
        assertThat(result.get(0).maxAllowedAmount()).isEqualByComparingTo("5000000");
        server.verify();
    }

    @Test
    @DisplayName("getAvailablePaymentMethods retorna lista vacía cuando Mercado Pago responde sin cuerpo")
    void getAvailablePaymentMethodsReturnsEmptyListWhenBodyIsNull() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payment_methods"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        MercadoPagoGatewayAdapter adapter = buildAdapter();

        List<PaymentMethodInfo> result = adapter.getAvailablePaymentMethods();

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("getAvailablePaymentMethods envuelve una respuesta de error de Mercado Pago en PaymentGatewayException")
    void getAvailablePaymentMethodsWrapsErrorResponse() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payment_methods"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"message\": \"boom\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        MercadoPagoGatewayAdapter adapter = buildAdapter();

        assertThatThrownBy(adapter::getAvailablePaymentMethods)
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("No se pudieron consultar los medios de pago de Mercado Pago");
        server.verify();
    }

    @Test
    @DisplayName("getAvailablePaymentMethods envuelve un error inesperado en PaymentGatewayException")
    void getAvailablePaymentMethodsWrapsUnexpectedError() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payment_methods"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        MercadoPagoGatewayAdapter adapter = buildAdapter();

        assertThatThrownBy(adapter::getAvailablePaymentMethods)
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("No se pudieron consultar los medios de pago de Mercado Pago");
        server.verify();
    }
}
