# Architecture

## System architecture

The service follows a **hexagonal architecture** (Ports & Adapters), separating business logic from infrastructure concerns: the domain never depends on Spring, PostgreSQL, or HTTP.

```mermaid
flowchart TB
    subgraph driving["Driving Adapters"]
        IMPL["controller/impl"]
        SCHED["scheduler"]
    end

    subgraph domain["Domain Layer"]
        PORTS["service/ports"]
        SVC["service/impl"]
        DOM["service (domain)"]
    end

    subgraph driven["Driven Adapters"]
        JPA_REPO["repository/jpa"]
        ADAPTER["repository/adapter"]
    end

    subgraph external["External Systems"]
        DB[("PostgreSQL")]
        MP["Mercado Pago API"]
    end

    IMPL --> PORTS
    SCHED --> PORTS
    PORTS --> SVC --> DOM
    SVC --> ADAPTER
    ADAPTER --> JPA_REPO --> DB
    ADAPTER --> MP
```

## Architecture decisions

### Why REST, not WebSocket?

A payment order goes through very few state transitions (`PENDING` → `AWAITING_BANK_CONFIRMATION` → `APPROVED`/`REJECTED`/`EXPIRED`), and nothing in this service needs to push updates to a client in real time. The frontend's Status Screen Brick and `mk-tournament-service` simply poll `GET /payment-orders/{enrollmentId}`, and Mercado Pago itself notifies status changes with a plain HTTP webhook (`POST /payment-orders/webhook`) instead of a persistent connection. Keeping everything as stateless request/response REST avoids the operational cost of holding open connections and keeps the service trivially scalable horizontally (see RNF-02 in [Requirements](requirements.md)).

### Why PostgreSQL and not NoSQL?

Payment orders are money-bearing records with a small, fixed, well-understood shape and strict consistency requirements: two concurrent updates to the same order (e.g. a webhook notification racing the expiration job) must not both succeed silently. PostgreSQL's transactions and the `version` column (`@Version`, optimistic locking) give that guarantee directly. Flyway migrations also make schema evolution explicit and auditable — properties a schemaless document store would trade away for flexibility this domain doesn't need.

### Inter-service communication: REST

All communication in this system is synchronous HTTP, in both directions — there is no message broker or event bus:

- **Inbound:** `mk-tournament-service` calls this service's REST API directly (`POST /payment-orders`, `GET /payment-orders/{enrollmentId}`).
- **Outbound:** this service calls Mercado Pago's REST API (`/v1/payments`, `/v1/payment_methods`) via `RestClient`.

See [Service Integration](service-integration.md) for the full picture of both integrations.

## Design patterns

- **Ports & Adapters (Hexagonal Architecture):** inbound ports (`XxxUseCase`) are implemented by `service/impl`; outbound ports (`XxxRepositoryPort`, `PaymentGatewayPort`) are implemented by `repository/adapter`.
- **Repository pattern:** `repository/jpa` wraps Spring Data JPA; `repository/adapter` adapts it (and Mercado Pago) to the domain's outbound ports.
- **Mapper pattern:** static mapper classes (`mapper/`) handle all domain↔entity and domain↔DTO conversion, keeping that logic out of services and controllers.
- **Scheduled Job pattern:** `scheduler/` triggers use cases by time (`ExpireTransaction`, `SyncPaymentMethods`) through the same inbound ports the HTTP controllers use, instead of duplicating logic.

Design principles behind these choices: dependency inversion (domain has no framework dependencies), single responsibility per layer, testability (use cases are tested without a Spring context), and independent evolution of adapters.

## Components

```
src/main/java/co/edu/escuelaing/techcup/payment/
├── config/                     ← CONFIG LAYER (Scheduling, infrastructure beans)
├── controller/impl/            ← DRIVING ADAPTERS (@RestController)
├── scheduler/                  ← DRIVING ADAPTERS triggered by time (@Scheduled)
├── dto/                        ← DATA TRANSFER OBJECTS
│   ├── request/                (Records for HTTP Requests)
│   └── response/               (Records for HTTP Responses)
├── entity/                     ← PERSISTENCE LAYER (JPA Entities)
├── exception/                  ← SYSTEM EXCEPTIONS
├── mapper/                     ← Static classes: domain↔entity, domain↔DTO
├── repository/                 ← REPOSITORIES & ADAPTERS
│   ├── jpa/                    (Spring Data JPA Repository interfaces)
│   └── adapter/                (Outbound Ports Implementation: PostgreSQL + Mercado Pago)
├── service/                    ← DOMAIN / CORE LAYER
│   ├── ports/                  (Inbound/Outbound Interfaces)
│   └── impl/                   (Use Cases and Business Rules)
└── PaymentApplication.java
```

| Layer | Package | Responsibility |
|-------|---------|-----------------|
| Config | `config` | Beans, scheduling, global configuration |
| Driving (HTTP) | `controller/impl` | Expose HTTP endpoints, validate input |
| Driving (cron) | `scheduler` | Trigger use cases by time instead of HTTP |
| DTO | `dto` | Input and output API contracts |
| Entity | `entity` | JPA persistence models |
| Exception | `exception` | Domain exceptions and global handlers |
| Mapper | `mapper` | Conversion between DTO, domain, and entities |
| Repository | `repository` | Data access and outbound port implementations |
| Service | `service` | Business rules and use cases |

## General flow

1. The HTTP client invokes an endpoint in `controller/impl` (or a cron triggers a job in `scheduler`).
2. The controller/job delegates to the corresponding inbound port in `service/ports` (`XxxUseCase`).
3. `service/impl` executes application rules, delegating business rules of the aggregate to the domain (`service`, root package).
4. If persistence is required, the outbound port (`XxxRepositoryPort`) is invoked, implemented in `repository/adapter`, which uses `repository/jpa` to communicate with PostgreSQL via Spring Data JPA.
5. If Mercado Pago communication is needed, `PaymentGatewayPort` is invoked, implemented by `MercadoPagoGatewayAdapter` via `RestClient`.
6. The result is mapped to a response DTO (`mapper/XxxRestMapper`) and returned to the client.

## UML and architecture diagrams

General component view of the service:

![General component diagram](assets/diagrams/Diagrama%20de%20componentes.png)

Detailed view of components and their responsibilities:

![Detailed component diagram](assets/diagrams/Diagrama%20de%20componentes%20especificos.png)

## Observability architecture

The following diagram shows how the three pillars of observability — metrics, logs, and traces — are instrumented, collected, and visualized across the Azure and Kubernetes stack:

```mermaid
flowchart TB
    subgraph azure["Azure Cloud"]
        direction TB
        subgraph monitor["Azure Monitor"]
            AI["App Insights<br/>(traces, dependencies)"]
            LA["Log Analytics<br/>(ContainerLogV2)"]
        end
        KV["Key Vault<br/>(secrets)"]
    end

    subgraph aks["AKS Cluster"]
        direction TB
        subgraph observability["Observability Namespace"]
            PROM["Prometheus<br/>(kube-prometheus-stack)"]
            GRAFANA["Grafana<br/>(dashboards)"]
        end

        subgraph payments_ns["payments Namespace"]
            INGRESS["NGINX Ingress"]
            API["payments-api<br/>(Spring Boot)"]
            OTEL["OTEL Collector"]
        end

        subgraph infra_ns["monitoring Namespace"]
            NODE_EXP["Node Exporter"]
            KSM["kube-state-metrics"]
        end
    end

    subgraph external["External"]
        USER["User / Frontend"]
        MP["Mercado Pago API"]
        DB[("PostgreSQL<br/>Azure DB")]
    end

    USER -->|"HTTP"| INGRESS
    INGRESS --> API

    API -->|"SQL"| DB
    API -->|"REST"| MP

    API -->|"stdout JSON"| LA
    API -->|"OTLP (traces/metrics/logs)"| OTEL
    API -->|"/actuator/prometheus"| PROM

    OTEL -->|"App Insights export"| AI

    PROM -->|"data source"| GRAFANA
    LA -->|"data source (KQL)"| GRAFANA
    AI -->|"data source"| GRAFANA

    NODE_EXP -->|"node metrics"| PROM
    KSM -->|"cluster metrics"| PROM

    KV -.->|"CSI Secrets Store<br/>Workload Identity"| API

    style API fill:#6db33f,color:#fff
    style OTEL fill:#f5a800,color:#000
    style PROM fill:#e6522c,color:#fff
    style GRAFANA fill:#f46800,color:#fff
    style AI fill:#0078d4,color:#fff
    style LA fill:#0078d4,color:#fff
    style KV fill:#0078d4,color:#fff
```

### Observability layers

| Layer | Tool | Endpoint / Source | Destination |
|---|---|---|---|
| **Metrics** | Micrometer → Prometheus | `GET /actuator/prometheus` | Prometheus → Grafana dashboards |
| **Logs** | Logback JSON (Logstash encoder) | stdout (structured JSON) | Container Insights → Log Analytics (`ContainerLogV2`) |
| **Traces** | OpenTelemetry (auto-instrument) | OTLP → OTEL Collector (`:4318`) | App Insights |
| **Secrets** | CSI Secrets Store + Workload Identity | Azure Key Vault | K8s Secret → env vars |
| **Infra metrics** | Node Exporter + kube-state-metrics | kubelet / API server | Prometheus → Grafana |

### Key configuration

**Spring Boot** (`application.yml`):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    tags:
      application: ${SERVICE_NAME}
      environment: ${ENVIRONMENT}
```

**Logback** (`logback-spring.xml`): writes structured JSON with `service.name`, `deployment.environment`, `trace.id`, `span.id` to stdout.

**OTEL Collector**: receives OTLP data from the app on port 4318, exports to Azure Application Insights.

**Prometheus**: scrapes `/actuator/prometheus` every 30s via ServiceMonitor.

**Grafana**: pre-configured with Prometheus data source. Additional data sources for Log Analytics and App Insights can be added.

## Diagrams

Sequence diagrams for each REST use case:

| Use case | Diagram |
|----------|---------|
| TC-PAY-01 — Create payment order | ![Create payment order](assets/diagrams/payment_tc01_create_payment_order.png) |
| TC-PAY-02 — Submit PSE transaction | ![Submit PSE transaction](assets/diagrams/payment_tc02_submit_pse_transaction_v2.png) |
| TC-PAY-03 — Process payment webhook | ![Process payment webhook](assets/diagrams/payment_tc03_process_payment_webhook.png) |
| TC-PAY-04 — Get payment order status | ![Get payment order status](assets/diagrams/payment_tc04_get_payment_order_status.png) |
| TC-PAY-06 — Get payment method limits | ![Get payment method limits](assets/diagrams/payment_tc05_get_payment_method_limits.png) |

Database tables:

![Database table diagram for the payment service](assets/img/tablas_service_payment.png)
