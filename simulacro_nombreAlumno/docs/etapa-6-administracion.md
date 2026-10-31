# Etapa 6 — Administración general del gimnasio

Todas las rutas nuevas empiezan con `/api/admin` y requieren un JWT de una cuenta **ADMIN activa**. La cadena HTTP, el controller y los services verifican el rol explícitamente. Un PROFESOR o ALUMNO recibe 403 antes de consultar IDs o validar el cuerpo; sin autenticación se devuelve 401. No existe jerarquía implícita de roles: ADMIN consulta y administra mediante estas operaciones, pero no recibe permiso para registrar entrenamientos como alumno ni modificar plantillas como profesor.

Se conservan las rutas anteriores y los contratos de las etapas 1–5. No se implementa la etapa 7 ni el frontend.

## Endpoints

Prefijo de toda la tabla: `/api/admin`.

| Método | Ruta | Comportamiento |
| --- | --- | --- |
| GET | `/usuarios` | Buscar y paginar cuentas de los tres roles |
| GET | `/usuarios/{id}` | Cuenta, estado, rol, revisión e IDs de perfiles conservados |
| POST | `/usuarios` | Crear cuenta ADMIN, PROFESOR o ALUMNO y su perfil cuando corresponde |
| PUT | `/usuarios/{id}/estado` | Activar/desactivar, con revocación y protección del último ADMIN activo |
| PUT | `/usuarios/{id}/rol` | Cambiar rol con perfil coherente, sin eliminar historial |
| PUT | `/usuarios/{id}/email` | Cambiar email de acceso y sincronizar todos los perfiles conservados; revoca tokens |
| GET, POST | `/alumnos` | Buscar/paginar o crear cuenta y perfil de alumno |
| GET, PUT | `/alumnos/{id}` | Consultar/editar datos de perfil |
| POST | `/alumnos/{id}/cuenta` | Dar cuenta a un perfil legado sin usuario, conservando su ID |
| GET, POST | `/profesores` | Buscar/paginar o crear cuenta y perfil de profesor |
| GET, PUT | `/profesores/{id}` | Consultar/editar datos de perfil |
| POST | `/profesores/{id}/cuenta` | Dar cuenta a un perfil legado sin usuario, conservando su ID |
| GET | `/profesores/{id}/alumnos` | Alumnos vinculados, paginados |
| GET | `/alumnos/{id}/profesores` | Profesores vinculados, paginados |
| PUT, DELETE | `/alumnos/{alumnoId}/profesores/{profesorId}` | Vincular/desvincular sin tocar entrenamientos ni asignaciones |
| PUT | `/alumnos/{alumnoId}/profesores/reemplazo` | Reemplazar un vínculo en una transacción; conserva otros profesores del alumno |
| GET | `/rutinas` | Una entrada por familia: última versión; incluye archivadas si no se filtra |
| GET | `/rutinas/{id}` | Última versión de una rutina, incluso archivada |
| GET | `/rutinas/{id}/versiones` | Todas las versiones, paginadas |
| GET | `/asignaciones`, `/asignaciones/{id}` | Asignaciones y referencia exacta a su versión |
| GET | `/alumnos/{id}/historial` | Historial de asignaciones, paginado; incluye activas e históricas |
| GET | `/sesiones`, `/sesiones/{id}` | Sesiones y sus series/resultados, incluyendo correcciones y anulaciones |
| GET | `/alumnos/{id}/sesiones` | Historial de sesiones, paginado |
| GET | `/sesiones/{id}/auditoria` | Trazabilidad de correcciones del entrenamiento |
| GET | `/resumen` | Recuentos generales para un dashboard administrativo |
| GET | `/auditoria` | Cambios administrativos paginados; filtro opcional `usuarioId` |

Las altas responden 201; las consultas y modificaciones responden 200. DELETE del vínculo responde 200 con el perfil actualizado, **no borra cuentas ni datos históricos**. No hay endpoint para borrar físicamente usuarios o perfiles.

## Paginación, filtros y orden

Los listados paginados responden:

```json
{"contenido": [], "pagina": 0, "tamanio": 20, "totalElementos": 0, "totalPaginas": 0}
```

Parámetros comunes: `pagina` (0–1.000.000), `tamanio` (1–100, por defecto 20), `orden` (por defecto `id`), `direccion=asc|desc` (por defecto asc). El ID desempata órdenes repetidos. Un orden no permitido devuelve 400; nunca se permite ordenar por contraseñas ni navegar propiedades arbitrarias. Las búsquedas `q` distinguen literalmente `%` y `_`, no permiten inyectar comodines SQL y no distinguen mayúsculas.

| Listado | Filtros | Valores permitidos para `orden` |
| --- | --- | --- |
| usuarios | `q` (email/nombres/apellidos), `rol`, `activo` | id, email, rol, activo |
| alumnos | `q` (email/nombre/apellido), `activo`, `vigente`, `sinCuenta`, `nivel`, `objetivo`, `profesorId` | id, nombre, apellido, email, nivel, objetivo |
| profesores | `q` (email/nombre/apellido), `activo`, `vigente`, `sinCuenta`, `especialidad`, `alumnoId` | id, nombre, apellido, email, especialidad |
| rutinas | `q` (nombre/descripcion de última versión), `profesorId` (autor), `archivada` | id de versión, nombre, capturadaEn |
| versiones | ID de familia en ruta | id, numeroVersion |
| asignaciones/historial | `alumnoId`, `profesorId` (autor), `rutinaId` (familia), `activo` | id, fechaInicio, fechaFin |
| sesiones | `alumnoId`, `profesorId` (autor de plantilla), `rutinaId` (familia), `finalizada`, `desde`, `hasta` | id, fecha original |
| auditoria | `usuarioId` | id, creadaEn |

`desde` y `hasta` son fechas ISO inclusivas de la **fecha efectiva** de la sesión, considerando sus correcciones. Un rango invertido da 400. Ordenar por `fecha` usa la fecha original conservada. Los filtros de autor de una rutina **no** representan al profesor vinculado actualmente: para ver todo el historial de un alumno se filtra por `alumnoId` o se utiliza su ruta de historial.

`vigente` significa que el rol actual de la cuenta corresponde al perfil; `activo` indica si puede autenticarse. Un perfil conservado tras un cambio de rol no es vigente. `sinCuenta` permite localizar perfiles legados sin usuario. Los IDs de ruta prevalecen sobre el filtro equivalente en las rutas anidadas. Los filtros aplicables a cada listado son los de esta tabla.

## Altas y perfiles

POST `/usuarios` recibe `email`, `password`, `rol` y solamente el perfil correspondiente: `alumno` o `profesor`. ADMIN se crea sin perfil. No permite asignar estado ni privilegios adicionales mediante campos del cuerpo.

Datos de alumno:

```json
{"nombre":"Luis","apellido":"Rios","edad":25,"nivel":"INTERMEDIO","objetivo":"MEJORAR_RESISTENCIA","emailContacto":"contacto@example.test"}
```

`fechaNacimiento` es opcional; si se informa debe ser pasada y concordar con la edad (1–150). Nombres/apellidos: hasta 20 caracteres. Datos de profesor: `nombre`, `apellido`, `especialidad` obligatorios (especialidad hasta 100), `emailContacto` opcional. Email/contacto hasta 150 caracteres. El email de acceso se edita exclusivamente mediante la operación de identidad; el contacto es independiente. PUT de perfil reemplaza los campos del DTO, pudiendo limpiar contacto/fecha opcionales con null.

POST `/alumnos` o `/profesores` recibe `{email,password,perfil}`, usando los mismos datos anidados. Las contraseñas iniciales tienen mínimo 8 caracteres (12 para ADMIN), máximo 72 bytes UTF-8, y se guardan con BCrypt. Ninguna respuesta administrativa entrega contraseñas, hashes o JWT.

POST `/{alumnos|profesores}/{id}/cuenta` recibe `{password,motivo}`. Usa el email del perfil, normalizado; exige que no tenga cuenta y que no colisione con otra identidad, incluso inactiva o sin cuenta. No fusiona perfiles, no reinicia credenciales de cuentas existentes ni inventa usuarios para perfiles históricos automáticamente.

## Estado, rol e identidad

`revision` es un contador no secreto de cambios de seguridad, obtenido de GET `/usuarios/{id}`. Es obligatorio para estado, email y rol. Evita sobrescribir una decisión administrativa o cambio de contraseña/email concurrente. Revisión obsoleta, email duplicado, último administrador activo o conflicto de integridad: 409.

Ejemplos de cuerpos (sin secretos):

```json
{"activo":false,"revision":0,"motivo":"Baja administrativa"}
```

```json
{"rol":"PROFESOR","revision":0,"motivo":"Nuevo puesto","profesor":{"nombre":"Ana","apellido":"Paz","especialidad":"Fuerza"}}
```

```json
{"email":"nuevo@example.test","revision":1,"motivo":"Actualización verificada"}
```

Cada cambio efectivo de estado, rol o email incrementa la revisión e invalida los tokens anteriores; reactivar no rehabilita tokens viejos. Estas respuestas no crean tokens para otra persona: deberá usar el login único. Repetir estado/rol/email sin cambios y con revisión vigente no revoca ni genera un evento de cambio ficticio. Para modificar un perfil sin cambiar rol se utiliza PUT de perfil.

Un cambio de rol exige un perfil existente para el destino o datos completos para crearlo. Se conservan los anteriores, asociados a la misma cuenta, con IDs estables. Solo el rol actual concede acceso. Al dejar PROFESOR se eliminan sus vínculos actuales con alumnos; al dejar ALUMNO se eliminan los vínculos del perfil con profesores. Al volver al rol, **no se restauran esos vínculos automáticamente**. La auditoría del cambio registra la cuenta y los IDs de vínculos terminados. Rutinas de autoría, asignaciones, sesiones, resultados y sus auditorías permanecen referenciando los mismos perfiles/versiones.

Las asignaciones activas y las sesiones abiertas del perfil conservado no se borran ni se cierran artificialmente. No puede entrenar con un rol diferente, pero puede continuar su asignación si vuelve a ALUMNO. Los cambios de profesor mantienen el historial completo: el nuevo vinculado puede leerlo y el anterior pierde acceso al terminar el vínculo. Nuevas vinculaciones exigen un profesor activo con rol PROFESOR y un perfil con cuenta de rol ALUMNO; también se verifica en las rutas antiguas de selección del profesor. Desactivar una cuenta no termina por sí solo sus vínculos: suspende el acceso y conserva la organización hasta que ADMIN decida modificarla.

El bloqueo global de administración serializa cambios administrativos, bootstrap y cambios de estado. Se revalida al administrador después de esperar el bloqueo; una autorización ya revocada no puede ejecutar una escritura pendiente. Desactivar o quitar el rol al último ADMIN activo falla, incluso ante solicitudes concurrentes. Un cambio propio de rol/estado/email exitoso invalida también el JWT que lo solicitó.

Vínculos PUT/DELETE reciben `{motivo}`. Reemplazo recibe `{profesorAnteriorId,profesorNuevoId,motivo}`. Motivo no vacío, hasta 500 caracteres. El reemplazo solo termina el vínculo indicado y conserva otros docentes, tal como permitía el modelo anterior.

## Auditoría y resumen

La tabla `auditoria_administrativa` registra actor, cuenta afectada (si existe), acción, motivo, fecha y estados antes/después; no contraseñas, hashes ni tokens. Altas/ediciones de perfil usan un motivo descriptivo de la operación; estado/rol/email/vínculos requieren motivo. Los cambios y su auditoría se confirman juntos o se revierten juntos. Las rutas administrativas nuevas se auditan; no se inventan eventos para operaciones históricas ni se cambian los contratos de las rutas anteriores.

`/resumen` informa usuarios activos/inactivos, administradores activos, cuentas de roles PROFESOR/ALUMNO, perfiles totales (incluyen conservados) y sin cuenta, vínculos actuales, familias y versiones de rutinas, archivadas, asignaciones activas/totales y sesiones finalizadas/totales. Las cuentas por rol incluyen las inactivas; los perfiles físicos incluyen los conservados. **Sesión finalizada no equivale a día cumplido**, distinción que sigue presente en el detalle de sesiones. No se inventan porcentajes de asistencia ni progreso a partir de asignaciones.

## Migración MySQL 8.4 y recuperación

No hay cambios adicionales de variables de entorno respecto de [etapa 5](etapa-5-seguridad.md). Se mantiene `ddl-auto=validate`; no usar `update` para sustituir las migraciones.

1. Detener todas las instancias/escritores y realizar un backup verificable de esquema y datos con las herramientas de MySQL, incluyendo rutinas/procedimientos si los hubiera. Probar que el backup puede restaurarse en otra base.
2. En una base existente, aplicar solo migraciones pendientes en orden: 002, 003, 004 y luego `src/main/resources/db/mysql/005_administracion.sql`. **001 solo corresponde a una base vacía**.
3. Ejecutar 005 con el cliente MySQL conectado a la base correcta, con credenciales fuera del historial del shell (por ejemplo `mysql --login-path=gimnasio APPgimnasioBD < src/main/resources/db/mysql/005_administracion.sql`). El usuario de migración necesita permisos para procedimientos y DDL; la cuenta normal de la aplicación no los necesita.
4. Verificar el marcador 5 en `gimnasio_schema_migrations`, la tabla `auditoria_administrativa` y el índice único `uk_alumno_profesor`. Iniciar esta versión del backend y comprobar login/lecturas.

005 exige 004, detiene la ejecución **antes del DDL** ante vínculos duplicados, cuentas sin perfil para su rol o emails incoherentes. No elige qué duplicado eliminar, no cambia roles ni fabrica datos faltantes. Regularizar esas anomalías solo con evidencia y conservar una exportación de los registros afectados. Los perfiles legados sin cuenta siguen admitidos y pueden regularizarse por los endpoints indicados.

El DDL de MySQL hace autocommit: si falla, mantener la aplicación detenida. Comparar el esquema con el script y reejecutar 005 después de corregir la causa; `CREATE TABLE IF NOT EXISTS`, índice condicionado y marcador idempotente permiten retomar tras el DDL completo aunque falte el marcador. Un objeto parcial o creado manualmente con otro esquema exige revisión, no simplemente insertar el marcador. Si no se puede reparar con seguridad, restaurar **todo el backup** en una base nueva, comprobar integridad y redirigir la aplicación a esa base; conservar la base fallida para diagnóstico.

No ejecutar una versión anterior después de haber usado cambios de rol: su verificador asumía un solo tipo de perfil por cuenta. Un rollback de aplicación exige también restaurar el backup coherente anterior, o preparar una adaptación explícita. No borrar perfiles conservados para satisfacer al código viejo. Los scripts 002–004 siguen siendo idempotentes; 004 ya aplicada no reinterpreta los perfiles conservados.

## Verificación

Resultado final: **181 pruebas aprobadas, 0 fallos, 0 errores y 0 omitidas**, con `clean test` y MySQL **8.4.13**. Compilación correcta. Son las 159 pruebas anteriores más 22 casos nuevos; no se omitieron las pruebas de migración.

Se ejecuta la suite completa con MySQL 8.4, además de pruebas nuevas de control HTTP/service para los tres roles, filtros/paginación, altas, coherencia de perfil, cambios de email/rol, revocación y reactivación, perfiles legados, reemplazos de profesores y conservación del historial después de archivar o editar rutinas. Las pruebas concurrentes usan una base aislada y verifican el último ADMIN, revisiones obsoletas y rollback si falla la auditoría. Las migraciones 001–005 se prueban con datos legados, reejecución, fallos previos al DDL y validación Hibernate del modelo actual.

El comando reproducible, usando exclusivamente un servidor MySQL **de pruebas** y la configuración local de Java/Maven, es:

```sh
MYSQL_TEST_URL=jdbc:mysql://127.0.0.1:13306/mysql bash mvnw \
  -Dspring.datasource.url=jdbc:mysql://127.0.0.1:13306/gimnasio_tests \
  -Dspring.datasource.username="$MYSQL_TEST_USERNAME" \
  -Dspring.datasource.password="$MYSQL_TEST_PASSWORD" \
  -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver \
  -Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect \
  clean test
```

`MYSQL_TEST_USERNAME` y `MYSQL_TEST_PASSWORD` se exportan sin registrarlos en archivos versionados. El usuario de TEST necesita crear/eliminar bases aleatorias para las pruebas de migraciones y concurrencia. Las pruebas no apuntan a una base real del gimnasio.

Pendiente: consultas analíticas y optimización específica para los dashboards de etapa 7; frontend; recuperación/entrega de credenciales por canales externos; no hay fusión automática de identidades ni eliminación física. El bloqueo global prioriza seguridad frente a alto volumen de escrituras administrativas. No se aplicaron migraciones a una base real ni se publicaron cambios en GitHub.
