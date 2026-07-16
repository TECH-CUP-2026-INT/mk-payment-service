package co.edu.escuelaing.techcup.payment.bdd.steps;

import co.edu.escuelaing.techcup.payment.exception.PaymentOrderExpiredException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotFoundException;
import co.edu.escuelaing.techcup.payment.exception.PaymentOrderNotPendingException;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.impl.SubmitPseTransactionService;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentGatewayPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.PseTransactionResult;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SubmitPseTransactionSteps {

    private static final String CALLBACK_URL = "https://example-frontend.invalid/checkout/pse-return";
    private static final String NOTIFICATION_URL = "https://example-test-tunnel.invalid/payment-orders/webhook";
    private static final String IP_ADDRESS = "200.10.20.30";

    private PaymentOrderRepositoryPort paymentOrderRepository;
    private PaymentGatewayPort paymentGateway;
    private SubmitPseTransactionService service;
    private PaymentOrder result;
    private Exception thrownException;

    @Before
    public void setUp() {
        paymentOrderRepository = mock(PaymentOrderRepositoryPort.class);
        paymentGateway = mock(PaymentGatewayPort.class);
        service = new SubmitPseTransactionService(paymentOrderRepository, paymentGateway, CALLBACK_URL, NOTIFICATION_URL);
        result = null;
        thrownException = null;
        when(paymentOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Given("existe una orden de pago pendiente para el enrollmentId {string}")
    public void existeOrdenPendientePara(String enrollmentId) {
        registerOrder(enrollmentId, PaymentOrderStatus.PENDING, LocalDateTime.now().plusMinutes(60));
    }

    @Given("existe una orden de pago en estado {string} para el enrollmentId {string}")
    public void existeOrdenEnEstadoPara(String status, String enrollmentId) {
        registerOrder(enrollmentId, PaymentOrderStatus.valueOf(status), LocalDateTime.now().plusMinutes(60));
    }

    @Given("existe una orden de pago pendiente y expirada para el enrollmentId {string}")
    public void existeOrdenPendienteYExpiradaPara(String enrollmentId) {
        registerOrder(enrollmentId, PaymentOrderStatus.PENDING, LocalDateTime.now().minusMinutes(1));
    }

    @Given("no hay ninguna orden de pago registrada para el enrollmentId {string}")
    public void noHayOrdenRegistradaPara(String enrollmentId) {
        when(paymentOrderRepository.findByEnrollmentId(enrollmentId)).thenReturn(Optional.empty());
    }

    @Given("Mercado Pago acepta la transacción PSE con referencia {string} y url {string}")
    public void mercadoPagoAceptaLaTransaccion(String mpPaymentId, String url) {
        when(paymentGateway.createPseTransaction(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PseTransactionResult(mpPaymentId, "pending", url));
    }

    @When("se envía la transacción PSE para el enrollmentId {string} con institución financiera {string} y pagador {string}")
    public void seEnviaLaTransaccionPse(String enrollmentId, String financialInstitution, String payerEmail) {
        try {
            Payer payer = new Payer(payerEmail, "CC", "123456789", "individual",
                    "Juan", "Pérez", "11001", "Calle 1", "123", "Centro", "Bogotá", "601", "12345");
            result = service.submit(enrollmentId, payer, financialInstitution, IP_ADDRESS);
        } catch (Exception ex) {
            thrownException = ex;
        }
    }

    @Then("la transacción PSE se envía exitosamente con estado {string}")
    public void laTransaccionSeEnviaConEstado(String status) {
        assertNull(thrownException, "No debería lanzarse ninguna excepción");
        assertNotNull(result);
        assertEquals(PaymentOrderStatus.valueOf(status), result.getStatus());
    }

    @Then("el envío se rechaza porque la orden no está pendiente")
    public void elEnvioSeRechazaPorNoEstarPendiente() {
        assertNotNull(thrownException);
        assertInstanceOf(PaymentOrderNotPendingException.class, thrownException);
    }

    @Then("el envío se rechaza porque la orden ya expiró")
    public void elEnvioSeRechazaPorExpirada() {
        assertNotNull(thrownException);
        assertInstanceOf(PaymentOrderExpiredException.class, thrownException);
    }

    @Then("el envío falla porque la orden no existe")
    public void elEnvioFallaPorNoExistir() {
        assertNotNull(thrownException);
        assertInstanceOf(PaymentOrderNotFoundException.class, thrownException);
    }

    private void registerOrder(String enrollmentId, PaymentOrderStatus status, LocalDateTime expiresAt) {
        PaymentOrder order = PaymentOrder.reconstruct(UUID.randomUUID(), enrollmentId, "team-1", "tournament-1",
                new BigDecimal("50000"), status, null, UUID.randomUUID().toString(), null, null, expiresAt, 0L);
        when(paymentOrderRepository.findByEnrollmentId(enrollmentId)).thenReturn(Optional.of(order));
    }
}
