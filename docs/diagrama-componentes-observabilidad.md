# Diagrama de Componentes Especifico — Payments API

```mermaid
flowchart TB
    subgraph driving["Driving Adapters"]
        direction LR
        PAYMENT_CTRL["PaymentOrderController"]
        METHOD_CTRL["PaymentMethodController"]
        WEBHOOK["Webhook endpoint"]
    end

    subgraph domain["Domain Layer (service/)"]
        direction TB
        subgraph ports["Inbound Ports"]
            CREATE_ORDER["CreatePaymentOrder<br/>UseCase"]
            SUBMIT_PSE["SubmitPseTransaction<br/>UseCase"]
            PROCESS_WEBHOOK["ProcessPaymentWebhook<br/>UseCase"]
            GET_STATUS["GetPaymentOrderStatus<br/>UseCase"]
            GET_LIMITS["GetPaymentMethodLimits<br/>UseCase"]
            EXPIRE_TX["ExpireTransaction<br/>UseCase"]
            SYNC_METHODS["SyncPaymentMethods<br/>UseCase"]
        end

        subgraph impl["Use Case Implementations"]
            SVC_IMPL["service/impl/*"]
        end

        subgraph domain_model["Domain Model"]
            PAYMENT_ORDER["PaymentOrder<br/>(aggregate root)"]
            PAYER["Payer"]
            STATUS["PaymentOrderStatus"]
            PM_LIMITS["PaymentMethodLimits"]
        end

        ports --> SVC_IMPL --> domain_model
    end

    subgraph driven["Driven Adapters"]
        direction LR

        subgraph outbound["Outbound Ports"]
            REPO_PORT["PaymentOrder<br/>RepositoryPort"]
            LIMITS_PORT["PaymentMethodLimits<br/>RepositoryPort"]
            GW_PORT["PaymentGatewayPort"]
        end

        subgraph adapters["Adapter Implementations"]
            JPA_ADAPTER["PaymentOrderRepository<br/>Adapter<br/>(+ JPA Repository)"]
            LIMITS_ADAPTER["PaymentMethodLimits<br/>RepositoryAdapter<br/>(+ JPA Repository)"]
            MP_ADAPTER["MercadoPago<br/>GatewayAdapter<br/>(RestClient)"]
        end
    end

    subgraph external["External Systems"]
        DB[("PostgreSQL<br/>payment-db-service")]
        MP["Mercado Pago API<br/>api.mercadopago.com"]
    end

    subgraph observability["Observability Stack"]
        direction TB
        subgraph app_obs["Instrumentation"]
            ACTUATOR["/actuator/prometheus<br/>/actuator/health"]
            LOGBACK["Logback JSON<br/>→ stdout"]
            OTEL_AGENT["OpenTelemetry<br/>auto-instrument"]
        end

        subgraph collectors["Collectors"]
            PROMETHEUS["Prometheus<br/>kube-prometheus-stack"]
            OTEL_COL["OTEL Collector<br/>(:4318 OTLP)"]
            CONTAINER_INS["Container Insights<br/>(Azure Monitor Agent)"]
        end

        subgraph visualization["Visualization"]
            GRAFANA["Grafana<br/>(dashboards)"]
            APP_INSIGHTS["Application Insights<br/>(traces, dependencies)"]
            LOG_ANALYTICS["Log Analytics<br/>(KQL queries)"]
        end

        ACTUATOR -->|"metrics scrape"| PROMETHEUS
        LOGBACK -->|"stdout JSON"| CONTAINER_INS
        OTEL_AGENT -->|"OTLP gRPC"| OTEL_COL

        PROMETHEUS -->|"data source"| GRAFANA
        OTEL_COL -->|"export"| APP_INSIGHTS
        CONTAINER_INS -->|"ContainerLogV2"| LOG_ANALYTICS
        LOG_ANALYTICS -->|"data source"| GRAFANA
        APP_INSIGHTS -->|"data source"| GRAFANA
    end

    PAYMENT_CTRL --> CREATE_ORDER
    PAYMENT_CTRL --> SUBMIT_PSE
    PAYMENT_CTRL --> GET_STATUS
    METHOD_CTRL --> GET_LIMITS
    WEBHOOK --> PROCESS_WEBHOOK
    WEBHOOK -.-> EXPIRE_TX
    WEBHOOK -.-> SYNC_METHODS

    SVC_IMPL --> REPO_PORT
    SVC_IMPL --> GW_PORT
    SVC_IMPL --> LIMITS_PORT

    REPO_PORT --> JPA_ADAPTER --> DB
    LIMITS_PORT --> LIMITS_ADAPTER --> DB
    GW_PORT --> MP_ADAPTER --> MP

    PAYMENT_CTRL -.->|"auto-instrument"| OTEL_AGENT
    METHOD_CTRL -.->|"auto-instrument"| OTEL_AGENT
    PAYMENT_CTRL -.->|"auto-instrument"| LOGBACK

    style ACTUATOR fill:#6db33f,color:#fff
    style OTEL_AGENT fill:#f5a800,color:#000
    style LOGBACK fill:#6db33f,color:#fff
    style PROMETHEUS fill:#e6522c,color:#fff
    style GRAFANA fill:#f46800,color:#fff
    style APP_INSIGHTS fill:#0078d4,color:#fff
    style LOG_ANALYTICS fill:#0078d4,color:#fff
    style DB fill:#336791,color:#fff
    style MP fill:#00b4e4,color:#000
```

## Leyenda de colores

| Color | Capa |
|---|---|
| Verde | Instrumentacion de la app (Actuator, Logback) |
| Naranja | OpenTelemetry (trazas distribuidas) |
| Rojo | Prometheus (metricas) |
| Naranja oscuro | Grafana (dashboard unificado) |
| Azul | Azure (App Insights + Log Analytics) |

## Flujos de telemetria

1. **Metricas**: `/actuator/prometheus` → Prometheus scrape (30s) → Grafana dashboards
2. **Logs**: stdout JSON estructurado → Container Insights → Log Analytics → consultas KQL / Grafana
3. **Trazas**: OTEL auto-instrument → OTLP (gRPC) → OTEL Collector → Application Insights

## Stack tecnologico de observabilidad

| Componente | Herramienta | Endpoint |
|---|---|---|
| Metricas app | Micrometer + Prometheus registry | `GET /actuator/prometheus` |
| Logging | Logback + Logstash encoder | stdout (JSON) |
| Trazas | OpenTelemetry spring-boot-starter | OTLP → Collector `:4318` |
| Collector | OTEL Collector (contrib) | → App Insights |
| Metricas infra | Node Exporter + kube-state-metrics | → Prometheus |
| Dashboard | Grafana (kube-prometheus-stack) | `http://20.12.84.133/` |
