package co.edu.escuelaing.techcup.payment.exception;

import co.edu.escuelaing.techcup.payment.dto.response.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("DuplicateEnrollmentOrderException se traduce a 409")
    void handlesDuplicateEnrollmentOrder() {
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateEnrollmentOrder(
                new DuplicateEnrollmentOrderException("duplicada"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("duplicada");
    }

    @Test
    @DisplayName("AmountOutOfRangeException se traduce a 422")
    void handlesAmountOutOfRange() {
        ResponseEntity<ErrorResponse> response = handler.handleAmountOutOfRange(
                new AmountOutOfRangeException("fuera de rango"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().message()).isEqualTo("fuera de rango");
    }

    @Test
    @DisplayName("PaymentOrderNotFoundException y PaymentMethodLimitsNotFoundException se traducen a 404")
    void handlesNotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new PaymentOrderNotFoundException("no encontrada"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("transiciones de estado inválidas se traducen a 409")
    void handlesInvalidStateTransition() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidStateTransition(
                new PaymentOrderNotPendingException("no pendiente"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("PaymentOrderExpiredException se traduce a 410")
    void handlesExpired() {
        ResponseEntity<ErrorResponse> response = handler.handlePaymentOrderExpired(
                new PaymentOrderExpiredException("expirada"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    @Test
    @DisplayName("PaymentGatewayException se traduce a 502")
    void handlesGatewayFailure() {
        ResponseEntity<ErrorResponse> response = handler.handlePaymentGatewayFailure(
                new PaymentGatewayException("MP no disponible", new RuntimeException("timeout")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    @DisplayName("MethodArgumentNotValidException se traduce a 400 con el primer error de campo")
    void handlesValidationErrors() {
        FieldError fieldError = new FieldError("request", "amount", "amount debe ser mayor que cero");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "amount: amount debe ser mayor que cero");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException sin errores de campo usa el mensaje por defecto")
    void handlesValidationErrorsWithoutFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertThat(response.getBody()).containsEntry("error", "Datos inválidos");
    }

    @Test
    @DisplayName("IllegalArgumentException se traduce a 400")
    void handlesIllegalArgument() {
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("dato inválido"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "dato inválido");
    }
}
