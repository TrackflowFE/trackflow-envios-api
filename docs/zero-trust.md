# Arquitectura Zero Trust: qué cumple TrackFlow y dónde

Revisión de los ocho controles exigidos, con el sitio exacto donde cada uno vive. Lo que
no se cumple aparece igual, con el motivo: un control que se da por hecho sin estarlo es
peor que uno declarado como pendiente.

Alcance: las tres APIs (envíos y acceso, reportes, logística).

## Resumen

| # | Control | Estado |
|---|---|---|
| 1 | Ninguna petición es confiable por defecto | Cumple |
| 2 | Autenticación por token firmado | Cumple |
| 3 | Claims mínimos, validación en filtros | Cumple |
| 4 | Límite de peticiones por cliente | Cumple |
| 5 | Límite centralizado en un API Gateway | Cumple en el clúster; en Render no |
| 6 | TLS/HTTPS en todas las llamadas | Cumple |
| 7 | Datos sensibles fuera de la URL | Cumple, con dos excepciones acotadas |
| 8 | Registro y supervisión de los accesos | Cumple |

## 1. Ninguna petición es confiable por defecto

No basta con que el token esté firmado y vigente. `SesionActivaValidator` consulta en
**cada petición** que la sesión siga abierta en la base de datos, así que un token de un
usuario desactivado, o al que le cambiaron el rol, deja de servir en la siguiente llamada
en lugar de seguir valiendo hasta que expire.

La regla por defecto es `anyRequest().denyAll()`: lo que no está explícitamente abierto,
está cerrado. Un endpoint nuevo no queda público por olvido, sino que falla hasta que
alguien decida su regla.

Lo mismo se aplica dentro del clúster: las APIs se autentican entre sí con el mismo token,
sin excepción para el tráfico interno.

## 2. Autenticación por token firmado

JWT con HS256, emitido solo por la API de envíos y acceso y verificado por las tres con el
mismo secreto. Cada token lleva un `jti` que identifica la sesión en el servidor, lo que
permite revocarla: un JWT sin estado no se puede cerrar antes de tiempo, y aquí sí.

## 3. Claims mínimos

El token lleva `jti`, `issuer`, `iat`, `exp`, `sub` y `roles`. Nada más: ninguna lista de
permisos, ningún dato personal. Cada usuario tiene **un solo rol** desde la migración V29,
y los permisos se derivan del rol en el servidor mediante la jerarquía declarada en
`SecurityConfig`, no viajan en el token.

La validación ocurre en la cadena de filtros de Spring Security, antes de llegar al
controlador.

## 4. Límite de peticiones por cliente

Cuatro límites por IP, uno por superficie expuesta:

| Dónde | Qué protege | Techo |
|---|---|---|
| `/api/auth/**` (envíos) | Fuerza bruta de credenciales y envío masivo de correos | 20/min |
| `/api/pickups/self-service/**` (envíos) | Sondeo de números de recogida | configurable |
| `/api/tracking/**` (reportes) | Enumeración de números de seguimiento | 60/min |
| `/api/shipments/*/events` y `/route` (logística) | Descarga masiva de historiales | 60/min |

El límite de autenticación merece una nota. El bloqueo de cuenta tras cinco fallos
(HU-09) protege a **un usuario**: cinco intentos y esa cuenta queda cerrada. No protege al
**servicio**, porque quien va rotando nombres de usuario nunca acumula cinco fallos en
ninguno y puede seguir indefinidamente. Son dos controles distintos para dos ataques
distintos, y hacían falta los dos.

Todos responden 429 con `Retry-After`, y sin revelar si la cuenta o el número existen.

## 5. API Gateway

**En el clúster de Kubernetes: cumple.** El controlador de Ingress hace de gateway y
declara el límite una sola vez, fuera del código (`k8s/40-gateway.yaml` en el repositorio
de logística). Las tres APIs quedan detrás de una entrada única, con un techo más bajo
para la autenticación que para el resto del tráfico.

**En Render: no, y es una limitación asumida.** Los tres servicios son despliegues
independientes, cada uno con su dominio, y no existe un punto por donde pase todo el
tráfico. Montar un gateway real allí significaría un cuarto servicio que además sería un
único punto de fallo en un plan gratuito donde los servicios se duermen a los 15 minutos.
Por eso el límite por IP dentro de cada API sigue siendo necesario en producción.

Que los filtros propios se queden **también cuando hay gateway** no es redundancia
accidental: es defensa en profundidad. Si alguien alcanza un servicio sin pasar por el
gateway —otro pod del clúster, una prueba contra el puerto directo— el límite propio
sigue aplicándose.

## 6. TLS/HTTPS

Render termina TLS en su proxy y redirige HTTP a HTTPS; `server.forward-headers-strategy`
hace que la aplicación sepa que la petición original fue HTTPS. Además, las tres APIs
mandan `Strict-Transport-Security` con un año de vigencia, de modo que el navegador se
niega a usar `http://` contra estos dominios aunque alguien escriba la dirección a mano.

La base de datos y el broker también van cifrados (`RABBITMQ_SSL=true`, TLS exigido por
Supabase).

## 7. Datos sensibles fuera de la URL

Ningún parámetro de consulta lleva credenciales ni datos personales: son búsquedas de
ciudades, rangos de fechas y estados. Las claves viajan siempre en el cuerpo de un POST
(`/api/auth/login`, `/api/auth/clave/*`).

Quedan dos casos en que una credencial viaja en una URL, y conviene nombrarlos en vez de
esconderlos:

1. **El número de seguimiento** (`/api/tracking/{numero}`). Es la credencial del cliente,
   que no tiene cuenta. Mitigado con 16¹² combinaciones posibles y el límite de 60/min.
2. **El token del enlace de recuperación de contraseña.** Es el patrón estándar: un
   correo no puede mandar un POST. Mitigado con un solo uso y caducidad corta.

Ambos están acotados con `Referrer-Policy: no-referrer`, que evita que la URL completa
salga hacia un sitio externo si el usuario pulsa un enlace, y con `Cache-Control:
no-store` para que la respuesta no quede en caches intermedias.

## 8. Registro y supervisión

**Quién, qué, cuándo y desde dónde.** La tabla `auth_audit` registra el actor, la acción,
sobre quién recae, el detalle y ahora la IP de origen (migración V30). Cubre creación de
usuarios, cambios de rol, activaciones, bloqueos, cambios de clave y correo, y el acceso
a datos personales completos de un envío (HU-13). Nunca guarda contraseñas ni hashes: el
registro no puede convertirse él mismo en una filtración.

Es solo de escritura: la aplicación no ofrece forma de editarlo ni de borrarlo, porque su
utilidad depende de que nadie pueda retocarlo después.

**Con qué patrón, en tiempo real.** Prometheus recoge cada 15 segundos las métricas de las
tres APIs y Grafana las dibuja: peticiones por segundo, errores por código de estado y
latencia, desglosados por API. Un pico de 401 o de 429 se ve en el panel mientras ocurre,
que es la diferencia entre detectar un abuso y reconstruirlo después.

## Lo que sigue pendiente

Honestidad sobre el techo actual:

- **Sin segundo factor de autenticación.** Quien conozca una contraseña entra.
- **Los límites viven en memoria de cada instancia.** Con varias réplicas, el techo real se
  multiplica por el número de réplicas. Centralizarlo exigiría un almacén compartido
  (Redis) o apoyarse solo en el gateway.
- **Nadie revisa la auditoría de forma periódica.** El registro existe y es consultable por
  el administrador, pero no hay una rutina ni alertas automáticas sobre él.
- **Sin gestión del riesgo de dependencias de terceros:** no se revisan versiones ni
  vulnerabilidades conocidas de las librerías.
