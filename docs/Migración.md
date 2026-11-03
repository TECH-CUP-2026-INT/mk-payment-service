# Migración de la Payments API a la Orders API (Mercado Pago)

> **Resumen:** el servicio de pagos usaba la **Payments API (legacy)** de Mercado
> Pago (`POST /v1/payments`). Mercado Pago marcó esa API como *legacy* y movió el
> soporte de **PSE** a la **Orders API** (`POST /v1/orders`). Como consecuencia, el
> endpoint legacy de PSE comenzó a responder `500 internal_error`, lo que se
> traducía en un `502 Bad Gateway` ("Mercado Pago rechazó la solicitud de
> transacción PSE") en nuestro backend.
>
> Esta guía documenta la migración realizada y todo lo que cambió.

---

## 1. Contexto y causa raíz

| Punto | Detalle |
|---|---|
| Síntoma | `POST /payment-orders/{enrollmentId}/pse` devolvía `502` con `"Mercado Pago rechazó la solicitud de transacción PSE"`. |
| Error real de Mercado Pago | `500 internal_error` al llamar `POST /v1/payments` con `payment_method_id = "pse"`. |
| Causa | Mercado Pago deprecó la **Payments API** (legacy). PSE ahora se integra vía la **Orders API**. |
| Verificación | Llamada directa a `POST /v1/payments` con credenciales sandbox válidas devolvía `500` de forma consistente, independientemente del IP, `callback_url`, email o banco. |

### Evidencia en la documentación oficial

En [Referencia API de Mercado Pago](https://www.mercadopago.com.co/developers/es/reference):

- **Checkout API → Orders API → Orders → Crear order** (`POST /v1/orders`) — API actual.
- **Payments API → Pagos → Crear pago** (`POST /v1/payments`) — marcada como **legacy**.
- Existe una guía: [Cómo migrar de la Payments API a la Orders API](https://www.mercadopago.com.co/developers/es/docs/checkout-api-orders/resources/migrate-payments-to-orders).

---

## 2. Qué cambió (resumen)

| Área | Antes (Payments API) | Ahora (Orders API) |
|---|---|---|
| Endpoint de creación | `POST /v1/payments` | `POST /v1/orders` |
| Endpoint de consulta | `GET /v1/payments/{id}` | `GET /v1/orders/{id}` |
| Medio de pago | `payment_method_id: "pse"` | `transactions.payments[0].payment_method.id: "pse"` |
| Tipo de medio | (implícito) | `transactions.payments[0].payment_method.type: "bank_transfer"` |
| Banco | `transaction_details.financial_institution` | `transactions.payments[0].payment_method.financial_institution` |
| Monto | `transaction_amount` (número) | `transactions.payments[0].amount` y `total_amount` (string) |
| IP del pagador | `additional_info.ip_address` | `additional_info.payer.ip_address` |
| URL de retorno | `callback_url` (nivel raíz) | `config.online.callback_url` |
| URL de notificación | `notification_url` (por solicitud) | **Ya no se envía** — se configura como webhook en el panel |
| Referencia persistida | `id` del pago | `id` de la **order** |
| URL de redirección | `transaction_details.external_resource_url` | `transactions.payments[0].payment_method.redirect_url` |

---

## 3. Cambios de código

### 3.1 `MercadoPagoGatewayAdapter`

- **`createPseTransaction(...)`** → ahora hace `POST /v1/orders` con la estructura
  de la Orders API (modo `automatic`). Retorna `PseTransactionResult` donde:
  - `mpPaymentId` = `id` de la **order** (referencia que persiste el dominio).
  - `status` = estado del **payment** (`transactions.payments[0].status`).
  - `externalResourceUrl` = `transactions.payments[0].payment_method.redirect_url`.
- **`getPaymentStatus(...)`** → ahora hace `GET /v1/orders/{id}` y extrae el estado
  del payment de `transactions.payments[0].status`.
- **`getAvailablePaymentMethods()`** → sin cambios (`GET /v1/payment_methods`).

> **Nota importante:** el parámetro `notificationUrl` se conserva en la firma del
> puerto por compatibilidad, pero **ya no se envía** en la petición. Las
> notificaciones (webhooks) se configuran desde el panel de Mercado Pago.

### 3.2 `ProcessPaymentWebhookService`

Se actualizó el mapeo de estados para tolerar ambos modelos:

```java
private static final String MP_STATUS_APPROVED   = "approved";   // Payments API (legacy)
private static final String MP_STATUS_PROCESSED  = "processed";  // Orders API (acreditado)
private static final String MP_STATUS_REJECTED   = "rejected";
private static final String MP_STATUS_CANCELLED  = "cancelled";
```

- **Aprobado**: `approved` (legacy) **o** `processed` (Orders API).
- **Rechazado**: `rejected` **o** `cancelled`.

### 3.3 `MercadoPagoGatewayAdapterTest`

Los tests se actualizaron para la nueva estructura (`/v1/orders`, DTOs de Orders API).

---

## 4. Modelo de estados (Orders API)

La Orders API consolida el modelo de estados. Los valores relevantes para PSE:

| Estado del pago | `status_detail` | Significado | Acción en el servicio |
|---|---|---|---|
| `pending` | `pending_waiting_transfer` | Esperando transferencia bancaria | Sin transición |
| `processed` | `accredited` | Pago acreditado | `approve()` |
| `rejected` | (varios) | Rechazado por el banco/riesgo | `reject()` |
| `cancelled` | — | Cancelado | `reject()` |

> ⚠️ **Pendiente de verificación end-to-end:** el mapeo exacto de `status_detail`
> debe validarse contra el sandbox real, ya que Mercado Pago puede ajustar los
> valores. El servicio decide la transición con el `status` (no con
> `status_detail`).

---

## 5. Webhook (notificaciones)

### Antes (Payments API)

```json
{ "action": "payment.updated", "type": "payment", "data": { "id": "1234567890" } }
```

El endpoint `POST /payment-orders/webhook` leía `data.id` (id del **pago**) y
consultaba `GET /v1/payments/{id}`.

### Ahora (Orders API)

Las notificaciones se configuran como **webhooks** en el panel de Mercado Pago
(en lugar del `notification_url` por solicitud). El endpoint
`POST /payment-orders/webhook` se mantiene, y ahora espera que `data.id` sea el
id de la **order** (la referencia que el servicio persiste).

> ⚠️ **Pendiente de verificación:** confirmar el `action`/`type` exacto del
> webhook de Orders API en el panel y ajustar `PaymentWebhookRequest` si fuera
> necesario. La lógica del servicio ya consulta `GET /v1/orders/{id}`, así que
> mientras `data.id` sea el id de la order, el flujo funciona.

---

## 6. Configuración requerida (variables de entorno)

| Variable | Estado |
|---|---|
| `MP_ACCESS_TOKEN_DEV` | Token sandbox (`TEST-...`). **No** usar `APP_USR-...` en dev. |
| `MP_PUBLIC_KEY_DEV` | Public key sandbox (`TEST-...`). |
| `MP_CALLBACK_URL_DEV` | URL del frontend (se envía en `config.online.callback_url`). |
| `MP_NOTIFICATION_URL_DEV` | Ya **no se usa por solicitud**; configurar webhook en el panel. |

---

## 7. Cómo probar el flujo PSE

1. **Crear orden**: `POST /payment-orders` (sin cambios).
2. **Enviar PSE**: `POST /payment-orders/{enrollmentId}/pse` con:
   ```json
   {
     "financialInstitution": "1007",
     "payerEmail": "test_user_123@testuser.com",
     "identificationType": "CC",
     "identificationNumber": "3548268924",
     "entityType": "individual",
     "firstName": "Juan",
     "lastName": "Perez",
     "addressZipCode": "11001",
     "addressStreetName": "Calle 1",
     "addressStreetNumber": "123",
     "addressNeighborhood": "Centro",
     "addressCity": "Bogota",
     "phoneAreaCode": "601",
     "phoneNumber": "12345"
   }
   ```
3. La respuesta `200` traerá `externalResourceUrl` (la URL de redirección del
   banco) y el estado `AWAITING_BANK_CONFIRMATION`.

> Bancos PSE válidos (sandbox Colombia): `1007` Bancolombia, `1051` Davivienda,
> `1065` Santander, `1013` BBVA, `1507` Nequi, `1551` DaviPlata, entre otros
> (consultables en `GET /v1/payment_methods`).

---

## 8. Referencias

- [Referencia API - Crear order (POST /v1/orders)](https://www.mercadopago.com.co/developers/es/reference/online-payments/checkout-api/create-order/post)
- [Cómo migrar de la Payments API a la Orders API](https://www.mercadopago.com.co/developers/es/docs/checkout-api-orders/resources/migrate-payments-to-orders)
- [PSE vía Orders API](https://www.mercadopago.com.co/developers/es/docs/checkout-api-orders/payment-integration/websites/pse)
- [Posibles errores](https://www.mercadopago.com.co/developers/es/docs/checkout-api-orders/payment-management/integration-errors/api)
