# Etapa 6 — integración y revisión final

Base aprobada: `a4167d749fbfe3276272079af72eeeb82f7674ba`. Rama: `codex/frontend-etapa-6-final`. Se conserva la implementación de las cinco etapas anteriores. El backend y master no se modifican.

## Alcance de la revisión

- Regresión de autenticación, permisos, cuentas, vínculos, rutinas/versiones/asignaciones, entrenamientos/correcciones/auditoría y estadísticas de los tres roles.
- Recorrido integrado con cambios visibles desde ADMIN, PROFESOR y ALUMNO.
- Pantallas principales y detalles en escritorio, tablet y celular; formularios, tablas, gráficos, diálogos, foco y etiquetas.
- Observación de errores de consola, recursos fallidos y respuestas HTTP, distinguiendo errores esperados de permisos/validación de fallos inesperados.
- Instrucciones de instalación local y despliegue con el backend real.

Se ejecutó una nueva validación integral en este entorno; los resultados de etapas anteriores se conservan como informes históricos. Los resultados finales y las limitaciones se detallan a continuación.

## Correcciones y cierre de interfaz

- Al navegar se enfoca el contenido principal: el foco ya no queda en un enlace del menú móvil que pasa a estar oculto/inert. Se conserva la trampa de foco del menú y la restauración de foco al cerrarlo y al salir de diálogos.
- Tablas desplazables accesibles por teclado, regiones identificadas y foco visible para selectores, áreas de texto y desplegables de valores exactos.
- Controles de al menos 44 px y campos móviles de al menos 16 px; los campos de entrenamiento mantienen su tamaño mayor. La acción inferior contempla el área segura del dispositivo y se respeta la preferencia de movimiento reducido.
- Encabezados principales en las páginas independientes de progreso; se mantienen tipografías del sistema, SVG locales, paletas y estructura diferenciadas por rol.
- El 409 al cambiar profesores del alumno ahora informa un conflicto de vínculo/estado del profesor, sin sugerir corregir un número de serie. Los conflictos de entrenamiento conservan su indicación específica.
- `npm run check` agrupa compilación de producción y pruebas unitarias/HTTP. `npm run e2e:real` valida la configuración privada y activa todos los casos reales; no permite una validación aparentemente exitosa por omisión accidental de credenciales.
- Modo `GYM_E2E_PRODUCTION=1` para verificar bundles optimizados en Chromium sin reutilizar un servidor de desarrollo previo. No cambia la aplicación ni despliega servicios.
- README completo de backend existente, base vacía/migraciones, variables privadas, bootstrap, proxy de API, comandos de Angular, pruebas y despliegue. El README raíz enlaza a esta guía.

## Cobertura de integración

| Ámbito | Verificación |
| --- | --- |
| Autenticación | Login real de los tres roles, persistencia, logout, guards, rutas, cambio de credenciales, revocación real y temporizadores de expiración |
| ADMIN | Creación/edición de cuentas y perfiles, estado, roles, email, vinculación/reemplazo/desvinculación, consulta de rutinas, versiones, asignaciones, sesiones y auditoría |
| PROFESOR | Alumnos vinculados, seguimiento, ejercicios, rutina propia con varios días, creación/versionado/asignación/archivo, sesiones autorizadas, perfil y credenciales |
| ALUMNO | Rutina activa e historia, sesión abierta con versión exacta, series realizadas, recarga, cierre completo/parcial, corrección/anulación/restauración, fechas y auditoría, perfil y profesores |
| Entre roles | Cambio de perfil en ADMIN visible en PROFESOR y ALUMNO; versión y asignación nuevas en PROFESOR visibles en ALUMNO y ADMIN sin reinterpretar la sesión anterior; resultados del alumno consultables por ADMIN/PROFESOR y métricas coincidentes |
| Estadísticas | Dashboards y resúmenes del mismo período, ejercicio y fechas, fecha efectiva corregida, cero/desconocido, volumen incompleto, exclusiones y sesiones abiertas, precisión textual, paginación y enlaces al detalle |
| Errores reales | 400 por orden de consulta inválido con recuperación del filtro; 401 por cuenta revocada; 403 por recursos ajenos/vínculo terminado; 404 con retorno al listado; 409 por revisiones, duplicados y profesor inactivo |
| Responsive | 45 rutas con datos reales en 1440, 820 y 360 px; campos etiquetados y ausencia de desbordamiento horizontal de la página; edición móvil de series y foco automático en repeticiones |
| Navegación accesible | Menú móvil con Tab/Shift+Tab/Escape, foco tras navegación, cancelación del diálogo con Escape, logout y ausencia de sesión persistida posterior |

Los recorridos positivos nuevos registran excepciones JavaScript, consola de nivel error, HTTP de API con estado >=400 y fallos de red distintos de navegación cancelada. Exigen listas vacías de errores inesperados. Los casos negativos verifican explícitamente las respuestas previstas; no se ocultan los errores del backend para hacer pasar el barrido.

La revisión visual incluye captura del formulario real de entrenamiento móvil. Se conserva el diseño existente, sin reconstruir paneles ni duplicar funciones.

## Límites pendientes y de despliegue

- No se modificó el backend. No hay refresh tokens, cierre de JWT por endpoint, recuperación de contraseña por email, modo offline, guardado de campos aún no enviados ni notificaciones de cambios entre roles en tiempo real. Consultar/recargar recupera lo confirmado en el servidor.
- Los casos de expiración local usan temporizadores controlados para no esperar la vigencia completa del JWT; la revocación y sustitución de tokens sí se verifican contra el backend real.
- El directorio de profesores no devuelve actividad/rol actual: el backend decide si un vínculo es válido. Se maneja su rechazo 409 y se conserva la sesión del alumno.
- Se mantienen ventana estadística de 1–366 días, datos paginados, trazado aproximado y decimales exactos en tablas. No se infieren unidades ni registros históricos ausentes. Las consultas no son una instantánea transaccional de varias peticiones concurrentes.
- Revisión responsive y accesibilidad básica en Chromium automatizado; no es una certificación WCAG ni una validación en dispositivos físicos, Safari/Firefox, lectores de pantalla o todas las combinaciones de ampliación de texto. Se conservaron etiquetas, foco y contraste de la paleta; estas comprobaciones no reemplazan una auditoría especializada.
- Se compiló y se probaron los bundles optimizados con `ng serve --configuration production`. No se desplegó Nginx/TLS ni se ejecutaron pruebas de carga o sobre una base productiva. Validar host, certificados, CORS, caché y fallback de rutas en la infraestructura de destino.
- Las pruebas crean y dejan cuentas/datos exclusivamente en una base local de pruebas. Mantener ese entorno separado de producción; los secretos y artefactos generados quedan fuera de Git.
- Se entrega una rama revisable. No se hace merge a master ni se publica un despliegue automáticamente.

## Resultados finales ejecutados

- `npm run check`: compilación de producción aprobada; **102 pruebas unitarias/HTTP aprobadas en 18 archivos**, sin fallos ni omisiones. TypeScript/plantillas estrictos y presupuestos del bundle aprobados.
- `GYM_E2E_PRODUCTION=1 CHROMIUM_PATH=/usr/bin/chromium GYM_E2E_CREDENTIALS_FILE=<archivo privado externo> npm run e2e:real`: **53 pruebas Chromium aprobadas, 0 fallos y 0 omitidas**, sobre la configuración optimizada de producción. Incluye 45 casos con backend real y 8 de interfaz aislada; todos los paneles conservan su regresión.
- Nueve casos nuevos finales: 135 visitas de revisión responsive (45 rutas × 3 anchuras), propagación de versiones/asignaciones y perfiles/entrenamientos entre roles, teclado/logout y recuperación de 400/401/404/409. La regresión existente cubre además 403, revisiones de series, estadísticas y rotación de credenciales.
- Se comprobó que `e2e:real` termina con error si falta la configuración privada de credenciales, sin imprimir valores secretos.
- Backend sin diferencias respecto de `a4167d749fbfe3276272079af72eeeb82f7674ba`. Sin artefactos generados ni secretos incluidos en los commits. Publicación en rama de etapa 6, conservando los commits aprobados de etapas 1–5 y sin merge a master.
