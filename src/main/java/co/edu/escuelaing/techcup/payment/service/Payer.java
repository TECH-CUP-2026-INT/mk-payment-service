package co.edu.escuelaing.techcup.payment.service;

import java.util.Set;

public record Payer(String email, String identificationType, String identificationNumber, String entityType) {

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
    }
}
