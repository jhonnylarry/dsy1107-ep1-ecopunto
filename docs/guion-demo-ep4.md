# Guion de la presentación EP4 (5–10 min)

EcoPunto · DSY1107 · Jonathan Larraguibel · enfoque: justificar cada decisión · duración objetivo: entre 9 y 10 min

## Parte 1: diapositivas (unos 5 min)

**Diapositiva 1 · Portada (10 s)**

> "Soy Jonathan Larraguibel. En esta etapa agregué mensajería asíncrona con RabbitMQ a EcoPunto. Más que mostrar lo que configuré, voy a explicar por qué tomé cada decisión."

**Diapositiva 2 · Qué agregamos (30 s)**

*Decisión: qué es asíncrono y qué no.*

> "El criterio fue simple: lo que el usuario necesita ver en ese momento es síncrono, y sus consecuencias son asíncronas. Crear un reporte es síncrono, porque el vecino necesita saber que quedó guardado. Avisar a los encargados, sumar estadísticas y generar el ticket son consecuencias: si las hiciera en la misma petición, el usuario esperaría más y, si un servicio estuviera caído, el reporte fallaría. Por eso viajan como mensajes. Además, agregué un administrador de RabbitMQ para el rol ADMIN."

**Diapositiva 3 · Arquitectura y despliegue en AWS (50 s)**

*Decisiones: Gateway como única entrada, frontend separado, docker compose y una base por servicio.*

> "Aquí está cada pieza y dónde corre.
>
> Todo pasa por el **API Gateway**: el frontend conoce una sola URL y el token se valida antes de llegar a la máquina.
>
> El **frontend** va en su propia t3.micro, porque solo entrega archivos estáticos; si el backend se satura, la página sigue cargando.
>
> El backend corre con **docker compose** en una t3.medium: ocho contenedores. Consideré ECS o Kubernetes, pero para el alcance del curso compose me da red interna, reinicio automático y arranque ordenado con mucha menos complejidad.
>
> Y **cada microservicio tiene su propia base**, para que ninguno dependa de las tablas de otro. Las dos instancias tienen Elastic IP, así el dominio no cambia al reiniciar el laboratorio."

**Diapositiva 4 · Identidad y Gateway (30 s)**

*Decisiones: identidad delegada, App Roles y validación en dos capas.*

> "No programé login ni guardo contraseñas: lo delego en Entra ID, que ya trae MFA y recuperación de cuenta. Uso **App Roles** porque el rol llega directo en el token. Y el token se valida **en dos capas**: el Gateway responde 401 si es falso, y cada servicio aplica el rol con 403. Lo hice así porque los puertos de la EC2 son alcanzables desde internet: si alguien se salta el Gateway, el servicio igual lo frena."

**Diapositiva 5 · Exchanges, colas y DLQ (40 s)**, una de las más importantes

*Decisiones: topic para eventos, direct para tareas y una DLQ por cola.*

> "Elegí el exchange según el tipo de mensaje.
>
> - Los **eventos** son hechos que interesan a varios, así que usé **topic**. Estadísticas se suscribe a `reporte.*` y recibe cualquier evento nuevo sin tocar el productor. Descarté fanout porque obliga a todos a recibir todo.
> - Las **tareas** son órdenes para un solo responsable, así que usé **direct**, en un exchange aparte.
>
> Cada cola tiene **su propia DLQ**, para saber qué servicio falló. Y los nombres salen del `application.yml`, porque una routing key mal escrita pierde el mensaje en silencio."

**Diapositiva 6 · Flujo de un reporte (30 s)**

*Decisión: publicar después del commit, con confirmación.*

> "El evento se publica **recién después del commit**: si lo publicara antes y el guardado fallara, los demás recibirían un reporte que no existe. Uso **publisher confirms** para saber que RabbitMQ lo recibió, y **mandatory** para que me devuelva lo que no calce con ninguna cola. La limitación: si el broker está caído justo después del commit, el evento queda en el log; la mejora sería el patrón *outbox*."

**Diapositiva 7 · ACK, reintentos y DLQ (45 s)**, la otra muy importante

*Decisiones: ACK manual, clasificar errores, nunca reencolar e idempotencia.*

> "En los consumidores:
>
> - **ACK manual, después de guardar**: si el servicio se cae a mitad de camino, el mensaje se vuelve a entregar.
> - **Dos tipos de error**: un JSON inválido nunca se arregla reintentando y va directo a la DLQ; una base caída sí, así que reintento tres veces.
> - **Nunca reencolar**, porque un mensaje que siempre falla crearía un bucle infinito. Al final va a la DLQ con el header `x-death`.
> - **Idempotencia**: un mensaje puede llegar dos veces, así que cada consumidor guarda su id. Un reporte nunca genera dos tickets."

**Diapositiva 8 · Clúster (35 s)**

*Decisiones: colas quorum y por qué 2 nodos aquí y 3 en producción.*

> "El clúster evita que el broker sea un punto único de falla. Usé **colas quorum** porque replican cada mensaje en los dos nodos, y son el tipo recomendado en RabbitMQ 4, que eliminó las colas espejadas. La limitación: una cola quorum necesita mayoría. Con dos nodos, si cae uno, las colas se detienen sin perder datos. La pauta pide dos; en producción usaría tres, en máquinas distintas, porque si cae uno, los otros dos siguen siendo mayoría."

**Diapositiva 9 · Microservicio administrador (25 s)**

*Decisiones: servicio aparte y validación estricta.*

> "La administración del broker está en un **microservicio aparte**: es una responsabilidad delicada, así que queda aislada y solo para ADMIN, y el backend no expone nada que pueda borrar una cola. Valido los nombres —ni vacíos ni con el prefijo reservado `amq.`— y registro cada operación con el correo del usuario."

## Parte 2: demo en vivo (unos 4 min 15 s)

Pasa a la **diapositiva 10** y di: *"Ahora muestro que estas decisiones funcionan"*.

**1. API Gateway (20 s)**

- Muestra las rutas, el authorizer `entra-jwt` y CORS.

> "Cada ruta va a su microservicio y todas las de `/api` pasan por el authorizer. CORS solo acepta mi dominio, así que una página de otro sitio no puede llamar a la API desde el navegador."

**2. Entra ID (20 s)**

- En *Usuarios*, muestra encargado1, usuario1 y la cuenta administradora. Luego, los App Roles y a quién está asignado cada uno.

> "Este es el tenant con sus usuarios. Los permisos se administran aquí, no en el código: para dar acceso basta con asignar un rol."

**3. Validación del JWT (1 min)**

- En una pestaña, abre `https://mld2c6mojh.execute-api.us-east-1.amazonaws.com/api/tickets`: aparece `Unauthorized` (401).

> "Sin token, el Gateway rechaza la petición antes de que llegue a la EC2."

- En la consola de esa misma pestaña, ejecuta el `fetch` con `Bearer token-inventado`: `Status: 401`.

> "Con un token inventado también: el authorizer revisa la firma con las claves públicas de Entra y no es de Microsoft."

- Con encargado1, abre la pestaña **Red**, filtra `execute-api` y entra a Tickets: **200**. Muestra la *Request URL* y el header `Authorization: Bearer…`.

> "Con un token real, emitido por Entra con OpenID Connect, el Gateway lo acepta. Y se ve que el frontend llama a la API a través del API Gateway."

- Con usuario1, entra a Tickets: **403**.

> "Este token también es válido y pasa el Gateway, pero usuario1 no tiene el rol y ms-tickets responde 403. El 401 lo pone el Gateway y el 403 el microservicio: son las dos capas."

**4. Flujo completo (1 min)**

- Con encargado1, crea un reporte y muestra en la terminal `[PUBLISH]`, `[NOTIFICACION]`, `[ESTADISTICAS]` y los `[ACK]`.

> "El backend respondió al instante y, después, cada consumidor procesó su copia. El ACK aparece después de guardar: esa es la decisión del ACK manual."

- Pasa el reporte a revisión: aparece `[TICKET]` con el ticket generado. Márcalo resuelto y abre Tickets y Estadísticas.

> "La tarea viajó por el exchange direct y solo ms-tickets la recibió."

**5. Falla controlada (1 min)**

- Publica `{esto no es json` en `ecopunto.tareas` con la key `ticket.mantencion` y muestra `[DLQ] motivo=NO_RECUPERABLE` y el header `x-death`.

> "Como es un error de datos, no reintenta: va directo a la DLQ y la cola sigue funcionando."

- Ejecuta `docker stop ecopunto-ms-tickets`, pasa otro reporte a revisión, muestra el mensaje en *Ready* y luego `docker start ecopunto-ms-tickets`.

> "La API respondió aunque ms-tickets estaba caído, y el mensaje esperó en la cola. Esto justifica haber hecho el ticket asíncrono."

**6. Clúster (35 s)**

- `docker compose ps` y, en el Management UI, los dos nodos, las 6 colas quorum con su DLQ y los exchanges topic y direct.

> "Aquí está docker compose corriendo en la nube, los dos nodos del clúster, las tres colas con su DLQ y los dos tipos de exchange."

**Diapositiva 11 · Cierre (20 s)**

> "En resumen: hice asíncrono solo lo que no necesita respuesta inmediata; elegí topic para hechos y direct para tareas; publico después del commit y con confirmación; confirmo los mensajes solo después de procesarlos, sin bucles y sin duplicados; y repliqué el broker en un clúster, sabiendo que en producción usaría tres nodos. Gracias, quedo atento a sus preguntas."

## Cobertura de la pauta EP4

| Requisito | Dónde se muestra |
|---|---|
| API Manager creado en la nube | Demo 1 · consola de API Gateway |
| Configuración del API Manager hacia el backend | Demo 1 · rutas e integraciones |
| El API Manager valida el JWT | Demo 3 · 401 sin token y con token falso, 200 con sesión |
| Tenant IDaaS con usuarios | Demo 2 · usuarios y App Roles en Entra ID |
| Login OAuth 2.0 / OIDC con JWT válido | Demo 3 · header `Authorization: Bearer` en la pestaña Red |
| Frontend consume la API vía API Manager | Demo 3 · llamadas a `execute-api…` en la pestaña Red |
| Backend y frontend desplegados e integrados | Diapositiva 3 y Demo 4 · flujo completo en producción |
| Dos nodos RabbitMQ en clúster | Diapositiva 8 y Demo 6 · *Nodes* en el Management UI |
| Tres colas con sus DLQ funcionando | Demo 5 y 6 · mensaje en `tickets.mantencion.dlq` y lista de colas |
| Exchanges direct y topic | Diapositiva 5 y Demo 6 · `ecopunto.tareas` y `ecopunto.events` |
| docker-compose levantando RabbitMQ en la nube | Demo 6 · `docker compose ps` en la EC2 |

La evidencia con capturas de cada punto está en [`evidencia/`](./evidencia/).
