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

## Conclusiones

- La validación de firma, issuer, audience y vigencia ocurre en el backend (Resource Server);
  el API Gateway solo enruta y aplica CORS.
- La autorización por rol se aplica en el servidor: ocultar los botones en la interfaz
  no es la única barrera, un usuario sin el rol recibe 403 aunque llame directo a la API.
- Los scopes delegados llegan a ambos usuarios por el consentimiento de administrador del
  tenant; el rol `ENCARGADO` solo lo tiene quien lo tiene asignado explícitamente.
