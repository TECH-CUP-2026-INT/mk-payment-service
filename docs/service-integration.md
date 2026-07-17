# Service Integration

The service talks to exactly two external systems: it is consumed by **`mk-tournament-service`** over REST, and it consumes **Mercado Pago**'s REST API as the payment gateway. There is no message broker or event bus involved — both directions are synchronous HTTP.

## mk-tournament-service (inbound)

`mk-tournament-service` is the **only** consumer of this service. It calls the payment service directly, never the other way around.

| Endpoint | When it's called |
|----------|-------------------|
| `POST /payment-orders` | When a captain registers their team for a tournament, to open a payment order for that enrollment |
| `GET /payment-orders/{enrollmentId}` | To poll the order's status and decide whether to confirm or release the enrollment slot |

!!! warning "Fragile contract"
    These two endpoints are consumed by `PaymentServiceClientAdapter` in `mk-tournament-service`. Renaming a response field — even just its casing — breaks that consumer silently: its adapter never throws, it just degrades the status to `UNKNOWN`. See [API](api.md) for the full contract.

## Mercado Pago (outbound)

Mercado Pago is the payment gateway. This service calls it through `MercadoPagoGatewayAdapter`, built on Spring's `RestClient` and configured with `mercadopago.base-url` / `connect-timeout-millis` / `read-timeout-millis` (see [Configuration](configuration.md)).

| Endpoint | Used by | Purpose |
|----------|---------|---------|
| `POST /v1/payments` | `SubmitPseTransaction` | Creates the actual PSE payment, sent with an `X-Idempotency-Key` header derived from the order |
| `GET /v1/payments/{id}` | `ProcessPaymentWebhook` | The single source of truth for a payment's status — the webhook notification body is never trusted, only `data.id` is read from it |
| `GET /v1/payment_methods` | `SyncPaymentMethods` (daily job) | Refreshes the min/max PSE amount limits used to validate new orders |

Mercado Pago also calls back into this service and into the frontend:

| Direction | Configured as | Purpose |
|-----------|----------------|---------|
| Mercado Pago → this backend | `mercadopago.notification-url` (`MP_NOTIFICATION_URL_DEV`/`_PROD`) | Webhook: `POST /payment-orders/webhook`, fired whenever a payment's status changes |
| Mercado Pago → frontend | `mercadopago.callback-url` (`MP_CALLBACK_URL_DEV`/`_PROD`) | Redirects the payer back to the frontend after bank authentication, so the Payment/Status Screen Brick can read `payment_id` from the query string |

Credentials and URLs differ by Spring profile (sandbox in `dev`, real in `prod`) — never by "local vs. deployed". See [Configuration](configuration.md) for the full variable list.
