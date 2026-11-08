# Etapa 5 — auditoría de estadísticas

Base aprobada: `3dac5be5a3e13536f86b3c36a2125ce49141f301`. Rama: `codex/frontend-etapa-5-estadisticas`.

## Auditoría inicial

Fuentes contrastadas: OpenAPI, catálogo de endpoints y `etapa-7-dashboards.md` del backend; dashboards, servicios, gráficos e historiales de los tres roles.

- Los tres dashboards consultan endpoints reales y distinguen recuentos actuales del período de entrenamiento. No hay métricas simuladas en producción.
- PROFESOR y ALUMNO comparten el componente de progreso mediante un adaptador; las rutas personales del alumno mantienen el alcance `/me`. Se conserva esta implementación.
- ADMIN dispone de dashboard y consultas históricas, pero no expone los endpoints estadísticos de actividad y evolución ya disponibles. Se reutilizará el mismo componente para cubrir esta diferencia.
- Los gráficos conservan decimales exactos en tablas y separan `null` de cero. Falta mostrar cobertura del volumen en cada punto y acceso directo a la sesión histórica.
- Las páginas mantienen el período aplicado, pero peticiones superpuestas de paginación pueden dejar errores/estados de carga obsoletos. Se corregirá sin cambiar métricas del servidor.
- La validación de fechas comprueba solamente el orden; falta comprobar calendario y ventana de 1–366 días documentada.
- En celular el SVG se reduce con tipografía pequeña. Se mejorará su legibilidad dentro de un área desplazable sin desbordar la página.

No se modificarán el backend ni sus fórmulas. Las sesiones parciales finalizadas con datos válidos cuentan como entrenamientos; las abiertas se informan aparte. Series anuladas/exentas se excluyen en el servidor. Los pesos desconocidos no se convierten en cero ni se sustituyen por sugerencias. No se reconstruyen datos históricos irrecuperables.
