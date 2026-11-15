# Arranque con Docker Compose

Requiere Docker Engine/Desktop y Compose v2. Python 3 es opcional para generar configuración privada. Se conservan Angular 22, la paleta neón, Java 21 y MySQL 8.4. No se modifica lógica ni configuración fuente del backend.

Desde la raíz del repositorio:

```sh
python3 docker/init-env.py
# Abrir .env localmente para consultar/cambiar el email y contraseña iniciales.
docker compose up -d --build --wait --wait-timeout 300
```

Abrir **http://localhost:8088** e ingresar con el email y contraseña guardados en `.env`. El generador usa `admin@localhost.test` como email local inicial y contraseñas aleatorias; no las imprime y nunca sobrescribe un `.env` existente. Para otro puerto editar `GYM_HTTP_PORT` antes de arrancar. En Windows usar Python 3 (`py -3`) o copiar `.env.example` a `.env` y completar los valores manualmente. Generar JWT con `openssl rand -base64 48`; usar claves aleatorias distintas para MySQL y ADMIN.

Después del primer login, cambiar `GYM_BOOTSTRAP_ADMIN_ENABLED=false`, vaciar las dos variables `GYM_BOOTSTRAP_ADMIN_EMAIL/PASSWORD` y ejecutar `docker compose up -d backend`. Conservar los secretos de MySQL y JWT mientras se use el mismo volumen. Las cuentas se guardan en MySQL, no en `.env`.

## Componentes

- `frontend`: build multietapa Node 24 y Angular optimizado; Nginx ejecutado sin root en puerto 8080 interno. Sirve rutas profundas y envía `/api/` a Java sin cambiar estados HTTP.
- `backend`: build Maven/Java 21 y runtime JRE 21 sin root. Recibe configuración en tiempo de ejecución; no incluye secretos en imágenes. La construcción empaqueta sin ejecutar tests; las pruebas se ejecutan por separado.
- `db`: MySQL 8.4 con volumen `mysql-data` del proyecto Compose. No expone puertos al host. Tampoco se publica el puerto del backend.
- Healthchecks y dependencias ordenan el arranque. Nginx resuelve nuevamente el servicio backend cuando cambia su IP.

## Base de datos y persistencia

En un volumen nuevo, la imagen oficial de MySQL ejecuta los scripts originales 001–005 del repositorio en orden. No monta ni modifica la base que usabas fuera de este Compose. No conectar un volumen viejo de otro esquema: seguir primero las guías de migración del backend.

`docker compose stop`, `start`, `restart` y `down` conservan datos. **`docker compose down -v` elimina el volumen y sus datos**: usarlo únicamente para descartar una base de pruebas. Cambiar contraseñas de `.env` no cambia las de un MySQL ya inicializado. No regenerar `.env` para un volumen existente.

Las actualizaciones futuras de scripts SQL no se aplican automáticamente a volúmenes existentes. Realizar backup y aplicar únicamente migraciones pendientes con el procedimiento documentado por el backend.

## Operación

```sh
docker compose ps
docker compose logs --tail=100 backend
docker compose logs --tail=100 frontend
docker compose down
docker compose up -d --build --wait --wait-timeout 300
```

Un fallo al inicializar SQL exige revisar `docker compose logs db`; no se debe ocultar el fallo marcando manualmente una migración como aplicada. No publicar salidas de `docker compose config` sin redactar: incluyen variables interpoladas. Para validar sintaxis sin mostrar secretos: `docker compose config --quiet`.

Backup lógico (bash, desde la raíz):

```sh
mkdir -p docker/backups
chmod 700 docker/backups
docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers --no-tablespaces gimnasio' > docker/backups/gimnasio.sql
chmod 600 docker/backups/gimnasio.sql
```

Copiar el backup a almacenamiento externo protegido. Los backups también pueden contener datos personales y hashes de contraseñas. El volumen no reemplaza un backup. Probar restauraciones en una base separada antes de depender de este procedimiento para producción.

## Pruebas y límites

La API del navegador sigue siendo `/api` del mismo origen. Ver [`frontend/README.md`](../frontend/README.md) para pruebas unitarias/HTTP/Chromium; sus comandos ordinarios usan un backend local de pruebas y puerto 4200. La validación del stack debe comprobar además `http://localhost:8088/healthz`, login, rutas profundas y consultas reales a través de Nginx.

Por defecto la aplicación se publica sólo en loopback. Para otros equipos configurar explícitamente `GYM_BIND_ADDRESS`, firewall y HTTPS/reverse proxy. Esta configuración local no instala certificados, backups programados ni un gestor de secretos de producción. MySQL se conecta sin TLS únicamente por la red privada de Compose; un despliegue con requisitos de cifrado interno deberá configurar certificados y JDBC conforme a su infraestructura.

Las imágenes usan tags de versión mantenidos. Para una entrega inmutable registrar/pinear sus digests y conservar imágenes por versión. Los servicios son de una sola instancia; no se incluye alta disponibilidad. No hace merge ni despliegue remoto automático.
