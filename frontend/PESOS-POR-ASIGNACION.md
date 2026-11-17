# Pesos individuales por asignación

La plantilla de rutina define días, bloques, series/vueltas, repeticiones y descansos. El editor no muestra ni envía `pesoSugerido`. Los pesos se configuran al asignar la plantilla y se editan desde el alumno vinculado → Rutinas e historial → Editar pesos individuales, o desde el enlace posterior a la asignación.

Cada valor corresponde a una posición prescrita de una versión concreta, de modo que ejercicios repetidos en distintos días o bloques pueden tener pesos distintos. Vacío significa sin peso sugerido; cero se conserva como valor conocido. Seleccionar otro alumno limpia los campos de peso. El autor de la rutina puede editar únicamente asignaciones activas de alumnos actualmente vinculados; se controla la revisión y se rechazan cambios obsoletos con 409. ADMIN conserva consulta y ALUMNO consulta solo sus propios datos.

Las sesiones capturan sus pesos sugeridos al iniciarse. Cambiar la asignación no modifica la plantilla, otros alumnos, versiones anteriores ni las referencias de sesiones ya iniciadas. Los resultados realizados siguen siendo registros separados de los pesos sugeridos.

## Migración 007

Aplicar `simulacro_nombreAlumno/src/main/resources/db/mysql/007_pesos_por_asignacion.sql` después de 006, con backup y escritores detenidos. Crea `pesos_asignacion`, `pesos_sesion` y revisión de asignaciones. Copia los pesos legados, incluido cero, sin borrar las columnas ni los valores originales. Las asignaciones/sesiones sin peso permanecen sin peso. Su reejecución no restaura valores que se hayan limpiado después de migrar.

Un cliente anterior puede seguir asignando sin cuerpo: se copian los pesos legados a esa asignación. El frontend nuevo siempre envía la configuración individual explícita (incluidos campos vacíos). Las escrituras de plantillas con pesos no nulos se rechazan con 400: configurar esos datos en la asignación.

## API

- `POST /api/rutinas/asignar/alumno/{alumnoId}/rutina/{rutinaId}` acepta cuerpo opcional `{ "pesos": [{ "diaEjercicioId": 123, "pesoSugerido": 60 }] }`.
- `GET /api/asignaciones/{id}` devuelve la versión exacta y los pesos asignados en `dias[].ejercicios[].pesoSugerido`, más `asignacionRevision`.
- `PUT /api/asignaciones/{id}/pesos` recibe `{ "revision": 0, "pesos": [...] }`. Reemplaza la configuración individual; posiciones no incluidas quedan sin peso. Solo PROFESOR autor y vinculado, asignación activa.
- `GET /api/rutinas/me/activa` e historiales devuelven los pesos de la asignación correspondiente. `SesionResponse.pesosSugeridos` devuelve la copia capturada al iniciar.

Los identificadores viajan solo dentro de los DTOs y las rutas; los formularios muestran nombres. Se quitaron asteriscos de etiquetas y las palabras de ubicación en los campos repetidos. Los grupos accesibles de día/ejercicio conservan contexto para lectores de pantalla y pruebas.

## Verificación

Angular compilado; 114 pruebas unitarias/HTTP aprobadas. La suite backend incorpora aislamiento entre alumnos, snapshots de sesión, cero/vacío, posiciones ajenas, permisos, revisiones obsoletas y rechazo de pesos en plantillas. Verificación completa de MySQL y Chromium en curso.
