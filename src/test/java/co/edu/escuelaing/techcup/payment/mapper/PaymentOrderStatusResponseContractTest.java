package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.dto.response.PaymentOrderStatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * mk-tournament-service's PaymentServiceClientAdapter deserializes GET
 * /payment-orders/{enrollmentId} with the private record
 * {@code PaymentOrderResponse(PaymentOrderStatus status)} and swallows ANY
 * exception into UNKNOWN - a field-name mismatch here would never surface as
 * an error there, just a silently null status. This test locks the exact
 * JSON shape that consumer expects.
 */
class PaymentOrderStatusResponseContractTest {

    private enum ConsumerPaymentOrderStatus {
        PENDING, AWAITING_BANK_CONFIRMATION, APPROVED, REJECTED, UNKNOWN
    }

    private record ConsumerSidePaymentOrderResponse(ConsumerPaymentOrderStatus status) {
    }

    @Test
    @DisplayName("El campo status se serializa en minúsculas y calza con el record consumidor de Torneos")
    void statusFieldMatchesTournamentConsumerContract() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        PaymentOrderStatusResponse response = new PaymentOrderStatusResponse("APPROVED");

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"status\"");

        ConsumerSidePaymentOrderResponse consumed = objectMapper.readValue(json, ConsumerSidePaymentOrderResponse.class);
        assertThat(consumed.status()).isEqualTo(ConsumerPaymentOrderStatus.APPROVED);
    }
}
