# Diagnóstico del error 502 en el flujo PSE

> **Conclusión:** la integración de PSE con Mercado Pago en Colombia usa la
> **Payments API (`POST /v1/payments`)**, no la Orders API. El error `502` que
> devolvía el backend era la aplicación **enmascarando el error real** de
> Mercado Pago.

---

## 1. Síntoma

`POST /payment-orders/{enrollmentId}/pse` respondía:

```json
{ "message": "Mercado Pago rechazó la solicitud de transacción PSE" }  // HTTP 502
```

El `502` es generado por el propio servicio (`PaymentGatewayException` → 502),
**ocultando** el código HTTP real que devolvía Mercado Pago.

## 2. Causa raíz

Al probar `POST /v1/payments` directamente se observó que el error real era:

```json
{ "message": "Invalid users involved", "error": "bad_request", "status": 400,
  "cause": [{ "code": 2034, "description": "Invalid users involved" }] }
```

Es decir, el **`payer.email` no correspondía a una cuenta de prueba real**.
En sandbox, Mercado Pago exige que el pagador sea un **usuario de prueba
(comprador)** creado en el panel (`...@testuser.com`). Un email genérico como
`payer@example.com` provoca rechazo (`424 BankTransfers Api fail` o
`500 internal_error`).

## 3. Hallazgos confirmados

| Punto | Detalle |
|---|---|
| Endpoint correcto (Colombia) | `POST /v1/payments` (Payments API). |
| Token sandbox | `TEST-...` funciona en la Payments API. |
| `additional_info.ip_address` | Va **directo** (`additional_info.ip_address`), no anidado en `payer`. |
| `transaction_details.financial_institution` | Obligatorio, código del banco PSE. |
| `payer.email` | Debe ser una **cuenta de prueba (comprador)** real. |
| `callback_url` / `notification_url` | A nivel raíz de la petición. |

### Referencia (estructura correcta)

```json
{
  "transaction_amount": 5000,
  "description": "Product description",
  "payment_method_id": "pse",
  "callback_url": "https://...",
  "notification_url": "https://...",
  "additional_info": { "ip_address": "127.0.0.1" },
  "transaction_details": { "financial_institution": "1009" },
  "payer": {
    "email": "TESTUSER...@testuser.com",
    "entity_type": "individual",
    "first_name": "Juan",
    "last_name": "Perez",
    "identification": { "type": "CC", "number": "..." },
    "address": { "zip_code": "11001", "street_name": "...", "street_number": "...", "neighborhood": "...", "city": "..." },
    "phone": { "area_code": "601", "number": "..." }
  }
}
```

## 4. Migración a Orders API (descartada)

Se intentó migrar a la Orders API (`POST /v1/orders`), pero se **revirtió** porque:

1. La Orders API **no acepta tokens `TEST-`** (devuelve `401 invalid_credentials`),
   solo `APP_USR-` (producción), incluso en sandbox.
2. Presenta una **inconsistencia en `additional_info`** (exige `payer.ip_address`
   pero a la vez lo rechaza como propiedad no permitida).
3. La Payments API legacy sigue siendo la vía correcta para PSE en Colombia.

## 5. Cómo probar el flujo PSE

1. Crear una **cuenta de prueba (comprador)** en el panel
   (`Configuración → Pruebas → Cuentas de prueba`).
2. `POST /payment-orders` (crear la orden, sin cambios).
3. `POST /payment-orders/{enrollmentId}/pse` usando el **email del comprador de
   prueba** en `payerEmail` y un banco PSE válido en `financialInstitution`
   (p. ej. `1009` Citibank, `1007` Bancolombia, `1051` Davivienda).
4. La respuesta `200` debe traer `externalResourceUrl` (URL de redirección al banco).

## 6. Mejora sugerida (diagnóstico)

Para no "tapar" el error real de Mercado Pago, conviene que el servicio devuelva
el `status` y `body` reales de Mercado Pago (o al menos los registre) en lugar de
un `502` genérico. El adaptador (`MercadoPagoGatewayAdapter`) ya registra el error
con `log.error("Mercado Pago respondió {} ...")`; el siguiente paso sería propagar
ese detalle en la respuesta HTTP para facilitar el diagnóstico.

---

### Referencias

- [Checkout Bricks → Payment Brick → PSE (server-side)](https://www.mercadopago.com.co/developers/es/docs/checkout-bricks/payment-brick/payment-submission/pse)
- [Referencia API](https://www.mercadopago.com.co/developers/es/reference)
- [Cuentas de prueba](https://www.mercadopago.com.co/developers/es/docs/checkout-api-orders/resources/test-accounts)
