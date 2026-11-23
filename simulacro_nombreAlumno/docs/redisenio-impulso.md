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

### Consulta y registro compactos de entrenamientos

En una sesión abierta, los resultados se cargan y editan en la misma grilla de ejercicios. Guardar un cambio conserva el identificador de la serie y utiliza su revisión; no crea una segunda serie. Recargar recupera los valores del servidor y guardar una fila conserva los borradores de otras. Se retiran el listado duplicado de datos guardados y el formulario avanzado adicional. Las series anuladas existentes pueden restaurarse desde su propia fila.

Los entrenamientos finalizados muestran filas compactas con ejercicio, número de serie, repeticiones o segundos y peso. El día y la fecha se muestran una sola vez en el encabezado. Las opciones de corregir, anular o restaurar permanecen desplegables por fila; el formulario de corrección aparece dentro de esa fila. El comentario y la auditoría se consultan a demanda. Los controles y columnas se adaptan al celular sin desplazamiento horizontal.

Esta mejora de presentación no modifica el backend ni requiere otra migración adicional a 008.

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

## Verificación final

Verificado el 23/11/2026:

- Compilación Angular de producción y empaquetado JAR aprobados.
- **139 pruebas unitarias/HTTP de Angular**, en 27 archivos.
- **219 pruebas del backend**, incluidas MySQL y repetición de la migración 008 sobre datos existentes; sin fallos, errores ni omisiones.
- **59 recorridos Chromium aprobados** con Angular en configuración de producción: 51 contra el backend real y 8 de interfaz aislada. Incluyen regresión ADMIN/PROFESOR/ALUMNO, escritorio/tablet/celular, JWT, permisos y respuestas 400/401/403/404/409. El recorrido nuevo comprueba datos por tiempo, personalización independiente de alumnos, persistencia, catálogo sin versiones duplicadas y pantallas móviles.

Durante la repetición se interrumpió el proceso local del backend y se descartaron esas corridas sin conexión; la ejecución completa posterior, con el servicio activo en una sesión persistente, aprobó los 59 casos sin omisiones.

El recorrido `frontend/e2e/ux-impulso.spec.ts` también comprueba que seleccionar un día del calendario abre el historial con ambas fechas aplicadas, y que guardar resultados de un ejercicio no borra el borrador de otro. Las pruebas de cálculos verifican semanas de lunes a domingo, continuidad semanal, sesiones por tiempo, series anuladas, fechas desconocidas o futuras y valores personalizados.

Comandos de verificación (con requisitos y credenciales configurados según los README):

```bash
# Backend: configurar MYSQL_TEST_URL, MYSQL_TEST_USERNAME y MYSQL_TEST_PASSWORD
cd simulacro_nombreAlumno
./mvnw test
./mvnw -DskipTests package

# Frontend; requiere API real iniciada para el último comando
cd ../frontend
npm ci
npm run check
GYM_E2E_PRODUCTION=1 GYM_E2E_CREDENTIALS_FILE=/ruta/privada/accounts.json CHROMIUM_PATH=/usr/bin/chromium npm run e2e:real
```

Limitaciones: las vistas móviles se verifican con Chromium emulando tamaños, no en dispositivos físicos. La evolución de peso conserva la paginación y precisión de la API; no se inventan cargas cuando faltan, ni volumen para ejercicios por tiempo. Las sesiones capturan la prescripción y las sugerencias al comenzar: los cambios posteriores no alteran su historial. Esta entrega no publica un servidor ni realiza merge a `master`.
