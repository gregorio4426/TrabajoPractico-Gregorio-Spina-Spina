# Etapa 3: historial duradero y progreso fiable

## Modelo y compatibilidad

La fila original de `rutinas` es la versión 1 y la identidad estable de la familia.
Las versiones posteriores son nuevas filas con `rutina_raiz_id` y un
`numero_version` consecutivo. No se renumeran las filas ni se trasladan los días
antiguos: asignaciones, días, sesiones y series existentes conservan sus FK.
La combinación raíz/número tiene una restricción única.

- `RutinaResponse.id` y los `rutinaId` de asignaciones/sesiones identifican la
  familia. Conservan el ID anterior para rutinas existentes.
- `rutinaVersionId` identifica la fila exacta de versión; `numeroVersion` indica
  su número. Los nuevos campos son aditivos.
- Los días y ejercicios prescritos de cada versión tienen IDs propios.
- Cada asignación conserva su FK a la versión concreta. Crear otra versión no
  altera una asignación activa. Para pasar al alumno a la última versión hay que
  asignarle la rutina otra vez, usando el endpoint existente.
- Cada sesión nueva registra `asignacionId`, la versión a través del día y
  `estadoVinculoAsignacion=REGISTRADA`. El inicio bloquea el alumno, como las
  asignaciones, para no vincular una sesión a una asignación elegida a medias.
- Cada serie conserva su ID, ejercicio del catálogo, prescripción, peso y
  repeticiones. `SerieResponse.fecha` proviene de la fecha original de su sesión,
  cuyo campo es inmutable. No se inventa una hora de ejecución por serie.

Los PUT siguen devolviendo 200 con la rutina. Una modificación estructural crea
otra versión sin tocar la anterior. Los cambios de nombre/descripción también
crean versión para que los entrenamientos anteriores conserven su contexto.
Un PUT idéntico no crea otra versión. Como antes, días nulos o vacíos en PUT
conservan la estructura, pero se copian con nuevos IDs si hay cambios de metadatos.

Se retiraron las cascadas REMOVE/orphanRemoval de las estructuras de rutina.
Los días y prescripciones son inmutables para Hibernate; los campos de contenido
histórico de Rutina no se actualizan en una fila existente. La única modificación
del encabezado original es el estado de archivo.

La prescripción guarda el nombre, descripción y grupo del ejercicio observados
al crear esa versión. Renombrar el catálogo no modifica esos snapshots, incluso
si el grupo original era desconocido/null. Se conserva la FK al ejercicio para
agrupar progreso por identidad, no por nombre. Eliminar ejercicios referenciados
se rechaza con 400; las FK siguen protegiendo los registros. Eliminar ejercicios
no utilizados continúa funcionando.

## Archivo y consultas

`DELETE /api/rutinas/{id}` conserva 204, pero marca la familia como archivada.
Repetirlo es idempotente. No borra versiones, asignaciones, sesiones ni series.
La rutina desaparece del catálogo y no se puede editar ni asignar de nuevo.
Las asignaciones activas no se cancelan: el alumno puede continuar o terminar sus
entrenamientos sobre esa versión, hasta que reciba otra rutina. Esto evita
interrumpir alumnos al retirar una plantilla del catálogo.

`GET /api/rutinas/{id}` devuelve la versión vigente de esa familia, incluso
archivada. El historial del alumno devuelve la versión exacta de cada asignación,
no la última versión de la plantilla. Se añaden estas consultas:

| Método | Ruta | Resultado |
| --- | --- | --- |
| GET | /api/rutinas/{id}/versiones | Lista de versiones en orden ascendente |
| GET | /api/rutinas/{id}/versiones/{numero} | Versión exacta, o 404 |

Los tres roles pueden consultar plantillas, como en la etapa 2, con los nombres
de alumnos filtrados por autorización. Solo el PROFESOR creador puede editar o
archivar una plantilla y asignarla a sus alumnos vinculados. Los historiales de
alumno/sesión mantienen las comprobaciones de propietario, ADMIN o profesor
vinculado. Cambiar profesor no cambia ninguna FK histórica y revoca el acceso
del anterior a esos historiales.

Edición, archivo y asignación coordinan un bloqueo de la raíz. Las escrituras
refrescan el encabezado después de esperar el bloqueo y consultan la última
versión con lectura bloqueante. Así evitan usar una versión o estado de archivo
obsoletos con el aislamiento REPEATABLE READ de MySQL.

## Migración conservadora de datos existentes

Las migraciones son scripts manuales versionados, no se ejecutan al arrancar ni
se introduce Flyway. `gimnasio_schema_migrations` registra la aplicación de 002.
El arranque pasa de `ddl-auto: update` a `validate`: no debe autocompletar columnas
antes de migrar ni arrancar con el esquema anterior. Además, antes del seeder,
HistorialMigrationVerifier comprueba la marca 002, el backfill y la coherencia de
las sesiones atribuidas. Un esquema con ALTERs completos pero datos pendientes
no puede arrancar en modo validate. Las pruebas con create-drop no activan este
verificador; la prueba de migración lo ejercita explícitamente en MySQL.

Una rutina existente se conserva como versión 1 `LEGADO_OBSERVADO`. `capturadaEn`
es la fecha de captura del estado disponible durante la migración, no la fecha
de creación original. Los nombres de ejercicios se copian del estado existente:
no es posible reconstruir nombres o estructuras que se sobrescribieron antes.
Las versiones nuevas usan `origenVersion=REGISTRADA`.

Para sesiones antiguas, el día existente determina su versión. La asignación se
busca por alumno, esa versión y los intervalos de fechas existentes:

| estadoVinculoAsignacion | Significado |
| --- | --- |
| REGISTRADA | Asignación registrada al iniciar la sesión con esta versión del backend |
| LEGADO_UNICO_POR_FECHA | Una única candidata y su intervalo cubre todo el día de la sesión |
| LEGADO_AMBIGUO | Varias candidatas; asignacionId queda null |
| LEGADO_FECHA_IMPRECISA | Una candidata, pero no cubre todo el día; asignacionId queda null |
| LEGADO_SIN_COINCIDENCIA | No hay candidata documentada; asignacionId queda null |

Se consideran los días limítrofes para detectar ambigüedades. No se supone que
una sesión ocurrió antes o después de una hora que no fue registrada. Tampoco se
crean asignaciones, rutinas ficticias ni fechas retroactivas. Los casos sin vínculo
siguen disponibles en todo el historial y en los cálculos de progreso.
`LEGADO_SIN_EVALUAR` es el valor transitorio al agregar la columna; la migración
lo resuelve antes de registrar que terminó.

La migración detiene el proceso ante referencias incompletas o series asociadas
a otro día. Esos casos requieren revisión con evidencia, sin borrarlos ni
atribuirles datos automáticamente. No se consolidan ni se eliminan asignaciones
activas duplicadas preexistentes; sus posibles anomalías deben diagnosticarse.

## Aplicación en MySQL 8.0+ / InnoDB

Ejecutar desde `simulacro_nombreAlumno`, con acceso administrativo al esquema.
Usar un login-path configurado de forma segura; no escribir contraseñas en los
comandos ni en el repositorio. Probar primero sobre una copia restaurada.

1. Detener todas las instancias y otros escritores. Registrar versión de la
   aplicación y recuentos de rutinas, días, prescripciones, asignaciones, sesiones
   y series. Guardar además IDs, fechas, repeticiones y pesos para comparar.
2. Crear y verificar un backup completo. Por ejemplo:

```bash
mysqldump --login-path=gimnasio --single-transaction --routines --triggers --events \
  --set-gtid-purged=OFF --databases APPgimnasioBD > backup_pre_etapa3.sql
```

3. **Base existente:** no ejecutar 001. Aplicar solo 002:

```bash
mysql --login-path=gimnasio --default-character-set=utf8mb4 APPgimnasioBD \
  < src/main/resources/db/mysql/002_historial_versionado.sql
```

   **Base nueva y vacía:** crear la base y ejecutar primero
   `001_esquema_anterior.sql`, después `002_historial_versionado.sql`. El primer
   script crea el esquema base sin borrar tablas y falla si ya existen.
4. No usar `--force`: cualquier error debe interrumpir la migración. Verificar:

```sql
SELECT * FROM gimnasio_schema_migrations WHERE version_num = 2;
SELECT estado_vinculo_asignacion, COUNT(*) FROM sesiones GROUP BY estado_vinculo_asignacion;
SELECT COUNT(*) FROM sesiones WHERE estado_vinculo_asignacion = 'LEGADO_SIN_EVALUAR';
SELECT COUNT(*) FROM sesiones;
SELECT COUNT(*) FROM series;
SELECT COUNT(*) FROM asignaciones_rutina;
```

   Debe existir una marca 2, ningún caso sin evaluar y los recuentos anteriores
   deben coincidir. Comparar también los datos originales; el script no cambia
   pesos, repeticiones, fechas, IDs ni FK anteriores.
5. Arrancar esta versión con Hibernate en `validate`. Revisar historiales y las
   atribuciones pendientes antes de reabrir el acceso. No volver a `update` para
   ocultar errores de validación.

Los ALTER de MySQL hacen autocommit: la migración completa no es una sola
transacción. Las adiciones de columnas/constraints comprueban existencia; el
backfill y su marca final sí son transaccionales. Con la aplicación aún detenida,
una interrupción puede reanudarse ejecutando nuevamente 002. Una migración ya
marcada no recaptura ni reasigna datos. No alterar la marca manualmente para
forzar una repetición.

## Recuperación ante un fallo

- Mantener todos los escritores detenidos, conservar logs y tomar una copia del
  estado fallido. Diagnosticar el error antes de reintentar.
- Si es una interrupción o permiso insuficiente, corregir la causa y reejecutar
  002; se ha probado la reanudación desde un ALTER parcial.
- Si hay relaciones inconsistentes o columnas incompatibles, no borrar registros
  ni ejecutar reparaciones automáticas. Revisar contra el backup y las evidencias.
- Para volver a la etapa 2, restaurar el backup en una instancia/esquema limpio,
  comprobar recuentos y datos, y desplegar el binario anterior apuntando a esa
  restauración. El dump de ejemplo incluye el nombre de la base; restaurarlo en
  una instancia separada evita sobrescribir la copia del fallo:

```bash
mysql --login-path=recuperacion < backup_pre_etapa3.sql
```

- Si ya hubo escrituras tras migrar, restaurar el backup perdería esos cambios:
  primero deben conservarse y reconciliarse. No hay un down-script destructivo.
  No desplegar el backend antiguo directamente sobre datos con nuevas versiones:
  sus operaciones de edición/eliminación no conocen esta estructura.

## Verificación reproducible

Java 21, desde `simulacro_nombreAlumno`:

```bash
bash mvnw clean test
```

La aplicación de pruebas usa H2 por defecto. `MysqlMigrationTest` se omite si no
hay servidor de pruebas explícito. Para validar migraciones en MySQL aislado,
configurar `MYSQL_TEST_URL` (por ejemplo `jdbc:mysql://127.0.0.1:13306/mysql`),
`MYSQL_TEST_USERNAME` y `MYSQL_TEST_PASSWORD` de forma segura. Esa cuenta de TEST
necesita crear bases; la prueba crea y elimina solo sus propias bases aleatorias
`stage3_migration_*`. No apuntarla a un servidor de producción.

Para toda la integración HTTP/JPA contra MySQL, usar además una base vacía y
exclusiva de pruebas y las propiedades de datasource correspondientes. La suite
usa create-drop por su configuración de test y elimina sus tablas al terminar.

```bash
MYSQL_TEST_URL=jdbc:mysql://127.0.0.1:13306/mysql bash mvnw clean test \
  -Dspring.datasource.url=jdbc:mysql://127.0.0.1:13306/gimnasio_tests \
  -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver \
  -Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

Agregar las propiedades de usuario y contraseña por configuración segura del
entorno de pruebas. La validación realizada usa MySQL 8.4 aislado, además de H2.
Las pruebas de migración comprueban datos legados, atribución única/ambigua/ausente,
fecha imprecisa, referencias de otro alumno/rutina, reanudación, repetición,
rechazo de inconsistencias y validación del esquema migrado contra Hibernate.
La integración funcional cubre versiones, metadatos, archivo, sesiones en curso,
reasignación, snapshots del ejercicio, cambio de profesor, rollback y ediciones
concurrentes, junto con las suites de las etapas 1 y 2.

No se implementan las validaciones y correcciones de series de la etapa 4.
Tampoco se han migrado datos de una base real del usuario: se ha validado el
procedimiento en bases desechables con datos de prueba.

### Resultado de esta implementación

La reconstrucción final `clean test` contra MySQL 8.4 aprobó 71 pruebas, sin
fallos, errores ni omisiones. Incluye la prueba original de arranque, las suites
anteriores y las pruebas nuevas de versionado, migración y protección del
arranque ante backfills incompletos. También se ejecutó la suite funcional con
H2 y las migraciones sobre MySQL. No se aplicaron scripts a datos reales.
