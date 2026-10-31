# Etapa 7 — Dashboards, estadísticas y entrega del backend

El backend ofrece un login único y consultas separadas para ADMIN, PROFESOR y ALUMNO. Esta entrega no incluye frontend. Conserva los endpoints, roles, autorizaciones, versionado y correcciones auditadas de las etapas anteriores. No requiere una migración 006: funciona sobre el esquema resultante de **001–005**.

## Endpoints nuevos

Todas las operaciones siguientes son **GET**, requieren JWT y responden 200 con JSON. No modifican entrenamientos ni perfiles.

| Rol | Ruta | Respuesta / propósito |
| --- | --- | --- |
| ADMIN | `/api/admin/dashboard` | `DashboardAdmin`: estado actual del gimnasio y entrenamiento del período |
| ADMIN | `/api/admin/estadisticas` | `ResumenEstadistico`: entrenamiento de todo el gimnasio |
| ADMIN | `/api/admin/estadisticas/actividad` | Página de `ActividadDia` |
| ADMIN | `/api/admin/estadisticas/ejercicios` | Página de `EjercicioEstadistica` |
| ADMIN | `/api/admin/estadisticas/ejercicios/{ejercicioId}/evolucion` | Página de `EvolucionEjercicio` de todo el gimnasio |
| ADMIN | `/api/admin/alumnos/{alumnoId}/estadisticas` | Estadísticas del alumno indicado |
| ADMIN | `/api/admin/alumnos/{alumnoId}/estadisticas/actividad` | Actividad del alumno |
| ADMIN | `/api/admin/alumnos/{alumnoId}/estadisticas/ejercicios` | Cargas y volumen del alumno por ejercicio |
| ADMIN | `/api/admin/alumnos/{alumnoId}/estadisticas/ejercicios/{ejercicioId}/evolucion` | Evolución del ejercicio para ese alumno |
| PROFESOR | `/api/profesores/me/dashboard` | `DashboardProfesor`: vínculos actuales, plantillas propias y entrenamiento de sus alumnos |
| PROFESOR | `/api/profesores/me/estadisticas` | Resumen de todos sus alumnos vinculados |
| PROFESOR | `/api/profesores/me/estadisticas/alumnos` | Página de `AlumnoEstadistica`, incluyendo alumnos sin entrenamientos |
| PROFESOR | `/api/profesores/me/estadisticas/actividad` | Actividad de sus alumnos |
| PROFESOR | `/api/profesores/me/estadisticas/ejercicios` | Cargas y volumen de sus alumnos por ejercicio |
| PROFESOR | `/api/profesores/me/estadisticas/ejercicios/{ejercicioId}/evolucion` | Evolución por sesión de sus alumnos |
| PROFESOR | `/api/profesores/me/alumnos/{alumnoId}/estadisticas` | Resumen de un alumno actualmente vinculado |
| PROFESOR | `/api/profesores/me/alumnos/{alumnoId}/estadisticas/actividad` | Actividad del alumno vinculado |
| PROFESOR | `/api/profesores/me/alumnos/{alumnoId}/estadisticas/ejercicios` | Cargas y volumen del alumno vinculado |
| PROFESOR | `/api/profesores/me/alumnos/{alumnoId}/estadisticas/ejercicios/{ejercicioId}/evolucion` | Evolución histórica del alumno vinculado |
| ALUMNO | `/api/alumnos/me/dashboard` | `DashboardAlumno`: asignación activa y métricas propias |
| ALUMNO | `/api/alumnos/me/estadisticas` | Resumen personal |
| ALUMNO | `/api/alumnos/me/estadisticas/actividad` | Actividad personal |
| ALUMNO | `/api/alumnos/me/estadisticas/ejercicios` | Cargas y volumen propios por ejercicio |
| ALUMNO | `/api/alumnos/me/estadisticas/ejercicios/{ejercicioId}/evolucion` | Evolución personal por sesión y ejercicio |

ADMIN no necesita perfil de profesor o alumno. Los endpoints de PROFESOR y ALUMNO exigen sus respectivos roles, incluso si un ADMIN conserva alguno de esos perfiles tras un cambio de rol. El alumno se obtiene de la identidad autenticada en las rutas `/me`; enviar un `alumnoId` adicional en query no cambia el alcance.

Para el profesor se consulta el vínculo actual tanto al autorizar como al construir la consulta de datos. Al terminar el vínculo pierde acceso; el nuevo profesor puede ver **todo** el historial del alumno, sin filtrar por autor de rutina. Un alumno compartido entre profesores se cuenta una sola vez en las métricas globales.

## Parámetros y orden

| Parámetro | Regla |
| --- | --- |
| `desde` | Fecha efectiva inicial inclusiva, ISO `yyyy-MM-dd`. Omitida: 29 días antes de `hasta` |
| `hasta` | Fecha efectiva final inclusiva. Omitida: fecha actual UTC |
| `pagina` | Desde 0; máximo 1.000.000; por defecto 0; solo aplica a páginas |
| `tamanio` | 1–100; por defecto 20; solo aplica a páginas |
| `alumnoId` / `ejercicioId` en ruta | ID positivo; el alcance autorizado se verifica antes de consultar el recurso |

La ventana admite **1–366 días inclusivos**. Para consultar varios años se solicitan ventanas sucesivas; el historial no se recorta ni se elimina. Fechas fuera del rango MySQL soportado, formatos inválidos, rangos invertidos y páginas inválidas devuelven 400. Las fechas explícitas permiten consultar registros legados; no se generan entrenamientos para fechas sin datos.

Los resúmenes y dashboards aceptan el mismo filtro de fechas; los parámetros de paginación no recortan sus totales. No se admite orden arbitrario: actividad por fecha ascendente; ejercicios por ID ascendente; alumnos por ID ascendente; evolución por fecha efectiva e ID de sesión ascendentes. Una página fuera de rango devuelve una lista vacía con el total real.

Ejemplo:

```text
GET /api/alumnos/me/estadisticas/ejercicios/12/evolucion?desde=2026-10-01&hasta=2026-10-30&pagina=0&tamanio=20
Authorization: Bearer <JWT obtenido en el login>
```

## Criterios de cálculo

1. **Fecha efectiva:** `fechaCorregida` si existe; de lo contrario, la fecha original de la sesión. Se conserva y expone la fecha original en la evolución. No se usa la fecha de asignación de rutina como fecha de entrenamiento.
2. **Sesión finalizada:** el estado `completada` de la sesión. Puede ser parcial respecto del día prescrito. No significa haber cumplido una rutina de varios días, ni acredita asistencia al gimnasio.
3. **Entrenamiento con datos válidos:** sesión finalizada que contiene al menos una serie válida. Las sesiones abiertas, vacías o con todas sus series excluidas no aumentan este contador.
4. **Serie válida:** completada, no anulada, no exenta por anomalía legada, número de serie y repeticiones positivos, peso ausente o finito no negativo, identidad del ejercicio y día coherentes con la sesión. Las series sugeridas no son resultados realizados.
5. **Repeticiones:** suma y máximo por serie, solo de series válidas de sesiones finalizadas. Una serie sin peso puede aportar repeticiones reales.
6. **Peso máximo:** máximo peso conocido de esas series. `0` es un valor conocido; `null` significa que no se informó ningún peso. No se sustituye un peso ausente por el sugerido en la rutina.
7. **Volumen registrado:** suma de `pesoUsado × repsHechas` de series válidas con peso conocido. Si no hay ninguna, es `null`. Si faltan pesos, se devuelve la suma conocida y `volumenCompleto=false`, junto con `seriesSinPeso`. No se presenta como volumen total cuando es parcial.
8. **Frecuencia:** `entrenamientosConDatosValidos × 7 / días inclusivos del período`; para días entrenados se usa el número de fechas distintas con entrenamiento válido. Se redondea a 4 decimales, HALF_UP. Es una frecuencia observada normalizada, no asistencia, cumplimiento de agenda ni pronóstico. Dos sesiones válidas el mismo día cuentan como dos entrenamientos y un día entrenado.
9. **Evolución:** un punto por sesión y ejercicio con series válidas, con sus pesos/repeticiones/volumen y nombre histórico. Los máximos de peso y repeticiones pueden provenir de series diferentes: no se combinan para estimar 1RM. El listado de ejercicios identifica por ID estable y muestra su nombre actual por separado.
10. **Datos legados:** una sesión sin asignación demostrable puede aportar resultados si su fecha y series son válidas. No se inventa una asignación. Las series marcadas `legadoExento` no aportan a las métricas hasta que una corrección controlada las regularice; las anomalías originales permanecen trazadas.

Editar o archivar una rutina, cambiar de rutina/profesor o renombrar un ejercicio no reinterpreta los resultados con la plantilla actual. Las consultas usan las referencias históricas conservadas. Una corrección o anulación auditada sí actualiza las estadísticas según el resultado vigente.

El backend guarda pesos sin un catálogo de unidades ni equivalencias. El volumen se expresa en **unidad de peso registrada × repeticiones**: no se rotula automáticamente como kg, no convierte máquinas/cargas equivalentes y no suma peso corporal supuesto. No se calculan asistencia, duración, calorías, medidas corporales, IMC ni datos que el sistema no registra.

## DTOs

`Periodo`: `desde`, `hasta`, `dias` y `referenciaFecha="UTC"`. La referencia UTC determina la fecha por defecto; no desplaza las fechas de entrenamiento, que son `LocalDate` sin hora.

`Metricas` contiene:

| Campos | Interpretación |
| --- | --- |
| `sesionesRegistradas`, `sesionesAbiertas`, `sesionesFinalizadas` | Recuentos de sesiones del ámbito y período |
| `entrenamientosConDatosValidos`, `sesionesFinalizadasSinDatosValidos` | Distinguen registros utilizables de sesiones cerradas sin resultados fiables |
| `alumnosConEntrenamiento`, `diasConEntrenamiento`, `ultimaFechaConEntrenamiento` | Alumnos/fechas distintos con entrenamiento válido; última fecha nullable |
| `seriesValidas`, `seriesExcluidas`, `seriesEnSesionesAbiertas` | Las exclusiones corresponden a series de sesiones finalizadas; las abiertas se informan aparte |
| `repeticionesTotales`, `repeticionesMaximasPorSerie` | Repeticiones realizadas; máximo nullable sin datos |
| `seriesConPeso`, `seriesSinPeso` | Cobertura de cargas informadas entre las series válidas |
| `pesoMaximo`, `volumenRegistrado`, `volumenCompleto` | Cargas conocidas y cobertura del volumen |

**Pesos, volumen y frecuencias se serializan como strings decimales**, para no perder precisión al consumirlos con JavaScript. Pesos/volumen pueden ser null. Los contadores son enteros JSON. El cálculo usa `BigDecimal`, evitando el desbordamiento de multiplicar/sumar doubles para el volumen. Al convertir un decimal a `number` para un gráfico, el frontend debe considerar su precisión; los valores originales siguen disponibles como texto.

`ResumenEstadistico` contiene `{periodo, alumnoId, datos, frecuencia}`. `alumnoId` es null en resúmenes de varios alumnos. `Frecuencia` contiene `entrenamientosPorSemana` y `diasEntrenadosPorSemana`.

Los listados devuelven:

```json
{
  "periodo": {"desde":"2026-10-01","hasta":"2026-10-30","dias":30,"referenciaFecha":"UTC"},
  "contenido": [],
  "pagina": 0,
  "tamanio": 20,
  "totalElementos": 0,
  "totalPaginas": 0
}
```

Elementos:

- `ActividadDia`: `{fecha, datos}`. Incluye días con sesiones abiertas o cerradas sin datos válidos y mantiene esos contadores separados. No rellena días sin registros ni los interpreta como ausencias.
- `EjercicioEstadistica`: `{ejercicioId, nombreActual, datos}`. Solo ejercicios con series válidas del período. `datos` se limita a ese ejercicio; los contadores de sesiones/series se refieren a los resultados seleccionados.
- `EvolucionEjercicio`: `{ejercicioId, nombreHistorico, sesionId, alumnoId, fechaOriginal, fechaEfectiva, datos}`. Solo resultados válidos del ejercicio y sesión, no todos los ejercicios de esa sesión.
- `AlumnoEstadistica`: `{alumnoId, nombre, apellido, datos, frecuencia}`. Incluye alumnos vinculados sin resultados: contadores cero, peso/volumen null.

El dashboard de ADMIN reutiliza `estadoActual` de la etapa 6: cuentas activas/inactivas, roles, perfiles conservados/sin cuenta, vínculos, familias/versiones/archivadas de rutinas, asignaciones y sesiones. Son **recuentos actuales**, sin simular cómo era la matrícula en una fecha pasada. `entrenamientos` y `frecuencia` sí corresponden al período. El historial del gimnasio incluye perfiles conservados o inactivos cuando tienen registros reales.

El dashboard del profesor informa `alumnosVinculados`, `rutinasPropias`, `rutinasPropiasArchivadas`, `asignacionesActivasDeSusAlumnos`, `entrenamientos` y `frecuencia`. Las asignaciones incluyen cualquier autor; las plantillas propias se cuentan por familia, no por versión. Los recuentos de vínculos/plantillas son actuales.

El dashboard del alumno informa `alumnoId`, `rutinaActiva` (el DTO de asignación de las etapas anteriores, con versión exacta; null si no tiene), `entrenamientos` y `frecuencia`. La falta de asignación activa no impide consultar el dashboard ni el progreso anterior.

## OpenAPI, errores y conexión con Angular 22

Con el backend ejecutándose en el puerto 8080:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`.
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`.
- [Contrato exportado](openapi.json) y [catálogo de endpoints](catalogo-endpoints.md): generados desde el contrato de la aplicación, sin JWT, contraseñas reales ni datos de alumnos.

Se usa OpenAPI 3.0 para representar `nullable` y facilitar generación de clientes TypeScript. Incluye DTOs concretos, parámetros, restricciones, esquema HTTP Bearer y respuestas. Login/registro público no exigen autorización en la documentación. Los filtros de administración se documentan como parámetros individuales. Los códigos 201/204 coinciden con los endpoints de creación/archivo existentes; los errores usan `ErrorResponse` sin detalles internos del servidor.

401: JWT ausente/vencido/manipulado/revocado o usuario inactivo. 403: rol incorrecto o alumno ajeno/no vinculado. 404: recurso inexistente dentro del alcance autorizado. 400: solicitud inválida. Las operaciones de escritura conservan 409 para conflictos de revisión/estado/integridad. Estas consultas estadísticas son de solo lectura.

Angular debe usar el mismo `POST /api/auth/login` para los tres roles y elegir navegación/dashboard según el `rol` devuelto, manteniendo guards propios además de los permisos del servidor. `perfilId` corresponde únicamente al rol vigente. El interceptor de `HttpClient` envía `Authorization: Bearer <token>`; un 401 exige volver a autenticar. La API no emite refresh tokens. Los tokens anteriores se invalidan tras los cambios de seguridad definidos en etapas 5–6.

Configurar `GYM_CORS_ALLOWED_ORIGINS=http://localhost:4200` para desarrollo. No se usan comodines ni cookies de autenticación (`allowCredentials=false`). En despliegue, configurar el origen HTTPS exacto del frontend. Las pruebas verifican preflight, solicitudes reales con Origin/Authorization y el contrato servido por `/v3/api-docs`. Esta preparación de integración no equivale a probar un frontend Angular todavía inexistente.

## Ejecución local

Requisitos: **Java 21**, MySQL **8.4**, Maven Wrapper del repositorio y una base de desarrollo independiente.

1. Entrar en `simulacro_nombreAlumno` y crear/configurar la base MySQL y su usuario local. Mantener las credenciales fuera del repositorio.
2. Aplicar los scripts `src/main/resources/db/mysql/001_esquema_anterior.sql` a `005_administracion.sql` en orden **solo en una base nueva**. En una base existente, detener escritores, verificar backup y aplicar solo migraciones pendientes, siguiendo las guías de etapas 3–6. No volver a ejecutar 001 sobre datos existentes.
3. Copiar `.env.example` a `.env`, ignorado por Git. Completar `GYM_DB_URL`, `GYM_DB_USERNAME`, `GYM_DB_PASSWORD` y `GYM_JWT_SECRET_BASE64` (clave aleatoria Base64 de al menos 32 bytes; `openssl rand -base64 32`). No reutilizar secretos publicados anteriormente.
4. Configurar el origen Angular en `GYM_CORS_ALLOWED_ORIGINS`. TTL/emisor: `GYM_JWT_TTL=PT24H` y `GYM_JWT_ISSUER=gimnasio-api`, o valores propios admitidos por la [guía de seguridad](etapa-5-seguridad.md).
5. Si la base no tiene ADMIN, activar temporalmente `GYM_BOOTSTRAP_ADMIN_ENABLED=true` y configurar `GYM_BOOTSTRAP_ADMIN_EMAIL` / `GYM_BOOTSTRAP_ADMIN_PASSWORD` (mínimo 12 caracteres, máximo 72 bytes UTF-8). Retirar esos secretos y deshabilitar bootstrap después del alta inicial. No existen credenciales predeterminadas.
6. Exportar las variables desde el IDE/gestor local, o cargar un archivo propio y confiable:

```bash
set -a
. ./.env
set +a
bash mvnw spring-boot:run
```

Para generar y ejecutar el JAR en una terminal con esas mismas variables:

```bash
bash mvnw clean package
java -jar target/simulacro_nombreAlumno-0.0.1-SNAPSHOT.jar
```

Las pruebas ordinarias aíslan su base; para ejecutarlas también contra MySQL, usar exclusivamente un servidor de TEST y el comando de la [etapa 6](etapa-6-administracion.md#verificación), configurando `MYSQL_TEST_URL`, `MYSQL_TEST_USERNAME` y `MYSQL_TEST_PASSWORD`. Ese usuario necesita crear/eliminar bases temporales para verificar las migraciones. No apuntar la suite a una base real.

Spring Boot no carga `.env` automáticamente. `source` ejecuta shell: el archivo debe ser confiable y los valores con metacaracteres deben estar citados correctamente. Se mantiene `ddl-auto=validate`, sin modificación automática de esquema. El arranque verifica las migraciones y las identidades coherentes.

## Validación y límites

Resultado de `clean test` contra MySQL 8.4.13: **209 pruebas aprobadas, 0 fallos, 0 errores y 0 omitidas**. Incluye las 181 pruebas anteriores, 11 casos unitarios de estadísticas y 17 casos de integración/autorización/OpenAPI. Las nueve pruebas de migraciones se ejecutaron, sin omitirlas.

Además se empaquetó el JAR ejecutable y se lo arrancó, sin configuración de test, contra una base vacía preparada con los scripts 001–005 y `ddl-auto=validate`. La comprobación por HTTP real verificó bootstrap seguro, login de los tres roles, dashboard sin asignación, alta de perfiles/vínculo/rutina, entrenamiento parcial, volumen coherente en los tres dashboards, respuestas 401/403 y preflight CORS. La base y los secretos aleatorios de esa comprobación fueron temporales.

La suite incluye pruebas unitarias de fórmulas, cero/desconocido, decimales, pesos extremos, ventanas inclusivas y año bisiesto; integración MySQL con datos reales de prueba, correcciones y exclusiones; permisos HTTP y services para los tres roles; cambio de profesor y revocación; archivo/versionado sin pérdida de progreso; OpenAPI, Swagger y CORS. También conserva todas las pruebas de las etapas anteriores y las migraciones 001–005, incluyendo reejecución y recuperación de DDL interrumpido.

Las consultas paginan claves en SQL y recorren proyecciones en streaming para calcular el volumen exacto, sin cargar grafos JPA ni listas completas de series. Los resúmenes recorren los resultados del período: una instalación con gran volumen deberá medir tiempos y evaluar índices/materializaciones con datos representativos antes de prometer capacidad de producción. No se ha ejecutado una prueba de carga ni se ha desarrollado el frontend.

Siguen vigentes las limitaciones de datos legados irrecuperables o ambiguos documentadas en etapa 3. Las estadísticas no reconstruyen resultados perdidos antes del versionado. No hay duración, asistencia, composición corporal, equivalencia de unidades ni recuperación externa de contraseñas. Tampoco se confunden asignaciones de rutinas con entrenamientos realizados.

No se aplicaron cambios a una base real ni se publicaron cambios en GitHub durante esta entrega.
