# Evidencia · Matriz de autorización end-to-end en AWS

Validación real de las rutas del backend a través del despliegue en AWS, con tokens emitidos
por Microsoft Entra ID. No incluye tokens, contraseñas ni secretos.

## Entorno probado

- **Fecha:** 2026-09-21
- **Frontend:** Angular + MSAL, servido por nginx en EC2 con HTTPS (Let's Encrypt), origen `https://ecopunto.larraguibel.dev`
- **Entrada a la API:** AWS API Gateway (HTTP API `ecopunto-api`, región `us-east-1`), integración HTTP hacia el backend Spring Boot en EC2 (Docker)
- **Cómo se ejecutó:** desde la consola del navegador de la propia página (`fetch` con el origen real), de modo que el CORS configurado en el Gateway también queda validado
- **Reproducible con:** [`probar-matriz.js`](./probar-matriz.js)

## Claims relevantes de los tokens

| Usuario | `iss` | `aud` | `scp` | `roles` |
|---|---|---|---|---|
| `encargado1` | `https://login.microsoftonline.com/<tenant>/v2.0` | `<client-id de la API>` | `reportes.read reportes.write` | `["ENCARGADO"]` |
| `usuario1` | `https://login.microsoftonline.com/<tenant>/v2.0` | `<client-id de la API>` | `reportes.read reportes.write` | *(sin claim)* |

Ambos tokens son v2.0. El claim `aud` es el Client ID sin el prefijo `api://` (comportamiento
de Entra ID para tokens v2 de una API propia).

## Resultados

| Escenario | Usuario | Esperado | Obtenido |
|---|---|---:|---:|
| `GET /public/puntos-limpios` sin token | — | 200 | 200 |
| `GET /api/puntos-limpios` sin token | — | 401 | 401 |
| `PUT /api/puntos-limpios/1` sin token | — | 401 | 401 |
| `GET /api/puntos-limpios` con token válido | `usuario1` | 200 | 200 |
| `GET /api/puntos-limpios/1/reportes` (scope `reportes.read`) | `usuario1` | 200 | 200 |
| `POST /api/puntos-limpios` sin rol `ENCARGADO` | `usuario1` | 403 | 403 |
| `PUT /api/puntos-limpios/1` sin rol `ENCARGADO` | `usuario1` | 403 | 403 |
| `DELETE /api/puntos-limpios/1` sin rol `ENCARGADO` | `usuario1` | 403 | 403 |
| `GET /api/puntos-limpios` con token válido | `encargado1` | 200 | 200 |
| `GET /api/puntos-limpios/1/reportes` (scope `reportes.read`) | `encargado1` | 200 | 200 |
| `POST /api/puntos-limpios` con rol `ENCARGADO` | `encargado1` | 201 | 201 |
| `PUT /api/puntos-limpios/{id}` con rol `ENCARGADO` | `encargado1` | 200 | 200 |
| `DELETE /api/puntos-limpios/{id}` con rol `ENCARGADO` | `encargado1` | 204 | 204 |
| `DELETE /api/puntos-limpios/9999` inexistente | `encargado1` | 404 | 404 |

Respuesta del backend en el caso 403 (token válido, permiso insuficiente):

```json
{"status":403,"error":"Forbidden","message":"El Access Token es válido, pero no posee el permiso requerido","path":"/api/puntos-limpios/1"}
```

Tras los intentos sin rol, el registro `id 1` se mantuvo sin cambios.

## Validación JWT en el API Gateway (2026-09-22)

Con el JWT authorizer asociado a las rutas `GET/POST/PUT/DELETE /api/{proxy+}`, los
tokens ausentes o inválidos se rechazan **en el Gateway**, sin llegar al backend (la
respuesta la emite API Gateway, no Spring):

| Escenario | Esperado | Obtenido | Respuesta |
|---|---:|---:|---|
| `GET /api/puntos-limpios` sin token | 401 | 401 | `{"message":"Unauthorized"}` (Gateway) |
| `GET /api/puntos-limpios` con token basura | 401 | 401 | `{"message":"Unauthorized"}` (Gateway) |
| `GET /api/puntos-limpios` con token de firma falsificada (issuer y audience correctos) | 401 | 401 | `{"message":"Unauthorized"}` (Gateway) |
| `PATCH /api/puntos-limpios/1` (método sin ruta) | 404 | 404 | `{"message":"Not Found"}` (Gateway) |
| Preflight `OPTIONS` de GET/POST/PUT/DELETE desde el origen del frontend | 204 | 204 | CORS del Gateway |
| `GET /api/puntos-limpios` con token válido (`encargado1`) | 200 | 200 | JSON del backend |
| `POST` / `PUT` / `DELETE` con rol `ENCARGADO` | 201/200/204 | 201/200/204 | JSON del backend |

## Reportes y persistencia (2026-09-22)

Con el backend ya conectado a PostgreSQL (contenedor con volumen persistente en la
instancia del backend):

| Escenario | Esperado | Obtenido |
|---|---:|---:|
| `GET /api/puntos-limpios/1/reportes` sin token | 401 | 401 (Gateway) |
| `POST /api/puntos-limpios/1/reportes` sin token | 401 | 401 (Gateway) |
| `POST /api/puntos-limpios/1/reportes` con token (`encargado1`) | 201 | 201 |
| `GET /api/puntos-limpios/1/reportes` con token | 200 | 200 |
| `POST /api/puntos-limpios/9999/reportes` (punto inexistente) | 404 | 404 |

JSON devuelto por `GET /api/puntos-limpios/1/reportes` (se abrevia el punto limpio
anidado). El autor sale del claim `preferred_username` del token, no del cliente:

```json
[
  {
    "puntoLimpio": { "id": 1, "nombre": "Punto Limpio Ñuñoa" },
    "descripcion": "Contenedor de vidrio lleno, no reciben más vidrio",
    "autorEmail": "encargado1@jolarraguibel.onmicrosoft.com",
    "creadoEn": "2026-09-22T03:12:52.348439Z",
    "estado": "PENDIENTE",
    "id": 1
  }
]
```

Persistencia: un registro insertado en la base siguió disponible tras reiniciar los
contenedores de PostgreSQL y del backend.

Los 403 por falta de rol los sigue emitiendo el backend (el token es válido para el
Gateway, pero no trae el rol que exige la operación).

## Conclusiones

- La validación de firma, issuer, audience y vigencia ocurre dos veces: primero en el API
  Gateway (JWT authorizer) y de nuevo en el backend (Resource Server).
- La autorización por rol se aplica en el servidor: ocultar los botones en la interfaz
  no es la única barrera, un usuario sin el rol recibe 403 aunque llame directo a la API.
- Los scopes delegados llegan a ambos usuarios por el consentimiento de administrador del
  tenant; el rol `ENCARGADO` solo lo tiene quien lo tiene asignado explícitamente.
