# Etapa 1 — validación del 7 de noviembre de 2026

El frontend se recuperó de la rama local y se publicó primero en el commit
`8c32c0e4ee699c8238c29d2565ffb33d2c78a9e3`, tras compilar y aprobar las 28 pruebas
básicas. Las comprobaciones adicionales y correcciones se guardan en un commit
posterior de la misma rama `codex/frontend-etapa-1`.

Resultados finales en este entorno:

- `npm run build`: compilación de producción aprobada con TypeScript y plantillas estrictos.
- `npm test`: 29 pruebas aprobadas en 4 archivos; ninguna omitida.
- Chromium con Playwright: 11 pruebas aprobadas; ninguna omitida.
- Login real de ADMIN, PROFESOR y ALUMNO; autorización comprobada contra los endpoints existentes.
- Registro real de un alumno de prueba, perfil autenticado, recarga y logout.
- Guards, redirecciones por rol, errores de login, expiración y persistencia.
- Navegación móvil y confirmación de logout; sin desborde horizontal a 360px.
- Revisión visual de escritorio y móvil. Iconos SVG sin dependencia de fuentes externas.
- Solo archivos de `frontend/` modificados; backend y contratos conservados.

Los tests reales se ejecutaron con `GYM_E2E_REAL_API=1`, `CHROMIUM_PATH` apuntando
al Chromium del sistema y un archivo temporal privado de credenciales de prueba
fuera del repositorio. La aplicación usa exclusivamente la API real. Los fixtures
sintéticos se limitan a las pruebas unitarias/HTTP y de interfaz aislada.

No se incluyen contraseñas, JWT de usuarios, bases de datos, node_modules,
compilados, capturas ni informes generados. La etapa 2 no se implementó.
