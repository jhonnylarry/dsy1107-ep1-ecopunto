# Evidencia · Pruebas de punta a punta de la EP3 (local y producción)

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
| [Mensaje en la DLQ](./capturas/ep3/aws-rabbit-dlq-mensaje.png) | Mensaje con JSON inválido publicado en `ecopunto.tareas`: ms-tickets lo rechaza (`[DLQ] motivo=NO_RECUPERABLE`) y queda en `tickets.mantencion.dlq` con su header `x-death` |

## 5. Producción con login real (Entra ID + API Gateway)

Prueba del flujo completo en la nube, **con usuarios reales del tenant** y el login Authorization Code + PKCE, recorriendo el frontend desplegado. Todas las llamadas pasan por el API Gateway (`mld2c6mojh`), que valida el JWT con el authorizer `entra-jwt` antes de llegar a los microservicios.

- **Fecha:** 2026-10-23
- **Frontend:** https://ecopunto.larraguibel.dev (EC2 `ecopunto-frontend`)
- **API:** `https://mld2c6mojh.execute-api.us-east-1.amazonaws.com` → EC2 `ecopunto-backend` (stack `docker-compose`)
- **Usuarios:** `encargado1@jolarraguibel.onmicrosoft.com` (rol `ENCARGADO`) y `jo.larraguibel@duocuc.cl` (rol `ADMIN`, sin `ENCARGADO`)

### 5.1 Flujo de reportes como encargado

| # | Acción en el frontend | Resultado | Captura |
|---|---|---|---|
| 1 | Login de `encargado1` | Inicio con *Estadísticas* y *Tickets de mantención*; sin *Administrar RabbitMQ* (no tiene `ADMIN`) | [01](./capturas/ep3/prod-front-01-inicio-encargado.jpg) |
| 2 | Puntos limpios | 7 puntos, con *Editar* / *Eliminar* (rol `ENCARGADO`) | [02](./capturas/ep3/prod-front-02-puntos-limpios.jpg) |
| 3 | *Ver y crear reportes* (Ñuñoa) | Reportes del punto con su estado | [03](./capturas/ep3/prod-front-03-reportes-del-punto.jpg) |
| 4 | Crear reporte | Reporte **#15** en `PENDIENTE` | [04](./capturas/ep3/prod-front-04-reporte-creado.jpg) |
| 5 | *Pasar a revisión* | `EN_REVISION`; se genera el ticket | [05](./capturas/ep3/prod-front-05-reporte-en-revision.jpg) |
| 6 | *Marcar resuelto* | `RESUELTO`; se notifica al autor | [06](./capturas/ep3/prod-front-06-reporte-resuelto.jpg) |
| 7 | Tickets | **`TK-000011`**, reporte #15, prioridad **ALTA** (la descripción dice "rebalsado") | [07](./capturas/ep3/prod-front-07-tickets.jpg) |
| 8 | Estadísticas | 15 creados · 11 en revisión · 6 resueltos; Ñuñoa con 3 | [08](./capturas/ep3/prod-front-08-estadisticas.jpg) |

Logs de producción del mismo recorrido (prefijo de Spring omitido):

```text
03:43:58.403 backend           [PUBLISH] ReporteCreadoEvent -> exchange=ecopunto.events routingKey=reporte.creado
03:43:58.451 backend           [PUBLISH-CONFIRM] broker confirmó messageId=92b5f4e1-…
03:44:00.211 ms-estadisticas   [ESTADISTICAS] evento=92b5f4e1-… tipo=creado punto="Punto Limpio Ñuñoa" comuna=Ñuñoa
03:44:00.276 ms-notificaciones [NOTIFICACION] para=encargados@ecopunto.cl | asunto="Nuevo reporte #15 en Punto Limpio Ñuñoa"
03:44:00.302 ms-notificaciones [ACK] cola=notificaciones.reportes messageId=92b5f4e1-… intento=1
03:44:00.314 ms-estadisticas   [ACK] cola=estadisticas.reportes messageId=92b5f4e1-… intento=1

03:44:18.713 backend           [PUBLISH] ReporteEstadoCambiadoEvent -> exchange=ecopunto.events routingKey=reporte.en_revision
03:44:18.727 backend           [PUBLISH] GenerarTicketMantencionCommand -> exchange=ecopunto.tareas routingKey=ticket.mantencion
03:44:18.756 ms-estadisticas   [ACK] cola=estadisticas.reportes messageId=1d027091-… intento=1
03:44:19.822 ms-tickets        [TICKET] generado TK-000011 para reporte 15 | Punto Limpio Ñuñoa (Ñuñoa) | prioridad ALTA
03:44:19.845 ms-tickets        [ACK] cola=tickets.mantencion messageId=ec49d6e2-… intento=1

03:44:27.373 backend           [PUBLISH] ReporteEstadoCambiadoEvent -> exchange=ecopunto.events routingKey=reporte.resuelto
03:44:27.409 ms-notificaciones [NOTIFICACION] para=encargado1@jolarraguibel.onmicrosoft.com | asunto="Tu reporte #15 fue resuelto"
03:44:27.418 ms-notificaciones [ACK] cola=notificaciones.reportes messageId=4e8c64c8-… intento=1
03:44:27.428 ms-estadisticas   [ACK] cola=estadisticas.reportes messageId=4e8c64c8-… intento=1
```

Un mismo evento llega a varias colas (topic: notificaciones y estadísticas), la tarea de ticket va por el exchange direct, y cada consumidor confirma con ACK manual.

### 5.2 Administrador de RabbitMQ (rol `ADMIN`)

| # | Acción | Resultado | Captura |
|---|---|---|---|
| 1 | Login de `jo.larraguibel@duocuc.cl` | Inicio con *Administrar RabbitMQ*; sin *Tickets* (no tiene `ENCARGADO`) | [09](./capturas/ep3/prod-front-09-inicio-admin.jpg) |
| 2 | *Administrar RabbitMQ* | Topología real del clúster: 6 colas quorum con su DLQ, 3 exchanges | [10](./capturas/ep3/prod-front-10-admin-topologia.jpg) |
| 3 | Crear exchange `amq.x` | **400** con el error del campo `nombre` mostrado en pantalla | [11](./capturas/ep3/prod-front-11-admin-validacion-400.jpg) |
| 4 | Crear exchange `ecopunto.auditoria` (topic), cola `auditoria.reportes` con DLQ y binding `reporte.#` | Creados; la DLQ y su binding a `ecopunto.dlx` se crean solos | [12](./capturas/ep3/prod-front-12-admin-creados.jpg) |
| 5 | Eliminar binding, colas y exchange | El broker vuelve a 6 colas, 3 exchanges y 7 bindings | — |

Auditoría registrada por `ms-rabbit-admin`:

```text
03:47:15 [ADMIN] jo.larraguibel@duocuc.cl creó el exchange ecopunto.auditoria (tipo=TOPIC)
03:47:22 [ADMIN] jo.larraguibel@duocuc.cl creó la cola auditoria.reportes (tipo=QUORUM, conDlq=true)
03:47:28 [ADMIN] jo.larraguibel@duocuc.cl creó el binding ecopunto.auditoria --reporte.#--> auditoria.reportes
03:47:43 [ADMIN] jo.larraguibel@duocuc.cl eliminó el binding ecopunto.auditoria --reporte.#--> auditoria.reportes
03:47:52 [ADMIN] jo.larraguibel@duocuc.cl eliminó la cola auditoria.reportes (forzar=false)
03:47:55 [ADMIN] jo.larraguibel@duocuc.cl eliminó la cola auditoria.reportes.dlq (forzar=false)
03:47:57 [ADMIN] jo.larraguibel@duocuc.cl eliminó el exchange ecopunto.auditoria
```

### 5.3 Autorización por rol

| Usuario | Página / llamada | Resultado | Captura |
|---|---|---|---|
| `jo.larraguibel@duocuc.cl` (`ADMIN`, sin `ENCARGADO`) | `/tickets` → `GET /api/tickets` vía Gateway | **403**: "Solo los encargados pueden ver los tickets de mantención" | [13](./capturas/ep3/prod-front-13-tickets-403-sin-encargado.jpg) |
| `encargado1` | `GET /api/estadisticas` vía Gateway | **200** (llamada `fetch` a `execute-api`, medida desde el navegador) | [08](./capturas/ep3/prod-front-08-estadisticas.jpg) |

El token es válido para el Gateway (pasa el authorizer), pero el microservicio aplica el permiso por rol y responde 403: defensa en profundidad.

### 5.4 Configuración del API Gateway

Rutas actuales de la HTTP API `ecopunto-api` (12), obtenidas con `aws apigatewayv2 get-routes`:

| Ruta | Autorización | Integración |
|---|---|---|
| `ANY /public/{proxy+}` | ninguna | `http://ecopunto-api.larraguibel.dev:8080/public/{proxy}` |
| `GET` · `POST` · `PUT` · `PATCH` · `DELETE /api/{proxy+}` | JWT (`entra-jwt`) | `http://ecopunto-api.larraguibel.dev:8080/api/{proxy}` |
| `GET /api/estadisticas` | JWT (`entra-jwt`) | `http://ecopunto-api.larraguibel.dev:8082/api/estadisticas` |
| `GET /api/tickets` | JWT (`entra-jwt`) | `http://ecopunto-api.larraguibel.dev:8083/api/tickets` |
| `GET /api/tickets/{proxy+}` | JWT (`entra-jwt`) | `http://ecopunto-api.larraguibel.dev:8083/api/tickets/{proxy}` |
| `GET` · `POST` · `DELETE /api/admin/{proxy+}` | JWT (`entra-jwt`) | `http://ecopunto-api.larraguibel.dev:8084/api/admin/{proxy}` |

- **Authorizer `entra-jwt`:** tipo JWT, issuer `https://login.microsoftonline.com/<tenant>/v2.0`, audience = Client ID de la API.
- **CORS:** origen `https://ecopunto.larraguibel.dev`; métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS`.

