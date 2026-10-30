# Etapa 1: contratos y compatibilidad

Se conservan rutas, métodos, estados HTTP y campos JSON anteriores. Los campos
nuevos son aditivos. Los arrays siguen siendo arrays; no se introduce paginación
en esta etapa. No hay cambios de esquema ni migración de base de datos.

| Respuesta | Campos agregados |
| --- | --- |
| AlumnoResponse | id, nivel, objetivo, profesores |
| ProfesorResponse | id |
| EjercicioResponse | id, grupoMuscular |
| RutinaResponse | id, profesorId, asignacionId, fechaInicio, fechaFin, activa |
| AsignacionResponse | id, rutinaId, alumnoId, profesorId |
| DiaEjercicioResponse | ejercicioId |
| SesionResponse | alumnoId, rutinaId, diaRutinaId |
| SerieResponse | sesionId, diaEjercicioId, ejercicioId |

`profesores` es una lista de referencias con id, nombre, apellido y especialidad.
No contiene alumnos, rutinas, usuarios ni credenciales. No se agrega la lista de
alumnos a ProfesorResponse porque ese DTO también está disponible para alumnos;
la consulta autorizada de "mis alumnos" corresponde a la etapa 2.

El id de DiaEjercicioResponse identifica la prescripción del ejercicio dentro de
un día; ejercicioId identifica el ejercicio del catálogo. No son intercambiables.
Las series exponen ambos identificadores para permitir agrupaciones por ejercicio.

Los endpoints de rutina activa e historial, tanto /me como por alumnoId, agregan
el contexto de asignación. Cada entrada del historial representa una asignación;
una misma rutina puede aparecer varias veces con distinto asignacionId. En el
catálogo y el detalle sin contexto, asignacionId, fechaInicio, fechaFin y activa
son null: no significan que la rutina esté inactiva. fechaFin null en una
asignación activa indica que aún no terminó. No cambia el orden histórico actual.
El mapeo de asignaciones completa explícitamente los nombres y las referencias.

Los constructores Java anteriores de los DTOs ampliados se mantienen, además del
builder y los getters existentes. AuthResponse y sus tres roles no cambian.

## Perfiles

Las variantes /me y por ID usan la misma lógica de actualización de perfil.
Alumno actualiza nombre, apellido, email de contacto, fecha de nacimiento, nivel
y objetivo; calcula la edad a partir de la fecha efectiva. Omitir la fecha o el
email opcionales conserva el valor existente. Los perfiles antiguos sin fecha
pueden actualizarse sin NullPointerException y conservan su edad.
Profesor actualiza nombre, apellido, email de contacto y especialidad.

El email de perfil es de contacto. No modifica Usuario.email, contraseña ni rol,
y por tanto no cambia el login ni invalida accidentalmente el token actual.
La gestión explícita de identidad de autenticación corresponde a la etapa 5.
Las actualizaciones de perfil se ejecutan dentro de una transacción.

## Pruebas

Con Java 21, desde simulacro_nombreAlumno:

```bash
bash mvnw '-Dtest=utn.simulacro_nombreAlumno.contract.*Test' test
```

- ResponseMappingTest: mappers MapStruct generados, referencias, datos reales,
  ciclos, ausencia de credenciales, asignaciones repetidas y constructores previos.
- ProfileCompatibilityTest: actualización, campos opcionales, nacimiento ausente,
  aislamiento de identidad de login y rechazo de actualización de otro alumno.
- HistoryContractTest: contexto real de asignación en los services de historial y
  rutina activa, conservando los formatos anteriores.
- HttpCompatibilityTest: rutas, JSON, arrays, respuestas 200/201 y login con los
  tres roles mediante MockMvc standalone y services simulados.

Estas pruebas no requieren MySQL. MockMvc standalone no aplica la cadena de
seguridad ni los proxies @PreAuthorize: no prueba autorización integrada.
La prueba original contextLoads requiere la base configurada; no se desactiva ni
se modifica. Las pruebas de integración MySQL se completarán en etapas posteriores.

## Límites pendientes

Esta etapa expone la información que ya existe; todavía no versiona rutinas ni
protege el historial ante cambios o eliminación. No modifica los permisos
actuales. Las vinculaciones, pérdida de acceso del profesor anterior y acceso
histórico del nuevo profesor se implementarán en las etapas 2 y 3. No debe
interpretarse esta etapa como habilitación para desplegar el backend en producción.
