# IMPULSO: entrenamiento, rutinas y experiencia de usuario

## Cambios funcionales

- Días con nombre opcional: el profesor puede escribir uno propio o dejar `Día N` según su posición. No se solicita orden manual.
- Bloques uniformes con vueltas; cada prescripción admite `REPETICIONES` o `SEGUNDOS`. Los valores generales son opcionales. Los segundos tienen campos propios; nunca se convierten en repeticiones.
- La plantilla no recibe pesos. La asignación admite por posición peso opcional y repeticiones **o** segundos personalizados, de acuerdo con su unidad. Vacío usa la referencia general, si existe. La edición usa la revisión de la asignación.
- Las sesiones capturan los valores personalizados al comenzar. Cambiar una plantilla o las sugerencias no altera sesiones anteriores ni las asignaciones de otros alumnos. Las rutinas siguen versionándose internamente; el catálogo muestra una familia una sola vez y conserva consulta avanzada del historial.
- ALUMNO ve valores efectivos sin sugerencias vacías. Registra resultados reales por vuelta con la unidad correcta y conserva borradores no enviados al guardar otras filas. Recargar recupera los resultados confirmados por el servidor.
- Mi progreso utiliza entrenamientos finalizados con resultados guardados y fecha conocida: sesiones de esta semana, días marcados de lunes a domingo, semanas consecutivas y sesiones del mes. No cuenta sesiones abiertas, series anuladas o actividad futura. La semana actual puede no tener actividad todavía sin cortar la continuidad de las semanas previas.
- Solo dos gráficos: entrenamientos por semana de las últimas ocho semanas y peso utilizado por ejercicio, con páginas y valores exactos consultables. Los segundos no suman repeticiones ni volumen de peso por repeticiones.
- El inicio de PROFESOR muestra accesos con Angular Router y una vista de hasta ocho alumnos filtrados por nombre. Consulta la rutina y el último entrenamiento real; no infiere actividad cuando no hay registros. Los gráficos permanecen en Progreso.
- Se elimina el contorno de foco del contenedor principal, conservando el foco de los controles. Header más legible, formularios compactos, fechas visibles `dd/mm/aaaa` y vocabulario cotidiano. Los selectores nativos de fecha respetan la configuración regional del navegador.

## Contratos y compatibilidad

La migración **008** agrega columnas y cuatro tablas de sugerencias individuales, sin eliminar registros. Las prescripciones y resultados anteriores siguen siendo `REPETICIONES`; no se inventan segundos históricos. La migración permite su repetición, verifica que exista 007 y actualiza la restricción de resultados manteniendo la excepción explícita de legado.

- `DiaEjercicioRequest/Response`: `tipoMedicion`, `segundosSugeridos`, y `repsSugeridas` opcional. Respuestas de asignación incluyen `repsPersonalizadas` y `segundosPersonalizados`.
- `PesosAsignacionRequest.Peso`: conserva `diaEjercicioId` y `pesoSugerido`; incorpora `repsSugeridas` y `segundosSugeridos` opcionales. Se conserva el endpoint de asignar y `/asignaciones/{id}/pesos`. La colección reemplaza las sugerencias de esa asignación y conserva el control de revisión y autoría.
- `SerieRequest/CorreccionSerieRequest`: exactamente uno entre `repsHechas` y `segundosHechos`, positivo y acorde a la prescripción. El servidor fija la unidad desde la rutina, no desde el cliente. Respuestas y auditorías incluyen la unidad y segundos.
- Las sesiones capturan las personalizaciones en mapas propios. Los registros de actividad y frecuencia incluyen sesiones con resultados por tiempo; repeticiones y volumen solo suman los ejercicios por repeticiones. No se cambia JWT, roles o permisos de vinculación.

## Actualizar una instalación Docker existente (PowerShell)

Esta entrega **requiere aplicar 008 antes de iniciar el backend nuevo**. No regenerar `.env`, no aplicar 001 sobre una base existente y no eliminar el volumen.

Con Docker Desktop y el servicio MySQL de esta instalación funcionando:

```powershell
cd C:\Users\spina\gimnasio-docker
git switch codex/mejoras-rutinas-selectores
git pull --ff-only
powershell -NoProfile -ExecutionPolicy Bypass -File docker/update-008.ps1
```

El script detiene frontend/backend, genera un respaldo con `mysqldump`, lo copia a `docker/backups`, aplica 008 dentro del contenedor y reconstruye/inicia el sistema. Se detiene si falla un comando; conserva el volumen y los secretos. `Bypass` se limita a ese proceso. El respaldo contiene datos privados: no publicarlo. Los comandos SQL equivalentes se validan contra MySQL real; el script PowerShell debe ejecutarse en Windows por el operador de la instalación.

En una instalación nueva, la imagen MySQL incorpora automáticamente 001–008. Después de actualizar, abrir http://localhost:8088 y recargar el navegador.

## Verificación en curso

La compilación de producción pasó, al igual que 139 pruebas unitarias/HTTP de Angular y 219 del backend, incluidas MySQL sin omisiones. El recorrido nuevo de Chromium ya comprueba datos por tiempo, personalización independiente de alumnos, persistencia, catálogo sin versiones duplicadas y pantallas móviles. Los resultados completos se actualizan al cerrar la regresión.
