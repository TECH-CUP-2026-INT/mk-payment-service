package co.edu.escuelaing.techcup.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TechCup Payment Service API")
                        .description("""
                                Handles PSE (bank transfer) payments for TechCup tournament enrollments \
                                through Mercado Pago: creating payment orders, submitting PSE transactions \
                                initiated by the Mercado Pago Payment Brick, receiving Mercado Pago's \
                                webhook notifications, and exposing payment status and PSE amount limits.""")
                        .version("v1")
                        .contact(new Contact().name("TechCup 2026").url("https://github.com/TECH-CUP-2026-INT")));
    }
}
