# Validación del stack Docker

Se construyeron imágenes multietapa de Angular 22/Node 24, Java 21/Maven y MySQL 8.4. Los scripts SQL originales se copiaron sin cambios a la imagen de MySQL; el arranque se comprobó sobre un volumen nuevo e independiente de las bases anteriores.

Resultados:

- Configuración Compose válida (`docker compose config --quiet`).
- Compilación de Angular optimizada y empaquetado JAR aprobados dentro de sus respectivos builds.
- 102 pruebas unitarias/HTTP de Angular aprobadas, en 18 archivos.
- 53 pruebas Chromium aprobadas, sin fallos ni omisiones, usando `GYM_E2E_BASE_URL=http://127.0.0.1:8088`: 45 casos con la API real de Compose y 8 de interfaz aislada.
- Login de los tres roles, paneles, permisos, cambios entre roles, rutinas, entrenamientos, correcciones e historiales, estadísticas y revisión responsive conservados.
- MySQL, Java y Nginx con healthchecks saludables; Nginx y Java comprobados con usuarios sin root.
- `nginx -t` aprobado; rutas profundas `/admin`, `/profesor` y `/alumno` sirven Angular y `/api/` se dirige al backend conservando estados HTTP.
- El generador de `.env` crea claves aleatorias y rechaza sobrescribir configuración existente.

La construcción en este entorno Cloud requirió una adaptación temporal fuera del repositorio: proxy Maven y certificado de la sesión montados como secretos BuildKit. No se desactivó TLS ni se incluyeron configuraciones/certificados de Cloud en las imágenes de ejecución o en los Dockerfiles entregados. En una red corporativa con proxy, el operador debe suministrar su configuración de Maven/npm y certificados de confianza durante el build.

Los Dockerfiles no ejecutan la suite Java: empaquetan la aplicación existente. No se modificaron archivos de `simulacro_nombreAlumno/`, ni lógica Angular; sólo se habilitó una URL externa para reutilizar Playwright contra el servidor Docker.

No se desplegó a un servidor remoto ni se hizo merge a master. TLS público, almacenamiento de backups fuera del host y operación productiva siguen siendo configuración de la instalación. Las imágenes usan tags mantenidos; fijar digests si se requiere reproducibilidad binaria de sus bases.

Persistencia y recuperación comprobadas:

- Backup con `mysqldump --single-transaction --routines --triggers --no-tablespaces` y restauración en una base temporal separada; recuento de cuentas coincidente. Se eliminó sólo esa base temporal de verificación.
- Recreación de contenedores con `docker compose down` y `up --no-build --wait`, sin `-v`. Los datos del volumen y el login se conservaron y los tres healthchecks volvieron a estar saludables.
- Prueba adicional tras ajustar la prioridad de la ubicación Nginx `/api/`: las rutas API con extensión no se confunden con recursos estáticos; mantienen el JSON y estado de autorización del backend. Nuevamente aprobados `nginx -t`, login y recarga de rutas profundas.

El archivo de backup y las cuentas privadas de prueba se guardaron fuera del repositorio. Ninguna contraseña, JWT real, volumen, compilado o informe generado se incluye en Git.

## Ampliación funcional: bloques, catálogo, objetivos y comentarios

Se volvieron a construir las tres imágenes con el código actualizado y la migración 006. El entorno Cloud requirió la misma adaptación privada de proxy/certificados, más resolución del proxy para BuildKit; estos archivos siguen fuera del repositorio y los secretos no se copian a las imágenes de ejecución. `docker compose config --quiet` aprobado.

Spring Boot aprobó 213 pruebas, sin omisiones, con MySQL de pruebas habilitado; Angular aprobó la compilación de producción y 113 pruebas unitarias/HTTP. La migración 006 se probó de manera repetible, conservando datos legados y validando Hibernate. Los recorridos nuevos usan el backend real y verifican comentarios, objetivos libres, biseries y nombres históricos de grupos.

Esta validación construye las imágenes actualizadas; el stack Compose instalado anteriormente conserva su volumen. Para actualizarlo, aplicar [el procedimiento con backup y migración 006](README.md#actualizar-un-volumen-existente-a-las-mejoras-funcionales). No se hizo merge a master.

La regresión Chromium final aprobó 55 pruebas sin omisiones, con Angular en configuración de producción y la API/MySQL locales reales: 47 recorridos reales y 8 de interfaz aislada. Esta corrida no usó el Nginx de Compose; las imágenes actualizadas se construyeron por separado.
