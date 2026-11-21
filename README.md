# Impulso Gym — gestión de gimnasio

Aplicación web completa con **Angular 22**, **Spring Boot 3.4.5 / Java 21** y **MySQL 8.4**. Incluye login único con JWT y paneles responsive para **ADMIN, PROFESOR y ALUMNO**, conectados al backend real. La interfaz utiliza negro, gris carbón y amarillo fluorescente.

## Funcionalidades

| Rol | Funciones principales |
| --- | --- |
| ADMIN | Dashboard, gestión de cuentas y perfiles, activación/desactivación, roles, vinculaciones profesor-alumno, consulta de rutinas, sesiones, estadísticas y auditoría. |
| PROFESOR | Alumnos vinculados, seguimiento y objetivos, rutinas propias, creación de versiones, duplicación, archivo y asignación, catálogo de ejercicios y grupos musculares, historiales y gráficos de progreso. |
| ALUMNO | Dashboard personal, rutina activa y asignaciones anteriores, registro y recuperación de sesiones abiertas, cierre completo o parcial, correcciones y anulaciones, evolución, perfil y gestión de profesores. |

Los tres espacios incluyen validaciones, confirmaciones, estados de carga/vacío/error y controles de acceso. Los selectores permiten buscar alumnos, profesores y ejercicios por nombre; los identificadores se utilizan internamente.

### Mejoras funcionales actuales

Nueva revisión: [bloques completos, buscadores y creación inline de ejercicios](frontend/BLOQUES-Y-BUSCADORES.md). Los circuitos tienen un solo número de vueltas para el bloque completo y conservan su estructura al guardar y volver a abrir.

- **Bloques de entrenamiento persistentes:** series, biseries y circuitos; nombre opcional, vueltas, orden, descansos entre ejercicios/vueltas y repeticiones por ejercicio. Se pueden agregar, quitar y reordenar ejercicios y bloques, con vista previa.
- **Pesos por alumno:** la plantilla no incluye pesos; cada asignación configura valores opcionales por ejercicio, independientes de otros alumnos. Las sesiones conservan una copia al iniciarse. Ver [pesos por asignación y migración 007](frontend/PESOS-POR-ASIGNACION.md).
- **Buscadores y alta inline:** ejercicios y grupos musculares con búsqueda, teclado y creación desde el editor de rutinas, sin salir de la pantalla. La descripción del ejercicio es opcional.
- **Catálogo personalizado:** creación y administración de grupos musculares y ejercicios. Se evitan nuevos duplicados por normalización del nombre y se conservan las referencias existentes.
- **Objetivo del alumno:** descripción libre editable de hasta 2000 caracteres, con categoría anterior opcional. Debe informarse una descripción o una categoría.
- **Comentarios de entrenamiento:** texto opcional de hasta 2000 caracteres al finalizar una sesión, consultable por el alumno y sus profesores actualmente vinculados mediante el historial/detalle autorizado.

Las ediciones de rutinas conservan las versiones utilizadas por asignaciones y sesiones anteriores. **Peso cero y peso no registrado son valores distintos.** Las estadísticas provienen de los registros del backend; no se completan resultados faltantes con datos simulados.

Los seis grupos musculares estándar se conservan por compatibilidad. Los personalizados pueden renombrarse manteniendo referencias y eliminarse únicamente cuando no están en uso. Los comentarios se guardan al cerrar la sesión; no existe edición posterior del comentario.

### Editor de rutinas y entrenamiento simplificados

Días con orden automático, bloques uniformes con vueltas y ejercicios reorganizables mediante arrastre o teclado. El alumno registra repeticiones y pesos reales por vuelta en una tabla compacta, sin confirmaciones cotidianas. ADMIN puede filtrar alumnos `Sin asignar` y PROFESOR puede encontrar y vincular alumnos disponibles con acceso limitado antes de vincularse.

Ver la [guía de uso, permisos y actualización](simulacro_nombreAlumno/docs/rutinas-simples.md). Esta mejora no requiere una migración nueva: conserva versiones, asignaciones, pesos individuales e historiales.

## Ejecutar todo con Docker

Es la opción para utilizar el sistema **sin abrir el IDE ni iniciar MySQL manualmente**. Requiere Docker Engine/Desktop iniciado y Compose v2; Java, Node y MySQL se ejecutan dentro de contenedores.

Abrir una terminal **en la raíz del repositorio, donde está `compose.yaml`**. Los siguientes pasos de inicialización son para una instalación nueva.

1. Crear la configuración privada. Si tenés Python 3:

   ```sh
   python3 docker/init-env.py
   ```

   En Windows se puede usar `py -3 docker/init-env.py` si el lanzador está instalado. Sin Python, copiar `.env.example` a `.env` y completar sus valores; en PowerShell:

   ```powershell
   Copy-Item .env.example .env
   notepad .env
   ```

   Configurar contraseñas aleatorias distintas para MySQL y el primer ADMIN, un email para ese ADMIN y una clave JWT Base64 de al menos 32 bytes aleatorios. El bootstrap exige una contraseña de al menos 12 caracteres y hasta 72 bytes UTF-8. No publicar `.env`. El generador crea valores aleatorios y no sobrescribe un archivo existente.

2. Construir e iniciar:

   ```sh
   docker compose up -d --build --wait --wait-timeout 300
   docker compose ps
   ```

3. Abrir **http://localhost:8088** e ingresar con las credenciales iniciales de `.env`.

Después del primer acceso, establecer `GYM_BOOTSTRAP_ADMIN_ENABLED=false`, vaciar `GYM_BOOTSTRAP_ADMIN_EMAIL` y `GYM_BOOTSTRAP_ADMIN_PASSWORD`, y ejecutar `docker compose up -d backend`. Las cuentas ya creadas se conservan en MySQL.

```sh
# Detener conservando los datos:
docker compose stop
# Volver a iniciar los contenedores existentes:
docker compose start
# Consultar errores de arranque:
docker compose logs --tail=100 backend
```

El volumen de MySQL conserva la información al detener o recrear contenedores. **`docker compose down -v` elimina los datos del volumen.** No regenerar las claves de MySQL/JWT de una instalación existente sin un procedimiento de cambio.

### Datos de prueba opcionales

Se pueden cargar **20 ejercicios, 5 alumnos y 1 profesor vinculado** mediante la API real, sin reemplazar datos existentes. Con los servicios saludables, crear la carpeta `docker/demo-data` y ejecutar `docker compose --profile demo run --rm --no-deps demo`. Solicita una cuenta ADMIN y guarda los accesos demo generados en un archivo privado. Ver las [instrucciones para PowerShell y persistencia](docker/DEMO.md).

### Actualizar una instalación existente: migraciones 006 y 007

Esta versión requiere `006_mejoras_funcionales.sql` y `007_pesos_por_asignacion.sql`, posteriores a 005. Las migraciones son aditivas y conservan rutinas, versiones, asignaciones y entrenamientos existentes. Si ya tenés 006, aplicar solo 007.

**Reconstruir las imágenes no aplica SQL sobre un volumen ya inicializado.** Antes de iniciar la nueva versión:

1. Actualizar el código de la rama `codex/mejoras-rutinas-selectores`.
2. Detener frontend y backend para evitar escrituras.
3. Hacer un backup y verificar su restauración en una base separada.
4. Aplicar únicamente las migraciones pendientes, incluida 007.
5. Reconstruir e iniciar el sistema.

Seguir los [comandos completos de actualización para Docker y PowerShell](docker/README.md#actualizar-desde-006-pesos-por-asignación). **No ejecutar 001 sobre una base existente ni eliminar el volumen para actualizar.**

Ver también la [guía de Docker](docker/README.md) y los [resultados de validación](docker/VALIDACION.md).

## Desarrollo local sin Docker

Requisitos: Java 21, MySQL 8.4, Node **24.15+ dentro de 24.x**, npm y Maven Wrapper incluido. No es necesario instalar Maven globalmente.

### Backend

1. Crear una base de desarrollo y un usuario MySQL con acceso a ella.
2. En una base vacía, aplicar en orden los scripts **001 a 007** de [`simulacro_nombreAlumno/src/main/resources/db/mysql/`](simulacro_nombreAlumno/src/main/resources/db/mysql/). En una base existente, aplicar solo los pendientes con backup y escritores detenidos.
3. Copiar `simulacro_nombreAlumno/.env.example` a `simulacro_nombreAlumno/.env` y completar:

   | Variable | Uso |
   | --- | --- |
   | `GYM_DB_URL` | JDBC de la base local. |
   | `GYM_DB_USERNAME`, `GYM_DB_PASSWORD` | Credenciales privadas de MySQL. |
   | `GYM_JWT_SECRET_BASE64` | Clave privada Base64 de al menos 32 bytes aleatorios. |
   | `GYM_CORS_ALLOWED_ORIGINS` | Orígenes exactos permitidos para el navegador. |
   | `GYM_BOOTSTRAP_ADMIN_*` | Creación inicial del primer ADMIN; deshabilitar después. |

4. Cargar las variables en el entorno y arrancar. Spring Boot **no carga `.env` automáticamente**. En bash, usando un archivo propio y confiable:

   ```sh
   cd simulacro_nombreAlumno
   set -a
   . ./.env
   set +a
   bash mvnw spring-boot:run
   ```

En PowerShell, definir las variables de entorno antes de ejecutar `./mvnw.cmd spring-boot:run`. La guía de [ejecución local](frontend/README.md#1-preparar-el-backend-local) detalla la configuración. El backend valida el esquema al iniciar (`ddl-auto: validate`).

### Frontend

En otra terminal, desde la raíz:

```sh
cd frontend
npm ci
npm start
```

Abrir **http://localhost:4200**. Angular usa `/api` del mismo origen; [`frontend/proxy.conf.json`](frontend/proxy.conf.json) redirige las consultas al backend local en `http://127.0.0.1:8080`. Para otro destino, cambiar el `target` y reiniciar Angular. No colocar contraseñas ni claves JWT en el frontend.

## Compilación y pruebas

Backend, desde `simulacro_nombreAlumno/`:

```sh
bash mvnw test
bash mvnw package
```

En Windows, usar `./mvnw.cmd` en lugar de `bash mvnw`. El JAR se genera en `target/simulacro_nombreAlumno-0.0.1-SNAPSHOT.jar` y puede ejecutarse con `java -jar`, con las variables de entorno configuradas.

Las pruebas de migración MySQL requieren `MYSQL_TEST_URL`, `MYSQL_TEST_USERNAME` y `MYSQL_TEST_PASSWORD` hacia un servidor **exclusivo de pruebas**, con permisos para crear/eliminar las bases aleatorias de los tests. Sin esa configuración, las pruebas MySQL opcionales se omiten.

Frontend, desde `frontend/`:

```sh
npm run check
npx playwright install chromium
npm run e2e:real
```

`check` ejecuta compilación de producción y pruebas unitarias/HTTP. Para `e2e:real`, el backend debe estar iniciado y `GYM_E2E_CREDENTIALS_FILE` debe apuntar a un JSON privado, fuera del repositorio, con cuentas reales `ADMIN` y `PROFESOR` (`email` y `password`). Usar una base de pruebas: los recorridos crean y modifican registros.

Configurar `GYM_E2E_PRODUCTION=1` para probar Angular en modo producción, o `GYM_E2E_BASE_URL=http://127.0.0.1:8088` para utilizar un stack Compose de pruebas. `CHROMIUM_PATH` permite seleccionar Chromium del sistema. Ver [configuración completa de Chromium](frontend/README.md#3-compilación-y-pruebas). `npm run e2e` sin configuración real puede omitir los recorridos que necesitan backend.

### Última validación de las mejoras funcionales

| Verificación | Resultado |
| --- | --- |
| Backend, incluidos contratos HTTP, seguridad y MySQL | **217 pruebas aprobadas, sin omisiones** |
| Angular: unitarias e integración HTTP | **121 pruebas aprobadas** |
| Chromium con Angular en configuración de producción | **57 aprobadas**: 49 con API real y 8 de interfaz aislada |
| Compilación de producción y construcción de imágenes Docker | Aprobadas |

Los recorridos incluyen regresión de los tres roles, permisos, versiones, sesiones parciales, comentarios, estadísticas y pantallas de escritorio/tablet/celular. Ver el [informe funcional](frontend/MEJORAS-FUNCIONALES.md).

## Arquitectura y contratos

```text
Angular 22 → /api → Spring Boot → servicios → repositorios JPA → MySQL
```

- Angular standalone, TypeScript estricto, Signals y formularios reactivos.
- Spring Security con JWT y autorización por rol, propiedad y vínculos vigentes. Un profesor accede al historial de sus alumnos actualmente vinculados; no existe herencia implícita de roles.
- DTOs de entrada/salida, Bean Validation, MapStruct y manejo global de errores.
- Rutinas versionadas y prescripciones históricas inmutables; correcciones y anulaciones de entrenamientos con control de revisiones.
- Nginx sirve Angular y actúa como reverse proxy de `/api/` en Docker.

Documentación de referencia:

- [OpenAPI y DTOs reales](simulacro_nombreAlumno/docs/openapi.json).
- [Catálogo de endpoints y permisos](simulacro_nombreAlumno/docs/catalogo-endpoints.md).
- [Bloques, catálogo personalizado, objetivos, comentarios y migración 006](simulacro_nombreAlumno/docs/mejoras-funcionales.md).
- [Seguridad y configuración JWT](simulacro_nombreAlumno/docs/etapa-5-seguridad.md).
- [Administración y auditoría](simulacro_nombreAlumno/docs/etapa-6-administracion.md).
- [Estadísticas y limitaciones de datos](simulacro_nombreAlumno/docs/etapa-7-dashboards.md).
- [Guía del frontend y producción](frontend/README.md).

Con el backend local iniciado, Swagger está en **http://localhost:8080/swagger-ui/index.html** y el contrato en `/v3/api-docs`. En Docker se accede a la aplicación mediante Nginx en 8088; el puerto del backend no se publica directamente.

## Producción

Usar HTTPS, secretos privados, backups protegidos y un procedimiento de migración previo al arranque. El Compose entregado publica la aplicación en loopback por defecto. Para acceso desde otros equipos, configurar explícitamente dirección de escucha, firewall y reverse proxy/TLS.

Para desplegar Angular por separado, publicar `frontend/dist/frontend/browser/` con fallback de rutas a `index.html` y `/api/` dirigido al backend real, conservando estados HTTP y respuestas JSON. `ng serve` se utiliza para desarrollo/pruebas, no como servidor de producción. El stack no incorpora alta disponibilidad ni backups programados.

## Autores

- **Facundo Gregorio**
- **Nicolás Spina**
