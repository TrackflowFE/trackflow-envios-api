# TrackFlow · API de Envíos (API 1)

API 1 de las tres Web APIs de TrackFlow (Fábrica Escuela, UdeA 2026-2). Registra los envíos, emite los tokens de sesión y es dueña del catálogo de ciudades y del esquema de la base de datos compartida.

- Responsable: Oliver Quiroz.
- Historias de usuario: HU-01 (registrar un envío) y HU-08 (ingreso y sesión).
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
| `POST /api/shipments` | Operador | Solicita el registro de un envío. Responde 202 con el número de seguimiento; el registro se completa de forma asíncrona. |
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

Los identificadores de ciudad salen de `GET /api/cities`. Tipos de documento: CC, CE, TI, PP y NIT. La respuesta trae `trackingNumber` y `processingStatus: QUEUED`. Antes de aceptar un envío se valida que las dos ciudades tengan cobertura (un centro activo) y que exista una ruta entre ellas.

## Mensajería (RabbitMQ)

| Qué | Intercambio | Clave | Cola |
| --- | --- | --- | --- |
| Recibe las solicitudes de registro (privado de esta API) | `shipments.topic` | `shipment.requested` | `shipments.requests.queue` (descartados: `shipments.requests.dlq`) |
| Publica "envío creado" | `trackflow.events` | `shipment.created` | La consumen la API 2 y la API 3 con colas propias |
| Consume "evento logístico registrado" | `trackflow.events` | `logistics.event.registered` | `shipments.logistics-events.queue` (descartados: `shipments.logistics-events.dlq`) |

Los mensajes viajan como JSON. El evento `shipment.created` lleva: `trackingNumber`, `status`, `senderName`, `originCityId`, `originCity`, `recipientName`, `recipientAddress`, `destinationCityId`, `destinationCity`, `registeredAt` y `occurredAt`. Al publicar, si hay una transacción en curso se espera a que se confirme.

## Base de datos

- Una sola base de Supabase (PostgreSQL 17) compartida por las tres APIs. Esta API es la dueña del esquema: las migraciones Flyway (V1 a V22) en `src/main/resources/db/migration` son la fuente de verdad y se aplican al arrancar. No se debe cargar `schema.sql` a mano, porque dejaría la base sin historial de Flyway.
- Escribe: `shipments`, `cities`, `auth_users`, `auth_sessions` y `flyway_schema_history`.
- Solo lee: `logistics_centers` y `logistics_route_legs`, para comprobar cobertura y ruta al registrar un envío.
- Las ciudades, los centros y los tramos de ruta no se siembran por código: los cargan las migraciones.
- Pool de conexiones: máximo 3, porque el plan gratuito de Supabase admite pocas conexiones para las tres APIs.

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

Los valores se definen en el panel de Render, nunca en el repositorio. Sin definirlas, la aplicación usa los valores por defecto de desarrollo local (PostgreSQL y RabbitMQ en `localhost`).

## Ejecutar en local

Requisitos: Java 21, PostgreSQL y RabbitMQ en la máquina.

~~~bash
export TRACKFLOW_OPERADOR_CLAVE=<clave>
export TRACKFLOW_ADMIN_CLAVE=<clave>
export TRACKFLOW_JWT_SECRET=<texto largo y aleatorio>
./mvnw spring-boot:run
~~~

Queda en http://localhost:8080 y su documentación en http://localhost:8080/swagger-ui/index.html. Las pruebas se corren con `./mvnw test`.

## Despliegue

El archivo `render.yaml` define el servicio: Docker, plan gratuito, región Ohio y comprobación de salud en `/actuator/health`. Se crea en Render con New, Blueprint, eligiendo este repositorio; Render pide los valores de las variables privadas. En el primer arranque Flyway crea las tablas si la base está vacía.

## Notas

- Los tokens que se emiten aquí los validan las APIs 2 y 3 con el mismo secreto, y comprueban la sesión en la tabla `auth_sessions`.
- `POST /api/admin/rebuild-projections` solo vuelve a publicar los envíos. La republicación de eventos logísticos la expone la API 3.
