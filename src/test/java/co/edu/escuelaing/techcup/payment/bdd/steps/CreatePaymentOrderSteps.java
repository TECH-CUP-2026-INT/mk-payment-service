package co.edu.escuelaing.techcup.payment.bdd.steps;

import co.edu.escuelaing.techcup.payment.exception.AmountOutOfRangeException;
import co.edu.escuelaing.techcup.payment.exception.DuplicateEnrollmentOrderException;
import co.edu.escuelaing.techcup.payment.service.PaymentMethodLimits;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import co.edu.escuelaing.techcup.payment.service.impl.CreatePaymentOrderService;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentMethodLimitsRepositoryPort;
import co.edu.escuelaing.techcup.payment.service.ports.PaymentOrderRepositoryPort;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreatePaymentOrderSteps {

    private PaymentOrderRepositoryPort paymentOrderRepository;
    private PaymentMethodLimitsRepositoryPort paymentMethodLimitsRepository;
    private CreatePaymentOrderService service;
    private PaymentOrder createdOrder;
    private Exception thrownException;

    @Before
    public void setUp() {
        paymentOrderRepository = mock(PaymentOrderRepositoryPort.class);
        paymentMethodLimitsRepository = mock(PaymentMethodLimitsRepositoryPort.class);
        service = new CreatePaymentOrderService(paymentOrderRepository, paymentMethodLimitsRepository);
        createdOrder = null;
        thrownException = null;
        when(paymentOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Given("los límites de PSE son un monto mínimo de {string} y máximo de {string}")
    public void losLimitesDePseSon(String min, String max) {
        when(paymentMethodLimitsRepository.findById("pse")).thenReturn(Optional.of(
                new PaymentMethodLimits("pse", new BigDecimal(min), new BigDecimal(max), "active", LocalDateTime.now())));
    }

    @Given("no existe una orden de pago para el enrollmentId {string}")
    public void noExisteOrdenPara(String enrollmentId) {
        when(paymentOrderRepository.existsByEnrollmentId(enrollmentId)).thenReturn(false);
    }

    @Given("ya existe una orden de pago para el enrollmentId {string}")
    public void yaExisteOrdenPara(String enrollmentId) {
        when(paymentOrderRepository.existsByEnrollmentId(enrollmentId)).thenReturn(true);
    }

    @When("se solicita crear una orden de pago para el enrollmentId {string}, equipo {string}, torneo {string} y monto {string}")
    public void seSolicitaCrearOrden(String enrollmentId, String teamId, String tournamentId, String amount) {
        try {
            createdOrder = service.create(enrollmentId, teamId, tournamentId, new BigDecimal(amount));
        } catch (Exception ex) {
            thrownException = ex;
        }
    }

    @Then("la orden de pago se crea exitosamente con estado {string}")
    public void laOrdenSeCreaConEstado(String status) {
        assertNull(thrownException, "No debería lanzarse ninguna excepción");
        assertNotNull(createdOrder);
        assertEquals(PaymentOrderStatus.valueOf(status), createdOrder.getStatus());
        verify(paymentOrderRepository, times(1)).save(any());
    }

    @Then("la creación se rechaza por monto fuera de rango")
    public void laCreacionSeRechazaPorMonto() {
        assertNotNull(thrownException);
        assertInstanceOf(AmountOutOfRangeException.class, thrownException);
        verify(paymentOrderRepository, never()).save(any());
    }

    @Then("la creación se rechaza por orden duplicada")
    public void laCreacionSeRechazaPorDuplicada() {
        assertNotNull(thrownException);
        assertInstanceOf(DuplicateEnrollmentOrderException.class, thrownException);
        verify(paymentOrderRepository, never()).save(any());
    }
}
