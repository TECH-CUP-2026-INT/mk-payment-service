# Anexos

## Glosario

| Término | Definición |
|---------|------------|
| **Hexagonal Architecture** | Patrón que separa la lógica de negocio de los adaptadores de entrada y salida |
| **Port** | Interfaz que define un contrato entre el dominio y el exterior |
| **Adapter** | Implementación concreta de un puerto (HTTP, PostgreSQL, etc.) |
| **DTO** | Objeto de transferencia de datos entre capas o servicios |
| **Use Case** | Caso de uso que encapsula una operación de negocio |
| **PSE** | Pagos Seguros en Línea — único método de pago que soporta este servicio |
| **Idempotency Key** | Clave única generada por orden que se envía a Mercado Pago para evitar procesar la misma solicitud dos veces |
| **Optimistic Locking** | Control de concurrencia vía columna `version` (`@Version`): si dos procesos intentan actualizar la misma orden, el segundo en escribir falla y se ignora silenciosamente |
| **EXPIRED** | Estado interno de una orden vencida sin confirmar; se mapea a `REJECTED` en la API pública, nunca se expone tal cual |

## Referencias

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Testcontainers](https://testcontainers.com/)
- [Mercado Pago — API de Pagos](https://www.mercadopago.com.co/developers/es/reference/payments/_payments/post)
- [Mercado Pago — Métodos de pago](https://www.mercadopago.com.co/developers/es/reference/payment_methods/_payment_methods/get)
- [springdoc-openapi](https://springdoc.org/)
- [Cucumber (BDD)](https://cucumber.io/docs/cucumber/)
- [MkDocs Material](https://squidfunk.github.io/mkdocs-material/)
- [Mermaid Diagrams](https://mermaid.js.org/)

## Estructura de documentación

```
docs/
├── index.md
├── introduccion.md
├── requerimientos.md
├── configuracion.md
├── arquitectura.md
├── api.md
├── pruebas.md
├── equipo.md
├── anexos.md
└── assets/
    ├── img/
    ├── diagrams/
    └── stylesheets/
        └── extra.css
```

## Paleta de colores TechCup

Los estilos personalizados se definen en `docs/assets/stylesheets/extra.css`:

| Modo | Color primario | Color de acento |
|------|----------------|-----------------|
| Claro | Morado `#6a1b9a` | Dorado `#c9a227` |
| Oscuro | Morado `#7b1fa2` | Dorado `#d4af37` |

## Historial de cambios

| Versión | Fecha | Descripción |
|---------|-------|-------------|
| 0.1.0 | 2026-07-13 | Scaffolding inicial del servicio y documentación MkDocs |
| 0.2.0 | 2026-07-13 | Implementación completa: dominio `PaymentOrder`, los 7 casos de uso PSE, persistencia PostgreSQL/JPA con optimistic locking, integración con Mercado Pago, Swagger UI, CI/CD (GitHub Actions + Docker), 58 pruebas (unitarias + BDD). Documentación actualizada para reflejar la implementación real. |
