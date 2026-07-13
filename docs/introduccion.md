# Introducción

## Contexto

El **Payment Service** (`mk-payment-service`) forma parte del ecosistema **Astro Merge** de TECH-CUP 2026. Su función es centralizar la lógica de procesamiento de pagos, desacoplando las reglas de negocio del resto de servicios del sistema.

## Objetivo

Proporcionar un microservicio robusto, escalable y mantenible que permita:

- Registrar y consultar transacciones de pago.
- Validar reglas de negocio asociadas a los pagos.
- Exponer una API REST documentada para consumo por otros microservicios.

## Alcance

!!! success "Incluido"
    - API REST para operaciones de pago.
    - Arquitectura hexagonal con separación de capas.
    - Persistencia en MongoDB (capa document).
    - Documentación técnica con MkDocs.

!!! warning "Fuera de alcance (por ahora)"
    - Integración con pasarelas de pago externas.
    - Procesamiento de reembolsos automáticos.
    - Notificaciones en tiempo real.

## Stack tecnológico

| Componente | Tecnología |
|------------|------------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5 |
| Persistencia | MongoDB |
| Mapeo | MapStruct |
| Documentación | MkDocs Material |
| Build | Maven |

## Convenciones del repositorio

- Paquete base: `co.edu.escuelaing.techcup.payment`
- Rama principal de desarrollo: `develop`
- Documentación en la carpeta `docs/`
