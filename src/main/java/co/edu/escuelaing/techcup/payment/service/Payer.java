package co.edu.escuelaing.techcup.payment.service;

import java.util.Set;

public record Payer(String email, String identificationType, String identificationNumber, String entityType,
        String firstName, String lastName, String addressZipCode, String addressStreetName,
        String addressStreetNumber, String addressNeighborhood, String addressCity, String phoneAreaCode,
        String phoneNumber) {

    private static final Set<String> VALID_ENTITY_TYPES = Set.of("individual", "association");

    public Payer {
        requireNonBlank(email, "El email del pagador es obligatorio");
        requireNonBlank(identificationType, "El tipo de identificación del pagador es obligatorio");
        requireNonBlank(identificationNumber, "El número de identificación del pagador es obligatorio");
        if (entityType == null || !VALID_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("El entityType del pagador debe ser 'individual' o 'association'");
        }
        requireNonBlank(firstName, "El nombre del pagador es obligatorio");
        requireNonBlank(lastName, "El apellido del pagador es obligatorio");
        requireNonBlank(addressZipCode, "El código postal del pagador es obligatorio");
        requireNonBlank(addressStreetName, "El nombre de la calle del pagador es obligatorio");
        requireNonBlank(addressStreetNumber, "El número de la calle del pagador es obligatorio");
        requireNonBlank(addressNeighborhood, "El barrio del pagador es obligatorio");
        requireNonBlank(addressCity, "La ciudad del pagador es obligatoria");
        requireNonBlank(phoneAreaCode, "El indicativo telefónico del pagador es obligatorio");
        requireNonBlank(phoneNumber, "El número telefónico del pagador es obligatorio");
    }

    private static void requireNonBlank(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
