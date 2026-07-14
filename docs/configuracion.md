# Configuración

## Requisitos del entorno

| Herramienta | Versión mínima |
|-------------|----------------|
| JDK | 21 |
| Maven | 3.9 (o usar `./mvnw`, ya incluido) |
| PostgreSQL | 16 (local vía `docker-compose.yml`) |
| Python (MkDocs) | 3.10+ |

## Perfiles de Spring

El servicio usa dos perfiles con propósitos distintos — **no representan "local vs. desplegado"**, sino "credenciales sandbox vs. credenciales reales de Mercado Pago":

| Perfil | Cuándo usarlo | Credenciales de Mercado Pago |
|--------|----------------|-------------------------------|
| `dev` | Desarrollo local o cualquier ambiente de prueba (incluido un despliegue de staging) | Sandbox (`MP_ACCESS_TOKEN_DEV` / `MP_PUBLIC_KEY_DEV`) |
| `prod` | Producción real | Reales (`MP_ACCESS_TOKEN_PROD` / `MP_PUBLIC_KEY_PROD`) |

Se activa con `SPRING_PROFILES_ACTIVE` (por defecto `dev` si no se define, ver `application.yml`).

## Variables de entorno

### Base de datos

| Variable | Descripción | Valor por defecto (perfil `dev`) |
|----------|-------------|-----------------------------------|
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/techcup_payments` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos | `techcup` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos | `techcup` |

En `prod` estas tres variables son **obligatorias**, sin valor por defecto — el servicio no arranca sin ellas.

### Mercado Pago

| Variable | Descripción | Perfil |
|----------|-------------|--------|
| `MP_ACCESS_TOKEN_DEV` / `MP_PUBLIC_KEY_DEV` | Credenciales de sandbox | `dev` |
| `MP_ACCESS_TOKEN_PROD` / `MP_PUBLIC_KEY_PROD` | Credenciales reales | `prod` |
| `MP_NOTIFICATION_URL_DEV` | URL pública (HTTPS) donde Mercado Pago entrega las notificaciones del webhook | `dev` |
| `MP_NOTIFICATION_URL_PROD` | Igual, para producción | `prod` |

!!! warning "El webhook necesita una URL pública"
    Mercado Pago **no puede** llamar a `localhost`. Para probar el flujo de webhook en desarrollo, expón el servicio con un túnel (ngrok o similar) y define `MP_NOTIFICATION_URL_DEV` con esa URL pública — nunca la dejes apuntando a `localhost` en ningún ambiente real.

Ninguna credencial vive hardcodeada en el código ni en los `.yml` — todas se resuelven vía variables de entorno (`application-dev.yml` / `application-prod.yml`).

## Archivos de configuración reales

`application.yml` (base, común a todos los perfiles):

```yaml
spring:
  application:
    name: payment
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

mercadopago:
  base-url: https://api.mercadopago.com
  connect-timeout-millis: 3000
  read-timeout-millis: 5000
```

`application-dev.yml` (extracto):

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

`application-prod.yml` no trae ningún valor por defecto — todo llega por variable de entorno.

## Base de datos local con Docker

```bash
docker compose up -d postgres
```

Levanta Postgres 16 con la base `techcup_payments` y usuario/clave `techcup`/`techcup` (coincide con los defaults de `application-dev.yml`). El esquema lo aplica Flyway automáticamente al arrancar el servicio (`src/main/resources/db/migration/V1__init.sql`).

## Ejecución local

### Compilar y ejecutar

```bash
./mvnw clean spring-boot:run -Dspring-boot.run.profiles=dev
```

### Ejecutar pruebas

```bash
./mvnw test
```

Las pruebas **no** requieren Postgres levantado: corren contra H2 en memoria en modo compatibilidad PostgreSQL (`src/test/resources/application.yml`), aplicando la misma migración de Flyway.

### Generar JAR

```bash
./mvnw clean package
java -jar target/payment-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Construir la imagen Docker

```bash
docker build -t mk-payment-service .
```

## Documentación interactiva de la API

Con el servicio corriendo, Swagger UI queda disponible en `http://localhost:8080/swagger-ui.html` (springdoc-openapi), y el JSON de OpenAPI en `/v3/api-docs`.

## Documentación MkDocs

### Instalar dependencias

```bash
python -m pip install mkdocs mkdocs-material
```

### Servidor de desarrollo

```bash
python -m mkdocs serve
```

La documentación estará disponible en `http://127.0.0.1:8000`.

### Build de producción

```bash
python -m mkdocs build
```

El sitio estático se genera en la carpeta `site/` — **no se sube a git** (está en `.gitignore`), así que ninguna imagen o asset debe referenciarse desde ahí en los `.md`; todo debe vivir en `docs/assets/`.

## Estructura de configuración del proyecto

```
mk-payment-service/
├── mkdocs.yml
├── docker-compose.yml
├── Dockerfile
├── docs/
│   └── assets/
│       ├── img/
│       └── stylesheets/
│           └── extra.css
├── .github/workflows/
│   ├── ci-push.yml
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
    │       └── db/migration/V1__init.sql
    └── test/
        ├── java/
        └── resources/
            ├── application.yml
            └── features/
```
