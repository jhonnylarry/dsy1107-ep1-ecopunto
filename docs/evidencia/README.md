# Evidencia

Evidencia de cada evaluación, separada por carpeta. Ningún documento incluye tokens,
contraseñas ni secretos.

## [`ep3-ep4/`](./ep3-ep4/) · Mensajería con RabbitMQ (actual)

| Documento | Contenido |
|---|---|
| [`flujo-encargado.md`](./ep3-ep4/flujo-encargado.md) | Recorrido en producción con login real: crear reporte, pasar a revisión, ticket, resolver y estadísticas, con los logs de cada mensaje |
| [`administrador-rabbitmq.md`](./ep3-ep4/administrador-rabbitmq.md) | Página *Administrar RabbitMQ* (rol `ADMIN`): topología, validación 400, crear y eliminar recursos, auditoría y 403 por rol |
| [`cluster-rabbitmq-aws.md`](./ep3-ep4/cluster-rabbitmq-aws.md) | Clúster de 2 nodos en AWS, 3 colas con DLQ, exchanges direct y topic, mensaje en la DLQ |
| [`api-gateway.md`](./ep3-ep4/api-gateway.md) | Las 12 rutas del API Gateway hacia el backend y los microservicios, authorizer JWT y CORS |
| [`pruebas-locales.md`](./ep3-ep4/pruebas-locales.md) | Matriz de 38 pruebas (EP1/EP2 + EP3) y pruebas de mensajería en el stack local, repetibles con [`pruebas-locales/`](./ep3-ep4/pruebas-locales/) |

## [`ep2/`](./ep2/) · API Gateway e IDaaS (presentación anterior)

| Documento | Contenido |
|---|---|
| [`usuario-sin-rol.md`](./ep2/usuario-sin-rol.md) | Registro de autoservicio y vista de un usuario sin rol |
| [`usuario-encargado.md`](./ep2/usuario-encargado.md) | CRUD de puntos limpios con el rol `ENCARGADO` |
| [`api-gateway-y-entra.md`](./ep2/api-gateway-y-entra.md) | Consola de AWS API Gateway y de Microsoft Entra ID |
| [`matriz-seguridad.md`](./ep2/matriz-seguridad.md) | Matriz 200/401/403 en AWS con tokens reales, repetible con [`probar-matriz.js`](./ep2/probar-matriz.js) |
