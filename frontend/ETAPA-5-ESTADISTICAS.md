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

## Mejoras implementadas

- ADMIN reutiliza el componente existente de progreso, con consultas globales y por alumno. El dashboard enlaza a gráficos conservando el período; el perfil enlaza a estadísticas individuales. El adaptador usa exclusivamente endpoints ADMIN existentes.
- La evolución conserva los decimales exactos, nombre histórico, alumno, sesión y fecha efectiva. Los valores exactos enlazan al detalle autorizado de la sesión en cada rol. Las fechas corregidas muestran también la original.
- Cada punto de volumen identifica cobertura completa, parcial y número de series sin peso, o ausencia de pesos conocidos. Los gráficos no calculan métricas nuevas ni sustituyen datos faltantes. Las marcas de valor cero son visibles también en gráficos de barras.
- La paginación descarta respuestas y errores atrasados y conserva el período aplicado. Cambiar el ámbito borra resultados anteriores; un permiso revocado limpia las estadísticas protegidas. Se agregó reintento de consulta.
- Los filtros estadísticos de los tres dashboards y del progreso validan calendario, orden, límites soportados y ventana inclusiva de 1–366 días. Las fechas omitidas respetan el valor por defecto UTC del servidor.
- Se corrigió una falla preexistente en las páginas independientes de progreso: `withComponentInputBinding` asignaba `undefined` a entradas ausentes, ocultaba filtros y podía consultar un ID de alumno indefinido en PROFESOR. Las páginas de ruta ahora contienen al componente reutilizable sin exponer sus entradas internas al query string.
- En celulares, gráficos y tablas tienen desplazamiento propio, texto de ayuda y tamaño legible. Se corrigió el tamaño mínimo del componente dentro de la grilla para evitar desbordamiento de la página.

## Límites conservados

Los resúmenes abarcan todo el período; actividad, ejercicios y evolución están paginados. El trazado muestra únicamente la página consultada, con separación por registro, no por tiempo transcurrido. Es aproximado; los valores exactos del servidor permanecen en las tablas, incluso cuando exceden la precisión representable por el gráfico. Los datos ausentes no generan puntos cero ni líneas que los atraviesen.

Se consulta un período de hasta 366 días por vez. La frecuencia es observada y no equivale a asistencia. No hay unidades de peso estandarizadas ni reconstrucción de datos históricos ausentes. El backend conserva la autoridad sobre exclusiones, anulaciones, correcciones y permisos. Las consultas separadas no constituyen una instantánea transaccional si hay entrenamientos modificándose simultáneamente.

## Validación final ejecutada

- `npm run build`: compilación de producción aprobada.
- `npm test`: **96 pruebas aprobadas, 17 archivos**, sin fallos. Incluye contratos HTTP, ventanas inclusivas/bisiestos/UTC, cobertura, precisión y huecos gráficos, respuestas atrasadas, revocación y enlace de entradas del router.
- `CHROMIUM_PATH=/usr/bin/chromium GYM_E2E_REAL_API=1 GYM_E2E_CREDENTIALS_FILE=<archivo privado externo> npm run e2e`: **44 pruebas aprobadas, sin omisiones**. 36 contra la API real y 8 de interfaz aislada; incluye regresión completa ADMIN, PROFESOR y ALUMNO.
- Ocho recorridos nuevos contrastan los tres dashboards con resúmenes del mismo período y las cinco vistas estadísticas (ADMIN global/individual, PROFESOR global/individual y ALUMNO). Comparan decimales exactos, filtros por ejercicio/fecha, cambio de fecha efectiva, volumen parcial, peso cero/desconocido, serie anulada, sesión abierta y cierre parcial, períodos vacíos, ventanas inválidas y navegación al detalle.
- Conjunto aislado real: tres entrenamientos válidos, seis series válidas, una serie anulada excluida y una sesión abierta. Resultado esperado y verificado: 44 repeticiones, volumen conocido 160 con dos series sin peso, máximo 12,5 y frecuencia de 10,5 entrenamientos por semana en dos días inclusivos. Al consultar solo la fecha final, la evolución excluye la sesión cuya fecha efectiva se corrigió al día anterior.
- Captura móvil revisada; comprobaciones Chromium de ausencia de desbordamiento general y desplazamiento de gráficos.
- Backend idéntico al commit base. Dependencias, compilaciones, informes y credenciales de pruebas excluidos de Git. No se implementó la etapa 6.
