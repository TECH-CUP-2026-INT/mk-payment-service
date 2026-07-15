package co.edu.escuelaing.techcup.payment.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SubmitPseTransactionRequest(
        @NotBlank(message = "financialInstitution es obligatorio") String financialInstitution,
        @NotBlank(message = "payerEmail es obligatorio")
        @Email(message = "payerEmail debe ser un correo válido")
        String payerEmail,
        @NotBlank(message = "identificationType es obligatorio") String identificationType,
        @NotBlank(message = "identificationNumber es obligatorio") String identificationNumber,
        @NotBlank(message = "entityType es obligatorio")
        @Pattern(regexp = "individual|association", message = "entityType debe ser 'individual' o 'association'")
        String entityType) {
}
