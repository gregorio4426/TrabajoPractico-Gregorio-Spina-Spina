# Mejoras funcionales — selección y rutinas

Base: `de53c3a30a48668827fedc614716145a3557716c` (Docker y frontend completo). El pedido adjunto autoriza ampliar backend y migraciones; se preservan JWT, ámbitos de acceso y los historiales anteriores.

## Arquitectura y compatibilidad

Actualmente una familia de rutinas conserva versiones como filas de `rutinas`; los días y sus prescripciones son inmutables. Asignaciones y sesiones apuntan a la versión exacta. Se reutilizan servicios, guards, cálculos y registros de series.

Los bloques guardan metadatos versionados de agrupación, rondas, orden y descansos junto a sus prescripciones. La numeración de series y la referencia al ejercicio prescrito no se sustituyen. Las rutinas existentes se interpretan visualmente como ejercicios individuales, manteniendo sus filas y sesiones intactas. Las modificaciones futuras crean versiones nuevas.

Se incorporan buscadores compartidos con selección por nombre/email; los identificadores seguirán viajando sólo internamente. El directorio de grupos musculares, objetivo personalizado y comentario de cierre se persisten con migración aditiva y controles de permisos. Los comentarios pertenecen a la sesión autenticada y el profesor los consulta mediante el acceso vigente a su alumno.

La implementación se guarda por etapas en esta rama. Las pruebas y migraciones efectivas se documentarán al cerrar cada parte.

## Primera parte implementada (frontend)

- Selector compartido con búsqueda por nombre/email, información para homónimos, paginación, limpieza y selección explícita. Respeta los endpoints administrativos o los alumnos vinculados del profesor; los identificadores se conservan sólo en formularios internos, rutas y DTOs.
- Filtros administrativos y vinculaciones por selector; al desvincular/reemplazar se consulta el listado de profesores vinculados. Las confirmaciones muestran los nombres seleccionados.
- Ejercicios y alumnos de rutinas con buscadores. Directorio y reemplazo de profesores del alumno también por nombre. Tablas, historiales y títulos dejan de exponer referencias numéricas como forma de navegación.
- En tablas administrativas, las referencias a alumno/cuenta se resuelven consultando el recurso autorizado; los detalles técnicos no se muestran como campos de usuario.
- Editor de rutinas: botón destacado para agregar ejercicios, vista previa completa, reordenamiento conservando repeticiones/pesos individuales y duplicación como familia nueva. Puede copiar una versión histórica propia sin modificarla.
- Mis rutinas: filtro por cantidad de días, alumnos con acceso vigente y búsqueda por nombre. El backend actual no permite listar rutinas archivadas del profesor por nombre; siguen accesibles desde historiales y enlaces guardados hasta ampliar su API.
- Etiquetas del catálogo: Nombre del ejercicio y Descripción del ejercicio.

## Persistencia y ampliación autorizada

El backend incorpora la migración aditiva 006, bloques versionados (vueltas, orden, nombre y descansos), catálogo de grupos personalizados con referencias estables, objetivo libre editable y comentarios opcionales al cerrar sesiones. Se conservan los contratos planos y categorías anteriores. Ver [contratos y migración](../simulacro_nombreAlumno/docs/mejoras-funcionales.md).

El editor permite crear bloques de series, biseries y circuitos, agregar ejercicios sin límite artificial, reordenar bloques y ejercicios, y previsualizar la prescripción completa. Las vueltas se comparten dentro del bloque; repeticiones, pesos y descanso posterior pertenecen a cada ejercicio. Cero y ausencia de peso siguen siendo distintos.

El perfil del alumno y el seguimiento del profesor admiten el objetivo libre como campo principal, con categoría opcional. Administración también puede crearlo/editarlo. Al finalizar un entrenamiento completo o parcial se puede guardar un comentario de hasta 2000 caracteres. Se consulta desde el detalle de la sesión, incluido el panel del profesor actualmente vinculado.

Los profesores administran grupos y ejercicios del catálogo compartido. Las normalizaciones evitan duplicados por espacios, mayúsculas y acentos; no se eliminan duplicados legados ni recursos referenciados. Los grupos estándar se conservan para compatibilidad con el enum anterior.

## Validación

- Backend: 213 pruebas aprobadas sin omisiones, con `MYSQL_TEST_URL` configurado. Incluyen seguridad/JWT, permisos, concurrencia, contratos HTTP y las 9 pruebas de migración MySQL (006 repetida, preservación de registros y validación Hibernate).
- Angular: compilación de producción y 113 pruebas unitarias/HTTP aprobadas.
- Chromium: 55 pruebas aprobadas sin omisiones, con Angular en configuración de producción: 47 contra Spring Boot/MySQL reales y 8 de interfaz aislada. Incluyen ADMIN/PROFESOR/ALUMNO, perfiles, vinculaciones, versiones, sesiones, comentarios, estadísticas, permisos y errores 400/401/403/404/409. Revisión responsive en 1440/820/360 px y editor/entrenamiento a 390 px.
- Docker: imágenes de backend, frontend y MySQL construidas; Compose válido. La adaptación privada de proxy Cloud permanece fuera del repositorio.

No se hizo merge a master. Los backups, credenciales, dependencias y artefactos generados quedan fuera de Git.
