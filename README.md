# payment-service

Microservicio encargado de gestionar el ciclo de vida de los pagos de inscripción a torneos de **TECH CUP 2026**, integrando con **Mercado Pago** mediante PSE (Pagos Seguros en Línea) como único método de pago.

## Integrantes

- Hernan David Sanchez Alarcon

---

## Descripción

`payment-service` es el microservicio responsable de procesar los pagos de inscripción de equipos a torneos. Es consumido exclusivamente por `mk-tournament-service`, que crea una orden de pago cuando un capitán inscribe su equipo y consulta su estado para confirmar o liberar el cupo.

El servicio sigue una **arquitectura hexagonal (Ports & Adapters)**, separando completamente la lógica de negocio de los detalles de infraestructura como la base de datos y la pasarela de pago.

---

## Funcionalidades

| Código | Funcionalidad |
|--------|--------------|
| TC-PAY-01 | Crear una orden de pago asociada a una inscripción, validando el monto contra los límites vigentes de Mercado Pago para PSE |
| TC-PAY-02 | Recibir los datos bancarios del pagador e iniciar la transacción PSE ante Mercado Pago |
| TC-PAY-03 | Procesar el webhook de Mercado Pago para confirmar o rechazar el pago, consultando siempre la fuente de verdad |
| TC-PAY-04 | Consultar el estado actual de una orden de pago por `enrollmentId` |
| TC-PAY-05 | Expirar automáticamente las órdenes que no se completan en 60 minutos |
| TC-PAY-06 | Consultar los límites de monto permitidos por Mercado Pago para PSE |
| TC-PAY-07 | Sincronizar diariamente los límites de métodos de pago desde Mercado Pago |

### Modelo de estados de una orden

```
PENDING → AWAITING_BANK_CONFIRMATION → APPROVED
                                     → REJECTED
PENDING → EXPIRED
AWAITING_BANK_CONFIRMATION → EXPIRED
```

---

## Tecnologías

| Componente | Tecnología |
|------------|------------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Persistencia | PostgreSQL 16 + Spring Data JPA + Flyway |
| Pasarela de pago | Mercado Pago API (PSE) vía `RestClient` |
| Documentación API | springdoc-openapi (Swagger UI) |
| Pruebas | JUnit 5, Mockito, AssertJ, Cucumber (BDD), Testcontainers |
| Build | Maven |
| Contenedores | Docker + Docker Compose |
| Mapeo | Lombok + MapStruct |

---

## Estructura del proyecto

```
src/main/java/co/edu/escuelaing/techcup/payment/
├── config/           → Configuración de scheduling y beans
├── controller/impl/  → Endpoints REST (@RestController)
├── scheduler/        → Jobs programados (@Scheduled)
├── dto/              → Records de request y response
├── entity/           → Entidades JPA (PostgreSQL)
├── exception/        → Excepciones de dominio y manejador global
├── mapper/           → Conversión dominio ↔ entidad ↔ DTO
├── repository/
│   ├── jpa/          → Interfaces Spring Data JPA
│   └── adapter/      → Implementación de puertos salientes
└── service/
    ├── ports/        → Interfaces de casos de uso y puertos
    └── impl/         → Implementación de casos de uso
```

---

## Diagramas

| Diagrama | Archivo |
|----------|---------|
| Diagrama de clases | [Diagrama de clases.png](docs/assets/diagrams/Diagrama%20de%20clases.png) |
| Diagrama de componentes | [Diagrama de componentes.png](docs/assets/diagrams/Diagrama%20de%20componentes.png) |
| Diagrama de componentes específicos | [Diagrama de componentes especificos.png](docs/assets/diagrams/Diagrama%20de%20componentes%20especificos.png) |
| Diagrama de secuencia TC-PAY-01 | [payment_tc01_create_payment_order.png](docs/assets/diagrams/payment_tc01_create_payment_order.png) |
| Diagrama de secuencia TC-PAY-02 | [payment_tc02_submit_pse_transaction_v2.png](docs/assets/diagrams/payment_tc02_submit_pse_transaction_v2.png) |
| Diagrama de secuencia TC-PAY-03 | [payment_tc03_process_payment_webhook.png](docs/assets/diagrams/payment_tc03_process_payment_webhook.png) |
| Diagrama de secuencia TC-PAY-04 | [payment_tc04_get_payment_order_status.png](docs/assets/diagrams/payment_tc04_get_payment_order_status.png) |
| Diagrama de secuencia TC-PAY-05 | [payment_tc05_get_payment_method_limits.png](docs/assets/diagrams/payment_tc05_get_payment_method_limits.png) |

---

## Cómo ejecutar el proyecto

### Prerrequisitos

- Java 21
- Maven 3.9+
- Docker y Docker Compose
- Credenciales de Mercado Pago (access token)

### 1. Clonar el repositorio

```bash
git clone https://github.com/TECH-CUP-2026-INT/mk-payment-service.git
cd mk-payment-service
```

### 2. Configurar variables de entorno

Crea un archivo `.env` en la raíz del proyecto con las siguientes variables:

```env
MP_ACCESS_TOKEN_DEV=<tu_access_token_de_mercado_pago>
MP_PUBLIC_KEY_DEV=<tu_public_key_de_mercado_pago>
MP_NOTIFICATION_URL_DEV=https://<tu-ngrok-url>.ngrok-free.app/payment-orders/webhook
MP_CALLBACK_URL_DEV=https://<tu-frontend-url>/checkout/pse-return
```

### 3. Levantar con Docker Compose

```bash
docker-compose up --build
```

El servicio quedará disponible en `http://localhost:5624`.

### 4. Ejecutar en local (sin Docker)

Levanta primero una instancia de PostgreSQL y luego:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 5. Ejecutar pruebas

```bash
./mvnw test
```

### 6. API Docs

Con el servicio corriendo, accede a la documentación interactiva en:

```
http://localhost:5624/swagger-ui.html
```

---

## Endpoints principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/payment-orders` | Crear orden de pago |
| `POST` | `/payment-orders/{enrollmentId}/pse` | Iniciar transacción PSE |
| `POST` | `/payment-orders/webhook` | Webhook de Mercado Pago |
| `GET`  | `/payment-orders/{enrollmentId}` | Consultar estado de orden |
| `GET`  | `/payment-methods/limits?amount=` | Consultar límites PSE |
