# Requerimientos

## Requerimientos funcionales

Actores externos del servicio (el propio servicio no se modela como actor):

- **Tournament Service**: sistema externo que origina las inscripciones y consulta el estado de sus pagos.
- **Pagador**: persona que realiza el pago de una inscripción mediante PSE.
- **Mercado Pago**: pasarela de pago externa que procesa la transacción PSE y notifica su resultado.

| ID | Actor | Requerimiento | Prioridad | Estado |
|----|-------|---------------|-----------|--------|
| RF-01 | Tournament Service | Crear una orden de pago para una inscripción (enrollmentId, teamId, tournamentId, monto), validando que el monto esté dentro de los límites vigentes de PSE y que no exista ya una orden para esa inscripción | Alta | Implementado |
| RF-02 | Tournament Service / Pagador | Consultar los límites de monto (mínimo y máximo) habilitados para el método de pago PSE | Alta | Implementado |
| RF-03 | Pagador | Iniciar una transacción PSE sobre una orden pendiente, indicando la entidad financiera y sus datos (correo, tipo/número de identificación, tipo de entidad), quedando la orden a la espera de confirmación bancaria | Alta | Implementado |
| RF-04 | Mercado Pago | Notificar al servicio el resultado de una transacción PSE (aprobada, rechazada o cancelada) mediante un webhook, para que la orden asociada se apruebe o se rechace | Alta | Implementado |
| RF-05 | Tournament Service | Consultar el estado actual de una orden de pago a partir del enrollmentId | Alta | Implementado |
| RF-06 | Mercado Pago (disparado por temporizador) | Sincronizar diariamente los límites de monto de PSE informados por Mercado Pago para mantener actualizada la validación de montos | Media | Implementado |
| RF-07 | Pagador / Tournament Service (disparado por temporizador) | Expirar automáticamente las órdenes de pago pendientes o a la espera de confirmación bancaria que superaron su tiempo límite (60 minutos), revisando periódicamente cada 5 minutos | Alta | Implementado |
| RF-08 | Pagador | Ser rechazado al intentar iniciar una transacción PSE sobre una orden ya vencida, incluso si el barrido periódico de expiración aún no la ha marcado como tal | Media | Implementado |

## Requerimientos no funcionales

| ID | Requerimiento | Criterio de aceptación |
|----|---------------|------------------------|
| RNF-01 | Disponibilidad | El servicio debe responder en menos de 500 ms bajo carga normal |
| RNF-02 | Escalabilidad | Diseño stateless compatible con despliegue horizontal |
| RNF-03 | Mantenibilidad | Arquitectura hexagonal con capas desacopladas |
| RNF-04 | Observabilidad | Logs estructurados y trazabilidad de transacciones |
| RNF-05 | Seguridad | Validación de entrada y manejo centralizado de excepciones |
| RNF-06 | Documentación | API y arquitectura documentadas en MkDocs |

