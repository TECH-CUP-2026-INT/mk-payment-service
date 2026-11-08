package co.edu.escuelaing.techcup.payment.repository.adapter;

import co.edu.escuelaing.techcup.payment.exception.PaymentGatewayException;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodId;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.ports.CardTransactionResult;
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
 * Adaptador de Mercado Pago — Payments API ({@code POST /v1/payments}).
 *
 * <p>PSE: {@code payment_method_id: "pse"} + {@code transaction_details.financial_institution}
 * + datos completos del pagador. La respuesta incluye {@code external_resource_url} al que
 * se redirige al usuario para completar la autenticación bancaria.</p>
 *
 * <p>Tarjetas: {@code token} + {@code payment_method_id} + {@code installments}.</p>
 *
 * @see <a href="https://www.mercadopago.com.co/developers/es/docs/checkout-bricks/payment-brick/payment-submission/pse">PSE con Payment Brick</a>
 */
@Component
public class MercadoPagoGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoGatewayAdapter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient restClient;
    private final String accessToken;
    private final boolean testToken;

    @Autowired
    public MercadoPagoGatewayAdapter(
            @Value("${mercadopago.base-url}") String baseUrl,
            @Value("${mercadopago.access-token}") String accessToken,
            @Value("${mercadopago.connect-timeout-millis}") int connectTimeoutMillis,
            @Value("${mercadopago.read-timeout-millis}") int readTimeoutMillis,
            @Value("${mercadopago.test-token:false}") boolean testToken) {
        this.accessToken = accessToken;
        this.testToken = testToken;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMillis);
        requestFactory.setReadTimeout(readTimeoutMillis);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /** Test-only constructor — Spring uses the @Autowired one above. */
    MercadoPagoGatewayAdapter(RestClient restClient, String accessToken) {
        this.restClient = restClient;
        this.accessToken = accessToken;
        this.testToken = false;
    }

    @Override
    public PseTransactionResult createPseTransaction(String idempotencyKey, BigDecimal amount,
            String financialInstitution, Payer payer, String ipAddress, String callbackUrl, String notificationUrl) {
        log.info("[PSE] Creando pago — idempotencyKey={} financialInstitution={} testToken={}",
                idempotencyKey, financialInstitution, testToken);
        try {
            var spec = restClient.post()
                    .uri("/v1/payments")
                    .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
                    .header("X-Idempotency-Key", idempotencyKey);
            if (testToken) spec = spec.header("x-test-token", "true");
            log.info("[PSE] Headers salientes — X-Idempotency-Key={} x-test-token={}",
                    idempotencyKey, testToken);
            PsePaymentApiResponse response = spec
                    .body(new CreatePsePaymentApiRequest(
                            amount,
                            PaymentMethodId.PSE,
                            callbackUrl,
                            notificationUrl,
                            new PseTransactionDetailsApiRequest(financialInstitution),
                            new PseAdditionalInfoApiRequest(ipAddress),
                            new PsePayerApiRequest(
                                    payer.email(),
                                    payer.entityType(),
                                    payer.firstName(),
                                    payer.lastName(),
                                    new IdentificationApiRequest(payer.identificationType(), payer.identificationNumber()),
                                    new AddressApiRequest(payer.addressZipCode(), payer.addressStreetName(),
                                            payer.addressStreetNumber(), payer.addressNeighborhood(), payer.addressCity(), "1"),
                                    new PhoneApiRequest(payer.phoneAreaCode(), payer.phoneNumber()))))
                    .retrieve()
                    .body(PsePaymentApiResponse.class);
            if (response == null) {
                throw new IllegalStateException("Respuesta vacía de Mercado Pago");
            }
            return new PseTransactionResult(String.valueOf(response.id()), response.status(), response.externalResourceUrl());
        } catch (RestClientResponseException ex) {
            log.error("Mercado Pago respondió {} al crear el pago PSE: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new PaymentGatewayException("No se pudo crear la transacción PSE en Mercado Pago", ex);
        } catch (Exception ex) {
            log.error("Error inesperado al crear la transacción PSE en Mercado Pago", ex);
            throw new PaymentGatewayException("No se pudo crear la transacción PSE en Mercado Pago", ex);
        }
    }

    @Override
    public CardTransactionResult createCardTransaction(String idempotencyKey, BigDecimal amount,
            String token, String paymentMethodId, Integer installments, String payerEmail,
            String identificationType, String identificationNumber, String notificationUrl) {
        boolean hasIdentification = identificationType != null && !identificationType.isBlank()
                && identificationNumber != null && !identificationNumber.isBlank();
        try {
            CardPaymentApiResponse response = restClient.post()
                    .uri("/v1/payments")
                    .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .body(new CreateCardPaymentApiRequest(
                            amount,
                            token,
                            paymentMethodId,
                            installments != null ? installments : 1,
                            notificationUrl,
                            new CardPayerApiRequest(payerEmail,
                                    hasIdentification
                                            ? new IdentificationApiRequest(identificationType, identificationNumber)
                                            : null)))
                    .retrieve()
                    .body(CardPaymentApiResponse.class);
            if (response == null) {
                throw new IllegalStateException("Respuesta vacía de Mercado Pago");
            }
            return new CardTransactionResult(String.valueOf(response.id()), response.status(), response.statusDetail());
        } catch (RestClientResponseException ex) {
            log.error("Mercado Pago respondió {} al crear el pago con tarjeta: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new PaymentGatewayException("No se pudo crear el pago con tarjeta en Mercado Pago", ex);
        } catch (Exception ex) {
            log.error("Error inesperado al crear el pago con tarjeta en Mercado Pago", ex);
            throw new PaymentGatewayException("No se pudo crear el pago con tarjeta en Mercado Pago", ex);
        }
    }

    @Override
    public PaymentStatusResult getPaymentStatus(String mpPaymentId) {
        try {
            PaymentStatusApiResponse response = restClient.get()
                    .uri("/v1/payments/{id}", mpPaymentId)
                    .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
                    .retrieve()
                    .body(PaymentStatusApiResponse.class);
            if (response == null) {
                throw new IllegalStateException("Respuesta vacía de Mercado Pago");
            }
            return new PaymentStatusResult(String.valueOf(response.id()), response.status());
        } catch (RestClientResponseException ex) {
            log.error("Mercado Pago respondió {} al consultar el pago {}: {}",
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

    // -------------------------------------------------------------------------
    // Request DTOs — PSE (Payments API)
    // -------------------------------------------------------------------------

    private record CreatePsePaymentApiRequest(
            @JsonProperty("transaction_amount") BigDecimal transactionAmount,
            @JsonProperty("payment_method_id") String paymentMethodId,
            @JsonProperty("callback_url") String callbackUrl,
            @JsonProperty("notification_url") String notificationUrl,
            @JsonProperty("transaction_details") PseTransactionDetailsApiRequest transactionDetails,
            @JsonProperty("additional_info") PseAdditionalInfoApiRequest additionalInfo,
            @JsonProperty("payer") PsePayerApiRequest payer) {
    }

    private record PseTransactionDetailsApiRequest(
            @JsonProperty("financial_institution") String financialInstitution) {
    }

    private record PseAdditionalInfoApiRequest(
            @JsonProperty("ip_address") String ipAddress) {
    }

    private record PsePayerApiRequest(
            @JsonProperty("email") String email,
            @JsonProperty("entity_type") String entityType,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            @JsonProperty("identification") IdentificationApiRequest identification,
            @JsonProperty("address") AddressApiRequest address,
            @JsonProperty("phone") PhoneApiRequest phone) {
    }

    private record IdentificationApiRequest(
            @JsonProperty("type") String type,
            @JsonProperty("number") String number) {
    }

    private record AddressApiRequest(
            @JsonProperty("zip_code") String zipCode,
            @JsonProperty("street_name") String streetName,
            @JsonProperty("street_number") String streetNumber,
            @JsonProperty("neighborhood") String neighborhood,
            @JsonProperty("city") String city,
            @JsonProperty("federal_unit") String federalUnit) {
    }

    private record PhoneApiRequest(
            @JsonProperty("area_code") String areaCode,
            @JsonProperty("number") String number) {
    }

    // -------------------------------------------------------------------------
    // Request DTOs — Cards (Payments API)
    // -------------------------------------------------------------------------

    private record CreateCardPaymentApiRequest(
            @JsonProperty("transaction_amount") BigDecimal transactionAmount,
            @JsonProperty("token") String token,
            @JsonProperty("payment_method_id") String paymentMethodId,
            @JsonProperty("installments") Integer installments,
            @JsonProperty("notification_url") String notificationUrl,
            @JsonProperty("payer") CardPayerApiRequest payer) {
    }

    private record CardPayerApiRequest(
            @JsonProperty("email") String email,
            @JsonProperty("identification") IdentificationApiRequest identification) {
    }

    // -------------------------------------------------------------------------
    // Response DTOs
    // -------------------------------------------------------------------------

    private record PsePaymentApiResponse(
            @JsonProperty("id") Long id,
            @JsonProperty("status") String status,
            @JsonProperty("external_resource_url") String externalResourceUrl) {
    }

    private record CardPaymentApiResponse(
            @JsonProperty("id") Long id,
            @JsonProperty("status") String status,
            @JsonProperty("status_detail") String statusDetail) {
    }

    private record PaymentStatusApiResponse(
            @JsonProperty("id") Long id,
            @JsonProperty("status") String status) {
    }

    private record PaymentMethodApiResponse(
            String id,
            String status,
            @JsonProperty("min_allowed_amount") BigDecimal minAllowedAmount,
            @JsonProperty("max_allowed_amount") BigDecimal maxAllowedAmount) {
    }
}
