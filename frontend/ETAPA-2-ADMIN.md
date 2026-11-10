# Etapa 2 — Panel ADMIN

Base: `codex/frontend-etapa-1`, commit `1c9905f92ba434851740c24a51b41d3533553479`.
Rama de trabajo: `codex/frontend-etapa-2-admin`.

## Funcionalidad

- Dashboard `/admin`: estado actual de cuentas, perfiles, vínculos, familias/versiones y asignaciones; entrenamientos del período filtrable por fechas. Los decimales estadísticos se conservan como strings. Sin volumen conocido se muestra «Sin dato»; el volumen parcial, las exclusiones, la frecuencia observada y las sesiones abiertas se distinguen de asistencia o cumplimiento.
- Usuarios: listado, búsquedas, filtros por rol/estado, orden y paginación del servidor; creación de ADMIN o de cuentas con su perfil correspondiente; detalle, estado, email y cambios de rol. La revisión de seguridad se toma del detalle, nunca se inventa. Los conflictos 409 requieren recarga explícita, sin reintentos automáticos de escrituras. Una modificación efectiva de seguridad propia cierra la sesión revocada.
- Alumnos y profesores: búsqueda, filtros por estado/vigencia/sin cuenta y campos admitidos, altas con perfil anidado, edición de perfil y creación de acceso para perfiles legados sin cuenta. El email de contacto se distingue del email de acceso; el cambio de identidad usa exclusivamente su endpoint específico.
- Vínculos: consulta paginada desde ambos perfiles, búsqueda de profesores activos/vigentes, selección por nombre/email/ID, vínculo, reemplazo individual y desvínculo con motivo. Reemplazar no termina otros vínculos ni borra datos históricos.
- Rutinas: listado de familias, filtro por autor/archivo, detalle y versiones paginadas. Seleccionar una versión muestra su respuesta histórica exacta, no consulta la última versión. La clave de cada fila es `rutinaVersionId` cuando se listan versiones.
- Asignaciones e historiales: referencia de versión exacta, fechas y estado, listado global e historial por alumno. Sesiones: filtros por alumno/autor/familia/estado/fechas, detalle de series, finalización/cumplimiento y auditoría del entrenamiento.
- Auditoría administrativa: paginación, filtro de cuenta afectada, actor, acción, motivo y estados anteriores/posteriores. La auditoría no dispone de GET por ID; el detalle usa el evento ya recibido en su página.

## Contratos, formularios y límites

Los contratos proceden de `../simulacro_nombreAlumno/docs/openapi.json`, `catalogo-endpoints.md`, `etapa-6-administracion.md` y `etapa-7-dashboards.md`, y se contrastaron con los DTOs Java existentes. Los tipos se generan únicamente dentro del frontend.

Formularios reactivos con campos dependientes del rol; validaciones de espacios, email, longitud, IDs enteros seguros, edad y fecha concordante; contraseñas de 8 caracteres (12 en ADMIN) y máximo 72 bytes UTF-8. Los campos opcionales de perfil se pueden limpiar con null. Las escrituras requieren confirmación; la confirmación muestra datos y motivo, excluyendo la contraseña. Hay estados de carga, vacío, error y éxito, botones de reintento y prevención de envíos duplicados.

No se agregan operaciones que el backend no ofrece: no hay borrado físico, reset administrativo de contraseñas ni edición de plantillas de profesor desde ADMIN. Los cambios de rol conservan perfiles/historial y terminan los vínculos indicados por las reglas del servidor. La protección del último ADMIN activo se respeta mediante el conflicto que devuelve el backend.

No se modificó el backend ni se implementaron etapas 3 o 4. La preparación de datos de las pruebas usa los endpoints existentes de profesor/alumno; no agrega esas funciones a sus interfaces.

## Verificación

Se ejecutan compilación de producción, pruebas unitarias/HTTP y Chromium, con el backend MySQL local. Consultar los comandos de `README.md`. Las pruebas reales requieren `GYM_E2E_REAL_API=1`, un archivo privado externo con las cuentas ADMIN/PROFESOR y una base aislada: crean y modifican datos de prueba. No usar producción.

Los commits se publicaron durante el desarrollo: `a502460` (dashboard/contratos), `56b321a` (gestión y consultas), `12e9eda` (flujos reales e historial). El commit final incorpora pruebas adicionales y esta documentación. No se incluyen secretos, node_modules, builds, trazas ni capturas.

### Resultado final verificado

El 7 de noviembre de 2026: compilación de producción aprobada; **47 pruebas unitarias/HTTP en 7 archivos y 18 pruebas de Chromium aprobadas**, sin fallos ni omisiones. La ejecución incluyó los tres logins reales, las siete pruebas administrativas contra API/MySQL, conflictos de identidad, revocación propia, versiones históricas y sesiones parciales. Se conservó la regresión de la etapa 1. También se revisaron capturas de dashboard, tabla de usuarios y formulario móvil. Las capturas y credenciales permanecen fuera del repositorio.
