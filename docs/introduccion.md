# Introducción

## Contexto

El **Payment Service** (`mk-payment-service`) forma parte del ecosistema **Astro Merge** de TECH-CUP 2026. Procesa el pago de inscripciones a torneos de TechCup Fútbol mediante **Mercado Pago**, usando exclusivamente **PSE** (Pagos Seguros en Línea) como método de pago.

`mk-tournament-service` es el único consumidor de este servicio: crea una orden de pago cuando un capitán inscribe a su equipo, y consulta su estado para decidir si confirma o libera el cupo.

## Objetivo

Proporcionar un microservicio que permita:

- Crear una orden de pago asociada a una inscripción y validar su monto contra los límites vigentes de Mercado Pago para PSE.
- Recibir los datos bancarios del pagador e iniciar la transacción PSE ante Mercado Pago.
- Confirmar el resultado del pago exclusivamente a través de la fuente de verdad de Mercado Pago (nunca confiando en el cuerpo del webhook).
- Expirar automáticamente las órdenes que no se completan a tiempo.
- Exponer una API REST estable y ya consumida por `mk-tournament-service`.

## Alcance

!!! success "Incluido"
    - Los 7 casos de uso del ciclo de vida de una orden de pago: `CreatePaymentOrder`, `SubmitPseTransaction`, `ProcessPaymentWebhook`, `GetPaymentOrderStatus`, `ExpireTransaction`, `GetPaymentMethodLimits`, `SyncPaymentMethods`.
    - Arquitectura hexagonal con separación de capas (ver [Arquitectura](arquitectura.md)).
    - Persistencia en MongoDB (Spring Data MongoDB) con índices declarados por anotación y optimistic locking.
    - Integración con la API real de Mercado Pago (`/v1/payments`, `/v1/payment_methods`) vía `RestClient`.
    - Documentación interactiva de la API con Swagger UI (springdoc-openapi).
    - Documentación técnica con MkDocs.

!!! warning "Fuera de alcance (por ahora)"
    - Métodos de pago distintos a PSE (tarjeta de crédito, efectivo, etc.).
    - Reembolsos.
    - Notificaciones push directas al usuario final — eso lo resuelve `mk-tournament-service` consultando el estado.

## Stack tecnológico

| Componente | Tecnología |
|------------|------------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Persistencia | MongoDB 7 + Spring Data MongoDB |
| Mapeo | Clases estáticas propias (dominio↔documento, dominio↔DTO) — sin MapStruct |
| Pasarela de pago | Mercado Pago (PSE), vía `RestClient` |
| Pruebas | JUnit 5, Mockito, AssertJ, Cucumber (BDD) |
| Documentación de API | springdoc-openapi (Swagger UI) |
| Documentación técnica | MkDocs Material |
| Build | Maven |

## Convenciones del repositorio

- Paquete base: `co.edu.escuelaing.techcup.payment`
- Rama por defecto del repositorio: `main`
- Documentación en la carpeta `docs/`
