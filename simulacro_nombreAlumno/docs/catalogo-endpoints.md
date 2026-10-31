# Catálogo de endpoints del backend

Generado desde OpenAPI 3.0.1: **108 operaciones HTTP en 90 rutas**. Incluye las 24 consultas nuevas de dashboards y estadísticas.

El rol listado no sustituye las verificaciones de propiedad, vínculos vigentes, autoría y estado. Ver [informe de etapa 7](etapa-7-dashboards.md) y [contrato completo con DTOs y parámetros](openapi.json). El servidor del contrato exportado se normaliza a `/`; al generar un cliente externo configurar la URL de la API, por ejemplo `http://localhost:8080`.

| Método | Ruta | Roles HTTP | Éxito | Operación |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/alumnos` | ADMIN | 200 | alumnos |
| POST | `/api/admin/alumnos` | ADMIN | 201 | crearAlumno |
| GET | `/api/admin/alumnos/{alumnoId}/estadisticas` | ADMIN | 200 | Estadísticas de un alumno autorizado; incluye todo su historial sin filtrar por autor de rutina |
| GET | `/api/admin/alumnos/{alumnoId}/estadisticas/actividad` | ADMIN | 200 | Actividad del alumno por fecha efectiva |
| GET | `/api/admin/alumnos/{alumnoId}/estadisticas/ejercicios` | ADMIN | 200 | Cargas y volumen por ejercicio del alumno |
| GET | `/api/admin/alumnos/{alumnoId}/estadisticas/ejercicios/{ejercicioId}/evolucion` | ADMIN | 200 | Evolución histórica del ejercicio para el alumno autorizado |
| PUT | `/api/admin/alumnos/{alumnoId}/profesores/reemplazo` | ADMIN | 200 | reemplazar_1 |
| DELETE | `/api/admin/alumnos/{alumnoId}/profesores/{profesorId}` | ADMIN | 200 | desvincular |
| PUT | `/api/admin/alumnos/{alumnoId}/profesores/{profesorId}` | ADMIN | 200 | vincular |
| GET | `/api/admin/alumnos/{id}` | ADMIN | 200 | alumno |
| PUT | `/api/admin/alumnos/{id}` | ADMIN | 200 | alumno_1 |
| POST | `/api/admin/alumnos/{id}/cuenta` | ADMIN | 201 | cuentaAlumno |
| GET | `/api/admin/alumnos/{id}/historial` | ADMIN | 200 | historial |
| GET | `/api/admin/alumnos/{id}/profesores` | ADMIN | 200 | profesoresAlumno |
| GET | `/api/admin/alumnos/{id}/sesiones` | ADMIN | 200 | sesionesAlumno |
| GET | `/api/admin/asignaciones` | ADMIN | 200 | asignaciones |
| GET | `/api/admin/asignaciones/{id}` | ADMIN | 200 | asignacion |
| GET | `/api/admin/auditoria` | ADMIN | 200 | auditoria_1 |
| GET | `/api/admin/dashboard` | ADMIN | 200 | Dashboard ADMIN |
| GET | `/api/admin/estadisticas` | ADMIN | 200 | Resumen de entrenamientos, exclusiones y cargas del período |
| GET | `/api/admin/estadisticas/actividad` | ADMIN | 200 | Actividad diaria paginada, en orden de fecha ascendente |
| GET | `/api/admin/estadisticas/ejercicios` | ADMIN | 200 | Ejercicios con datos válidos, paginados por ID ascendente |
| GET | `/api/admin/estadisticas/ejercicios/{ejercicioId}/evolucion` | ADMIN | 200 | Evolución observada por sesión y ejercicio, ordenada por fecha efectiva e ID ascendentes |
| GET | `/api/admin/profesores` | ADMIN | 200 | profesores |
| POST | `/api/admin/profesores` | ADMIN | 201 | crearProfesor |
| GET | `/api/admin/profesores/{id}` | ADMIN | 200 | profesor |
| PUT | `/api/admin/profesores/{id}` | ADMIN | 200 | profesor_1 |
| GET | `/api/admin/profesores/{id}/alumnos` | ADMIN | 200 | alumnosProfesor |
| POST | `/api/admin/profesores/{id}/cuenta` | ADMIN | 201 | cuentaProfesor |
| GET | `/api/admin/resumen` | ADMIN | 200 | resumen_2 |
| GET | `/api/admin/rutinas` | ADMIN | 200 | rutinas |
| GET | `/api/admin/rutinas/{id}` | ADMIN | 200 | rutina |
| GET | `/api/admin/rutinas/{id}/versiones` | ADMIN | 200 | versiones_1 |
| GET | `/api/admin/sesiones` | ADMIN | 200 | sesiones |
| GET | `/api/admin/sesiones/{id}` | ADMIN | 200 | sesion |
| GET | `/api/admin/sesiones/{id}/auditoria` | ADMIN | 200 | auditoriaSesion |
| GET | `/api/admin/usuarios` | ADMIN | 200 | usuarios |
| POST | `/api/admin/usuarios` | ADMIN | 201 | crear_1 |
| GET | `/api/admin/usuarios/{id}` | ADMIN | 200 | usuario |
| PUT | `/api/admin/usuarios/{id}/email` | ADMIN | 200 | email |
| PUT | `/api/admin/usuarios/{id}/estado` | ADMIN | 200 | estado |
| PUT | `/api/admin/usuarios/{id}/rol` | ADMIN | 200 | rol |
| GET | `/api/alumnos` | ADMIN, PROFESOR | 200 | listarTodos_1 |
| GET | `/api/alumnos/me` | ALUMNO | 200 | getMiPerfil_1 |
| PUT | `/api/alumnos/me` | ALUMNO | 200 | updateMiPerfil_1 |
| GET | `/api/alumnos/me/dashboard` | ALUMNO | 200 | Dashboard ALUMNO |
| GET | `/api/alumnos/me/estadisticas` | ALUMNO | 200 | Resumen de entrenamientos, exclusiones y cargas del período |
| GET | `/api/alumnos/me/estadisticas/actividad` | ALUMNO | 200 | Actividad diaria paginada, en orden de fecha ascendente |
| GET | `/api/alumnos/me/estadisticas/ejercicios` | ALUMNO | 200 | Ejercicios con datos válidos, paginados por ID ascendente |
| GET | `/api/alumnos/me/estadisticas/ejercicios/{ejercicioId}/evolucion` | ALUMNO | 200 | Evolución observada por sesión y ejercicio, ordenada por fecha efectiva e ID ascendentes |
| DELETE | `/api/alumnos/me/profesores/{profesorId}` | ALUMNO | 200 | desvincularMe |
| POST | `/api/alumnos/me/profesores/{profesorId}` | ALUMNO | 200 | elegirProfesor_1 |
| PUT | `/api/alumnos/{alumnoId}/profesores/{anteriorId}/reemplazo/{nuevoId}` | ADMIN, ALUMNO | 200 | reemplazar |
| DELETE | `/api/alumnos/{alumnoId}/profesores/{profesorId}` | ADMIN, ALUMNO | 200 | desvincular_1 |
| POST | `/api/alumnos/{alumnoId}/profesores/{profesorId}` | ADMIN, ALUMNO | 200 | elegirProfesor |
| GET | `/api/alumnos/{id}` | ADMIN, PROFESOR, ALUMNO | 200 | findById_1 |
| PUT | `/api/alumnos/{id}` | ADMIN, ALUMNO | 200 | update_2 |
| PATCH | `/api/alumnos/{id}/seguimiento` | ADMIN, PROFESOR | 200 | seguimiento |
| POST | `/api/auth/login` | Público | 200 | Login único para ADMIN, PROFESOR y ALUMNO |
| PUT | `/api/auth/me/email` | ADMIN, PROFESOR, ALUMNO | 200 | cambiarEmail |
| PUT | `/api/auth/me/password` | ADMIN, PROFESOR, ALUMNO | 200 | cambiarPassword |
| POST | `/api/auth/register/alumno` | Público | 201 | Registro público exclusivamente de ALUMNO |
| POST | `/api/auth/register/profesor` | ADMIN | 201 | registerProfesor |
| GET | `/api/ejercicios` | ADMIN, PROFESOR, ALUMNO | 200 | findAll |
| POST | `/api/ejercicios` | PROFESOR | 201 | create |
| DELETE | `/api/ejercicios/{id}` | PROFESOR | 204 | delete_1 |
| PUT | `/api/ejercicios/{id}` | PROFESOR | 200 | update_1 |
| GET | `/api/profesores` | ADMIN, ALUMNO, PROFESOR | 200 | listarTodos |
| GET | `/api/profesores/me` | PROFESOR | 200 | getMiPerfil |
| PUT | `/api/profesores/me` | PROFESOR | 200 | updateMiPerfil |
| GET | `/api/profesores/me/alumnos` | PROFESOR | 200 | misAlumnos |
| DELETE | `/api/profesores/me/alumnos/{alumnoId}` | PROFESOR | 200 | desvincularAlumno |
| GET | `/api/profesores/me/alumnos/{alumnoId}/estadisticas` | PROFESOR | 200 | Estadísticas de un alumno autorizado; incluye todo su historial sin filtrar por autor de rutina |
| GET | `/api/profesores/me/alumnos/{alumnoId}/estadisticas/actividad` | PROFESOR | 200 | Actividad del alumno por fecha efectiva |
| GET | `/api/profesores/me/alumnos/{alumnoId}/estadisticas/ejercicios` | PROFESOR | 200 | Cargas y volumen por ejercicio del alumno |
| GET | `/api/profesores/me/alumnos/{alumnoId}/estadisticas/ejercicios/{ejercicioId}/evolucion` | PROFESOR | 200 | Evolución histórica del ejercicio para el alumno autorizado |
| GET | `/api/profesores/me/dashboard` | PROFESOR | 200 | Dashboard PROFESOR |
| GET | `/api/profesores/me/estadisticas` | PROFESOR | 200 | Resumen de entrenamientos, exclusiones y cargas del período |
| GET | `/api/profesores/me/estadisticas/actividad` | PROFESOR | 200 | Actividad diaria paginada, en orden de fecha ascendente |
| GET | `/api/profesores/me/estadisticas/alumnos` | PROFESOR | 200 | Estadísticas de mis alumnos vinculados, incluyendo alumnos sin entrenamientos |
| GET | `/api/profesores/me/estadisticas/ejercicios` | PROFESOR | 200 | Ejercicios con datos válidos, paginados por ID ascendente |
| GET | `/api/profesores/me/estadisticas/ejercicios/{ejercicioId}/evolucion` | PROFESOR | 200 | Evolución observada por sesión y ejercicio, ordenada por fecha efectiva e ID ascendentes |
| GET | `/api/profesores/{id}` | ADMIN, ALUMNO, PROFESOR | 200 | findById_2 |
| PATCH | `/api/profesores/{id}` | ADMIN, PROFESOR | 200 | update_3 |
| GET | `/api/rutinas` | ADMIN, ALUMNO, PROFESOR | 200 | listarTodas |
| POST | `/api/rutinas` | PROFESOR | 201 | crear |
| GET | `/api/rutinas/activa/alumno/{alumnoId}` | ADMIN, PROFESOR, ALUMNO | 200 | getRutinaActiva |
| POST | `/api/rutinas/asignar/alumno/{alumnoId}/rutina/{rutinaId}` | PROFESOR | 200 | asignar |
| GET | `/api/rutinas/historial/alumno/{alumnoId}` | ADMIN, PROFESOR, ALUMNO | 200 | getHistorial |
| GET | `/api/rutinas/me/activa` | ALUMNO | 200 | getRutinaActivaMe |
| GET | `/api/rutinas/me/historial` | ALUMNO | 200 | getHistorialMe_1 |
| DELETE | `/api/rutinas/{id}` | PROFESOR | 204 | delete |
| GET | `/api/rutinas/{id}` | ADMIN, PROFESOR, ALUMNO | 200 | findById |
| PUT | `/api/rutinas/{id}` | PROFESOR | 200 | update |
| GET | `/api/rutinas/{id}/versiones` | ADMIN, PROFESOR, ALUMNO | 200 | versiones |
| GET | `/api/rutinas/{id}/versiones/{numero}` | ADMIN, PROFESOR, ALUMNO | 200 | version |
| POST | `/api/sesiones` | ALUMNO | 201 | iniciarSesion |
| GET | `/api/sesiones/alumno/{alumnoId}/historial` | ADMIN, PROFESOR | 200 | getHistorialDeAlumno |
| GET | `/api/sesiones/me/ejercicios/{ejercicioId}/ultima` | ALUMNO | 200 | getUltimoEjercicio |
| GET | `/api/sesiones/me/historial` | ALUMNO | 200 | getHistorialMe |
| GET | `/api/sesiones/{id}` | ADMIN, ALUMNO, PROFESOR | 200 | getDetalle |
| GET | `/api/sesiones/{id}/auditoria` | ADMIN, PROFESOR, ALUMNO | 200 | auditoria |
| PUT | `/api/sesiones/{id}/completar` | ALUMNO | 200 | completarSesion |
| PATCH | `/api/sesiones/{id}/correccion` | ALUMNO | 200 | corregirSesion |
| POST | `/api/sesiones/{id}/series` | ALUMNO | 201 | registrarSerie |
| DELETE | `/api/sesiones/{id}/series/{serieId}` | ALUMNO | 200 | anularSerie |
| PATCH | `/api/sesiones/{id}/series/{serieId}` | ALUMNO | 200 | corregirSerie |
| PUT | `/api/sesiones/{id}/series/{serieId}/restaurar` | ALUMNO | 200 | restaurarSerie |
