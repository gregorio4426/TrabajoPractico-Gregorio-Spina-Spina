# Datos de prueba opcionales

La carga agrega datos ficticios a MySQL mediante los endpoints reales y los permisos actuales. No utiliza mocks del frontend ni cambia el backend. No se ejecuta al arrancar normalmente Docker.

Incluye 20 ejercicios habituales, un profesor Demo y cinco alumnos Demo: Juan, Lucía, Pedro, Sofía y Mateo. Los alumnos quedan vinculados al profesor y tienen distintos niveles. Se reutilizan los ejercicios que ya existen con el mismo nombre, sin cambiar su descripción ni grupo muscular.

## PowerShell

Con Docker Desktop iniciado y los tres servicios saludables, ejecutar desde la carpeta que contiene `compose.yaml`, con esta rama actualizada:

```powershell
cd "C:\Users\spina\gimnasio-docker"
New-Item -ItemType Directory -Force docker/demo-data
docker compose --profile demo run --rm --no-deps demo
```

Ingresar el email y la contraseña de una cuenta ADMIN existente. La contraseña no se muestra mientras se escribe. No hace falta instalar Python, Node ni abrir el IDE. La primera ejecución descarga la imagen de Node que utiliza este comando.

Para consultar los emails y contraseñas generados:

```powershell
notepad docker/demo-data/accounts.json
```

Abrir **http://localhost:8088**, cerrar cualquier sesión anterior e ingresar con alguna de esas cuentas. El profesor puede crear y asignar rutinas a los cinco alumnos; cada alumno puede registrar sus entrenamientos. No se generan rutinas, entrenamientos ni estadísticas ficticias: los indicadores reflejan únicamente la actividad registrada.

## Persistencia y repetición

Los emails son únicos por conjunto de prueba y las contraseñas son aleatorias. El archivo `docker/demo-data/accounts.json` es privado y está excluido de Git y del contexto de construcción de imágenes. No contiene la contraseña ADMIN ni tokens JWT. Dentro del contenedor, ese mismo archivo se encuentra en `/seed-data/accounts.json`.

**Conservar el archivo**: permite repetir el comando sin duplicar cuentas ni ejercicios y continuar una carga interrumpida. Si se elimina, la próxima ejecución crea otro conjunto de cuentas; las anteriores permanecen en MySQL. No elimina datos existentes ni modifica rutinas, asignaciones o historiales.

Si una cuenta demo se desactiva o se cambia su contraseña manualmente, la carga se detiene antes de intentar reemplazar sus credenciales. No restablece cuentas. Si se cambia de base de datos, conservar por separado el archivo correspondiente a cada instalación.

En Linux/macOS, crear primero la carpeta con `mkdir -p docker/demo-data` y ejecutar el mismo comando Compose. El proceso utiliza el usuario no privilegiado `node`; la carpeta debe permitir escritura a ese usuario (UID 1000 en la imagen).

## Verificación del cargador

Con Node 24 disponible, desde la raíz del repositorio:

```sh
node docker/tests/seed-demo.test.mjs
```

Las pruebas verifican reutilización de ejercicios, repetición sin duplicados, protección de credenciales, rechazo de archivos de otra API y recuperación de una creación interrumpida. La verificación contra el backend real también comprueba login de las seis cuentas, vinculación de los cinco alumnos y conservación de los ejercicios previos.
