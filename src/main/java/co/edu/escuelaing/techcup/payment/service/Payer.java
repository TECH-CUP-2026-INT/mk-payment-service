package co.edu.escuelaing.techcup.payment.service;

import java.util.Set;

public record Payer(String email, String identificationType, String identificationNumber, String entityType,
        String firstName, String lastName, String addressZipCode, String addressStreetName,
        String addressStreetNumber, String addressNeighborhood, String addressCity, String phoneAreaCode,
        String phoneNumber) {

    private static final Set<String> VALID_ENTITY_TYPES = Set.of("individual", "association");

    public Payer {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email del pagador es obligatorio");
        }
        if (identificationType == null || identificationType.isBlank()) {
            throw new IllegalArgumentException("El tipo de identificación del pagador es obligatorio");
        }
        if (identificationNumber == null || identificationNumber.isBlank()) {
            throw new IllegalArgumentException("El número de identificación del pagador es obligatorio");
        }
        if (entityType == null || !VALID_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("El entityType del pagador debe ser 'individual' o 'association'");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("El nombre del pagador es obligatorio");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("El apellido del pagador es obligatorio");
        }
        if (addressZipCode == null || addressZipCode.isBlank()) {
            throw new IllegalArgumentException("El código postal del pagador es obligatorio");
        }
        if (addressStreetName == null || addressStreetName.isBlank()) {
            throw new IllegalArgumentException("El nombre de la calle del pagador es obligatorio");
        }
        if (addressStreetNumber == null || addressStreetNumber.isBlank()) {
            throw new IllegalArgumentException("El número de la calle del pagador es obligatorio");
        }
        if (addressNeighborhood == null || addressNeighborhood.isBlank()) {
            throw new IllegalArgumentException("El barrio del pagador es obligatorio");
        }
        if (addressCity == null || addressCity.isBlank()) {
            throw new IllegalArgumentException("La ciudad del pagador es obligatoria");
        }
        if (phoneAreaCode == null || phoneAreaCode.isBlank()) {
            throw new IllegalArgumentException("El indicativo telefónico del pagador es obligatorio");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("El número telefónico del pagador es obligatorio");
        }
    }
}
