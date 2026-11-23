# Rutinas simples, alumnos disponibles y resultados por vuelta

Esta guía describe la entrega anterior. La versión actual incorpora medición por tiempo y requiere [migración 008 y pasos de actualización](redisenio-impulso.md).

## Uso

- El profesor crea días con nombre; su orden se calcula por posición. Cada día ofrece `+ Agregar día` justo debajo y el nuevo se inserta a continuación.
- Todos los bloques usan el mismo editor: nombre opcional, vueltas y descanso opcional entre vueltas. Se agregan vacíos; cada ejercicio tiene repeticiones y descanso opcional. No se solicitan tipos ni pesos de plantilla.
- El asa `⠿` permite arrastrar bloques dentro del día y ejercicios dentro de un bloque o entre bloques del mismo día. También admite teclado: arriba/abajo cambia el orden; izquierda/derecha cambia de bloque un ejercicio. El destino determina las vueltas y el descanso entre vueltas; se conservan repeticiones y descanso individual.
- Papelera elimina ejercicios, bloques o días. Se pide confirmación cuando hay contenido cargado. Agregar y guardar no abren confirmaciones. Un bloque vacío debe completarse o eliminarse antes de guardar. El guardado conserva el contrato de versionado; guardar una definición idéntica no crea versiones innecesarias.
- La edición de pesos individuales también guarda directamente, con mensaje inline. Los pesos sugeridos continúan siendo individuales por asignación y se capturan en cada sesión. No se agregan pesos a las plantillas.
- ADMIN ve `Sin asignar` / `Asignado` independientemente de si la cuenta está activa. El filtro `Sin profesor` opera en el servidor y conserva la paginación. La creación administrativa vuelve al listado existente.
- En `Mis alumnos`, el profesor abre `Buscar alumnos disponibles`, busca nombre/email y vincula a su propio perfil un alumno activo sin profesor. La lista y el estado se actualizan después de confirmar la escritura en el servidor.
- La sesión del ALUMNO muestra todas las vueltas de cada ejercicio con repeticiones reales y peso real independientes. Cada fila guarda sin modal. La actualización posterior conserva los valores todavía no enviados de las demás filas; los campos se bloquean mientras una escritura está pendiente. Los campos nuevos no se rellenan con resultados sugeridos; vacío y cero conservan significados diferentes. Las filas guardadas pueden corregirse con auditoría y revisión optimista. Las series anuladas se restauran desde la sección de historial, sin sobrescribirlas.
- El registro avanzado anterior queda disponible en una sección plegable para números de serie adicionales y consulta de la última ejecución. La finalización mantiene confirmación y las validaciones del backend.

## Seguridad y contratos

No se modifica el esquema ni se requiere una migración nueva. Se conservan los datos y versiones existentes.

`GET /api/profesores/me/alumnos-disponibles?q=&pagina=0&tamanio=20` requiere PROFESOR. Devuelve una página de `{id,nombre,apellido,email}` de alumnos con cuenta ALUMNO activa y sin profesores. No expone edad, objetivos, rutinas ni entrenamientos. El filtro escapa comodines SQL y limita la búsqueda y el tamaño de página.

`POST /api/profesores/me/alumnos-disponibles/{alumnoId}/vincular` requiere PROFESOR y devuelve 204. Obtiene el profesor desde el JWT; el cliente no puede elegir otro profesor. Bloquea el alumno y vuelve a comprobar su disponibilidad en la transacción. Rechaza con 409 alumnos inactivos o ya vinculados. El resto de las operaciones mantienen las comprobaciones de vinculación actuales.

`GET /api/admin/alumnos` incorpora `sinProfesor=true|false`. No confundirlo con `sinCuenta`, `activo` o `vigente`.

El entrenamiento reutiliza los endpoints existentes de series. Cuando un ejercicio aparece varias veces en el día, la numeración enviada es global para ese ejercicio dentro de la sesión; visualmente cada prescripción muestra sus propias vueltas. Solo se muestran en una fila los registros de su prescripción exacta (`diaEjercicioId`), ejercicio y número. Las series adicionales o históricas fuera de las vueltas planificadas permanecen en el historial y el registro avanzado; no se reasignan a otra aparición del mismo ejercicio ni se infieren registros ausentes. Una respuesta 409 o una interrupción no provoca reintentos automáticos de escritura.

## Actualizar Docker en PowerShell

Desde la raíz del repositorio, conservando `.env` y el volumen:

```powershell
git switch codex/mejoras-rutinas-selectores
git pull --ff-only
docker compose up -d --build --wait --wait-timeout 300
```

Abrir http://localhost:8088 y recargar el navegador. No volver a inicializar MySQL, no regenerar secretos ni eliminar el archivo de cuentas demo.

## Validación

Compilación de producción aprobada; 131 pruebas Angular unitarias/HTTP y 218 pruebas backend, incluidas MySQL, sin fallos ni omisiones. El recorrido nuevo de Chromium contra MySQL pasó con los tres roles, arrastre real, tres días, creación/edición versionada, pesos por asignación y recuperación de resultados distintos después de recargar. La ejecución completa de Chromium con Angular en modo producción aprobó **58 recorridos sin omisiones** (50 contra API real y ocho de interfaz aislada), incluida la regresión de ADMIN, PROFESOR y ALUMNO, comentarios, pesos por asignación, permisos, estadísticas y pantallas de escritorio/tablet/celular. La suite añade cobertura de inserción/orden automático de días, bloques vacíos, arrastre entre bloques, resultados individuales, persistencia, peso cero/desconocido y búsquedas/vinculación seguras.


Comandos de referencia (con las variables privadas de pruebas ya configuradas):

```sh
# Desde simulacro_nombreAlumno/, con MYSQL_TEST_URL/USERNAME/PASSWORD de un servidor exclusivo de pruebas:
bash mvnw test
# Desde frontend/:
npm run build
npm test
GYM_E2E_PRODUCTION=1 npm run e2e:real
```

`GYM_E2E_CREDENTIALS_FILE` debe apuntar al JSON privado de cuentas ADMIN y PROFESOR; `CHROMIUM_PATH` permite usar Chromium del sistema. Los recorridos crean registros y se ejecutaron en una base de pruebas. Las capturas y los archivos generados quedaron fuera de Git. No se modificó el esquema ni se requirió SQL adicional. La prueba móvil utiliza Chromium con tamaños de pantalla emulados; no reemplaza una validación en dispositivos físicos. Los borradores no enviados se conservan al guardar otras series dentro de la pantalla, pero solo los resultados confirmados por la API se recuperan después de recargar el navegador.
