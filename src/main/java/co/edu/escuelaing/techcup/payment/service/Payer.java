package co.edu.escuelaing.techcup.payment.service;

public record Payer(String email, String identificationType, String identificationNumber) {

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
    }
}
