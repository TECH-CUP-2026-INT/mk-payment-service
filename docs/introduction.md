# Introduction

## Context

The **Payment Service** (`mk-payment-service`) is part of the **Mortal Kodebat** ecosystem of TECH-CUP 2026. It processes tournament enrollment payments for TechCup Football via **Mercado Pago**, exclusively using **PSE** (Pagos Seguros en Línea) as the payment method.

`mk-tournament-service` is the only consumer of this service: it creates a payment order when a captain registers their team, and queries its status to decide whether to confirm or release the slot.

## Objective

Provide a microservice that enables:

- Creating a payment order associated with an enrollment and validating its amount against Mercado Pago's current PSE limits.
- Receiving the payer's banking details and initiating the PSE transaction with Mercado Pago.
- Confirming the payment result exclusively through Mercado Pago's source of truth (never trusting the webhook body).
- Automatically expiring orders that are not completed in time.
- Exposing a stable REST API already consumed by `mk-tournament-service`.

## Scope

!!! success "Included"
    - The 7 use cases of the payment order lifecycle: `CreatePaymentOrder`, `SubmitPseTransaction`, `ProcessPaymentWebhook`, `GetPaymentOrderStatus`, `ExpireTransaction`, `GetPaymentMethodLimits`, `SyncPaymentMethods`.
    - Hexagonal architecture with layered separation (see [Architecture](architecture.md)).
    - Persistence in PostgreSQL (Spring Data JPA) with Flyway-managed schema and optimistic locking.
    - Integration with Mercado Pago's real API (`/v1/payments`, `/v1/payment_methods`) via `RestClient`.
    - Interactive API documentation with Swagger UI (springdoc-openapi).
    - Technical documentation with MkDocs.

!!! warning "Out of scope (for now)"
    - Payment methods other than PSE (credit card, cash, etc.).
    - Refunds.
    - Direct push notifications to the end user — that is handled by `mk-tournament-service` querying the status.

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Persistence | PostgreSQL 16 + Spring Data JPA + Flyway |
| Mapping | MapStruct (domain↔entity, domain↔DTO) |
| Payment gateway | Mercado Pago (PSE), via `RestClient` |
| Testing | JUnit 5, Mockito, AssertJ, Cucumber (BDD) |
| API documentation | springdoc-openapi (Swagger UI) |
| Technical documentation | MkDocs Material |
| Build | Maven |

## Repository Conventions

- Base package: `co.edu.escuelaing.techcup.payment`
- Default repository branch: `main`
- Documentation in the `docs/` folder
