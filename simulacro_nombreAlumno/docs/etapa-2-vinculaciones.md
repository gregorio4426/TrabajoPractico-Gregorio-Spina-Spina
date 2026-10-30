# Etapa 2: vinculaciones y autorización

## Política

Se conservan ADMIN, PROFESOR y ALUMNO y el único login existente. ADMIN mantiene
el alta de profesores y ahora puede consultar alumnos, rutinas e historiales,
actualizar perfiles y gestionar vinculaciones. Esto no introduce herencia de
roles: las escrituras del catálogo de ejercicios y las plantillas de rutinas
siguen reservadas al PROFESOR, como antes. La administración completa de usuarios
se implementará en la etapa 6.

El profesor solo puede consultar o gestionar el seguimiento de alumnos vinculados
actualmente. Puede ver su historial completo, no solo sesiones de plantillas que
él creó. Puede asignar sus propias rutinas a esos alumnos. No puede editar la
identidad, el email o las credenciales de un alumno ni modificar perfiles de otros
profesores. Nivel y objetivo se gestionan mediante el endpoint de seguimiento.

El alumno consulta y modifica sus propios datos. Las rutas /me no aceptan un ID
ajeno; las variantes por ID comprueban la identidad. Solo ALUMNO puede registrar
series e iniciar/completar entrenamientos propios. ADMIN y PROFESOR pueden
consultar los registros autorizados, pero no escribir resultados en nombre suyo.

La propiedad de la plantilla sigue rigiendo su edición: el nuevo profesor puede
consultar rutinas históricas de sus alumnos, pero no editar plantillas ajenas.
El catálogo de plantillas permanece compartido. El campo alumnos de cada rutina
conserva su formato, pero filtra los nombres según la autorización del solicitante:
ADMIN ve todos, PROFESOR solo vinculados y ALUMNO solo su propio nombre.

## Endpoints nuevos

Todas las rutas empiezan en /api. Las respuestas de alumno usan AlumnoResponse.

| Método | Ruta | Acceso / respuesta |
| --- | --- | --- |
| GET | /profesores/me/alumnos | PROFESOR, lista de sus alumnos, 200; [] si no tiene |
| DELETE | /profesores/me/alumnos/{alumnoId} | PROFESOR vinculado, termina su propia relación, 204 |
| DELETE | /alumnos/me/profesores/{profesorId} | ALUMNO propietario, 200 |
| DELETE | /alumnos/{alumnoId}/profesores/{profesorId} | ADMIN o ALUMNO propietario, 200 |
| PUT | /alumnos/{alumnoId}/profesores/{anteriorId}/reemplazo/{nuevoId} | ADMIN o ALUMNO propietario, reemplaza solo esa relación, 200 |
| PATCH | /alumnos/{id}/seguimiento | ADMIN o PROFESOR vinculado, 200 |

Body para seguimiento:

```json
{"nivel":"AVANZADO","objetivo":"GANAR_MASA_MUSCULAR"}
```

Ambos campos son obligatorios y deben pertenecer a los enums existentes.
El reemplazo requiere que el profesor anterior esté vinculado (400 si no lo está)
y que el nuevo exista (404). Reemplazar por el mismo profesor es una operación
sin cambios si la vinculación existe. Si el nuevo ya estaba vinculado, no se duplica.

## Compatibilidad

- No se elimina ninguna ruta anterior.
- GET /alumnos devuelve todos para ADMIN y solo vinculados para PROFESOR.
- GET /alumnos/{id} admite además ALUMNO propietario y ADMIN.
- PUT /alumnos/{id} admite al propietario y ADMIN; no a profesores.
- POST /alumnos/{alumnoId}/profesores/{profesorId} sigue agregando una relación;
  ahora admite también ADMIN. Su variante /me sigue siendo solo para ALUMNO.
- Agregar una relación existente o retirar una inexistente es idempotente, si
  ambos perfiles existen y el solicitante tiene permiso.
- PATCH /profesores/{id} exige perfil propio para PROFESOR; ADMIN puede actualizar
  cualquiera. Los endpoints /me mantienen su rol específico.
- GET /rutinas y /rutinas/{id} permiten consultar plantillas a los tres roles sin
  revelar nombres de alumnos ajenos. No otorgan permiso de edición.
- Los historiales y rutina activa por alumnoId comprueban la vinculación actual,
  la propiedad del alumno o el rol ADMIN. El historial vacío autorizado devuelve
  200 con [], no 403. La ausencia de rutina activa sigue devolviendo 404.
- GET /sesiones/{id} y /sesiones/alumno/{alumnoId}/historial permiten ADMIN y
  profesores vinculados; el detalle admite también al alumno propietario.
- Registro público de alumnos y alta de profesores por ADMIN se conservan.

## Integridad y revocación

Se usa la relación muchos-a-muchos existente; no se fuerza un único profesor.
Agregar al nuevo profesor no elimina a los anteriores. Para un cambio sin ventana
intermedia debe usarse PUT de reemplazo. Las demás relaciones se conservan.

Agregar, quitar y reemplazar usan transacciones y un bloqueo de la fila de alumno.
Las escrituras de seguimiento y asignación usan el mismo bloqueo antes de comprobar
la autorización, coordinándose con la desvinculación. Las operaciones no borran ni
reescriben rutinas, asignaciones, sesiones o series. El alumno conserva su rutina
activa al cambiar de profesor, hasta que se le asigne otra.

AccesoService valida roles y perfiles y consulta la vinculación en la base en cada
operación. No usa las listas de relaciones del principal como caché de permisos.
Las solicitudes siguientes a una desvinculación quedan denegadas incluso con un
JWT emitido antes del cambio. Esto no cancela respuestas de lectura ya en curso.

Los controllers aplican @PreAuthorize y los services comprueban autorización
independientemente del controller. El alta de profesores y las escrituras de
ejercicios tienen además seguridad de métodos en sus services. Los métodos
internos de búsqueda de entidades siguen siendo auxiliares, no endpoints públicos.

## Pruebas

Desde simulacro_nombreAlumno, con Java 21:

```bash
bash mvnw clean test
```

La dependencia H2 es solo de test. src/test/resources/application.properties usa
una base en memoria, create-drop y open-in-view=false. No se necesita la base de
desarrollo para correr la suite. No se cambia application.yaml de producción.

- Se conserva la suite de contratos de la etapa 1; se adapta a las dependencias
  de autorización y a la inicialización de profesores como lista vacía.
- AccesoServiceTest verifica los tres roles, autenticación/perfiles ausentes,
  acceso ajeno y consulta actual de la vinculación.
- VinculacionAuthorizationTest usa login y JWT reales, filtros de Spring Security,
  @PreAuthorize, services, mappers y JPA contra H2. Cubre URLs manipuladas,
  autorización sin controller, cambio con el mismo token anterior, desvinculación,
  vinculación concurrente/idempotente, reemplazo inválido, historial vacío,
  escritura de resultados ajenos, privacidad de catálogos y permisos de ADMIN.
- El cambio de profesor se verifica conservando el número de asignaciones,
  sesiones y series y los pesos/repeticiones históricos, incluso con sesiones
  de rutinas creadas por diferentes profesores.
- La prueba original contextLoads se ejecuta con la configuración aislada de test.

H2 valida este flujo de integración, pero no sustituye una comprobación contra
MySQL real de sus particularidades de SQL y bloqueo. Esa verificación sigue
pendiente. Los cambios no requieren una migración estructural de la base existente.

## Fuera de esta etapa

No se implementan todavía versionado/archivo de rutinas, auditoría de vínculos,
renovación de JWT, rotación de secretos ni la administración completa de cuentas.
La conservación ante edición o eliminación de rutinas sigue pendiente de la etapa 3.
El historial sí se conserva ante cambios de vinculación, verificado en esta etapa.
Las respuestas de autenticación anónima mantienen el comportamiento anterior
(403); su normalización y el manejo de JWT inválidos corresponden a la etapa 5.
