# Guion de la demo EP4 (5–10 min)

Orden sugerido: cada bloque cubre uno o más requisitos de la pauta. Tener abiertas de antemano las pestañas indicadas.

| # | Requisito EP4 | Qué mostrar | Dónde |
|---|---|---|---|
| 1 | API Manager creado en la nube | La HTTP API en la consola de API Gateway | AWS Console |
| 2 | Configuración del API Manager hacia el backend | Rutas `/api/*`, `/api/estadisticas`, `/api/tickets`, `/api/admin/*` y sus integraciones | AWS Console → Routes / Integrations |
| 3 | El API Manager valida el JWT | `curl` sin token → 401; con token → 200 | Terminal |
| 4 | Tenant IDaaS con usuarios | Usuarios y App roles (`ENCARGADO`, `ADMIN`) | Portal de Entra ID |
| 5 | Login OAuth 2.0 / OIDC con JWT válido | Iniciar sesión en el frontend; mostrar el token (roles) | Navegador |
| 6 | Frontend consume la API vía API Manager | Pestaña Network: llamadas a `execute-api…` | DevTools |
| 7 | Backend y frontend desplegados en la nube | `docker compose ps` en la EC2 y el frontend por HTTPS | SSH + navegador |
| 8 | docker-compose levantando RabbitMQ en la nube | `docker compose ps` (rabbit1, rabbit2 healthy) | SSH |
| 9 | Dos nodos RabbitMQ en clúster | `rabbitmq-diagnostics cluster_status` y la sección *Nodes* del Management UI | SSH + `:15672` |
| 10 | Exchanges direct y topic | Exchanges `ecopunto.tareas` (direct), `ecopunto.events` (topic), `ecopunto.dlx` | Management UI |
| 11 | Tres colas con sus DLQ | `notificaciones.reportes`, `estadisticas.reportes`, `tickets.mantencion` + sus `.dlq` | Management UI → Queues |

## Flujo en vivo (bloque central, ~3 min)

1. Como **vecino**: crear un reporte en un punto limpio → mostrar en los logs `[PUBLISH]` del backend y `[NOTIFICACION]` + `[ESTADISTICAS]` en los consumidores.
2. Como **encargado**: *Pasar a revisión* → aparece el ticket en la vista **Tickets** (`TK-…`).
3. *Marcar resuelto* → notificación al vecino; **Estadísticas** actualizadas.
4. **Falla controlada:** publicar desde el Management UI (*Publish message* en `ecopunto.tareas`, routing key `ticket.mantencion`) un JSON inválido → log `[DLQ] motivo=NO_RECUPERABLE` y el mensaje en `tickets.mantencion.dlq` (*Get messages*, ver `x-death`).
5. **Consumidor caído:** `docker stop ecopunto-ms-tickets`, pasar otro reporte a revisión (responde al instante), mostrar el mensaje *Ready* en la cola, `docker start` y ver el ticket generado.
6. **Administrador** (usuario con rol `ADMIN` → *Administrar RabbitMQ*): crear una cola con nombre `amq.x` → error de validación mostrado en pantalla (400); crear `ecopunto.auditoria` (topic), la cola `auditoria.reportes` con DLQ y el binding `reporte.#` → aparecen en la página y en el Management UI; intentar eliminar `tickets.mantencion` → 409 (tiene consumidor); eliminar lo creado. En los logs de `ms-rabbit-admin` queda `[ADMIN] <correo> creó …`.

```bash
# logs en vivo durante la demo
docker compose logs -f backend ms-notificaciones ms-estadisticas ms-tickets | grep -E '\[(PUBLISH|ACK|DLQ|RETRY|TICKET|NOTIFICACION|ESTADISTICAS|DUPLICADO)\]'
```

## Preguntas probables

- **¿Por qué topic y direct?** Eventos con varios interesados filtrados por patrón (topic) frente a tareas para un único trabajador con coincidencia exacta (direct).
- **¿Qué diferencia hay entre ACK, durabilidad y persistencia?** ACK: el consumidor confirma que procesó. Durable: la cola o exchange sobrevive a un reinicio del broker. Persistente: el mensaje se escribe a disco (`delivery_mode: 2`). Se necesitan las tres cosas para no perder mensajes.
- **¿Qué reencolas y qué no?** Nunca se reencola directo (evita bucles). Error transitorio → reintento con espera; error de datos → DLQ inmediata.
- **¿Por qué quorum con 2 nodos?** Replicación de los mensajes en ambos nodos; la limitación es que con 2 nodos no hay mayoría si uno cae (en producción, 3 nodos).
- **¿Cómo evitas duplicados?** `eventId` / `reporteId` únicos: el consumidor detecta la reentrega y solo hace ACK.
