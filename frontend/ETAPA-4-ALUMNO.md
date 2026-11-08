# Etapa 4 — ALUMNO

Implementada sobre `fd1eb6447f66914e57d17f4b51542a89d592056d`, conservando los paneles ADMIN y PROFESOR. No se modificó el backend.

## Funcionalidad

- Dashboard personal con estadísticas y gráficos de los endpoints `/api/alumnos/me`.
- Rutina activa, días, prescripciones, historial de asignaciones y versiones exactas.
- Inicio de sesiones, registro móvil de series, repeticiones y pesos, recuperación de sesiones abiertas y cierre completo o parcial.
- Historial con filtros, correcciones, anulación, restauración y auditoría según los permisos existentes.
- Evolución de cargas, repeticiones, volumen y frecuencia con datos reales.
- Perfil, cambio de email y contraseña con renovación del JWT y gestión de vinculaciones con profesores.

## Contratos y recuperación

Los contratos proceden de `simulacro_nombreAlumno/docs/openapi.json`, el catálogo de endpoints y la documentación/implementación del backend. El cliente estadístico utiliza exclusivamente rutas propias del alumno autenticado. El detalle de sesión verifica además el propietario antes de cargar su prescripción y auditoría.

Al recargar se recuperan la sesión y sus series confirmadas desde el servidor, junto con la versión exacta de rutina registrada en la sesión. Una asignación posterior no reemplaza la prescripción histórica. Los campos sin enviar no se guardan como borradores. No se reintenta automáticamente una creación o escritura cuyo resultado sea incierto.

Los pesos sugeridos no se convierten en valores realizados: el registro empieza vacío. Peso desconocido (`null`) y peso cero se conservan distintos. La numeración de series respeta el ejercicio compartido entre bloques. El backend decide si el entrenamiento cumple el día; el cierre parcial requiere confirmación y no se permite cerrar sin registros válidos.

Las correcciones usan la revisión recibida del servidor y exigen motivo. Ante conflicto 409 se solicita recargar, sin sobrescribir silenciosamente. Una sesión cerrada no admite nuevas series; no se permite anular su última serie válida. Las correcciones de fecha conservan la fecha original y muestran la efectiva.

El directorio de profesores no informa estado de cuenta ni rol actual: el backend valida la vinculación elegida. Reemplazar un profesor utiliza el ID del perfil propio obtenido de la API. El backend no permite borrar una fecha de nacimiento enviando vacío; el formulario lo indica y conserva la existente.

## Validación ejecutada

- `npm run build`: compilación de producción aprobada.
- `npm test`: 82 pruebas aprobadas en 15 archivos, incluidos contratos HTTP y componentes.
- Chromium: 36 pruebas aprobadas, sin omisiones, con `GYM_E2E_REAL_API=1`. Incluye regresión ADMIN y PROFESOR y nueve recorridos ALUMNO contra el backend real.
- ALUMNO: recuperación de una versión anterior después de nueva asignación, peso cero/desconocido, cierre completo/parcial, validaciones móviles, corrección/anulación/restauración y auditoría, conflictos de revisión, aislamiento de datos, gráficos, perfil, rotación de credenciales y gestión de profesores.

Las cuentas y datos de prueba se crean en una base local de pruebas. Credenciales, resultados, dependencias y compilaciones quedan fuera del repositorio. La etapa 5 no está incluida.
