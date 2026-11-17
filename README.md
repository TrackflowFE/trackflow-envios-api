# TrackFlow · API de Envíos (API 1)

API 1 de las tres Web APIs de TrackFlow (Fábrica Escuela, UdeA 2026-2). Registra los envíos, emite los tokens de sesión y es dueña del catálogo de ciudades y del esquema de la base de datos compartida.

- Responsable: Oliver Quiroz.
- Historias de usuario: HU-01 (registrar un envío), HU-08 (ingreso y sesión), HU-14 (imprimir o copiar el comprobante con el número de seguimiento) y HU-20 (solicitar la recogida del envío a domicilio).
- Despliegue: https://trackflow-envios-api.onrender.com (Render, plan gratuito: la primera consulta tarda cerca de un minuto porque el servicio duerme).
- Documentación interactiva: https://trackflow-envios-api.onrender.com/swagger-ui/index.html

## Las tres APIs

| API | Qué hace | Repositorio |
| --- | --- | --- |
| API 1: Envíos y acceso (este repositorio) | Registro de envíos, ingreso y sesión, catálogo de ciudades, migraciones de la base | trackflow-envios-api |
| API 2: Reportes | Rastreo y reportes | trackflow-reports-api |
| API 3: Logística | Eventos, historial, rutas y centros | Repositorio propio de la API 3 |

Las APIs no se llaman entre sí: se comunican por eventos en RabbitMQ y comparten la base de datos, cada una con sus tablas. El frontend llama a cada API por su dirección.

## Endpoints

| Método y ruta | Acceso | Descripción |
| --- | --- | --- |
| `POST /api/shipments` | Operador | Solicita el registro de un envío. Responde 202 con el número de seguimiento y el comprobante (`receipt`); el registro se completa de forma asíncrona. |
| `GET /api/shipments/{trackingNumber}/receipt` | Operador | Comprobante del envío (HU-14): número, fecha, remitente, origen, destinatario y destino. Sin teléfonos ni direcciones. |
| `GET /api/pickups/operating-hours` | Público | Días, franjas, anticipación mínima y horizonte de las recogidas a domicilio. |
| `POST /api/pickups` | Operador | Solicita una recogida. Responde 202 con número `RC…` y `managementCode`, mostrado una sola vez. |
| `POST /api/pickups/self-service` | Público | Solicita una recogida sin guía; devuelve número y código de gestión una sola vez. Un operador podrá vincular la guía. |
| `GET /api/pickups/self-service/{pickupNumber}` | Código | Consulta la recogida y sus datos privados con `X-Codigo-Gestion`. |
| `PUT /api/pickups/self-service/{pickupNumber}/schedule` | Código | Reprograma con `{scheduledDate,timeSlot}`, sin cambiar la asignación. |
| `POST /api/pickups/self-service/{pickupNumber}/cancel` | Código | Cancela con `{reason}` opcional. |
| `GET /api/pickups` | Operador | Lista las recogidas; filtros opcionales `status` y `assignedTo=me`. |
| `GET /api/pickups/{pickupNumber}` | Operador | Una recogida con su historial. La dirección y el contacto solo llegan si el usuario es el asignado. |
| `GET /api/pickups/assignees` | Operador | Operadores activos a los que se puede asignar una recogida. |
| `PUT /api/pickups/{pickupNumber}/schedule` | Operador | Reprograma fecha y franja; si trae `assignedUsername`, la asigna. |
| `POST /api/pickups/{pickupNumber}/cancel` | Operador | Cancela una recogida que todavía no salió. Cuerpo opcional `{reason}`. |
| `POST /api/pickups/{pickupNumber}/dispatch` | Operador | Marca que el operador va en camino. |
| `POST /api/pickups/{pickupNumber}/complete` | Operador | Marca la recogida como realizada. |
| `PUT /api/pickups/{pickupNumber}/shipment` | Operador | Vincula un envío (`{trackingNumber}`); al completarse la recogida el envío entra en custodia. |
| `GET /api/cities` | Público | Busca ciudades. Parámetros `q` y `onlyCovered=true` (solo las que tienen cobertura). |
| `GET /api/cities/{id}` | Público | Una ciudad por su identificador. |
| `POST /api/auth/login` | Público | Ingreso con `username` y `password`. Devuelve el token. |
| `GET /api/auth/sesion` | Con token | Estado de la sesión; renueva su actividad. |
| `POST /api/auth/logout` | Con token | Cierra la sesión: el token deja de servir. |
| `POST /api/admin/rebuild-projections` | Administrador | Vuelve a publicar `shipment.created` de todos los envíos. |
| `GET /actuator/health` | Público | Estado del servicio. |

Ejemplo de solicitud de registro:

~~~json
{
  "sender": { "fullName": "Ana Torres", "documentType": "CC", "documentNumber": "1000000001",
              "phone": "3000000001", "address": "Calle 1 # 1-1", "cityId": 1 },
  "recipient": { "fullName": "Luis Gómez", "documentType": "CC", "documentNumber": "2000000002",
                 "phone": "3000000002", "address": "Carrera 2 # 2-2", "cityId": 10 },
  "description": "Documentos"
}
~~~

Los identificadores de ciudad salen de `GET /api/cities`. Tipos de documento: CC, CE, TI, PP y NIT. La respuesta trae `trackingNumber`, `processingStatus: QUEUED` y `receipt`, el comprobante que se imprime al registrar. Antes de aceptar un envío se valida que las dos ciudades tengan cobertura (un centro activo) y que exista una ruta entre ellas.

El comprobante (`receipt`) tiene seis campos: `trackingNumber`, `registeredAt`, `senderName`, `originCity`, `recipientName` y `destinationCity`. `GET /api/shipments/{trackingNumber}/receipt` devuelve exactamente los mismos datos para reimprimirlo, con `Cache-Control: no-store`; un número inexistente responde 404 y uno con formato inválido, 400. La fecha se recorta a microsegundos al admitir el envío, que es la precisión con que la guarda Postgres, para que la reimpresión no caiga en otro minuto.

Ejemplo de solicitud de recogida:

~~~json
{
  "address": "Calle 45 # 12-30, apto 201",
  "cityId": 1,
  "contactName": "Ana Torres",
  "contactPhone": "3001234567",
  "scheduledDate": "2026-11-20",
  "timeSlot": "08:00-10:00",
  "packageSize": "MEDIUM",
  "weightKg": 5.5,
  "shipmentTrackingNumber": "TF0123456789AB"
}
~~~

La guía es opcional: se puede pedir la recogida antes de registrar el envío y vincularla después. El autoservicio no acepta guía al crear, pues no comprueba su titularidad; la vinculación la realiza un operador. Solo se admiten guías en `REGISTERED` y no vinculadas a otra recogida no cancelada. Si se completa sin guía, la custodia comienza al vincularla. Estados de una recogida: `REQUESTED`, `SCHEDULED`, `ON_THE_WAY`, `PICKED_UP` y `CANCELLED`. La dirección y el contacto solo se devuelven al operador asignado o al remitente con código válido; para otros llegan vacíos con `accessRestricted: true`. El historial registra `RESCHEDULED` con fecha y franja anteriores; el motivo libre de cancelación es privado. Número o código incorrectos responden el mismo 404. Recogidas anteriores a V24 no admiten autoservicio.

El código tiene ocho caracteres generados con `SecureRandom`; solo su hash BCrypt se guarda o se envía en el comando. Hay que guardarlo en la primera respuesta: no existe recuperación. Si la respuesta se pierde, el resultado es incierto y un reintento puede crear otra recogida. Los cambios y la creación responden 202 al encolar, no al aplicarse.

## Mensajería (RabbitMQ)

| Qué | Intercambio | Clave | Cola |
| --- | --- | --- | --- |
| Recibe las solicitudes de registro (privado de esta API) | `shipments.topic` | `shipment.requested` | `shipments.requests.queue` (descartados: `shipments.requests.dlq`) |
| Publica "envío creado" | `trackflow.events` | `shipment.created` | La consumen la API 2 y la API 3 con colas propias |
| Consume "evento logístico registrado" | `trackflow.events` | `logistics.event.registered` | `shipments.logistics-events.queue` (descartados: `shipments.logistics-events.dlq`) |
| Recibe los comandos de recogida (privado de esta API) | `pickups.topic` | `pickup.command` | `pickups.requests.queue` (descartados: `pickups.requests.dlq`) |
| Publica "recogida completada" | `trackflow.events` | `pickup.completed` | La consume la API 3 con cola propia |

Los mensajes viajan como JSON. El evento `shipment.created` lleva: `trackingNumber`, `status`, `senderName`, `originCityId`, `originCity`, `recipientName`, `recipientAddress`, `destinationCityId`, `destinationCity`, `registeredAt` y `occurredAt`. Al publicar, si hay una transacción en curso se espera a que se confirme.

El evento `pickup.completed` lleva solo `pickupNumber`, `trackingNumber`, `cityId`, `pickedUpAt` y `occurredAt`. No contiene dirección ni contacto.

La API guarda `pickup.completed` en `pickup_outbox` dentro de la transacción de la recogida y reintenta su publicación hasta recibir confirmación del broker. La entrega es al menos una vez: logística deduplica usando el número de recogida. Hay que desplegar primero la API 3 para que su cola esté enlazada al exchange.

El comando de recogida viaja con `eventId`, `comando` (`CREAR`, `REPROGRAMAR`, `CANCELAR`, `DESPACHAR`, `COMPLETAR`, `VINCULAR`), `pickupNumber`, `actor` y `occurredAt`, más los datos que ese comando usa. El broker entrega al menos una vez: la reentrega se descarta por `eventId`.

Si al aplicar el comando la recogida ya no existe (una entrega tardía o un mensaje de otro entorno), el consumidor guarda un evento `REJECTED` con el número recibido y confirma el mensaje en vez de enviarlo a `pickups.requests.dlq`: reintentarlo no cambiaría el resultado.

El comando `CREAR` y `pickups.requests.dlq` contienen dirección y teléfono en claro y el hash del código. Restringir mediante ACL en CloudAMQP el acceso a las colas y sus respaldos, limitar su retención y usar TLS; estas acciones requieren configuración operativa y no las aplica este repositorio. Supervisar las colas de descartados, también la de logística: un confirm del broker no acredita que logística aplicó la custodia. Las comprobaciones al admitir y consumir reducen carreras pero una guía puede avanzar después; un 202 puede terminar descartado y no existe reconciliación automática. El publicador de comandos tampoco espera confirmación del broker.

## Base de datos

- Una sola base de Supabase (PostgreSQL 17) compartida por las tres APIs. Esta API es la dueña del esquema: las migraciones Flyway (V1 a V24) en `src/main/resources/db/migration` son la fuente de verdad y se aplican al arrancar. No se debe cargar `schema.sql` a mano, porque dejaría la base sin historial de Flyway.
- Escribe: `shipments`, `pickups`, `pickup_events`, `pickup_outbox`, `cities`, `auth_users`, `auth_sessions` y `flyway_schema_history`.
- Solo lee: `logistics_centers` y `logistics_route_legs`, para comprobar cobertura y ruta al registrar un envío.
- Las ciudades, los centros y los tramos de ruta no se siembran por código: los cargan las migraciones.
- Pool de conexiones: máximo 3, porque el plan gratuito de Supabase admite pocas conexiones para las tres APIs.

Antes de V24, revisar y resolver manualmente cualquier duplicado activo sin borrar historia: `select shipment_tracking_number, count(*) from pickups where status <> 'CANCELLED' and shipment_tracking_number is not null group by shipment_tracking_number having count(*) > 1;`. El índice único fallará si quedan duplicados. Desplegar la migración antes de ejecutar el código nuevo.

## Variables de entorno

| Variable | Para qué sirve |
| --- | --- |
| `TRACKFLOW_DB_URL` | Dirección JDBC de la base: `jdbc:postgresql://SERVIDOR:5432/postgres?sslmode=require` (pooler de Supabase en modo sesión) |
| `TRACKFLOW_DB_USER` | Usuario de la base: `postgres.` más el identificador del proyecto |
| `TRACKFLOW_DB_PASSWORD` | Contraseña de la base |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | Máximo de conexiones (3) |
| `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST` | Conexión al RabbitMQ compartido (instancia `trackflow-apis` en CloudAMQP) |
| `RABBITMQ_PORT`, `RABBITMQ_SSL` | `5671` y `true` para la conexión segura |
| `TRACKFLOW_JWT_SECRET` | Secreto de firma de los tokens. Tiene que ser idéntico en las tres APIs |
| `TRACKFLOW_OPERADOR_CLAVE` | Clave del usuario `operador`. Sin ella la escritura queda abierta |
| `TRACKFLOW_ADMIN_CLAVE` | Clave del usuario `admin`. Sin ella `/api/admin/**` queda inaccesible |
| `TRACKFLOW_CORS_ORIGINS` | Orígenes del frontend, separados por comas. `http://localhost:5173` siempre se admite |
| `TRACKFLOW_PICKUPS_DIAS` | Días de atención de las recogidas, en número ISO (1 lunes ... 7 domingo). Por defecto `1,2,3,4,5,6` |
| `TRACKFLOW_PICKUPS_FRANJAS` | Franjas de recogida, separadas por comas y con formato `HH:MM-HH:MM`. Por defecto de 08:00 a 18:00 en bloques de dos horas |
| `TRACKFLOW_PICKUPS_ANTICIPACION_HORAS` | Antelación mínima para programar una recogida. Por defecto 4 horas |
| `TRACKFLOW_PICKUPS_HORIZONTE_DIAS` | Cuántos días por adelantado se puede agendar. Por defecto 30 |
| `TRACKFLOW_PICKUPS_ZONA` | Zona horaria en que se cuentan los días y las franjas. Por defecto `America/Bogota` |
| `TRACKFLOW_PICKUPS_LIMITE_IP` | Máximo de peticiones de autoservicio por IP y ventana (100). |
| `TRACKFLOW_PICKUPS_LIMITE_RECOGIDA_POR_IP` | Máximo de intentos fallidos o en curso por IP y recogida (5). |
| `TRACKFLOW_PICKUPS_LIMITE_RECOGIDA` | Máximo global por recogida e instancia (30). |
| `TRACKFLOW_PICKUPS_LIMITE_SALTOS_PROXY` | Posición desde la derecha en `X-Forwarded-For`; 0 desactiva su uso (valor por defecto). |
| `TRACKFLOW_PICKUPS_LIMITE_PROXIES_CONFIABLES` | IP literales o rangos CIDR de proxies confiables, separados por comas (`10.0.0.0/8`, `fd00::/8`); obligatorios si se usan saltos. |
| `TRACKFLOW_PICKUPS_LIMITE_VENTANA_SEGUNDOS` | Ventana fija en segundos (900; máximo 86400). |

Los límites están en memoria por instancia y se pierden al reiniciar. Cuando el mapa se llena, LRU desaloja ventanas activas y puede reiniciar sus contadores; el límite global también permite bloquear temporalmente una recogida con suficientes IP. Se necesita limitación distribuida en el borde para una protección efectiva.

En local dejar saltos en 0: así se ignora `X-Forwarded-For` y se usa la IP de conexión original; si llega esa cabecera con saltos en 0, el arranque registra una advertencia porque todos los clientes compartirían la cuota. En Render, antes de activar saltos, verificar mediante un despliegue de prueba la IP del proxy y la cadena cruda `X-Forwarded-For`, y restringir el acceso directo a la API. Solo configurar proxies cuya identidad y comportamiento se hayan comprobado; para un salto, el último valor de la cabecera debe ser añadido por el proxy confiable. Los proxies se declaran por IP literal o por rango CIDR y todos los intermediarios incluidos deben ser confiables; un prefijo fuera del máximo de su familia se rechaza al arrancar. Si no pueden verificarse, mantener 0 y limitar peticiones en el borde; los clientes pueden compartir la cuota del proxy. La estrategia Spring de cabeceras reenviadas sigue activa para HTTPS y también exige un proxy confiable que filtre cabeceras de clientes.

Los valores se definen en el panel de Render, nunca en el repositorio. Sin definirlas, la aplicación usa los valores por defecto de desarrollo local (PostgreSQL y RabbitMQ en `localhost`).

## Ejecutar en local

Requisitos: Java 21, PostgreSQL y RabbitMQ en la máquina.

El proyecto apunta a JDK 21. Con JDK más recientes pueden aparecer avisos de
JaCoCo sobre versiones de archivos de clase no compatibles; usar JDK 21 para
obtener también un informe de cobertura fiable.

~~~bash
export TRACKFLOW_OPERADOR_CLAVE=<clave>
export TRACKFLOW_ADMIN_CLAVE=<clave>
export TRACKFLOW_JWT_SECRET=<texto largo y aleatorio>
./mvnw spring-boot:run
~~~

Queda en http://localhost:8080 y su documentación en http://localhost:8080/swagger-ui/index.html. Las pruebas se corren con `./mvnw test`.

Para ejecutar toda la suite sin usar un PostgreSQL local en el puerto 5432, se
pueden levantar servicios aislados en otros puertos:

~~~bash
docker run -d --rm --name tf-test-pg -e POSTGRES_DB=trackflow -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 127.0.0.1:55432:5432 postgres:16
docker run -d --rm --name tf-test-rabbit -p 127.0.0.1:55672:5672 rabbitmq:3-management
TRACKFLOW_DB_URL=jdbc:postgresql://localhost:55432/trackflow RABBITMQ_PORT=55672 ./mvnw test
docker stop tf-test-pg tf-test-rabbit
~~~

Esperar a que ambos servicios estén listos antes de ejecutar las pruebas.

## Despliegue

El archivo `render.yaml` define el servicio: Docker, plan gratuito, región Ohio y comprobación de salud en `/actuator/health`. Se crea en Render con New, Blueprint, eligiendo este repositorio; Render pide los valores de las variables privadas. En el primer arranque Flyway crea las tablas si la base está vacía.

## Notas

- Los tokens que se emiten aquí los validan las APIs 2 y 3 con el mismo secreto, y comprueban la sesión en la tabla `auth_sessions`.
- `POST /api/admin/rebuild-projections` solo vuelve a publicar los envíos. La republicación de eventos logísticos la expone la API 3.
- Los envíos recogidos a domicilio (HU-20) pasan al estado `IN_CUSTODY`, entre `REGISTERED` y `AT_DISTRIBUTION_CENTER`: el paquete ya está en la empresa, pero todavía no ha entrado a un centro. Ese estado lo aplica el evento `logistics.event.registered` que publica la API 3 al registrar la recogida.
- La admisión y la aplicación de los comandos de recogida son dos pasos: quien pide una recogida recibe el número `RC…` al momento, pero el registro en la base ocurre cuando el consumidor de la cola procesa el comando. Si se consulta en medio, puede salir un 404 o el estado anterior.

## Verificación de la HU-20

Estos escenarios se comprueban con la API levantada contra los servicios aislados de la
sección anterior, sin tocar la base de despliegue:

- Un comando de recogida que llega tarde, cuando la recogida ya no existe, deja un
  evento `REJECTED` en `pickup_events` y `pickups.requests.dlq` sigue vacía.
- La petición que excede el límite de intentos responde 429 con la cabecera
  `Retry-After` y con las cabeceras CORS, de modo que el navegador puede leerla.
- Un 404 en una subruta que no existe no consume la cuota de la recogida: solo lo hacen
  las rutas que autentican con el código de gestión.
- Un número inexistente y un código incorrecto responden el mismo 404 y el mismo
  mensaje: no revelan cuál de los dos falló.
