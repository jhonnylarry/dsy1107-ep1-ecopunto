# Evidencia · Pruebas de punta a punta de la EP3 (stack local)

Pruebas del sistema completo con mensajería: las funciones de la EP1/EP2 (seguridad y CRUD de puntos limpios) y las nuevas de la EP3 (reportes con eventos, tickets, estadísticas y administrador de RabbitMQ). No incluye tokens, contraseñas ni secretos.

## Entorno probado

- **Fecha:** 2026-10-21
- **Stack:** el mismo `docker-compose.yml` que corre en AWS: PostgreSQL, clúster RabbitMQ de 2 nodos (`rabbit1`, `rabbit2`), backend y los 4 microservicios.
- **Tokens:** emitidos por un emisor de **prueba** local (RS256, con su propio JWKS), con los mismos claims que usa Entra ID (`iss`, `aud`, `scp`, `roles`). Así se prueban roles y scopes sin usar cuentas reales. La validación del JWT es exactamente la misma que en producción, porque issuer, JWKS y audience se configuran por variables de entorno.
- **Acceso:** directo a cada servicio (`:8080`, `:8082`, `:8083`, `:8084`). El API Gateway y el login con PKCE se prueban en producción (ver [ep2-matriz-seguridad.md](./ep2-matriz-seguridad.md)).
- **Reproducible con:** [`pruebas-locales/levantar.sh`](./pruebas-locales/levantar.sh) y luego [`pruebas-locales/pruebas.sh`](./pruebas-locales/pruebas.sh).

## Usuarios de prueba (claims del token)

| Usuario | `roles` | `scp` |
|---|---|---|
| `vecino@ejemplo.cl` | *(sin rol)* | `reportes.read reportes.write` |
| `lector@ejemplo.cl` | *(sin rol)* | `reportes.read` |
| `encargado1@ecopunto.cl` | `ENCARGADO` | `reportes.read reportes.write` |
| `admin@ecopunto.cl` | `ADMIN` | `reportes.read reportes.write` |

## 1. Pruebas de API

| # | Prueba | Esperado | Obtenido | |
|---|---|---|---|---|
| | **Seguridad (EP1/EP2)** | | | |
| 1 | GET público sin token | 200 | 200 | ✅ |
| 2 | GET /api sin token | 401 | 401 | ✅ |
| 3 | GET /api con token basura | 401 | 401 | ✅ |
| 4 | GET /api con firma de otra clave | 401 | 401 | ✅ |
| 5 | GET /api con token vencido | 401 | 401 | ✅ |
| 6 | GET /api con token válido (vecino) | 200 | 200 | ✅ |
| 7 | GET reportes con scope reportes.read | 200 | 200 | ✅ |
| 8 | POST reporte sin scope reportes.write | 403 | 403 | ✅ |
| | **CRUD de puntos limpios (EP2)** | | | |
| 9 | POST punto como vecino (sin rol) | 403 | 403 | ✅ |
| 10 | POST punto como encargado | 201 | 201 | ✅ |
| 11 | PUT punto como vecino | 403 | 403 | ✅ |
| 12 | PUT punto como encargado | 200 | 200 | ✅ |
| 13 | DELETE punto como encargado | 204 | 204 | ✅ |
| 14 | DELETE punto inexistente | 404 | 404 | ✅ |
| | **Reportes y mensajería (EP3)** | | | |
| 15 | POST reporte (vecino) | 201 | 201 | ✅ |
| 16 | POST reporte con descripción vacía | 400 | 400 | ✅ |
| 17 | PATCH estado como vecino | 403 | 403 | ✅ |
| 18 | PATCH a EN_REVISION (encargado) | 200 | 200 | ✅ |
| 19 | PATCH transición inválida | 409 | 409 | ✅ |
| 20 | PATCH a RESUELTO (encargado) | 200 | 200 | ✅ |
| 21 | GET estadísticas (vecino) | 200 | 200 | ✅ |
| 22 | GET estadísticas sin token | 401 | 401 | ✅ |
| 23 | GET tickets (encargado) | 200 | 200 | ✅ |
| 24 | GET tickets (vecino sin rol) | 403 | 403 | ✅ |
| | **Administrador de RabbitMQ (EP3)** | | | |
| 25 | GET colas como encargado (sin ADMIN) | 403 | 403 | ✅ |
| 26 | GET colas como admin | 200 | 200 | ✅ |
| 27 | POST cola con nombre amq.x | 400 | 400 | ✅ |
| 28 | POST cola con nombre vacío | 400 | 400 | ✅ |
| 29 | POST exchange ecopunto.auditoria | 201 | 201 | ✅ |
| 30 | POST cola auditoria.reportes con DLQ | 201 | 201 | ✅ |
| 31 | POST cola repetida | 409 | 409 | ✅ |
| 32 | POST binding reporte.# | 201 | 201 | ✅ |
| 33 | POST binding a cola inexistente | 404 | 404 | ✅ |
| 34 | DELETE cola con consumidor | 409 | 409 | ✅ |
| 35 | DELETE binding | 204 | 204 | ✅ |
| 36 | DELETE cola auditoria.reportes | 204 | 204 | ✅ |
| 37 | DELETE cola auditoria.reportes.dlq | 204 | 204 | ✅ |
| 38 | DELETE exchange ecopunto.auditoria | 204 | 204 | ✅ |

**Resultado: 38 de 38 pruebas OK, 0 fallidas.**

## 2. Pruebas de mensajería

| Prueba | Cómo | Resultado |
|---|---|---|
| Flujo completo de un reporte | Crear reporte → `EN_REVISION` → `RESUELTO` | El backend registra `[PUBLISH]` y `[PUBLISH-CONFIRM]`; ms-notificaciones avisa a los encargados y al vecino; ms-estadisticas suma contadores; ms-tickets genera `TK-000004` con prioridad `ALTA`. Todos con `[ACK]` |
| Mensaje inválido | Publicar `{esto no es json` en `ecopunto.tareas` con routing key `ticket.mantencion` | `[DLQ] motivo=NO_RECUPERABLE: JSON inválido`; `tickets.mantencion.dlq` sube de 2 a 3 mensajes, con el header `x-death` (`reason: rejected`, `queue: tickets.mantencion`) |
| Consumidor caído | `docker stop ecopunto-ms-tickets` y pasar un reporte a revisión | La API responde **200 en 0,04 s**; el mensaje queda en `tickets.mantencion`. Con `docker start`, ms-tickets genera `TK-000005` |
| Clúster | `rabbitmqctl cluster_status` | `running_nodes: rabbit@rabbit1, rabbit@rabbit2` |
| Topología | `rabbitmqctl list_queues` / `list_exchanges` | 6 colas quorum (3 + 3 DLQ) con consumidor en las 3 principales; exchanges `ecopunto.events` (topic), `ecopunto.tareas` (direct), `ecopunto.dlx` (direct) |

## 3. Capturas del Management UI

| Captura | Muestra |
|---|---|
| [Nodos](./capturas/ep3/local-rabbit-overview-nodos.png) | Los dos nodos del clúster en ejecución |
| [Colas](./capturas/ep3/local-rabbit-colas-dlq.png) | Las 6 colas quorum con `DLX` y `DLK` |
| [Exchanges](./capturas/ep3/local-rabbit-exchanges.png) | `ecopunto.events` (topic), `ecopunto.tareas` y `ecopunto.dlx` (direct) |
| [Bindings topic](./capturas/ep3/local-rabbit-exchange-topic-bindings.png) | `reporte.creado`, `reporte.resuelto` y `reporte.*` |
| [Bindings direct](./capturas/ep3/local-rabbit-exchange-direct-bindings.png) | `ticket.mantencion` → `tickets.mantencion` |
| [Mensaje en la DLQ](./capturas/ep3/local-rabbit-dlq-mensaje.png) | El mensaje inválido con su header `x-death` |

## 4. El mismo clúster en AWS

Capturas del Management UI en la EC2 `ecopunto-backend` (2026-10-21), con la misma topología que en local:

| Captura | Muestra |
|---|---|
| [Nodos](./capturas/ep3/aws-rabbit-overview-nodos.png) | `rabbit@rabbit1` y `rabbit@rabbit2` en ejecución; 6 colas y 3 consumidores |
| [Colas](./capturas/ep3/aws-rabbit-colas-dlq.png) | Las 6 colas quorum con `DLX` y `DLK`, con líderes repartidos entre los dos nodos |
| [Exchanges](./capturas/ep3/aws-rabbit-exchanges.png) | `ecopunto.events` (topic), `ecopunto.tareas` y `ecopunto.dlx` (direct) |
| [Bindings topic](./capturas/ep3/aws-rabbit-exchange-topic-bindings.png) | `reporte.creado`, `reporte.resuelto` y `reporte.*` |
| [Bindings direct](./capturas/ep3/aws-rabbit-exchange-direct-bindings.png) | `ticket.mantencion` → `tickets.mantencion` |
