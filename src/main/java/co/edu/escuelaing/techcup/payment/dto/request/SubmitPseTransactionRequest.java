package co.edu.escuelaing.techcup.payment.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SubmitPseTransactionRequest(
        @NotBlank(message = "financialInstitution es obligatorio") String financialInstitution,
        @NotBlank(message = "payerEmail es obligatorio")
        @Email(message = "payerEmail debe ser un correo válido")
        String payerEmail,
        @NotBlank(message = "identificationType es obligatorio") String identificationType,
        @NotBlank(message = "identificationNumber es obligatorio") String identificationNumber) {
}
