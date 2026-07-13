---
name: hexagonal-architecture-payment-service
description: Use this skill whenever creating, editing, reviewing, or planning any Java class inside mk-payment-service (package co.edu.escuelaing.techcup.payment), or any other TechCup microservice following the same convention (e.g. mk-tournament-service). Trigger for any request involving use cases, ports, adapters, domain entities, controllers, DTOs, mappers, or persistence in this codebase — even if the request doesn't mention "hexagonal" explicitly. This skill defines the mandatory package layout, naming rules, and dependency direction for the project; violating it produces code that doesn't match the rest of the codebase.
---

# Arquitectura hexagonal — TechCup microservices

Este skill fija las reglas de paquetes, nombres y dependencias que ya usa
`mk-tournament-service` en producción. `mk-payment-service` (y cualquier
microservicio nuevo de TechCup) debe seguir exactamente el mismo patrón,
para que cualquier desarrollador que conozca uno pueda leer el otro sin
sorpresas.

## La regla que no se negocia

**Las flechas de dependencia solo apuntan hacia adentro, hacia el dominio.**

```
controller/impl  ──depends on──>  service/ports (Use Cases)
repository/adapter ──implements──> service/ports (outbound ports)
service/impl     ──implements──>  service/ports (Use Cases)
service/impl     ──depends on──>  service/ports (outbound ports)
service (domain) ──depends on──>  nada. Cero imports de Spring, JPA, HTTP.
```

Si una clase en `service/` (el paquete raíz, no `service/impl` ni
`service/ports`) importa algo de `org.springframework.*`,
`jakarta.persistence.*`, o cualquier librería HTTP, **está en el paquete
equivocado**. El dominio no sabe que existe una base de datos, un
framework web, o Mercado Pago.

## Mapa de paquetes

| Paquete | Contiene | Regla de nombres |
|---|---|---|
| `service/` (raíz) | Aggregate roots y value objects del dominio. Reglas de negocio, validaciones, transiciones de estado. | Sin sufijo: `PaymentOrder`, `Payer`, `PaymentOrderStatus` |
| `service/ports/` | **Todas** las interfaces de puertos, tanto de entrada (casos de uso) como de salida (repositorios, gateways externos). No se separan en carpetas distintas — así lo hace ya Torneos. | Entrada: `XxxUseCase`. Salida: `XxxPort` |
| `service/impl/` | Servicios de aplicación: implementan los `XxxUseCase`, orquestan llamadas a los puertos de salida. Sin lógica de negocio propia — esa vive en el dominio. | `XxxService implements XxxUseCase` |
| `controller/impl/` | Controladores REST. Reciben DTOs, llaman a un `XxxUseCase`, devuelven DTOs. Nunca tocan entidades JPA ni lógica de negocio directamente. | `XxxController` |
| `repository/adapter/` | Implementaciones concretas de los puertos de salida: JPA, HTTP hacia Mercado Pago, etc. | `XxxRepositoryAdapter`, `XxxGatewayAdapter` |
| `repository/jpa/` | Interfaces `JpaRepository` de Spring Data — solo acceso crudo a la tabla, sin lógica. | `XxxJpaRepository extends JpaRepository<...>` |
| `entity/` | Entidades `@Entity` de JPA — persistencia pura, sin comportamiento de negocio. Nunca salen de `repository/adapter/` ni `mapper/`. | `XxxEntity` |
| `mapper/` | Traducción explícita entre capas: dominio ↔ entidad JPA, dominio ↔ DTO. Clases estáticas, sin estado. | `XxxPersistenceMapper` (dominio↔entidad), `XxxRestMapper` (dominio↔DTO) |
| `dto/request/`, `dto/response/` | Contratos HTTP de entrada/salida. Nunca se reutilizan como modelo de dominio. | `XxxRequest`, `XxxResponse` |
| `exception/` | Una clase por regla de negocio violada, más `GlobalExceptionHandler` que las traduce a códigos HTTP. | `XxxException` |
| `config/` | Configuración de Spring (beans, `RestClient`, propiedades). | `XxxConfig` |
| `scheduler/` | Disparadores por tiempo (`@Scheduled`) que invocan un caso de uso. Sin lógica propia, igual que un controller pero por cron en vez de HTTP. | `XxxJob` |

> Nota sobre el scaffolding actual: existe una carpeta duplicada con typo
> `tehcup/payment` junto a `techcup/payment` — bórrala antes de generar
> código nuevo, para que no queden clases repartidas en dos árboles de
> paquete distintos por error de autocompletado.

## Receta para agregar un caso de uso nuevo (de punta a punta)

Cuando te pidan un caso de uso nuevo (p. ej. "agrega `CreatePaymentOrder`"),
crea/edita en este orden — cada paso depende del anterior:

1. **Dominio** (`service/`): si el caso de uso requiere un nuevo estado,
   transición o regla, agrégala al aggregate root o value object
   correspondiente. Las validaciones lanzan excepciones de `exception/`,
   nunca devuelven `null` ni booleanos silenciosos.
2. **Puerto de entrada** (`service/ports/XxxUseCase.java`): la interfaz
   mínima que expresa la intención del caso de uso, en términos de
   dominio (nunca recibe ni devuelve DTOs ni entidades JPA).
3. **Puertos de salida nuevos**, si aplica (`service/ports/XxxPort.java`):
   solo si el caso de uso necesita hablar con algo externo (base de
   datos, otro microservicio, Mercado Pago) que aún no tiene puerto.
4. **Servicio de aplicación** (`service/impl/XxxService.java`): implementa
   el `XxxUseCase`, inyecta los puertos de salida por constructor, y
   solo orquesta — valida invariantes de aplicación (p. ej. "¿ya existe
   una orden para este enrollmentId?") pero delega las reglas de negocio
   del propio agregado al dominio.
5. **Adaptadores** (`repository/adapter/`), si el paso 3 agregó un
   puerto nuevo: la implementación concreta.
6. **DTOs** (`dto/request/`, `dto/response/`) y **mapper** si el caso de
   uso se expone por HTTP.
7. **Controlador** (`controller/impl/`): traduce HTTP ↔ caso de uso.
8. **Tests**: unitarios del `XxxService` con Mockito (mockeando los
   puertos), más el feature de Cucumber si es un flujo de negocio
   completo.

## Convenciones de test

- Unitarios: JUnit 5 + Mockito + AssertJ, con `@Nested` agrupando por
  escenario y `@DisplayName` en español describiendo el comportamiento
  esperado (no el nombre técnico del método).
- Los tests de `XxxService` mockean **solo los puertos** (`XxxPort`,
  `XxxRepositoryPort`), nunca instancian un adaptador real ni levantan
  Spring context — eso es lo que te da velocidad.
- BDD/Cucumber: solo para flujos de negocio completos de cara al usuario
  (p. ej. "un capitán paga con PSE"), no para casos internos como
  `ExpireTransaction`.

## Errores comunes a evitar en este proyecto

- **Filtrar una `XxxEntity` (JPA) fuera de `repository/adapter/` o
  `mapper/`.** Si un `Service` o `Controller` importa algo de
  `entity/`, es un error — deben trabajar solo con el objeto de dominio.
- **Llamar a la API de Mercado Pago directamente desde un `XxxService`.**
  Siempre pasa por `PaymentGatewayPort` — así los tests no necesitan red
  real, y si cambias de pasarela algún día, solo tocas el adaptador.
- **Confiar en datos externos sin revalidarlos.** Cualquier dato que
  venga de un webhook, de otro microservicio, o del cliente HTTP, se
  valida en el dominio o en el servicio de aplicación — nunca se asume
  correcto solo porque llegó con esa forma.
- **Poner un `if` de regla de negocio en el `Controller`.** El
  controlador no decide nada, solo traduce y delega.
- **Optimistic locking:** cualquier entidad que pueda ser modificada por
  dos flujos distintos al mismo tiempo (como `PaymentOrder`, tocable por
  el webhook y por el job de expiración) debe tener `@Version` en su
  `XxxEntity`, y el `Service` debe capturar
  `OptimisticLockingFailureException` como un no-op silencioso, no como
  un error a propagar.
