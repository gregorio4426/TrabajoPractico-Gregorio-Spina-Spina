# Impulso — frontend Angular 22, etapas 1 a 6

Aplicación standalone con Signals, formularios reactivos, JWT y TypeScript/plantillas estrictos. Login único y espacios responsive ADMIN, PROFESOR y ALUMNO conectados exclusivamente al backend de este repositorio. No contiene datos simulados en producción.

[Revisión final y límites](ETAPA-6-FINAL.md) · [ADMIN](ETAPA-2-ADMIN.md) · [PROFESOR](ETAPA-3-PROFESOR.md) · [ALUMNO](ETAPA-4-ALUMNO.md) · [Estadísticas](ETAPA-5-ESTADISTICAS.md).

## Requisitos

- Node **24.15+ dentro de 24.x**, npm (el proyecto declara npm 11.9.0). Dependencias reproducibles en `package-lock.json`.
- Backend: **Java 21**, MySQL **8.4** y Maven Wrapper incluido. La configuración y migraciones existentes no se modifican desde Angular.
- Chromium de Playwright o Chromium del sistema para los recorridos automatizados.

## 1. Preparar el backend local

Desde la raíz del repositorio, crear una base MySQL **vacía y exclusiva de desarrollo**, por ejemplo `APPgimnasioBD`, y un usuario con acceso a ella. Aplicar en orden los cinco scripts existentes:

```sh
cd simulacro_nombreAlumno
mysql -u USUARIO_LOCAL -p APPgimnasioBD < src/main/resources/db/mysql/001_esquema_anterior.sql
mysql -u USUARIO_LOCAL -p APPgimnasioBD < src/main/resources/db/mysql/002_historial_versionado.sql
mysql -u USUARIO_LOCAL -p APPgimnasioBD < src/main/resources/db/mysql/003_consistencia_entrenamientos.sql
mysql -u USUARIO_LOCAL -p APPgimnasioBD < src/main/resources/db/mysql/004_seguridad_identidad.sql
mysql -u USUARIO_LOCAL -p APPgimnasioBD < src/main/resources/db/mysql/005_administracion.sql
cp .env.example .env
```

En una base existente **no volver a aplicar 001**: seguir las [instrucciones de migración del backend](../simulacro_nombreAlumno/docs/etapa-6-administracion.md). Los comandos anteriores requieren que la base ya exista; reemplazar `USUARIO_LOCAL` por el usuario configurado.

Completar el `.env` local, ignorado por Git:

| Variable | Configuración |
| --- | --- |
| `GYM_DB_URL` | JDBC hacia la base local, por ejemplo `jdbc:mysql://localhost:3306/APPgimnasioBD` |
| `GYM_DB_USERNAME`, `GYM_DB_PASSWORD` | Credenciales privadas de MySQL |
| `GYM_JWT_SECRET_BASE64` | Clave privada de al menos 32 bytes; generar con `openssl rand -base64 32` |
| `GYM_JWT_TTL`, `GYM_JWT_ISSUER` | Valores existentes: `PT24H`, `gimnasio-api` |
| `GYM_CORS_ALLOWED_ORIGINS` | Orígenes exactos usados, por ejemplo `http://localhost:4200,http://127.0.0.1:4200` |
| `GYM_BOOTSTRAP_ADMIN_ENABLED` | `true` únicamente para crear el primer ADMIN |
| `GYM_BOOTSTRAP_ADMIN_EMAIL`, `GYM_BOOTSTRAP_ADMIN_PASSWORD` | Datos privados elegidos para ese primer ADMIN; contraseña de al menos 12 caracteres y hasta 72 bytes UTF-8 |

Spring Boot no carga `.env` automáticamente. En bash, cargar únicamente un archivo propio y confiable, con valores correctamente citados:

```sh
set -a
. ./.env
set +a
bash mvnw spring-boot:run
```

Una vez creada la primera cuenta, deshabilitar bootstrap y retirar sus credenciales del entorno del servidor. No hay cuentas ni contraseñas predeterminadas. ADMIN puede crear profesores y alumnos desde la interfaz. El frontend tiene login único; no incorpora una pantalla de registro público ni recuperación de contraseña por email.

El backend escucha en `http://localhost:8080`. Contrato en `/v3/api-docs`, Swagger en `/swagger-ui/index.html`. Para generar un JAR con la configuración ya cargada: `bash mvnw package` y luego `java -jar target/simulacro_nombreAlumno-0.0.1-SNAPSHOT.jar`.

## 2. Ejecutar Angular

En otra terminal, desde la raíz del repositorio:

```sh
cd frontend
npm ci
npm start
```

Abrir `http://localhost:4200`. Ingresar con una cuenta real: el rol devuelto por el servidor selecciona el espacio correspondiente. Usuarios inactivos o sin rol/perfil vigente son rechazados por el backend.

La API del navegador es **`/api` del mismo origen**, definida en `src/app/core/api.ts`. `proxy.conf.json` envía `/api/**` a `http://127.0.0.1:8080`. Si el backend usa otro host/puerto en desarrollo, cambiar solamente el `target` del proxy y reiniciar `npm start`. No colocar claves JWT ni credenciales en Angular. Los `.env` no configuran automáticamente el bundle de Angular.

## 3. Compilación y pruebas

Desde `frontend/`:

```sh
npm run check
npx playwright install chromium
npm run e2e
```

`check` ejecuta compilación de producción y todas las pruebas unitarias/HTTP. `npm run e2e` permite ejecutar los casos de interfaz aislada; **omite los casos reales si falta su configuración**. Para la validación completa usar una base exclusiva de pruebas y el comando estricto siguiente, que rechaza credenciales ausentes o mal configuradas en lugar de terminar con pruebas omitidas.

Crear un JSON **privado fuera del repositorio**, con claves `ADMIN` y `PROFESOR`; cada una debe contener `email` y `password` de cuentas reales de esa base de prueba. Puede generarse con un gestor de secretos; no usar contraseñas de producción. ADMIN permite crear la cuenta PROFESOR inicial. Los casos crean y modifican sus propias cuentas, vínculos, rutinas y sesiones; dejan datos de prueba en esa base, que debe reiniciarse separadamente cuando sea necesario.

```sh
export GYM_E2E_CREDENTIALS_FILE=/ruta/privada/cuentas-pruebas.json
# Si se usa Chromium del sistema; omitir si se instaló con Playwright:
export CHROMIUM_PATH=/usr/bin/chromium
npm run e2e:real
```

Para comprobar también los bundles optimizados con el servidor de pruebas de Angular:

```sh
GYM_E2E_PRODUCTION=1 npm run e2e:real
```

Este modo exige el puerto 4200 libre para no reutilizar un servidor de desarrollo. El backend debe estar funcionando en 8080. Playwright arranca y detiene su servidor Angular. `ng serve --configuration production` sirve para esta verificación, **no es el servidor de despliegue**. Se puede seleccionar un archivo con `npm run e2e:real -- e2e/final.spec.ts`.

Los fixtures sintéticos se limitan a tests. Dependencias, bundles, informes, capturas y secretos locales no se versionan. Las pruebas reales incluyen controles de autorización y respuestas esperadas 400/401/403/404/409; la auditoría positiva final exige ausencia de errores inesperados de consola/API y fallos de red.

## 4. Producción

```sh
npm ci
npm run build
```

Publicar **`dist/frontend/browser/`** en un servidor HTTP estático con HTTPS. Configurar fallback de rutas Angular a `index.html` y un reverse proxy `/api/` hacia el backend real. Los errores de API deben conservar su estado y JSON; nunca transformarlos en `index.html`.

Ejemplo de las ubicaciones de un servidor Nginx, ajustando raíz y destino a la infraestructura propia:

```nginx
root /srv/impulso/browser;
location /api/ {
    proxy_pass http://backend:8080;
    proxy_set_header Host $http_host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
location = /index.html {
    add_header Cache-Control "no-store" always;
}
location / {
    try_files $uri $uri/ /index.html;
}
```

Terminar TLS en el servidor o balanceador de la instalación. Preservar host/origen o configurar los orígenes exactos permitidos en el backend. No incluir secretos en los archivos estáticos; configurar MySQL/JWT/bootstrap únicamente en el servidor. Los archivos JS/CSS de producción tienen hash; evitar cachear permanentemente `index.html` para que futuras versiones se carguen correctamente. Comprobar recarga directa de `/admin`, `/profesor` y `/alumno`, respuestas de `/api`, HTTPS y caché en el despliegue elegido. No se publicó un despliegue ni se hizo merge a master durante esta entrega.

## Sesión, datos y límites

El JWT se conserva en localStorage, nunca la contraseña. No hay refresh token ni endpoint de logout: salir elimina la sesión del navegador; la API controla firma, vencimiento, estado y revisiones. Un 401 vigente cierra la sesión; 403/404/409 se muestran sin eliminarla. Los cambios de credenciales reemplazan el JWT y revocan los anteriores. Usar HTTPS y proteger el origen frente a scripts ajenos.

Los registros de entrenamiento confirmados se recuperan del servidor al recargar. No existe modo offline ni guardado de campos aún no enviados. Una escritura incierta no se repite automáticamente: recargar para comprobar qué se guardó. Las correcciones usan revisiones y motivos auditados.

Las estadísticas usan las fórmulas del backend y la fecha efectiva. Sesión finalizada parcial no equivale a día cumplido; anulaciones y anomalías legadas se excluyen según la API. Peso cero y peso desconocido se mantienen distintos. La ventana es de hasta 366 días inclusivos, los gráficos muestran una página de datos y son aproximados; las tablas conservan decimales exactos. No se infieren unidades, asistencia, calorías ni resultados históricos ausentes. No hay actualización por WebSocket: consultar/recargar para ver cambios realizados por otro rol.

Las limitaciones restantes y el alcance de revisión visual/accesibilidad están en [ETAPA-6-FINAL.md](ETAPA-6-FINAL.md). Los informes de etapas anteriores son históricos y no representan el alcance final por sí solos.

## Contratos

Fuentes: [OpenAPI](../simulacro_nombreAlumno/docs/openapi.json), [catálogo](../simulacro_nombreAlumno/docs/catalogo-endpoints.md), [seguridad](../simulacro_nombreAlumno/docs/etapa-5-seguridad.md) y [estadísticas](../simulacro_nombreAlumno/docs/etapa-7-dashboards.md). Para regenerar tipos sin modificar el backend:

```sh
python3 scripts/generate-contracts.py
npm exec -- prettier --write src/app/admin/contracts.ts
```
