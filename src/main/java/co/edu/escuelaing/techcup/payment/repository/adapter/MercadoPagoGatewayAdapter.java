package co.edu.escuelaing.techcup.payment.repository.adapter;

import co.edu.escuelaing.techcup.payment.exception.PaymentGatewayException;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodId;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodInfo;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentStatusResult;
import co.edu.escuelaing.techcup.payment.service.ports.PseTransactionResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Adaptador de Mercado Pago basado en la <b>Orders API</b> (Checkout API vía
 * Orders), que reemplaza a la antigua Payments API (legacy).
 *
 * <p>Desde la migración, una transacción PSE se materializa creando una
 * <i>order</i> en {@code POST /v1/orders} en modo {@code automatic}, en lugar
 * del antiguo {@code POST /v1/payments}. Los cambios principales son:</p>
 *
 * <ul>
 *   <li>El método de pago se declara dentro de {@code transactions.payments[].payment_method}
 *       (con {@code id}, {@code type} y {@code financial_institution}).</li>
 *   <li>El {@code callback_url} se envía en {@code config.online.callback_url}.</li>
 *   <li>La IP del pagador va en {@code additional_info.payer.ip_address}.</li>
 *   <li>El {@code notification_url} ya no se envía por solicitud: se configura
 *       como webhook en el panel de Mercado Pago.</li>
 * </ul>
 *
 * @see <a href="https://www.mercadopago.com.co/developers/es/reference/online-payments/checkout-api/create-order/post">Referencia: crear order</a>
 */
@Component
public class MercadoPagoGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoGatewayAdapter.class);
    private static final String PSE_PAYMENT_METHOD_ID = PaymentMethodId.PSE;
    private static final String PSE_PAYMENT_TYPE = "bank_transfer";
    private static final String ORDER_TYPE = "online";
    private static final String PROCESSING_MODE_AUTOMATIC = "automatic";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient restClient;
    private final String accessToken;

    @Autowired
    public MercadoPagoGatewayAdapter(
            @Value("${mercadopago.base-url}") String baseUrl,
            @Value("${mercadopago.access-token}") String accessToken,
            @Value("${mercadopago.connect-timeout-millis}") int connectTimeoutMillis,
            @Value("${mercadopago.read-timeout-millis}") int readTimeoutMillis) {
        this.accessToken = accessToken;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMillis);
        requestFactory.setReadTimeout(readTimeoutMillis);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Test-only: lets MercadoPagoGatewayAdapterTest inject a RestClient bound
     * to MockRestServiceServer. Spring never sees this one — @Autowired above
     * pins the constructor it must use, since it can't disambiguate on its own
     * once a class has more than one constructor.
     */
    MercadoPagoGatewayAdapter(RestClient restClient, String accessToken) {
        this.restClient = restClient;
        this.accessToken = accessToken;
    }

    @Override
    public PseTransactionResult createPseTransaction(String idempotencyKey, BigDecimal amount,
            String financialInstitution, Payer payer, String ipAddress, String callbackUrl, String notificationUrl) {
        try {
            OrderApiResponse response = restClient.post()
                    .uri("/v1/orders")
                    .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .body(new CreateOrderApiRequest(
                            ORDER_TYPE,
                            new TransactionsApiRequest(List.of(new PaymentApiRequest(
                                    amount.toPlainString(),
                                    new PaymentMethodApiRequest(PSE_PAYMENT_METHOD_ID, PSE_PAYMENT_TYPE,
                                            financialInstitution)))),
                            new PayerApiRequest(payer.email(), payer.entityType(), payer.firstName(), payer.lastName(),
                                    new IdentificationApiRequest(payer.identificationType(), payer.identificationNumber()),
                                    new PhoneApiRequest(payer.phoneAreaCode(), payer.phoneNumber()),
                                    new AddressApiRequest(payer.addressZipCode(), payer.addressStreetName(),
                                            payer.addressStreetNumber(), payer.addressNeighborhood(), payer.addressCity())),
                            amount.toPlainString(),
                            PROCESSING_MODE_AUTOMATIC,
                            new ConfigApiRequest(new OnlineConfigApiRequest(callbackUrl)),
                            new AdditionalInfoApiRequest(new PayerAdditionalInfoApiRequest(ipAddress))))
                    .retrieve()
                    .body(OrderApiResponse.class);
            if (response == null) {
                throw new IllegalStateException("Respuesta vacía de Mercado Pago");
            }
            // La referencia de la order es el identificador que el dominio persiste
            // como mpPaymentId y que el webhook usa para localizar la orden.
            String orderId = response.id();
            String paymentStatus = paymentStatusOf(response);
            String redirectUrl = redirectUrlOf(response);
            return new PseTransactionResult(orderId, paymentStatus, redirectUrl);
        } catch (RestClientResponseException ex) {
            log.error("Mercado Pago respondió {} al crear la order PSE: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new PaymentGatewayException("No se pudo crear la transacción PSE en Mercado Pago", ex);
        } catch (Exception ex) {
            log.error("Error inesperado al crear la transacción PSE en Mercado Pago", ex);
            throw new PaymentGatewayException("No se pudo crear la transacción PSE en Mercado Pago", ex);
        }
    }

    @Override
    public PaymentStatusResult getPaymentStatus(String mpPaymentId) {
        try {
            OrderApiResponse response = restClient.get()
                    .uri("/v1/orders/{id}", mpPaymentId)
                    .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
                    .retrieve()
                    .body(OrderApiResponse.class);
            if (response == null) {
                throw new IllegalStateException("Respuesta vacía de Mercado Pago");
            }
            return new PaymentStatusResult(response.id(), paymentStatusOf(response));
        } catch (RestClientResponseException ex) {
            log.error("Mercado Pago respondió {} al consultar la order {}: {}",
                    ex.getStatusCode(), mpPaymentId, ex.getResponseBodyAsString(), ex);
            throw new PaymentGatewayException("No se pudo consultar el estado del pago %s en Mercado Pago".formatted(mpPaymentId), ex);
        } catch (Exception ex) {
            log.error("Error inesperado al consultar el estado del pago {} en Mercado Pago", mpPaymentId, ex);
            throw new PaymentGatewayException("No se pudo consultar el estado del pago %s en Mercado Pago".formatted(mpPaymentId), ex);
        }
    }

    @Override
    public List<PaymentMethodInfo> getAvailablePaymentMethods() {
        try {
            PaymentMethodApiResponse[] response = restClient.get()
                    .uri("/v1/payment_methods")
                    .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
                    .retrieve()
                    .body(PaymentMethodApiResponse[].class);
            if (response == null) {
                return List.of();
            }
            return List.of(response).stream()
                    .map(pm -> new PaymentMethodInfo(pm.id(), pm.status(), pm.minAllowedAmount(), pm.maxAllowedAmount()))
                    .toList();
        } catch (RestClientResponseException ex) {
            log.error("Mercado Pago respondió {} al consultar los medios de pago: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new PaymentGatewayException("No se pudieron consultar los medios de pago de Mercado Pago", ex);
        } catch (Exception ex) {
            log.error("Error inesperado al consultar los medios de pago de Mercado Pago", ex);
            throw new PaymentGatewayException("No se pudieron consultar los medios de pago de Mercado Pago", ex);
        }
    }

    private static String paymentStatusOf(OrderApiResponse response) {
        if (response.transactions() != null && response.transactions().payments() != null
                && !response.transactions().payments().isEmpty()) {
            return response.transactions().payments().get(0).status();
        }
        return response.status();
    }

    private static String redirectUrlOf(OrderApiResponse response) {
        if (response.transactions() != null && response.transactions().payments() != null
                && !response.transactions().payments().isEmpty()
                && response.transactions().payments().get(0).paymentMethod() != null) {
            return response.transactions().payments().get(0).paymentMethod().redirectUrl();
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // Request DTOs (Orders API)
    // ---------------------------------------------------------------------

    private record CreateOrderApiRequest(
            @JsonProperty("type") String type,
            @JsonProperty("transactions") TransactionsApiRequest transactions,
            @JsonProperty("payer") PayerApiRequest payer,
            @JsonProperty("total_amount") String totalAmount,
            @JsonProperty("processing_mode") String processingMode,
            @JsonProperty("config") ConfigApiRequest config,
            @JsonProperty("additional_info") AdditionalInfoApiRequest additionalInfo) {
    }

    private record TransactionsApiRequest(@JsonProperty("payments") List<PaymentApiRequest> payments) {
    }

    private record PaymentApiRequest(
            @JsonProperty("amount") String amount,
            @JsonProperty("payment_method") PaymentMethodApiRequest paymentMethod) {
    }

    private record PaymentMethodApiRequest(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("financial_institution") String financialInstitution) {
    }

    private record PayerApiRequest(
            @JsonProperty("email") String email,
            @JsonProperty("entity_type") String entityType,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            @JsonProperty("identification") IdentificationApiRequest identification,
            @JsonProperty("phone") PhoneApiRequest phone,
            @JsonProperty("address") AddressApiRequest address) {
    }

    private record IdentificationApiRequest(String type, String number) {
    }

    private record PhoneApiRequest(@JsonProperty("area_code") String areaCode, String number) {
    }

    private record AddressApiRequest(
            @JsonProperty("zip_code") String zipCode,
            @JsonProperty("street_name") String streetName,
            @JsonProperty("street_number") String streetNumber,
            String neighborhood,
            String city) {
    }

    private record ConfigApiRequest(@JsonProperty("online") OnlineConfigApiRequest online) {
    }

    private record OnlineConfigApiRequest(@JsonProperty("callback_url") String callbackUrl) {
    }

    private record AdditionalInfoApiRequest(@JsonProperty("payer") PayerAdditionalInfoApiRequest payer) {
    }

    private record PayerAdditionalInfoApiRequest(@JsonProperty("ip_address") String ipAddress) {
    }

    // ---------------------------------------------------------------------
    // Response DTOs (Orders API)
    // ---------------------------------------------------------------------

    private record OrderApiResponse(
            String id,
            String status,
            @JsonProperty("transactions") TransactionsApiResponse transactions) {
    }

    private record TransactionsApiResponse(@JsonProperty("payments") List<PaymentApiResponse> payments) {
    }

    private record PaymentApiResponse(
            String id,
            String status,
            @JsonProperty("payment_method") OrderPaymentMethodApiResponse paymentMethod) {
    }

    private record OrderPaymentMethodApiResponse(
            @JsonProperty("redirect_url") String redirectUrl) {
    }

    // ---------------------------------------------------------------------
    // Payment methods DTOs (unchanged endpoint /v1/payment_methods)
    // ---------------------------------------------------------------------

    private record PaymentMethodApiResponse(
            String id,
            String status,
            @JsonProperty("min_allowed_amount") BigDecimal minAllowedAmount,
            @JsonProperty("max_allowed_amount") BigDecimal maxAllowedAmount) {
    }
}
