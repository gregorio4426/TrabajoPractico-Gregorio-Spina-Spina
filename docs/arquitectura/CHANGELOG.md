# Changelog de arquitectura

## Línea base — b638109 (2026-09-14)
- Análisis retroactivo del primer estado con seguridad JWT y sesiones/series.
- Contexto: ADMIN (sembrado por `DataSeeder`), PROFESOR y ALUMNO consumen la API.
- Contenedores: API Spring Boot (+ springdoc) y MySQL. No existía frontend en el repositorio.
- Componentes backend: security, controllers, services, mappers, repositories, model, config, exception.
- No se genera `c4-componentes-frontend.mmd`: no hay código frontend en este commit.

## Actualización 1 — b638109 → addc535 (2026-10-31)
- Diff: 121 archivos en `src/main` (+3515 / −666), consistente con la rama `backend-etapas-1-7`.
- Nuevos módulos: `admin/` (6 archivos), `estadisticas/` (12), `validation/` (4).
- `security/` pasa de 5 a 13 archivos (AccesoService, CORS, PasswordPolicy, JwtProperties, bootstrap admin).
- `config/` suma verificadores de migración que usan `JdbcTemplate`; `resources/db/mysql/001-005` define el esquema.
- Entidades de auditoría (`AuditoriaAdministrativa`, `AuditoriaEntrenamiento`).
- Diagramas afectados: contenedores, componentes-backend. Contexto sin cambios de estructura.
- Frontend: sin cambios (todavía no existe).

## Actualización 2 — addc535 → 9f1b029 (2026-11-10)
- Diff: 129 archivos, +19862 líneas: 128 en `frontend/` y el README (backend sin cambios).
- Nuevo contenedor: Angular 22 (`frontend/src/app`, 94 archivos) con rutas lazy-loaded por rol `admin/`, `profesor/`, `alumno/`, guards, interceptor JWT y cliente `core/api.ts` (base `/api`).
- `frontend/proxy.conf.json` reenvía `/api/**` a `http://127.0.0.1:8080`.
- Diagramas afectados: contexto, contenedores y componentes-frontend (nuevo contenido).
- Correcciones de evidencia sobre la versión heredada: se eliminó "MySQL 8.4" (el repo dice 8+) y "HTTPS" (sin TLS observable).
