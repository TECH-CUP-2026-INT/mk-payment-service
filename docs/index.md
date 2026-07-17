# Payment Service

Welcome to the technical documentation of the **payment microservice** of the **Astro Merge** system, developed by the **TECH-CUP 2026 INT** team.

It processes tournament enrollment payments through **Mercado Pago**, exclusively using **PSE** (Pagos Seguros en Línea / Online Secure Payments), and exposes the REST contract already consumed by `mk-tournament-service`.

## Quick Start

=== "Prerequisites"

    - Java 21
    - Maven 3.9+
    - PostgreSQL 16 (local via `docker compose up -d postgres`, see `docker-compose.yml`)
    - Mercado Pago sandbox credentials (see [Configuration](configuration.md))

=== "Run locally"

    ```bash
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
    ```

=== "Run tests"

    ```bash
    mvn test
    ```

=== "Documentation"

    ```bash
    python -m mkdocs serve
    ```

## Navigation

| Section | Description |
|---------|-------------|
| [Introduction](introduction.md) | Context and scope of the service |
| [Requirements](requirements.md) | Functional and non-functional requirements |
| [Configuration](configuration.md) | Environment variables and deployment |
| [Architecture](architecture.md) | Hexagonal architecture and code structure |
| [API](api.md) | Exposed REST endpoints |
| [Testing](testing.md) | Test strategy and execution |
| [Team](team.md) | Project members |
| [Appendices](appendices.md) | References and supplementary material |

[View architecture :material-arrow-right:](architecture.md){ .md-button .md-button--primary }
