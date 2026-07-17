# Appendices

## Glossary

| Term | Definition |
|------|------------|
| **Hexagonal Architecture** | Pattern that separates business logic from input and output adapters |
| **Port** | Interface that defines a contract between the domain and the outside |
| **Adapter** | Concrete implementation of a port (HTTP, PostgreSQL, etc.) |
| **DTO** | Data Transfer Object between layers or services |
| **Use Case** | Encapsulates a business operation |
| **PSE** | Pagos Seguros en Línea — the only payment method supported by this service |
| **Idempotency Key** | Unique key generated per order, sent to Mercado Pago to prevent processing the same request twice |
| **Optimistic Locking** | Concurrency control via the `version` column (`@Version`): if two processes try to update the same order, the second write fails and is silently ignored |
| **EXPIRED** | Internal status of an overdue order without confirmation; mapped to `REJECTED` in the public API, never exposed as-is |

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Testcontainers](https://testcontainers.com/)
- [Mercado Pago — Payments API](https://www.mercadopago.com.co/developers/en/reference/payments/_payments/post)
- [Mercado Pago — Payment Methods](https://www.mercadopago.com.co/developers/en/reference/payment_methods/_payment_methods/get)
- [springdoc-openapi](https://springdoc.org/)
- [Cucumber (BDD)](https://cucumber.io/docs/cucumber/)
- [MkDocs Material](https://squidfunk.github.io/mkdocs-material/)
- [Mermaid Diagrams](https://mermaid.js.org/)

## Documentation Structure

```
docs/
├── index.md
├── introduction.md
├── requirements.md
├── configuration.md
├── architecture.md
├── api.md
├── testing.md
├── team.md
├── appendices.md
└── assets/
    ├── img/
    ├── diagrams/
    └── stylesheets/
        └── extra.css
```

## TechCup Color Palette

Custom styles are defined in `docs/assets/stylesheets/extra.css`:

| Mode | Primary Color | Accent Color |
|------|---------------|--------------|
| Light | Purple `#6a1b9a` | Gold `#c9a227` |
| Dark | Purple `#7b1fa2` | Gold `#d4af37` |

## Changelog

| Version | Date | Description |
|---------|------|-------------|
| 0.1.0 | 2026-07-13 | Initial scaffolding of the service and MkDocs documentation |
| 0.2.0 | 2026-07-13 | Full implementation: `PaymentOrder` domain, 7 PSE use cases, PostgreSQL/JPA persistence with optimistic locking, Mercado Pago integration, Swagger UI, CI/CD (GitHub Actions + Docker), 58 tests (unit + BDD). Documentation updated to reflect the actual implementation. |
