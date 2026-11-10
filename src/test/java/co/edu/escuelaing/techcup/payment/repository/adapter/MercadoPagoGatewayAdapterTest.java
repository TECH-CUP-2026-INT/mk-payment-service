package co.edu.escuelaing.techcup.payment.repository.adapter;

import co.edu.escuelaing.techcup.payment.exception.PaymentGatewayException;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.ports.CardPayer;
import co.edu.escuelaing.techcup.payment.service.ports.CardTransactionResult;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodInfo;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentStatusResult;
import co.edu.escuelaing.techcup.payment.service.ports.PseTransactionResult;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.paymentmethod.PaymentMethodClient;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPResourceList;
import com.mercadopago.net.MPResponse;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentTransactionDetails;
import com.mercadopago.resources.paymentmethod.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MercadoPagoGatewayAdapterTest {

    private static final Payer PAYER = new Payer("payer@test.com", "CC", "123456789", "individual",
            "Juan", "Pérez", "11001", "Calle 1", "123", "Centro", "Bogotá", "601", "12345");

    private static final CardPayer CARD_PAYER = new CardPayer("payer@test.com", "CC", "123456789",
            "individual", "Juan", "Pérez", "11001", "Calle 1", "123", "Centro", "Bogotá", "601", "12345");

    private PaymentClient paymentClient;
    private PaymentMethodClient paymentMethodClient;
    private MercadoPagoGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        paymentClient = mock(PaymentClient.class);
        paymentMethodClient = mock(PaymentMethodClient.class);
        adapter = new MercadoPagoGatewayAdapter(paymentClient, paymentMethodClient,
                "test-token", 3000, 5000, false);
    }

    private static Payment payment(Long id, String status, String statusDetail, String externalResourceUrl) {
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(id);
        when(payment.getStatus()).thenReturn(status);
        when(payment.getStatusDetail()).thenReturn(statusDetail);
        if (externalResourceUrl != null) {
            PaymentTransactionDetails details = mock(PaymentTransactionDetails.class);
            when(details.getExternalResourceUrl()).thenReturn(externalResourceUrl);
            when(payment.getTransactionDetails()).thenReturn(details);
        }
        return payment;
    }

    private ArgumentCaptor<PaymentCreateRequest> capturePaymentRequest() throws Exception {
        ArgumentCaptor<PaymentCreateRequest> captor = ArgumentCaptor.forClass(PaymentCreateRequest.class);
        verify(paymentClient).create(captor.capture(), any(MPRequestOptions.class));
        return captor;
    }

    @Test
    @DisplayName("createPseTransaction mapea el pago PSE al request del SDK oficial")
    void createsPsePaymentWithMappedRequest() throws Exception {
        Payment response = payment(123456L, "pending", null, "https://mp.test/ticket/1");
        when(paymentClient.create(any(PaymentCreateRequest.class), any(MPRequestOptions.class)))
                .thenReturn(response);

        PseTransactionResult result = adapter.createPseTransaction("idem-1", new BigDecimal("50000"), "1007",
                PAYER, "200.10.20.30", "https://frontend.test/checkout/pse-return",
                "https://backend.test/payment-orders/webhook", "enr-1");

        PaymentCreateRequest request = capturePaymentRequest().getValue();
        assertThat(request.getTransactionAmount()).isEqualByComparingTo("50000");
        assertThat(request.getPaymentMethodId()).isEqualTo("pse");
        assertThat(request.getCallbackUrl()).isEqualTo("https://frontend.test/checkout/pse-return");
        assertThat(request.getNotificationUrl()).isEqualTo("https://backend.test/payment-orders/webhook");
        assertThat(request.getExternalReference()).isEqualTo("enr-1");
        assertThat(request.getTransactionDetails().getFinancialInstitution()).isEqualTo("1007");
        assertThat(request.getAdditionalInfo().getIpAddress()).isEqualTo("200.10.20.30");
        assertThat(request.getPayer().getEmail()).isEqualTo("payer@test.com");
        assertThat(request.getPayer().getEntityType()).isEqualTo("individual");
        assertThat(request.getPayer().getIdentification().getType()).isEqualTo("CC");
        assertThat(request.getPayer().getIdentification().getNumber()).isEqualTo("123456789");
        assertThat(request.getPayer().getFirstName()).isEqualTo("Juan");
        assertThat(request.getPayer().getLastName()).isEqualTo("Pérez");
        assertThat(request.getPayer().getAddress().getZipCode()).isEqualTo("11001");
        assertThat(request.getPayer().getAddress().getStreetName()).isEqualTo("Calle 1");
        assertThat(request.getPayer().getAddress().getStreetNumber()).isEqualTo("123");
        assertThat(request.getPayer().getAddress().getNeighborhood()).isEqualTo("Centro");
        assertThat(request.getPayer().getAddress().getCity()).isEqualTo("Bogotá");
        assertThat(request.getPayer().getAddress().getFederalUnit()).isEqualTo("1");
        assertThat(request.getPayer().getPhone().getAreaCode()).isEqualTo("601");
        assertThat(request.getPayer().getPhone().getNumber()).isEqualTo("12345");

        assertThat(result.mpPaymentId()).isEqualTo("123456");
        assertThat(result.status()).isEqualTo("pending");
        assertThat(result.externalResourceUrl()).isEqualTo("https://mp.test/ticket/1");
    }

    @Test
    @DisplayName("createPseTransaction usa el access token y el idempotency key por request")
    void sendsAccessTokenAndIdempotencyKeyPerRequest() throws Exception {
        Payment response = payment(1L, "pending", null, null);
        when(paymentClient.create(any(PaymentCreateRequest.class), any(MPRequestOptions.class)))
                .thenReturn(response);

        adapter.createPseTransaction("idem-1", new BigDecimal("50000"), "1007", PAYER,
                "200.10.20.30", "https://frontend.test/checkout/pse-return",
                "https://backend.test/payment-orders/webhook", "enr-1");

        ArgumentCaptor<MPRequestOptions> optionsCaptor = ArgumentCaptor.forClass(MPRequestOptions.class);
        verify(paymentClient).create(any(PaymentCreateRequest.class), optionsCaptor.capture());

        MPRequestOptions options = optionsCaptor.getValue();
        assertThat(options.getAccessToken()).isEqualTo("test-token");
        assertThat(options.getConnectionTimeout()).isEqualTo(3000);
        assertThat(options.getSocketTimeout()).isEqualTo(5000);
        assertThat(options.getCustomHeaders()).containsEntry("X-Idempotency-Key", "idem-1");
        assertThat(options.getCustomHeaders()).doesNotContainKey("x-test-token");
    }

    @Test
    @DisplayName("createPseTransaction devuelve externalResourceUrl nulo cuando MP no lo incluye")
    void createsPsePaymentWithoutRedirectUrl() throws Exception {
        Payment response = payment(789L, "pending", null, null);
        when(paymentClient.create(any(PaymentCreateRequest.class), any(MPRequestOptions.class)))
                .thenReturn(response);

        PseTransactionResult result = adapter.createPseTransaction("idem-3", new BigDecimal("30000"), "1007",
                PAYER, "200.10.20.30", "https://frontend.test/checkout/pse-return",
                "https://backend.test/payment-orders/webhook", "enr-1");

        assertThat(result.mpPaymentId()).isEqualTo("789");
        assertThat(result.status()).isEqualTo("pending");
        assertThat(result.externalResourceUrl()).isNull();
    }

    @Test
    @DisplayName("createCardTransaction envía external_reference y los datos completos del pagador")
    void createsCardPaymentWithExternalReferenceAndPayerData() throws Exception {
        Payment response = payment(555L, "approved", "accredited", null);
        when(paymentClient.create(any(PaymentCreateRequest.class), any(MPRequestOptions.class)))
                .thenReturn(response);

        CardTransactionResult result = adapter.createCardTransaction("idem-card", new BigDecimal("50000"),
                "tok-visa", "visa", 1, CARD_PAYER, "https://backend.test/payment-orders/webhook", "enr-1");

        PaymentCreateRequest request = capturePaymentRequest().getValue();
        assertThat(request.getTransactionAmount()).isEqualByComparingTo("50000");
        assertThat(request.getToken()).isEqualTo("tok-visa");
        assertThat(request.getPaymentMethodId()).isEqualTo("visa");
        assertThat(request.getInstallments()).isEqualTo(1);
        assertThat(request.getNotificationUrl()).isEqualTo("https://backend.test/payment-orders/webhook");
        assertThat(request.getExternalReference()).isEqualTo("enr-1");
        assertThat(request.getPayer().getEmail()).isEqualTo("payer@test.com");
        assertThat(request.getPayer().getIdentification().getType()).isEqualTo("CC");
        assertThat(request.getPayer().getIdentification().getNumber()).isEqualTo("123456789");
        assertThat(request.getPayer().getEntityType()).isEqualTo("individual");
        assertThat(request.getPayer().getFirstName()).isEqualTo("Juan");
        assertThat(request.getPayer().getLastName()).isEqualTo("Pérez");
        assertThat(request.getPayer().getAddress().getZipCode()).isEqualTo("11001");
        assertThat(request.getPayer().getAddress().getStreetName()).isEqualTo("Calle 1");
        assertThat(request.getPayer().getAddress().getStreetNumber()).isEqualTo("123");
        assertThat(request.getPayer().getAddress().getNeighborhood()).isEqualTo("Centro");
        assertThat(request.getPayer().getAddress().getCity()).isEqualTo("Bogotá");
        assertThat(request.getPayer().getPhone().getAreaCode()).isEqualTo("601");
        assertThat(request.getPayer().getPhone().getNumber()).isEqualTo("12345");

        assertThat(result.mpPaymentId()).isEqualTo("555");
        assertThat(result.status()).isEqualTo("approved");
        assertThat(result.statusDetail()).isEqualTo("accredited");
    }

    @Test
    @DisplayName("createCardTransaction omite los datos del pagador que no vienen informados")
    void omitsMissingCardPayerData() throws Exception {
        Payment response = payment(556L, "approved", "accredited", null);
        when(paymentClient.create(any(PaymentCreateRequest.class), any(MPRequestOptions.class)))
                .thenReturn(response);
        CardPayer partialPayer = new CardPayer("payer@test.com", null, null, null, null, null,
                null, null, null, null, null, null, null);

        adapter.createCardTransaction("idem-card-2", new BigDecimal("50000"),
                "tok-visa", "visa", 1, partialPayer, "https://backend.test/payment-orders/webhook", "enr-1");

        PaymentCreateRequest request = capturePaymentRequest().getValue();
        assertThat(request.getExternalReference()).isEqualTo("enr-1");
        assertThat(request.getPayer().getEmail()).isEqualTo("payer@test.com");
        assertThat(request.getPayer().getIdentification()).isNull();
        assertThat(request.getPayer().getAddress()).isNull();
        assertThat(request.getPayer().getPhone()).isNull();
    }

    @Test
    @DisplayName("getPaymentStatus consulta el pago por id y retorna el estado")
    void getPaymentStatusReturnsStatus() throws Exception {
        Payment response = payment(123456L, "approved", null, null);
        when(paymentClient.get(eq(123456L), any(MPRequestOptions.class)))
                .thenReturn(response);

        PaymentStatusResult result = adapter.getPaymentStatus("123456");

        assertThat(result.mpPaymentId()).isEqualTo("123456");
        assertThat(result.status()).isEqualTo("approved");
        verify(paymentClient).get(eq(123456L), any(MPRequestOptions.class));
    }

    @Test
    @DisplayName("getPaymentStatus envuelve un id inválido en PaymentGatewayException")
    void getPaymentStatusWrapsInvalidId() {
        assertThatThrownBy(() -> adapter.getPaymentStatus("not-a-number"))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("No se pudo consultar el estado del pago not-a-number")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getAvailablePaymentMethods mapea la lista de medios de pago del SDK")
    void getAvailablePaymentMethodsReturnsMappedList() throws Exception {
        PaymentMethod pse = mock(PaymentMethod.class);
        when(pse.getId()).thenReturn("pse");
        when(pse.getStatus()).thenReturn("active");
        when(pse.getMinAllowedAmount()).thenReturn(new BigDecimal("1000"));
        when(pse.getMaxAllowedAmount()).thenReturn(new BigDecimal("5000000"));
        MPResourceList<PaymentMethod> list = new MPResourceList<>();
        list.setResults(List.of(pse));
        when(paymentMethodClient.list(any(MPRequestOptions.class))).thenReturn(list);

        List<PaymentMethodInfo> result = adapter.getAvailablePaymentMethods();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("pse");
        assertThat(result.get(0).status()).isEqualTo("active");
        assertThat(result.get(0).minAllowedAmount()).isEqualByComparingTo("1000");
        assertThat(result.get(0).maxAllowedAmount()).isEqualByComparingTo("5000000");
    }

    @Test
    @DisplayName("getAvailablePaymentMethods retorna lista vacía cuando el SDK no trae resultados")
    void getAvailablePaymentMethodsReturnsEmptyListWhenResultsAreNull() throws Exception {
        when(paymentMethodClient.list(any(MPRequestOptions.class))).thenReturn(new MPResourceList<>());

        assertThat(adapter.getAvailablePaymentMethods()).isEmpty();
    }

    @Test
    @DisplayName("createPseTransaction envuelve un error de la API en PaymentGatewayException")
    void createPseTransactionWrapsApiError() throws Exception {
        MPResponse response = new MPResponse(400, Map.of(), "{\"message\": \"invalid payer\"}");
        when(paymentClient.create(any(PaymentCreateRequest.class), any(MPRequestOptions.class)))
                .thenThrow(new MPApiException("bad request", response));

        assertThatThrownBy(() -> adapter.createPseTransaction("idem-4", new BigDecimal("10000"), "1007",
                PAYER, "200.10.20.30", "https://frontend.test/checkout/pse-return",
                "https://backend.test/payment-orders/webhook", "enr-1"))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("No se pudo crear la transacción PSE en Mercado Pago")
                .hasCauseInstanceOf(MPApiException.class);
    }

    @Test
    @DisplayName("createCardTransaction envuelve un error del SDK en PaymentGatewayException")
    void createCardTransactionWrapsSdkError() throws Exception {
        when(paymentClient.create(any(PaymentCreateRequest.class), any(MPRequestOptions.class)))
                .thenThrow(new MPException("boom"));

        assertThatThrownBy(() -> adapter.createCardTransaction("idem-5", new BigDecimal("10000"),
                "tok-visa", "visa", 1, CARD_PAYER, "https://backend.test/payment-orders/webhook", "enr-1"))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("No se pudo crear el pago con tarjeta en Mercado Pago")
                .hasCauseInstanceOf(MPException.class);
    }

    @Test
    @DisplayName("getAvailablePaymentMethods envuelve un error de la API en PaymentGatewayException")
    void getAvailablePaymentMethodsWrapsApiError() throws Exception {
        when(paymentMethodClient.list(any(MPRequestOptions.class)))
                .thenThrow(new MPApiException("boom", new MPResponse(500, Map.of(), "{}")));

        assertThatThrownBy(adapter::getAvailablePaymentMethods)
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("No se pudieron consultar los medios de pago de Mercado Pago")
                .hasCauseInstanceOf(MPApiException.class);
    }
}
