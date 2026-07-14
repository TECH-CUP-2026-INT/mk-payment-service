# Payment Service

Bienvenido a la documentación técnica del **microservicio de pagos** del sistema **Astro Merge**, desarrollado por el equipo **TECH-CUP 2026 INT**.

Procesa el pago de inscripciones a torneos mediante **Mercado Pago**, exclusivamente con **PSE** (Pagos Seguros en Línea), y expone el contrato REST que ya consume `mk-tournament-service`.

## Inicio rápido

=== "Requisitos previos"

    - Java 21
    - Maven 3.9+
    - MongoDB 7 (local vía `docker compose up -d mongodb`, ver `docker-compose.yml`)
    - Credenciales de sandbox de Mercado Pago (ver [Configuración](configuracion.md))

=== "Ejecutar localmente"

    ```bash
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
    ```

=== "Ejecutar pruebas"

    ```bash
    mvn test
    ```

=== "Documentación"

    ```bash
    python -m mkdocs serve
    ```

## Navegación

| Sección | Descripción |
|---------|-------------|
| [Introducción](introduccion.md) | Contexto y alcance del servicio |
| [Requerimientos](requerimientos.md) | Requisitos funcionales y no funcionales |
| [Configuración](configuracion.md) | Variables de entorno y despliegue |
| [Arquitectura](arquitectura.md) | Diseño hexagonal y estructura del código |
| [API](api.md) | Endpoints REST expuestos |
| [Pruebas](pruebas.md) | Estrategia y ejecución de tests |
| [Equipo](equipo.md) | Integrantes del proyecto |
| [Anexos](anexos.md) | Referencias y material complementario |

[Ver arquitectura :material-arrow-right:](arquitectura.md){ .md-button .md-button--primary }
