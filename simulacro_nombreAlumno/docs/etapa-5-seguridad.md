# Etapa 5: seguridad, identidad y errores

Se conservan los tres roles y los permisos de las etapas anteriores. ADMIN no
hereda PROFESOR ni ALUMNO. El alta pública sigue creando exclusivamente ALUMNO;
el alta de profesores continúa requiriendo ADMIN tanto en HTTP como en service.
No se implementa todavía la administración general de usuarios de la etapa 6.

## Variables y ejecución local

`application.yaml` ya no contiene credenciales de MySQL ni claves JWT. No hay
contraseña ni email predeterminados del administrador. `.env.example` solo contiene
nombres de variables, valores no secretos y espacios vacíos para completar.

| Variable | Uso / valor por defecto |
| --- | --- |
| GYM_DB_URL | JDBC MySQL; por defecto jdbc:mysql://localhost:3306/APPgimnasioBD |
| GYM_DB_USERNAME | Usuario de base de datos; obligatorio |
| GYM_DB_PASSWORD | Contraseña de base de datos; obligatoria como configuración explícita |
| GYM_JWT_SECRET_BASE64 | Clave aleatoria Base64 de al menos 32 bytes; obligatoria, sin fallback |
| GYM_JWT_TTL | Duración ISO-8601 positiva, máximo 24 horas; por defecto PT24H |
| GYM_JWT_ISSUER | Emisor esperado; por defecto gimnasio-api |
| GYM_CORS_ALLOWED_ORIGINS | Lista separada por comas de orígenes explícitos; vacía por defecto |
| GYM_BOOTSTRAP_ADMIN_ENABLED | false por defecto; activar solo para crear el primer administrador |
| GYM_BOOTSTRAP_ADMIN_EMAIL | Email inicial, requerido si se habilita bootstrap y no existe ADMIN |
| GYM_BOOTSTRAP_ADMIN_PASSWORD | Secreto inicial, mínimo 12 caracteres y máximo 72 bytes UTF-8 |

Desde `simulacro_nombreAlumno`, con Java 21 y MySQL preparado:

```bash
cp .env.example .env
# Completar .env de forma segura y aplicar las migraciones pendientes.
# El archivo debe ser propio y confiable: source ejecuta contenido de shell.
set -a
. ./.env
set +a
bash mvnw spring-boot:run
```

Si se usa source, los valores con espacios o metacaracteres deben citarse como
valores de Bash. Spring Boot no carga automáticamente el archivo .env. Se puede usar un gestor
de secretos o la configuración de ejecución del IDE en lugar de source. No
registrar esos valores ni versionar .env. Para generar una clave nueva localmente:

```bash
openssl rand -base64 32
```

Guardar el resultado de forma segura; no pegarlo en documentación, commits o chat.
No reutilizar la clave que estaba publicada en el código anterior. Al desplegar
esta etapa todos los usuarios deben volver a iniciar sesión. La clave anterior
publicada no puede considerarse secreta aunque se borre del archivo actual.

## Creación inicial del administrador

Bootstrap está deshabilitado por defecto. Para una base nueva, configurar email
y contraseña iniciales mediante variables seguras y activar temporalmente el flag.
La creación se ejecuta después de verificar las migraciones. Un bloqueo de la
fila de coordinación `seguridad_sistema` serializa el bootstrap entre instancias.

Solo se crea un ADMIN si no existe ninguno. Se valida email y longitud de contraseña,
se almacena BCrypt y no se imprime la contraseña ni el token. Si el email pertenece
a una cuenta existente, el proceso falla: nunca eleva un ALUMNO o PROFESOR a ADMIN.
Si ya existe ADMIN, no lo duplica, no resetea sus credenciales ni lo reactiva, aunque
esté desactivado. Luego de la creación, desactivar el flag y retirar los secretos
iniciales de la configuración de ejecución.

Las contraseñas existentes no cambian durante la migración. Un administrador que
haya usado las credenciales públicas anteriores debe rotar su contraseña antes
de reabrir el servicio. El bootstrap no es un mecanismo anónimo de recuperación.
Para rotación con sesión autenticada se incluye el cambio de contraseña propio
explicado más abajo.

## JWT y revocación

Se firma y se acepta exclusivamente HS256. Se verifican firma, emisor, vencimiento,
fecha de emisión, asunto y claims enteros obligatorios `uid` y `ver`. El token no
incluye contraseñas. Un token sin esos claims, vencido, con firma/algoritmo inválido,
con usuario inexistente o manipulado recibe 401.

En cada solicitud se carga el usuario por su ID estable y se comprueban:

- activo=true;
- versión del token igual a la del usuario;
- email/asunto igual al email actual.

Las autoridades se obtienen del rol actual de la base; el claim roles no otorga
privilegios. Así tampoco se puede reutilizar un token con email antiguo para
suplantar a otra cuenta que registre posteriormente ese email.

Desactivar una cuenta incrementa `tokenVersion`. Reactivarla también lo incrementa:
los tokens anteriores no recuperan validez. Cambiar email o contraseña invalida
todos los tokens anteriores de esa cuenta y devuelve un AuthResponse con token
nuevo para la operación actual. Cada respuesta conserva token, email, rol y perfilId.

Se preparó `UsuarioEstadoService.cambiarEstado`, autorizado solo para ADMIN y con
protección del último administrador activo. No hay endpoints de activar/desactivar
usuarios en esta etapa: corresponden a la etapa 6. Las transiciones deben usar
ese servicio; cambiar flags directamente en SQL sin incrementar tokenVersion no
es un mecanismo soportado de revocación. Las solicitudes ya autorizadas y en curso
no se cancelan retroactivamente.

Rotar la clave y reiniciar invalida globalmente los JWT anteriores. No se agregan
refresh tokens, revocación individual de un JWT ni recuperación de contraseña.

## Coherencia entre identidad y perfiles

`Usuario.email` es la identidad de acceso canónica: trim y minúsculas con Locale.ROOT.
El email de un perfil vinculado debe coincidir con el del usuario. El nuevo campo
`emailContacto` permite conservar y editar un contacto alternativo independiente.

GET y PUT de perfiles conservan sus rutas. Un PUT puede omitir email o repetir el
email de acceso actual. Si solicita cambiarlo, devuelve 400 e indica usar el
endpoint explícito de identidad: evita cambiar credenciales inadvertidamente o
mantener dos emails de acceso distintos. `emailContacto` sigue siendo editable
con las reglas de perfil existentes. Esta precisión reemplaza la interpretación
transitoria del campo email como contacto de las etapas 1–4.

Rutas nuevas para los tres roles autenticados, siempre sobre su propia cuenta:

| Método | Ruta | Cuerpo |
| --- | --- | --- |
| PUT | /api/auth/me/email | email, passwordActual |
| PUT | /api/auth/me/password | nuevaPassword, passwordActual |

No aceptan un ID de otro usuario ni permiten cambiar el rol. La confirmación de
contraseña es obligatoria. Los nuevos registros y contraseñas requieren al menos
8 caracteres y no más de 72 bytes UTF-8 (límite de BCrypt); bootstrap exige 12.
El login sigue permitiendo verificar contraseñas legadas existentes sin reescribir
sus hashes.

El cambio bloquea el usuario, confirma la contraseña contra el hash vigente,
comprueba duplicados y actualiza identidad/perfiles/versiones en una transacción.
Un conflicto de email devuelve 409 sin cambios parciales. Email disponible significa
que no está ocupado por otro usuario, incluso desactivado, ni por un perfil legado
que impediría la sincronización. Una contraseña incorrecta devuelve 401 genérico.
Los perfiles sin usuario no reciben cuentas ficticias: conservan su dato legado y
requieren regularización administrativa para habilitar identidad de acceso.

El frontend debe guardar el AuthResponse nuevo después de cambiar identidad.
El login y registro público deben enviarse sin un Authorization antiguo: un Bearer
inválido se rechaza también en rutas públicas, no se ignora silenciosamente.

## CORS para Angular

Para desarrollo usar GYM_CORS_ALLOWED_ORIGINS=http://localhost:4200. Cada origen
contiene esquema, host y puerto si corresponde, sin ruta ni barra final. No se
permiten comodines ni patrones, userinfo, query o fragmentos. Una configuración
inválida hace fallar el arranque. En producción configurar los orígenes HTTPS reales.

Se permiten GET, POST, PUT, PATCH, DELETE y OPTIONS, con Authorization, Content-Type
y Accept. Se expone WWW-Authenticate para que el cliente identifique 401. No se
habilitan credenciales/cookies CORS: Angular debe enviar Bearer mediante HttpClient
sin withCredentials. CORS no autentica ni amplía permisos. El preflight válido
se procesa antes del JWT; un origen no permitido recibe 403 sin autorización CORS.
Por defecto no se habilita ningún origen web; clientes sin Origin siguen sujetos
al JWT y permisos normales.

## Contrato de errores

Las respuestas controladas mantienen `{mensaje, fecha}` sin mensajes internos,
SQL, stack traces, hashes, contraseñas ni tokens.

| Estado | Casos |
| --- | --- |
| 400 | Validaciones, JSON/tipos/parámetros inválidos y reglas de negocio |
| 401 | Falta de autenticación, contraseña incorrecta, JWT inválido/vencido/revocado, cuenta desactivada |
| 403 | Usuario autenticado sin rol/propiedad/vinculación suficiente; origen CORS rechazado |
| 404 | Recurso/ruta inexistente que el solicitante está autorizado a consultar |
| 409 | Email o clave única en conflicto, revisión obsoleta, integridad/concurrencia |
| 405 / 415 | Método o formato HTTP no soportado |
| 500 | Fallo inesperado o de infraestructura, con mensaje genérico |

Los 401 incluyen WWW-Authenticate: Bearer y no distinguen entre cuenta inexistente,
contraseña incorrecta y cuenta desactivada. Los errores de filtros se escriben como
JSON sin sendError. Los errores posteriores del negocio no se convierten en 401.
Solo el despacho ERROR del contenedor puede llegar al controlador de error sin
login, para conservar el 500 correcto; un GET normal a /error continúa protegido.
La respuesta de fallback de Spring Boot también tiene deshabilitados mensajes,
excepciones y trazas internas. No expone detalles aunque otro componente falle.

## Migración 004 y recuperación

Archivo: src/main/resources/db/mysql/004_seguridad_identidad.sql. Requiere 003,
MySQL >= 8.0.16 y detener todos los escritores con backup restaurable, como en las
guías anteriores. No ejecutar 001 sobre una base existente.

```bash
mysql --login-path=gimnasio --default-character-set=utf8mb4 APPgimnasioBD \
  < src/main/resources/db/mysql/004_seguridad_identidad.sql
```

La migración agrega activo/tokenVersion, contactos y la fila de coordinación.
Normaliza emails de usuarios y sincroniza perfiles vinculados. Si un email de perfil
era un contacto diferente, lo conserva en emailContacto antes de sincronizar. No
cambia roles, hashes, vínculos profesor/alumno ni historial de entrenamientos.
Los perfiles sin usuario se conservan, sin crear credenciales ni asignarles un rol.

Se detiene antes del DDL si hay emails canónicos duplicados, direcciones ocupadas
por otro perfil o perfiles incompatibles con el rol del usuario. No fusiona cuentas
ni elige automáticamente cuál conservar. Esos casos necesitan revisión con evidencia.

Los DDL son reanudables y el backfill/registro final son transaccionales. Una vez
marcada no se reactivan usuarios ni se resetean versiones al repetirla. El arranque
en validate verifica la marca 004, la coordinación y la coherencia de los perfiles.
Comprobar además:

```sql
SELECT * FROM gimnasio_schema_migrations WHERE version_num = 4;
SELECT COUNT(*) FROM seguridad_sistema WHERE id = 1;
SELECT COUNT(*) FROM alumnos p JOIN usuarios u ON u.id=p.usuario_id
 WHERE BINARY p.email <> BINARY u.email;
SELECT COUNT(*) FROM profesores p JOIN usuarios u ON u.id=p.usuario_id
 WHERE BINARY p.email <> BINARY u.email;
```

Comparar recuentos e identidades históricas con el backup. No borrar marcas para
forzar recapturas. Ante fallo, mantener escritores detenidos, conservar el estado
fallido, corregir la causa y reejecutar si procede. Para retroceder, restaurar el
backup en una instancia limpia y usar el binario correspondiente. Si ya hubo
escrituras posteriores, reconciliarlas antes de restaurar. No desplegar el binario
anterior sobre datos donde ya se aplicaron revocaciones o cambios de identidad:
no conoce estas garantías. La clave expuesta anterior no debe reutilizarse ni
siquiera en una recuperación; el código recuperado necesita una clave segura.

## Pruebas y límites

Con Java 21: `bash mvnw clean test`. El classpath de pruebas genera claves JWT
aleatorias efímeras; no se usa una clave fija de tests como fallback de producción.
Bootstrap se deshabilita en las pruebas generales. Sus pruebas usan una base
propia y ejercitan también creación concurrente e idempotencia. Esos valores de
test y los passwords sintéticos de fixtures no forman parte del jar de aplicación.

Para toda la suite contra MySQL, usar el servidor y base exclusivos de pruebas y
MYSQL_TEST_URL/propiedades de datasource descritos en la etapa 3. Las migraciones
crean y eliminan solo bases aleatorias; BootstrapSecurityTest usa una base propia
`stage5_bootstrap_*`. No ejecutar estas pruebas destructivas sobre producción.

La suite conserva las verificaciones de las etapas 1–4 e incluye login de los tres
roles, JWT vencidos/manipulados/incompletos, claims de rol no confiables, revocación
y reactivación, cambios de email/password, no suplantación al reutilizar un email,
CORS, códigos de error y no exposición de excepciones. La migración se prueba con
emails/contactos legados y con conflictos, conservando hashes y progreso.

No se aplicaron scripts a una base real del usuario. No se agregaron los endpoints
administrativos de la etapa 6, recuperación de contraseña ni refresh tokens.

### Resultado verificado

La reconstrucción final `clean test` aprobó 159 pruebas contra MySQL 8.4, sin
fallos, errores ni omisiones. Se ejecutaron las suites de etapas anteriores y
las migraciones 001–004. Las claves JWT de las pruebas fueron efímeras y la base
de bootstrap fue desechable. No se aplicaron scripts a datos reales ni se publicó
código en GitHub.
