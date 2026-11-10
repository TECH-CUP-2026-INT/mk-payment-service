package co.edu.escuelaing.techcup.payment.service.ports;

/**
 * Datos del pagador para un pago con tarjeta. Todos los campos son opcionales:
 * se envían a Mercado Pago sólo los que vengan informados. A diferencia de
 * {@link co.edu.escuelaing.techcup.payment.service.Payer} (usado en PSE, donde
 * Mercado Pago exige todos los campos), el flujo de tarjeta tolera datos
 * parciales.
 */
public record CardPayer(
        String email,
        String identificationType,
        String identificationNumber,
        String entityType,
        String firstName,
        String lastName,
        String addressZipCode,
        String addressStreetName,
        String addressStreetNumber,
        String addressNeighborhood,
        String addressCity,
        String phoneAreaCode,
        String phoneNumber) {
}
