# Etapa 4: validaciones y consistencia de entrenamientos

## Compatibilidad y reglas de entrada

Se mantienen ADMIN, PROFESOR, ALUMNO, el login y todas las rutas anteriores. Las
operaciones que antes aceptaban datos inválidos ahora responden 400 o 409 según
el caso. Las estructuras de rutina siguen versionándose; no se modifican sus
versiones anteriores.

- Se valida el árbol completo de días y ejercicios, incluidos elementos null.
- Crear una rutina exige al menos un día, con nombre, orden positivo, ejercicios
  no vacíos y sin números de orden de día repetidos.
- Series y repeticiones sugeridas son positivas; IDs de ejercicio son positivos.
- Un PUT de metadatos con días omitidos o [] conserva la compatibilidad: copia la
  estructura anterior en una nueva versión si hay cambios de metadatos.
- Al registrar/corregir una serie: número y repeticiones realizadas obligatorios,
  enteros y positivos. Los números decimales no se truncan silenciosamente.
- Los pesos pueden ser null (no registrado), cero o positivos, pero siempre
  finitos. Se rechazan negativos, NaN e infinitos, también en valores sugeridos.
- La fecha de entrenamiento/corrección es obligatoria y no puede ser futura.
- Las correcciones exigen motivo no vacío (hasta 500 caracteres) y revisión
  esperada no negativa. La revisión proviene de la última respuesta consultada.

Controllers y services validan las solicitudes. Validar solo en Angular no será
suficiente. Solicitudes JSON ilegibles o tipos incompatibles responden 400.

## Finalización y cumplimiento son conceptos diferentes

Los campos anteriores `completada` y la ruta PUT /api/sesiones/{id}/completar se
conservan. En una sesión `completada` significa **finalizada**, no que se cumplió
por completo la prescripción. La respuesta agrega:

| Campo | Significado |
| --- | --- |
| finalizada | Alias explícito de completada |
| finalizadaEn | Momento de cierre registrado por este backend; null si antes no se registró |
| diaCumplido | Cumplimiento de series/repeticiones del día prescrito; null si el plan legado no es evaluable |
| seriesEsperadas | Suma de series sugeridas del día; 0 si no es evaluable |
| seriesRealizadas | Series vigentes, completadas y válidas |
| revision | Revisión actual del agregado para correcciones |
| fechaEfectiva | Fecha corregida si existe, o la fecha original |

Se puede finalizar un entrenamiento parcial, con `finalizada=true` y
`diaCumplido=false`. No se permite cerrar una sesión sin series válidas vigentes.
Después del cierre no se agregan series; sí pueden corregirse, anularse o
restaurarse registros existentes con las reglas auditadas siguientes.

Para `diaCumplido=true`, cada prescripción debe tener al menos las series y
repeticiones sugeridas. La numeración debe ser continua desde 1 por ejercicio.
Un ejercicio puede aparecer en dos bloques del día: su numeración es global para
ese ejercicio dentro de la sesión, aunque cada bloque tenga su propia prescripción.
El peso sugerido no es un mínimo obligatorio: no se presupone una carga necesaria
para considerar realizadas las series/repeticiones. Esto tampoco afirma haber
cumplido todos los días de una rutina ni un objetivo físico.

## Identidad, números únicos y progreso

Un número de serie no puede repetirse para el mismo ejercicio del catálogo dentro
de una sesión, incluso si aparece en dos prescripciones del día. La comprobación
se hace en el service y mediante clave única en la base. Las anulaciones liberan
el número sin borrar la fila original.

SerieResponse agrega `anulada`, `legadoExento`, `incidenciaLegado`, `revision` y
`fechaEfectiva`. Las series anuladas o con incidencias legadas pendientes siguen
visibles en el historial. Se excluyen de los contadores de cumplimiento y de la
consulta de última ejecución. Las series no completadas tampoco se usan en esa
consulta. El frontend deberá aplicar el mismo criterio al calcular métricas.
Peso null sigue siendo desconocido: no debe convertirse en cero para calcular
volumen o cargas.

`fecha` conserva siempre el dato original. Corregir una fecha solo escribe
`fechaCorregida`; `fechaEfectiva` refleja esa corrección y sirve para ordenar
historiales y elegir la última ejecución. Las FK de alumno, asignación, versión,
día y ejercicio no se cambian durante una corrección. Corregir la fecha no vuelve
a atribuir la sesión a otra asignación ni inventa una hora de ejecución.

## Correcciones y auditoría

Todas las rutas nuevas usan /api/sesiones como prefijo:

| Método | Ruta | Acceso |
| --- | --- | --- |
| PATCH | /{id}/series/{serieId} | ALUMNO propietario; corrige número, repeticiones y peso |
| DELETE | /{id}/series/{serieId} | ALUMNO propietario; anulación lógica, devuelve 200 con la serie |
| PUT | /{id}/series/{serieId}/restaurar | ALUMNO propietario; restauración explícita del mismo registro |
| PATCH | /{id}/correccion | ALUMNO propietario; corrige la fecha efectiva |
| GET | /{id}/auditoria | Propietario, ADMIN o PROFESOR actualmente vinculado |

Corregir/restaurar serie:

```json
{
  "numeroSerie": 1,
  "repsHechas": 8,
  "pesoUsado": 60,
  "motivo": "Peso anotado incorrectamente",
  "revision": 0
}
```

Anular serie (el DELETE requiere cuerpo):

```json
{"motivo":"Registro duplicado","revision":0}
```

Corregir fecha (usar la revisión de la sesión, no la de la serie):

```json
{"fecha":"2026-10-29","motivo":"Fecha anotada incorrectamente","revision":2}
```

Las revisiones del ejemplo son ilustrativas; deben consultarse antes de escribir.
Una revisión desactualizada o número ocupado responde 409. Una operación de
estado inválida (por ejemplo, corregir una serie anulada sin restaurarla) responde
400. No se permite anular la última serie válida de una sesión cerrada. La
restauración no crea otro entrenamiento ni otra serie: conserva su identidad y
exige datos válidos y un número disponible.

Cada corrección, anulación y restauración guarda un evento inmutable con actor,
fecha, motivo, IDs y snapshots antes/después. Incluye el estado anterior de
completada, incluso al corregir registros legados incompletos. Los valores
originales quedan en esos eventos aunque cambie la vista efectiva. No se crean
supuestas auditorías retroactivas durante la migración.

La modificación y su auditoría están en una sola transacción. Si no se puede
guardar la auditoría, se revierten también los resultados y las revisiones.
ADMIN y PROFESOR pueden consultar la auditoría autorizada, pero no modificar los
resultados del alumno. El profesor anterior pierde este acceso al desvincularse,
incluso con un JWT anterior, como en la etapa 2.

## Concurrencia y ordenación

- Agregar series, finalizar, corregir, anular y restaurar bloquean la misma fila
  de sesión. Dos registros simultáneos no pueden duplicar números ni agregarse
  después de un cierre comprometido.
- `@Version` y la revisión esperada protegen las correcciones. Dos correcciones
  con la misma revisión no se sobrescriben: una gana y la otra recibe 409.
- Asignaciones, cambios de profesor e inicio de sesión comparten el bloqueo del
  alumno. La asignación también bloquea la raíz de rutina para fijar la versión.
- Al reemplazar una asignación activa se hace flush de su desactivación antes
  de insertar la siguiente, evitando que el orden de flush de Hibernate viole
  la unicidad. La desactivación y el alta se confirman o revierten juntas.
- Una columna generada con clave única garantiza una sola asignación activa por
  alumno, también ante escritores concurrentes fuera del flujo normal.
- Historiales de sesión: fecha efectiva descendente e ID descendente como desempate.
  Asignaciones: inicio descendente e ID descendente; inicios desconocidos al final.
  Series: número e ID ascendentes. Auditoría: ID ascendente.

Los errores de integridad, revisión y bloqueo se traducen a 409 sin exponer SQL.
En MySQL los fallos de CHECK pueden llegar como JpaSystemException/3819; se tratan
como integridad. Otros fallos de persistencia conservan 500, sin fingir que son
conflictos de edición.

## Migración 003 en MySQL

Archivo: `src/main/resources/db/mysql/003_consistencia_entrenamientos.sql`.
Requiere MySQL >= 8.0.16 (CHECK efectivo), InnoDB y migración 002 aplicada.

La migración agrega las columnas de revisión, fecha efectiva y cierre, flags de
serie, identidad directa de ejercicio, auditoría, columnas generadas e índices.
Conserva las fechas originales, pesos, repeticiones, números, IDs y versiones.
No inventa fechas de cierre antiguas: `finalizadaEn` permanece null para ellas.

Las series legadas inválidas o duplicadas no se borran ni renumeran. Se marcan
`legadoExento=true` y una incidencia (`VALORES_INVALIDOS`, `NUMERO_DUPLICADO` o
ambas). En un grupo duplicado se marcan todas: no se decide cuál registro es real.
La clave generada excluye esas filas del conjunto que debe ser único, pero las
conserva. Los CHECK exigen valores válidos para los registros no exentos.
La incidencia original se conserva también después de corregir y validar una
serie; `legadoExento=false` indica que ya es utilizable si no está anulada.

Las sesiones antiguas vacías que ya estaban cerradas se conservan. No se reabren
ni se les agregan resultados ficticios. Las asignaciones activas duplicadas hacen
fallar el preflight antes de los ALTER: deben resolverse con evidencia, sin elegir
ni borrar una automáticamente. Referencias inconsistentes también bloquean la
migración. Los planes históricos inválidos no se reescriben: pueden producir
`diaCumplido=null` y deben sustituirse por una versión nueva válida.

Aplicación desde simulacro_nombreAlumno:

1. Detener todos los escritores y verificar/restaurar un backup de prueba. Guardar
   recuentos y valores de tablas históricas, como en la guía de etapa 3.
2. Diagnosticar antes de migrar:

```sql
SELECT alumno_id, COUNT(*) FROM asignaciones_rutina
 WHERE activa = 1 GROUP BY alumno_id HAVING COUNT(*) > 1;
SELECT s.sesion_id, d.ejercicio_id, s.numero_serie, COUNT(*)
 FROM series s JOIN dias_ejercicio d ON d.id = s.dia_ejercicio_id
 GROUP BY s.sesion_id, d.ejercicio_id, s.numero_serie HAVING COUNT(*) > 1;
SELECT id FROM series WHERE numero_serie <= 0 OR reps_hechas IS NULL
 OR reps_hechas <= 0 OR peso_usado < 0;
```

3. En una base existente de etapa 3, aplicar solo 003. En una base vacía, aplicar
   001, 002 y 003 en ese orden. En una base anterior, completar 002 antes de 003.

```bash
mysql --login-path=gimnasio --default-character-set=utf8mb4 APPgimnasioBD \
  < src/main/resources/db/mysql/003_consistencia_entrenamientos.sql
```

   Usar credenciales seguras mediante login-path y no pasar contraseñas en comandos.
   No usar --force. La cuenta migradora requiere DDL y creación de procedimientos.
4. Verificar la marca 3 y los datos:

```sql
SELECT * FROM gimnasio_schema_migrations WHERE version_num = 3;
SELECT id, sesion_id, ejercicio_id, numero_serie, incidencia_legado
 FROM series WHERE legado_exento = 1;
SELECT COUNT(*) FROM series;
SELECT COUNT(*) FROM sesiones;
SELECT COUNT(*) FROM asignaciones_rutina;
SELECT COUNT(*) FROM auditoria_entrenamientos;
```

   Los recuentos históricos y sus valores deben coincidir con el backup; auditoría
   empieza vacía. Las nuevas columnas no implican nuevos entrenamientos.
5. Arrancar con ddl-auto=validate. Además del verificador de 002, un verificador
   exige la marca 003 y la coherencia de la identidad del ejercicio/incidencias.

MySQL hace autocommit en DDL. El script es reanudable: comprueba columnas e índices,
realiza el backfill transaccionalmente y registra la marca solo al finalizar.
Puede haber DDL y backfill ya confirmados si un paso posterior falla; mantener la
aplicación detenida, diagnosticar la causa y reejecutar 003. Una migración marcada
no vuelve a clasificar registros ni sobrescribe correcciones posteriores.

Para recuperar el sistema, conservar una copia del fallo. Si no se puede reanudar,
restaurar el backup en una instancia limpia y desplegar el binario anterior contra
esa restauración. No eliminar columnas o auditorías como rollback improvisado.
Si ya hubo escrituras posteriores, conservarlas y reconciliarlas antes de volver
al backup. Ver también el procedimiento completo de backup/restauración de
[etapa 3](etapa-3-historial.md). No se aplicó esta migración a datos reales del usuario.

## Pruebas y límites

Ejecutar `bash mvnw clean test` con Java 21. Para la verificación real de MySQL,
configurar MYSQL_TEST_URL y las propiedades de datasource de TEST como se indica
en la etapa 3. MysqlMigrationTest crea y elimina únicamente bases aleatorias
`stage3_migration_*` / `stage4_migration_*`; no debe apuntar a producción. Las
pruebas de aplicación usan create-drop en una base exclusiva de pruebas.

Las pruebas cubren validación anidada y numérica (incluidos NaN, infinitos y
fracciones), cierres vacíos/parciales, ordenación, números repetidos por ejercicio,
correcciones/restauración auditadas, permisos de los tres roles, revisiones
obsoletas, fallo de auditoría con rollback, concurrencia de cierre/registro y
asignaciones, migraciones parciales/repetidas e incidencias históricas conservadas.
Las suites de etapas 1–3 siguen ejecutándose.

No se ha implementado la etapa 5 (seguridad y gestión de autenticación). Tampoco
se afirma cumplimiento global de una rutina de varios días ni se reparan datos
históricos sin evidencia. Las incidencias quedan disponibles para revisión y
corrección autorizada, no desaparecen del historial.

### Resultado verificado

La reconstrucción limpia final aprobó 119 pruebas contra MySQL 8.4, sin fallos,
errores ni omisiones. La ejecución completa con la configuración H2 también
aprobó las 119 pruebas, manteniendo activadas las migraciones en MySQL. No se
aplicaron migraciones a una base real del usuario ni se publicó código en GitHub.
