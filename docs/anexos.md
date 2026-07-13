# Anexos

## Glosario

| Término | Definición |
|---------|------------|
| **Hexagonal Architecture** | Patrón que separa la lógica de negocio de los adaptadores de entrada y salida |
| **Port** | Interfaz que define un contrato entre el dominio y el exterior |
| **Adapter** | Implementación concreta de un puerto (HTTP, MongoDB, etc.) |
| **DTO** | Objeto de transferencia de datos entre capas o servicios |
| **Use Case** | Caso de uso que encapsula una operación de negocio |

## Referencias

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [MapStruct](https://mapstruct.org/documentation/stable/reference/html/)
- [MkDocs Material](https://squidfunk.github.io/mkdocs-material/)
- [Mermaid Diagrams](https://mermaid.js.org/)

## Estructura de documentación

```
docs/
├── index.md
├── introduccion.md
├── requerimientos.md
├── configuracion.md
├── arquitectura.md
├── api.md
├── pruebas.md
├── equipo.md
├── anexos.md
└── assets/
    ├── img/
    ├── diagrams/
    └── stylesheets/
        └── extra.css
```

## Paleta de colores TechCup

Los estilos personalizados se definen en `docs/assets/stylesheets/extra.css`:

| Modo | Color primario | Color de acento |
|------|----------------|-----------------|
| Claro | Morado `#6a1b9a` | Dorado `#c9a227` |
| Oscuro | Morado `#7b1fa2` | Dorado `#d4af37` |

## Historial de cambios

| Versión | Fecha | Descripción |
|---------|-------|-------------|
| 0.1.0 | 2026-07-13 | Scaffolding inicial del servicio y documentación MkDocs |
