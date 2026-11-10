package co.edu.escuelaing.techcup.payment.repository.adapter;

import co.edu.escuelaing.techcup.payment.exception.PaymentGatewayException;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodId;
import co.edu.escuelaing.techcup.payment.service.ports.CardPayer;
import co.edu.escuelaing.techcup.payment.service.ports.CardTransactionResult;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodInfo;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentStatusResult;
import co.edu.escuelaing.techcup.payment.service.ports.PseTransactionResult;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentAdditionalInfoRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerAddressRequest;
import com.mercadopago.client.payment.PaymentPayerPhoneRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.client.payment.PaymentTransactionDetailsRequest;
import com.mercadopago.client.paymentmethod.PaymentMethodClient;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPResourceList;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.paymentmethod.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de Mercado Pago construido sobre el SDK oficial ({@code com.mercadopago:sdk-java}).
 *
 * <p>PSE: {@code payment_method_id: "pse"} + {@code transaction_details.financial_institution}
 * + datos completos del pagador. La respuesta incluye {@code external_resource_url} al que
 * se redirige al usuario para completar la autenticación bancaria.</p>
 *
 * <p>Tarjetas: {@code token} + {@code payment_method_id} + {@code installments}. El pagador
 * se envía con los datos que estén disponibles (a diferencia de PSE, Mercado Pago tolera
 * datos parciales en tarjeta).</p>
 *
 * <p>El access token, los timeouts y los headers se pasan por request vía
 * {@link MPRequestOptions}; no se muta la configuración global estática del SDK.</p>
 *
 * @see <a href="https://www.mercadopago.com.co/developers/es/docs/checkout-bricks/payment-brick/payment-submission/pse">PSE con Payment Brick</a>
 */
@Component
public class MercadoPagoGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoGatewayAdapter.class);
    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final String TEST_TOKEN_HEADER = "x-test-token";

    private final PaymentClient paymentClient;
    private final PaymentMethodClient paymentMethodClient;
    private final String accessToken;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final boolean testToken;

    @Autowired
    public MercadoPagoGatewayAdapter(
            PaymentClient paymentClient,
            PaymentMethodClient paymentMethodClient,
            @Value("${mercadopago.access-token}") String accessToken,
            @Value("${mercadopago.connect-timeout-millis}") int connectTimeoutMillis,
            @Value("${mercadopago.read-timeout-millis}") int readTimeoutMillis,
            @Value("${mercadopago.test-token:false}") boolean testToken) {
        this.paymentClient = paymentClient;
        this.paymentMethodClient = paymentMethodClient;
        this.accessToken = accessToken;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.testToken = testToken;
    }

    @Override
    public PseTransactionResult createPseTransaction(String idempotencyKey, BigDecimal amount,
            String financialInstitution, Payer payer, String ipAddress, String callbackUrl,
            String notificationUrl, String externalReference) {
        log.info("[PSE] Creando pago — idempotencyKey={} financialInstitution={} externalReference={} testToken={}",
                idempotencyKey, financialInstitution, externalReference, testToken);
        try {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .transactionAmount(amount)
                    .paymentMethodId(PaymentMethodId.PSE)
                    .callbackUrl(callbackUrl)
                    .notificationUrl(notificationUrl)
                    .externalReference(externalReference)
                    .transactionDetails(PaymentTransactionDetailsRequest.builder()
                            .financialInstitution(financialInstitution)
                            .build())
                    .additionalInfo(PaymentAdditionalInfoRequest.builder()
                            .ipAddress(ipAddress)
                            .build())
                    .payer(PaymentPayerRequest.builder()
                            .email(payer.email())
                            .entityType(payer.entityType())
                            .firstName(payer.firstName())
                            .lastName(payer.lastName())
                            .identification(IdentificationRequest.builder()
                                    .type(payer.identificationType())
                                    .number(payer.identificationNumber())
                                    .build())
                            .address(PaymentPayerAddressRequest.builder()
                                    .zipCode(payer.addressZipCode())
                                    .streetName(payer.addressStreetName())
                                    .streetNumber(payer.addressStreetNumber())
                                    .neighborhood(payer.addressNeighborhood())
                                    .city(payer.addressCity())
                                    .federalUnit("1")
                                    .build())
                            .phone(PaymentPayerPhoneRequest.builder()
                                    .areaCode(payer.phoneAreaCode())
                                    .number(payer.phoneNumber())
                                    .build())
                            .build())
                    .build();

            Payment response = paymentClient.create(request, requestOptions(idempotencyKey, true));
            requirePaymentId(response);
            return new PseTransactionResult(
                    String.valueOf(response.getId()),
                    response.getStatus(),
                    response.getTransactionDetails() == null
                            ? null
                            : response.getTransactionDetails().getExternalResourceUrl());
        } catch (MPApiException ex) {
            log.error("Mercado Pago respondió {} al crear el pago PSE: {}",
                    ex.getStatusCode(), apiContent(ex), ex);
            throw new PaymentGatewayException("No se pudo crear la transacción PSE en Mercado Pago", ex);
        } catch (MPException ex) {
            log.error("Error de comunicación con Mercado Pago al crear la transacción PSE", ex);
            throw new PaymentGatewayException("No se pudo crear la transacción PSE en Mercado Pago", ex);
        } catch (Exception ex) {
            log.error("Error inesperado al crear la transacción PSE en Mercado Pago", ex);
            throw new PaymentGatewayException("No se pudo crear la transacción PSE en Mercado Pago", ex);
        }
    }

    @Override
    public CardTransactionResult createCardTransaction(String idempotencyKey, BigDecimal amount,
            String token, String paymentMethodId, Integer installments, CardPayer payer,
            String notificationUrl, String externalReference) {
        try {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .transactionAmount(amount)
                    .token(token)
                    .paymentMethodId(paymentMethodId)
                    .installments(installments != null ? installments : 1)
                    .notificationUrl(notificationUrl)
                    .externalReference(externalReference)
                    .payer(toCardPayer(payer))
                    .build();

            Payment response = paymentClient.create(request, requestOptions(idempotencyKey, false));
            requirePaymentId(response);
            return new CardTransactionResult(String.valueOf(response.getId()),
                    response.getStatus(), response.getStatusDetail());
        } catch (MPApiException ex) {
            log.error("Mercado Pago respondió {} al crear el pago con tarjeta: {}",
                    ex.getStatusCode(), apiContent(ex), ex);
            throw new PaymentGatewayException("No se pudo crear el pago con tarjeta en Mercado Pago", ex);
        } catch (MPException ex) {
            log.error("Error de comunicación con Mercado Pago al crear el pago con tarjeta", ex);
            throw new PaymentGatewayException("No se pudo crear el pago con tarjeta en Mercado Pago", ex);
        } catch (Exception ex) {
            log.error("Error inesperado al crear el pago con tarjeta en Mercado Pago", ex);
            throw new PaymentGatewayException("No se pudo crear el pago con tarjeta en Mercado Pago", ex);
        }
    }

    @Override
    public PaymentStatusResult getPaymentStatus(String mpPaymentId) {
        try {
            Payment response = paymentClient.get(parsePaymentId(mpPaymentId), requestOptions(null, false));
            requirePaymentId(response);
            return new PaymentStatusResult(String.valueOf(response.getId()), response.getStatus());
        } catch (MPApiException ex) {
            log.error("Mercado Pago respondió {} al consultar el pago {}: {}",
                    ex.getStatusCode(), mpPaymentId, apiContent(ex), ex);
            throw new PaymentGatewayException(
                    "No se pudo consultar el estado del pago %s en Mercado Pago".formatted(mpPaymentId), ex);
        } catch (MPException ex) {
            log.error("Error de comunicación con Mercado Pago al consultar el pago {}", mpPaymentId, ex);
            throw new PaymentGatewayException(
                    "No se pudo consultar el estado del pago %s en Mercado Pago".formatted(mpPaymentId), ex);
        } catch (Exception ex) {
            log.error("Error inesperado al consultar el estado del pago {} en Mercado Pago", mpPaymentId, ex);
            throw new PaymentGatewayException(
                    "No se pudo consultar el estado del pago %s en Mercado Pago".formatted(mpPaymentId), ex);
        }
    }

    @Override
    public List<PaymentMethodInfo> getAvailablePaymentMethods() {
        try {
            MPResourceList<PaymentMethod> response = paymentMethodClient.list(requestOptions(null, false));
            if (response == null || response.getResults() == null) {
                return List.of();
            }
            return response.getResults().stream()
                    .map(pm -> new PaymentMethodInfo(pm.getId(), pm.getStatus(),
                            pm.getMinAllowedAmount(), pm.getMaxAllowedAmount()))
                    .toList();
        } catch (MPApiException ex) {
            log.error("Mercado Pago respondió {} al consultar los medios de pago: {}",
                    ex.getStatusCode(), apiContent(ex), ex);
            throw new PaymentGatewayException("No se pudieron consultar los medios de pago de Mercado Pago", ex);
        } catch (MPException ex) {
            log.error("Error de comunicación con Mercado Pago al consultar los medios de pago", ex);
            throw new PaymentGatewayException("No se pudieron consultar los medios de pago de Mercado Pago", ex);
        } catch (Exception ex) {
            log.error("Error inesperado al consultar los medios de pago de Mercado Pago", ex);
            throw new PaymentGatewayException("No se pudieron consultar los medios de pago de Mercado Pago", ex);
        }
    }

    private MPRequestOptions requestOptions(String idempotencyKey, boolean includeTestHeader) {
        Map<String, String> headers = new HashMap<>();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            headers.put(IDEMPOTENCY_HEADER, idempotencyKey);
        }
        if (includeTestHeader && testToken) {
            headers.put(TEST_TOKEN_HEADER, "true");
        }
        return MPRequestOptions.builder()
                .accessToken(accessToken)
                .connectionTimeout(connectTimeoutMillis)
                .connectionRequestTimeout(connectTimeoutMillis)
                .socketTimeout(readTimeoutMillis)
                .customHeaders(headers)
                .build();
    }

    /**
     * Builds the card payer with only the fields that were actually provided. Gson omits
     * null values when the SDK serializes the request, so empty nested objects are avoided.
     */
    private static PaymentPayerRequest toCardPayer(CardPayer payer) {
        if (payer == null) {
            return null;
        }
        var builder = PaymentPayerRequest.builder()
                .email(blankToNull(payer.email()))
                .entityType(blankToNull(payer.entityType()))
                .firstName(blankToNull(payer.firstName()))
                .lastName(blankToNull(payer.lastName()));

        String identificationType = blankToNull(payer.identificationType());
        String identificationNumber = blankToNull(payer.identificationNumber());
        if (identificationType != null && identificationNumber != null) {
            builder.identification(IdentificationRequest.builder()
                    .type(identificationType)
                    .number(identificationNumber)
                    .build());
        }

        PaymentPayerAddressRequest address = buildAddress(payer);
        if (address != null) {
            builder.address(address);
        }

        PaymentPayerPhoneRequest phone = buildPhone(payer);
        if (phone != null) {
            builder.phone(phone);
        }

        return builder.build();
    }

    private static PaymentPayerAddressRequest buildAddress(CardPayer payer) {
        String zipCode = blankToNull(payer.addressZipCode());
        String streetName = blankToNull(payer.addressStreetName());
        String streetNumber = blankToNull(payer.addressStreetNumber());
        String neighborhood = blankToNull(payer.addressNeighborhood());
        String city = blankToNull(payer.addressCity());
        boolean hasAny = zipCode != null || streetName != null || streetNumber != null
                || neighborhood != null || city != null;
        if (!hasAny) {
            return null;
        }
        return PaymentPayerAddressRequest.builder()
                .zipCode(zipCode)
                .streetName(streetName)
                .streetNumber(streetNumber)
                .neighborhood(neighborhood)
                .city(city)
                .build();
    }

    private static PaymentPayerPhoneRequest buildPhone(CardPayer payer) {
        String areaCode = blankToNull(payer.phoneAreaCode());
        String number = blankToNull(payer.phoneNumber());
        if (areaCode == null && number == null) {
            return null;
        }
        return PaymentPayerPhoneRequest.builder()
                .areaCode(areaCode)
                .number(number)
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Long parsePaymentId(String mpPaymentId) {
        try {
            return Long.valueOf(mpPaymentId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("mpPaymentId inválido: " + mpPaymentId, ex);
        }
    }

    private static void requirePaymentId(Payment payment) {
        if (payment == null || payment.getId() == null) {
            throw new IllegalStateException("Respuesta vacía de Mercado Pago");
        }
    }

    private static String apiContent(MPApiException ex) {
        return ex.getApiResponse() == null ? null : ex.getApiResponse().getContent();
    }
}
