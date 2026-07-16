package co.edu.escuelaing.techcup.payment.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitPseTransactionRequest(
        @NotBlank(message = "financialInstitution es obligatorio") String financialInstitution,
        @NotBlank(message = "payerEmail es obligatorio")
        @Email(message = "payerEmail debe ser un correo válido")
        String payerEmail,
        @NotBlank(message = "identificationType es obligatorio") String identificationType,
        @NotBlank(message = "identificationNumber es obligatorio") String identificationNumber,
        @NotBlank(message = "entityType es obligatorio")
        @Pattern(regexp = "individual|association", message = "entityType debe ser 'individual' o 'association'")
        String entityType,
        @NotBlank(message = "firstName es obligatorio")
        @Size(min = 1, max = 32, message = "firstName debe tener entre 1 y 32 caracteres")
        String firstName,
        @NotBlank(message = "lastName es obligatorio")
        @Size(min = 1, max = 32, message = "lastName debe tener entre 1 y 32 caracteres")
        String lastName,
        @NotBlank(message = "addressZipCode es obligatorio")
        @Size(min = 5, max = 5, message = "addressZipCode debe tener exactamente 5 caracteres")
        String addressZipCode,
        @NotBlank(message = "addressStreetName es obligatorio")
        @Size(min = 1, max = 18, message = "addressStreetName debe tener entre 1 y 18 caracteres")
        String addressStreetName,
        @NotBlank(message = "addressStreetNumber es obligatorio")
        @Size(min = 1, max = 5, message = "addressStreetNumber debe tener entre 1 y 5 caracteres")
        String addressStreetNumber,
        @NotBlank(message = "addressNeighborhood es obligatorio")
        @Size(min = 1, max = 18, message = "addressNeighborhood debe tener entre 1 y 18 caracteres")
        String addressNeighborhood,
        @NotBlank(message = "addressCity es obligatorio")
        @Size(min = 1, max = 18, message = "addressCity debe tener entre 1 y 18 caracteres")
        String addressCity,
        @NotBlank(message = "phoneAreaCode es obligatorio")
        @Pattern(regexp = "\\d{3}", message = "phoneAreaCode debe tener exactamente 3 dígitos")
        String phoneAreaCode,
        @NotBlank(message = "phoneNumber es obligatorio")
        @Pattern(regexp = "\\d{1,5}", message = "phoneNumber debe tener entre 1 y 5 dígitos")
        String phoneNumber) {
}
