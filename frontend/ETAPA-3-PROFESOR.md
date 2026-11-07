# Etapa 3 — Panel PROFESOR

Base aprobada: `codex/frontend-etapa-2-admin`, commit `9f595aeb440c309dba6a396a4a19fe30288cf7fd`.
Rama: `codex/frontend-etapa-3-profesor`. El backend permanece intacto; el panel ADMIN se conserva y se incluye en la regresión.

## Pantallas y contratos

- `/profesor`: dashboard desde `GET /api/profesores/me/dashboard`, con vínculos/rutinas/asignaciones actuales y entrenamientos del período. La fecha aplicada al dashboard se transmite a los gráficos para mantener el mismo período.
- `/profesor/alumnos`: exclusivamente `GET /api/profesores/me/alumnos`. El endpoint devuelve un array sin filtros ni paginación: búsqueda por nombre/apellido/email, nivel, objetivo y páginas de 20 se aplican localmente, indicándolo en pantalla.
- `/profesor/alumnos/:id`: perfil autorizado, seguimiento de nivel/objetivo por PATCH, rutina activa (404 se interpreta como ausencia de asignación, no como un error de permisos), historial de asignaciones y entrenamientos. Cada historial enlaza la versión exacta usada. No se modifica la identidad del alumno ni se registran resultados en su nombre.
- `/profesor/sesiones/:id`: consulta de detalle y auditoría de correcciones/anulaciones; las sesiones finalizadas se distinguen del cumplimiento del día.
- `/profesor/rutinas`: catálogo compartido, con filtro local de autoría propia y búsqueda. El backend solo lista familias no archivadas; una archivada puede consultarse por ID o historial. No se inventan filtros ni endpoints para listar/restaurar archivadas.
- `/profesor/rutinas/nueva` y `/:id/editar`: formularios reactivos con arrays de días/ejercicios reales, órdenes únicos y validación de IDs, series/repeticiones y pesos. Se envía RutinaRequest; no se copian IDs de días/versiones al request ni se calcula una versión ficticia. El servidor crea una versión nueva solo cuando corresponde. El editor comprueba autoría y archivo antes de mostrar sus controles; el servidor vuelve a autorizar la escritura.
- `/profesor/rutinas/:id` y `/:id/versiones/:version`: lectura actual/histórica, versiones conservadas, archivo lógico de la familia y asignación de la última versión propia no archivada a alumnos actualmente vinculados. Archivar no borra asignaciones ni entrenamientos. Una nueva asignación desactiva la anterior del alumno.
- `/profesor/ejercicios`: alta, edición y eliminación del catálogo compartido según los permisos reales. El GET prioriza grupoMuscular sobre nombre: al combinar filtros se consulta el grupo al servidor y se filtra el nombre en el array recibido. La paginación es local. La eliminación de ejercicios referenciados se rechaza y se muestra el mensaje del servidor, sin alterar el historial.
- `/profesor/progreso` y la pestaña de progreso del alumno: resumen, actividad paginada, ejercicios con resultados válidos y evolución paginada por ejercicio. Se utilizan exclusivamente las rutas estadísticas de `/api/profesores/me`; para alumno se incluye `/alumnos/{id}` y el backend comprueba el vínculo actual.
- `/profesor/perfil`: PUT de perfil propio, contacto independiente de la identidad y cambios de email/contraseña con passwordActual. Se valida el AuthResponse y se reemplaza el JWT persistido con el token nuevo. No existen refresh tokens. Las contraseñas no se guardan en localStorage ni se incluyen en confirmaciones.

Los DTOs se reutilizan desde los tipos ya generados en `admin/contracts.ts`; no agregan operaciones administrativas al cliente del profesor. Fuentes revisadas: OpenAPI, catálogo de endpoints, documentación de vínculos/historial/seguridad/estadísticas y services/controllers actuales del backend.

## Gráficos, precisión y seguridad

Los gráficos SVG presentan pesos máximos registrados, repeticiones realizadas, volumen por sesión/ejercicio, actividad por fecha efectiva y frecuencia semanal observada del período. No se calculan asistencia, calorías, duración, 1RM ni equivalencias de máquinas. No se etiqueta el peso como kg porque el backend no conserva unidades. Se distingue volumen parcial/completo y series excluidas/sin peso.

Los decimales originales se conservan como strings y están disponibles en tablas accesibles. El trazado es aproximado: solo representa números finitos dentro de límites de precisión conservadores; otros valores quedan en la tabla sin dibujarse. Los valores ausentes no se convierten en cero ni se unen a través de huecos. No se rellenan días sin registros. Los puntos se espacian por registro, no por tiempo transcurrido; cada etiqueta/tabla conserva fecha efectiva, sesión y nombre histórico.

La paginación de actividad, ejercicios y evolución utiliza los parámetros reales y queda anclada al período consultado hasta aplicar nuevas fechas. Las respuestas de una consulta anterior no reemplazan el estado de una consulta nueva. Una denegación de vínculo elimina los datos protegidos del panel y exige volver al listado o recargar; las URLs y controles no sustituyen la autorización del servidor.

Hay confirmaciones nativas, prevención de envíos duplicados y estados de carga, vacío, error y éxito. Los formularios conservan Angular 22 standalone, Signals y ReactiveForms. Se corrigió el submit de las búsquedas con controles independientes (consulta por ID y selector de profesores del ADMIN), cubierto por pruebas para evitar recargas del navegador.

## Pruebas y respaldo

Comandos desde `frontend/`:

```sh
npm run build
npm test
CHROMIUM_PATH=/ruta/a/chromium GYM_E2E_REAL_API=1 GYM_E2E_CREDENTIALS_FILE=/ruta/privada/cuentas.json npm run e2e
```

Usar exclusivamente un backend/base de pruebas. El JSON externo contiene cuentas ADMIN/PROFESOR existentes; las pruebas crean cuentas docentes propias para modificar credenciales sin alterar las del archivo. La preparación de series/entrenamientos usa cuentas ALUMNO y endpoints existentes solamente como fixtures; no implementa la interfaz de la etapa 4. Sin las variables de integración, las pruebas reales se omiten explícitamente.

Respaldos intermedios verificados en GitHub: `f75a933` (dashboard/gráficos/contratos), `551a534` (gestión completa), `fa3f01a` (recorridos reales y correcciones). Los commits posteriores completan la documentación y la regresión. No se versionan builds, node_modules, trazas, capturas ni credenciales.

## Resultado final verificado

El 7 de noviembre de 2026: compilación de producción aprobada; **66 pruebas unitarias/HTTP en 12 archivos y 27 escenarios Chromium aprobados, sin fallos ni omisiones**. Chromium incluye 9 recorridos reales del profesor, los 7 recorridos administrativos, 3 integraciones de autenticación real y 8 pruebas de interfaz/sesión aisladas.

Se verificaron multiday/versionado/asignación/archivo, catálogo y protección de referencias, alumnos vinculados y seguimiento, entrenamiento histórico de otros autores, pérdida de acceso al revocar el vínculo, desvinculación propia, perfil, cambios de email/contraseña y rechazo de los JWT anteriores. Se mantuvo la regresión de ADMIN; su prueba de auditoría ahora espera la navegación antes de tomar el ID de cuenta. También se revisaron capturas del dashboard real y el editor móvil, sin desborde horizontal.

No se modificaron archivos del backend. Solo se prepararon datos en la base local de pruebas mediante APIs existentes. La etapa 4 permanece sin implementar.
