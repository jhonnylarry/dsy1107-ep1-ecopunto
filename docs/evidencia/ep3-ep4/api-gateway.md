# Evidencia · API Gateway con los microservicios

Configuración actual de la HTTP API `ecopunto-api` (región `us-east-1`), obtenida con
`aws apigatewayv2 get-routes`, `get-authorizers` y `get-api`. Respecto a la EP2 (ver
[`../ep2/api-gateway-y-entra.md`](../ep2/api-gateway-y-entra.md)) se agregó el método `PATCH`
y las rutas hacia los tres microservicios con API REST.

- **Fecha:** 2026-10-23
- **URL de invocación:** `https://mld2c6mojh.execute-api.us-east-1.amazonaws.com`

## 1 · Rutas e integraciones

Las rutas más específicas tienen prioridad sobre `/api/{proxy+}`, así que `/api/estadisticas`,
`/api/tickets` y `/api/admin/*` llegan a su microservicio y el resto al backend.

| Ruta | Autorización | Integración |
|---|---|---|
| `ANY /public/{proxy+}` | ninguna | `http://ecopunto-api.larraguibel.dev:8080/public/{proxy}` |
| `GET /api/{proxy+}` | JWT (`entra-jwt`) | `http://ecopunto-api.larraguibel.dev:8080/api/{proxy}` |
| `POST /api/{proxy+}` | JWT (`entra-jwt`) | ídem |
| `PUT /api/{proxy+}` | JWT (`entra-jwt`) | ídem |
| `PATCH /api/{proxy+}` | JWT (`entra-jwt`) | ídem — nuevo, para `PATCH /api/reportes/{id}/estado` |
| `DELETE /api/{proxy+}` | JWT (`entra-jwt`) | ídem |
| `GET /api/estadisticas` | JWT (`entra-jwt`) | `http://ecopunto-api.larraguibel.dev:8082/api/estadisticas` (ms-estadisticas) |
| `GET /api/tickets` | JWT (`entra-jwt`) | `http://ecopunto-api.larraguibel.dev:8083/api/tickets` (ms-tickets) |
| `GET /api/tickets/{proxy+}` | JWT (`entra-jwt`) | `http://ecopunto-api.larraguibel.dev:8083/api/tickets/{proxy}` (ms-tickets) |
| `GET /api/admin/{proxy+}` | JWT (`entra-jwt`) | `http://ecopunto-api.larraguibel.dev:8084/api/admin/{proxy}` (ms-rabbit-admin) |
| `POST /api/admin/{proxy+}` | JWT (`entra-jwt`) | ídem |
| `DELETE /api/admin/{proxy+}` | JWT (`entra-jwt`) | ídem |

## 2 · Authorizer JWT

| Campo | Valor |
|---|---|
| Nombre | `entra-jwt` |
| Tipo | JWT |
| Fuente del token | `$request.header.Authorization` |
| Issuer | `https://login.microsoftonline.com/<tenant>/v2.0` |
| Audience | Client ID de la API en Entra ID |

## 3 · CORS

| Campo | Valor |
|---|---|
| Origen permitido | `https://ecopunto.larraguibel.dev` |
| Métodos | `GET, POST, PUT, PATCH, DELETE, OPTIONS` |
| Headers | `authorization, content-type` |

## 4 · Verificación

| Escenario | Esperado | Obtenido |
|---|---:|---:|
| `GET /public/puntos-limpios` sin token | 200 | 200 |
| `GET /api/puntos-limpios`, `/api/estadisticas`, `/api/tickets`, `/api/admin/queues` sin token | 401 | 401 |
| Cualquier ruta `/api` con un token firmado por otro emisor | 401 | 401 |
| Preflight `PATCH` desde `https://ecopunto.larraguibel.dev` | 204 con `Access-Control-Allow-Origin` | 204 con `Access-Control-Allow-Origin` |
| Preflight desde otro origen | sin `Access-Control-Allow-Origin` | sin `Access-Control-Allow-Origin` |
| `GET /api/estadisticas` desde el frontend con sesión (`encargado1`) | 200 | 200 |
| `GET /api/tickets` desde el frontend con usuario sin `ENCARGADO` | 403 | 403 |

Las dos últimas filas son las del recorrido con login real: [`flujo-encargado.md`](./flujo-encargado.md) y [`administrador-rabbitmq.md`](./administrador-rabbitmq.md).
