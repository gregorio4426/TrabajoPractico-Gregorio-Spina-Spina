# Impulso — frontend, etapas 1 y 2

Angular 22 standalone, Signals y TypeScript/plantillas estrictos. Autenticación y estructura: login único, JWT, interceptor limitado a la API, guards por rol, persistencia, vencimiento, logout y tres variantes responsive de sidebar/header. La etapa 2 incorpora el panel ADMIN completo sobre los endpoints existentes. PROFESOR y ALUMNO conservan únicamente la estructura aprobada de la etapa 1. Ver [funcionalidad administrativa](ETAPA-2-ADMIN.md).

## Desarrollo

Requiere Node 24.15+ (24.x) y npm. Desde esta carpeta:

```sh
npm ci
npm start
```

El backend existente debe funcionar en `http://127.0.0.1:8080`. El proxy de desarrollo envía `/api/**` al backend conservando el host para solicitudes del mismo origen. No se modificó el backend. En producción servir `dist/frontend/browser` con fallback a `index.html` y un reverse proxy `/api` hacia el backend; preservar host/origen o configurar los orígenes permitidos en el entorno del servidor.

## Contratos y sesión

Fuentes: `../simulacro_nombreAlumno/docs/openapi.json`, `catalogo-endpoints.md` y `etapa-5-seguridad.md`. Login: `POST /api/auth/login`, cuerpo `{email,password}`, respuesta `{token,email,rol,perfilId}`. No hay refresh tokens ni endpoint de logout. El frontend conserva la sesión en localStorage (nunca la contraseña); el backend verifica firma, estado y permisos. Decodificar el JWT en el navegador solo permite controlar el vencimiento y la interfaz. Un 401 de la API cierra la sesión vigente; un 403 no la elimina. Login y registro se envían sin Bearer previo. No se agregan credenciales predeterminadas ni datos simulados a la aplicación.

## Validación

```sh
npm run build
npm test
npx playwright install chromium
npm run e2e
```

Si Chromium está instalado en el sistema, configurar `CHROMIUM_PATH` con su ruta. Las pruebas HTTP y de interfaz usan fixtures aislados exclusivamente en tests. Las pruebas adicionales contra el backend real se activan con `GYM_E2E_REAL_API=1`; crean cuentas, perfiles, vínculos, rutinas versionadas y sesiones de prueba en una base local exclusiva. Las pruebas administrativas también modifican estado, email y roles de esas cuentas de prueba. Para probar ADMIN y PROFESOR, configurar `GYM_E2E_CREDENTIALS_FILE` con la ruta a un JSON privado externo al repositorio con las claves ADMIN y PROFESOR, cada una con email/password de cuentas de prueba existentes. No usar una base de producción. No versionar ese archivo ni capturas con credenciales. Sin estas opciones las pruebas reales correspondientes se omiten explícitamente.

Los archivos generados, node_modules, informes y secretos locales están excluidos de Git.

Para regenerar los tipos desde el contrato local sin modificar el backend:

```sh
python3 scripts/generate-contracts.py
npm exec -- prettier --write src/app/admin/contracts.ts
```
