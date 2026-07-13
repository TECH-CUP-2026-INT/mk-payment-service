# Requerimientos

## Requerimientos funcionales

| ID | Requerimiento | Prioridad | Estado |
|----|---------------|-----------|--------|
| RF-01 | Crear un pago asociado a una orden | Alta | Pendiente |
| RF-02 | Consultar el estado de un pago por ID | Alta | Pendiente |
| RF-03 | Listar pagos filtrados por usuario | Media | Pendiente |
| RF-04 | Actualizar el estado de un pago | Alta | Pendiente |
| RF-05 | Validar montos y moneda antes de persistir | Alta | Pendiente |

## Requerimientos no funcionales

| ID | Requerimiento | Criterio de aceptación |
|----|---------------|------------------------|
| RNF-01 | Disponibilidad | El servicio debe responder en menos de 500 ms bajo carga normal |
| RNF-02 | Escalabilidad | Diseño stateless compatible con despliegue horizontal |
| RNF-03 | Mantenibilidad | Arquitectura hexagonal con capas desacopladas |
| RNF-04 | Observabilidad | Logs estructurados y trazabilidad de transacciones |
| RNF-05 | Seguridad | Validación de entrada y manejo centralizado de excepciones |
| RNF-06 | Documentación | API y arquitectura documentadas en MkDocs |

## Restricciones técnicas

- Java 21 como versión mínima del runtime.
- Spring Boot 3.x como framework base.
- MongoDB como base de datos principal.
- Comunicación entre servicios vía HTTP/REST.

## Supuestos

1. Los identificadores de orden provienen de otro microservicio del ecosistema.
2. La autenticación se delega a un servicio de identidad externo.
3. El despliegue se realiza en contenedores Docker sobre infraestructura cloud.

## Criterios de aceptación generales

- [ ] Todos los endpoints documentados en `api.md` están implementados.
- [ ] Cobertura de pruebas unitarias mayor al 80% en la capa de dominio.
- [ ] La documentación MkDocs se despliega correctamente en GitHub Pages.
- [ ] El servicio arranca sin errores con la configuración por defecto.
