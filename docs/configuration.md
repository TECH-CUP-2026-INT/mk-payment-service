# Configuration

## Environment Requirements

| Tool | Minimum Version |
|------|-----------------|
| JDK | 21 |
| Maven | 3.9 (or use `./mvnw`, already included) |
| PostgreSQL | 16 (local via `docker-compose.yml`) |
| Python (MkDocs) | 3.10+ |

## Spring Profiles

The service uses two profiles for distinct purposes — they do **not** represent "local vs. deployed", but rather "sandbox vs. real Mercado Pago credentials":

| Profile | When to use | Mercado Pago Credentials |
|---------|-------------|--------------------------|
| `dev` | Local development or any test environment (including a staging deployment) | Sandbox (`MP_ACCESS_TOKEN_DEV` / `MP_PUBLIC_KEY_DEV`) |
| `prod` | Real production | Real (`MP_ACCESS_TOKEN_PROD` / `MP_PUBLIC_KEY_PROD`) |

Activated via `SPRING_PROFILES_ACTIVE` (defaults to `dev` if not set, see `application.yml`).

## Environment Variables

### Database

| Variable | Description | Default Value (profile `dev`) |
|----------|-------------|-------------------------------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/techcup_payments` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username | `techcup` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password | `techcup` |

In `prod`, these variables are **mandatory** with no defaults — the service will not start without them.

### Mercado Pago

| Variable | Description | Profile |
|----------|-------------|---------|
| `MP_ACCESS_TOKEN_DEV` / `MP_PUBLIC_KEY_DEV` | Sandbox credentials | `dev` |
| `MP_ACCESS_TOKEN_PROD` / `MP_PUBLIC_KEY_PROD` | Real credentials | `prod` |
| `MP_NOTIFICATION_URL_DEV` | Public URL (HTTPS) where Mercado Pago delivers webhook notifications | `dev` |
| `MP_NOTIFICATION_URL_PROD` | Same, for production | `prod` |

!!! warning "The webhook requires a public URL"
    Mercado Pago **cannot** call `localhost`. To test the webhook flow in development, expose the service with a tunnel (ngrok or similar) and set `MP_NOTIFICATION_URL_DEV` to that public URL — never leave it pointing to `localhost` in any real environment.

No credentials are hardcoded in the code or in `.yml` files — all are resolved via environment variables (`application-dev.yml` / `application-prod.yml`).

## Actual Configuration Files

`application.yml` (base, common to all profiles):

```yaml
spring:
  application:
    name: payment
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  datasource:
    url: jdbc:postgresql://localhost:5432/techcup_payments
    username: techcup
    password: techcup
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        jdbc.time_zone: UTC

mercadopago:
  base-url: https://api.mercadopago.com
  connect-timeout-millis: 3000
  read-timeout-millis: 5000
```

`application-dev.yml` (excerpt):

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/techcup_payments}
    username: ${SPRING_DATASOURCE_USERNAME:techcup}
    password: ${SPRING_DATASOURCE_PASSWORD:techcup}

mercadopago:
  access-token: ${MP_ACCESS_TOKEN_DEV}
  public-key: ${MP_PUBLIC_KEY_DEV}
  notification-url: ${MP_NOTIFICATION_URL_DEV:https://REPLACE-WITH-YOUR-NGROK-URL.ngrok-free.app/payment-orders/webhook}
```

`application-prod.yml` has no default values — everything comes via environment variables.

## Local Database with Docker

```bash
docker compose up -d postgres
```

Starts PostgreSQL 16 with the `techcup_payments` database and user `techcup`/`techcup` (matches the defaults in `application-dev.yml`). The schema is managed automatically by Flyway on service startup — migrations are in `src/main/resources/db/migration/`.

## Local Execution

### Build and run

```bash
./mvnw clean spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run tests

```bash
./mvnw test
```

Tests require Docker available: Testcontainers starts a real PostgreSQL container automatically for the context smoke test (`PaymentApplicationTests`), with no static configuration — the URI is injected at runtime via `@ServiceConnection`. The remaining tests (unit and BDD) do not touch the database: they mock the output ports directly.

### Generate JAR

```bash
./mvnw clean package
java -jar target/payment-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Build Docker image

```bash
docker build -t mk-payment-service .
```

## Interactive API Documentation

With the service running, Swagger UI is available at `http://localhost:8080/swagger-ui.html` (springdoc-openapi), and the OpenAPI JSON at `/v3/api-docs`.

## MkDocs Documentation

### Install dependencies

```bash
python -m pip install mkdocs mkdocs-material
```

### Development server

```bash
python -m mkdocs serve
```

Documentation will be available at `http://127.0.0.1:8000`.

### Production build

```bash
python -m mkdocs build
```

The static site is generated in the `site/` folder — **it is not uploaded to git** (it is in `.gitignore`), so no image or asset should be referenced from there in the `.md` files; everything must live in `docs/assets/`.

## Project Configuration Structure

```
mk-payment-service/
├── mkdocs.yml
├── requirements.txt
├── docker-compose.yml
├── Dockerfile
├── docs/
│   └── assets/
│       ├── img/
│       └── stylesheets/
│           └── extra.css
├── .github/workflows/
│   ├── ci-push.yml
│   ├── docs-pages.yml
│   ├── pr-master.yml
│   └── pr-qa.yml
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       └── db/migration/
    └── test/
        ├── java/
        └── resources/
            ├── application.yml
            └── features/
```
