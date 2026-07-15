package co.edu.escuelaing.techcup.payment.repository.adapter;

import co.edu.escuelaing.techcup.payment.exception.PaymentGatewayException;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodId;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodInfo;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentStatusResult;
import co.edu.escuelaing.techcup.payment.service.ports.PseTransactionResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MercadoPagoGatewayAdapter implements PaymentGatewayPort {

    private static final String PSE_PAYMENT_METHOD_ID = PaymentMethodId.PSE;

    private final RestClient restClient;
    private final String accessToken;

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

    @Override
    public PseTransactionResult createPseTransaction(String idempotencyKey, BigDecimal amount,
            String financialInstitution, Payer payer, String notificationUrl) {
        try {
            PaymentApiResponse response = restClient.post()
                    .uri("/v1/payments")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .body(new CreatePaymentApiRequest(
                            PSE_PAYMENT_METHOD_ID,
                            amount,
                            new PayerApiRequest(payer.email(), payer.entityType(),
                                    new IdentificationApiRequest(payer.identificationType(), payer.identificationNumber())),
                            new TransactionDetailsApiRequest(financialInstitution),
                            notificationUrl,
                            notificationUrl))
                    .retrieve()
                    .body(PaymentApiResponse.class);
            if (response == null) {
                throw new IllegalStateException("Respuesta vacía de Mercado Pago");
            }
            return new PseTransactionResult(String.valueOf(response.id()), response.status(),
                    response.transactionDetails() != null ? response.transactionDetails().externalResourceUrl() : null);
        } catch (Exception ex) {
            throw new PaymentGatewayException("No se pudo crear la transacción PSE en Mercado Pago", ex);
        }
    }

    @Override
    public PaymentStatusResult getPaymentStatus(String mpPaymentId) {
        try {
            PaymentApiResponse response = restClient.get()
                    .uri("/v1/payments/{id}", mpPaymentId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(PaymentApiResponse.class);
            if (response == null) {
                throw new IllegalStateException("Respuesta vacía de Mercado Pago");
            }
            return new PaymentStatusResult(String.valueOf(response.id()), response.status());
        } catch (Exception ex) {
            throw new PaymentGatewayException("No se pudo consultar el estado del pago %s en Mercado Pago".formatted(mpPaymentId), ex);
        }
    }

    @Override
    public List<PaymentMethodInfo> getAvailablePaymentMethods() {
        try {
            PaymentMethodApiResponse[] response = restClient.get()
                    .uri("/v1/payment_methods")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(PaymentMethodApiResponse[].class);
            if (response == null) {
                return List.of();
            }
            return List.of(response).stream()
                    .map(pm -> new PaymentMethodInfo(pm.id(), pm.status(), pm.minAllowedAmount(), pm.maxAllowedAmount()))
                    .toList();
        } catch (Exception ex) {
            throw new PaymentGatewayException("No se pudieron consultar los medios de pago de Mercado Pago", ex);
        }
    }

    private record CreatePaymentApiRequest(
            @JsonProperty("payment_method_id") String paymentMethodId,
            @JsonProperty("transaction_amount") BigDecimal transactionAmount,
            @JsonProperty("payer") PayerApiRequest payer,
            @JsonProperty("transaction_details") TransactionDetailsApiRequest transactionDetails,
            @JsonProperty("callback_url") String callbackUrl,
            @JsonProperty("notification_url") String notificationUrl) {
    }

    private record PayerApiRequest(String email, @JsonProperty("entity_type") String entityType,
            IdentificationApiRequest identification) {
    }

    private record IdentificationApiRequest(String type, String number) {
    }

    private record TransactionDetailsApiRequest(@JsonProperty("financial_institution") String financialInstitution) {
    }

    private record PaymentApiResponse(
            Long id,
            String status,
            @JsonProperty("transaction_details") TransactionDetailsApiResponse transactionDetails) {
    }

    private record TransactionDetailsApiResponse(
            @JsonProperty("external_resource_url") String externalResourceUrl) {
    }

    private record PaymentMethodApiResponse(
            String id,
            String status,
            @JsonProperty("min_allowed_amount") BigDecimal minAllowedAmount,
            @JsonProperty("max_allowed_amount") BigDecimal maxAllowedAmount) {
    }
}
