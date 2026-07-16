package co.edu.escuelaing.techcup.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = """
        Data submitted by the Mercado Pago Payment Brick's onSubmit callback for a PSE transaction, \
        plus the payer details the Brick does not collect on its own (name, address, phone) that \
        Mercado Pago has required for PSE payments since 2024-12-31. The caller's IP address is \
        captured server-side from the HTTP request and is not part of this body.""")
public record SubmitPseTransactionRequest(
        @Schema(description = "Mercado Pago financial institution code for the payer's bank.", example = "1007")
        @NotBlank(message = "financialInstitution es obligatorio") String financialInstitution,

        @Schema(description = "Payer's email address.", example = "payer@example.com")
        @NotBlank(message = "payerEmail es obligatorio")
        @Email(message = "payerEmail debe ser un correo válido")
        String payerEmail,

        @Schema(description = "Colombian identification document type.", example = "CC")
        @NotBlank(message = "identificationType es obligatorio") String identificationType,

        @Schema(description = "Identification document number.", example = "123456789")
        @NotBlank(message = "identificationNumber es obligatorio") String identificationNumber,

        @Schema(description = "Whether the payer is a natural person or a legal entity.",
                example = "individual", allowableValues = {"individual", "association"})
        @NotBlank(message = "entityType es obligatorio")
        @Pattern(regexp = "individual|association", message = "entityType debe ser 'individual' o 'association'")
        String entityType,

        @Schema(description = "Payer's first name.", example = "Juan")
        @NotBlank(message = "firstName es obligatorio")
        @Size(min = 1, max = 32, message = "firstName debe tener entre 1 y 32 caracteres")
        String firstName,

        @Schema(description = "Payer's last name.", example = "Pérez")
        @NotBlank(message = "lastName es obligatorio")
        @Size(min = 1, max = 32, message = "lastName debe tener entre 1 y 32 caracteres")
        String lastName,

        @Schema(description = "Payer's address ZIP code, exactly 5 characters.", example = "11001")
        @NotBlank(message = "addressZipCode es obligatorio")
        @Size(min = 5, max = 5, message = "addressZipCode debe tener exactamente 5 caracteres")
        String addressZipCode,

        @Schema(description = "Payer's street name.", example = "Calle 1")
        @NotBlank(message = "addressStreetName es obligatorio")
        @Size(min = 1, max = 18, message = "addressStreetName debe tener entre 1 y 18 caracteres")
        String addressStreetName,

        @Schema(description = "Payer's street number.", example = "123")
        @NotBlank(message = "addressStreetNumber es obligatorio")
        @Size(min = 1, max = 5, message = "addressStreetNumber debe tener entre 1 y 5 caracteres")
        String addressStreetNumber,

        @Schema(description = "Payer's neighborhood.", example = "Centro")
        @NotBlank(message = "addressNeighborhood es obligatorio")
        @Size(min = 1, max = 18, message = "addressNeighborhood debe tener entre 1 y 18 caracteres")
        String addressNeighborhood,

        @Schema(description = "Payer's city.", example = "Bogotá")
        @NotBlank(message = "addressCity es obligatorio")
        @Size(min = 1, max = 18, message = "addressCity debe tener entre 1 y 18 caracteres")
        String addressCity,

        @Schema(description = "Payer's phone area code, exactly 3 digits.", example = "601")
        @NotBlank(message = "phoneAreaCode es obligatorio")
        @Pattern(regexp = "\\d{3}", message = "phoneAreaCode debe tener exactamente 3 dígitos")
        String phoneAreaCode,

        @Schema(description = "Payer's phone number, 1 to 5 digits.", example = "12345")
        @NotBlank(message = "phoneNumber es obligatorio")
        @Pattern(regexp = "\\d{1,5}", message = "phoneNumber debe tener entre 1 y 5 dígitos")
        String phoneNumber) {
}
