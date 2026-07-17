# Appendices

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Testcontainers](https://testcontainers.com/)
- [Mercado Pago — Payments API](https://www.mercadopago.com.co/developers/en/reference/payments/_payments/post)
- [Mercado Pago — Payment Methods](https://www.mercadopago.com.co/developers/en/reference/payment_methods/_payment_methods/get)
- [Cucumber (BDD)](https://cucumber.io/docs/cucumber/)

## Bibliography

### Documentation tools

- [MkDocs Material](https://squidfunk.github.io/mkdocs-material/)
- [springdoc-openapi](https://springdoc.org/)
- [Mermaid Diagrams](https://mermaid.js.org/)

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
