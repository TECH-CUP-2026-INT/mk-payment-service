package co.edu.escuelaing.techcup.payment.config;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.paymentmethod.PaymentMethodClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the Mercado Pago official SDK clients as Spring beans.
 *
 * <p>The SDK is configured per request (access token, timeouts and headers) through
 * {@link com.mercadopago.core.MPRequestOptions}; no global {@code MercadoPagoConfig}
 * mutation is needed, which keeps the adapter free of shared mutable state.</p>
 */
@Configuration
public class MercadoPagoClientConfig {

    @Bean
    public PaymentClient mercadoPagoPaymentClient() {
        return new PaymentClient();
    }

    @Bean
    public PaymentMethodClient mercadoPagoPaymentMethodClient() {
        return new PaymentMethodClient();
    }
}
