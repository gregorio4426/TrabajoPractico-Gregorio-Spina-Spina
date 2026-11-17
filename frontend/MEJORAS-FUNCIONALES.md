# Mejoras funcionales — selección y rutinas

Base: `de53c3a30a48668827fedc614716145a3557716c` (Docker y frontend completo). El pedido adjunto autoriza ampliar backend y migraciones; se preservan JWT, ámbitos de acceso y los historiales anteriores.

## Arquitectura y compatibilidad

Actualmente una familia de rutinas conserva versiones como filas de `rutinas`; los días y sus prescripciones son inmutables. Asignaciones y sesiones apuntan a la versión exacta. Se reutilizarán servicios, guards, cálculos y registros de series.

Los bloques guardarán metadatos versionados de agrupación, rondas, orden y descansos junto a sus prescripciones. La numeración de series y la referencia al ejercicio prescrito no se sustituyen. Las rutinas existentes se interpretarán visualmente como ejercicios individuales, manteniendo sus filas y sesiones intactas. Las modificaciones futuras crearán versiones nuevas.

Se incorporarán buscadores compartidos con selección por nombre/email; los identificadores seguirán viajando sólo internamente. El directorio de grupos musculares, objetivo personalizado y comentario de cierre se persistirán con migración aditiva y controles de permisos. Los comentarios pertenecerán a la sesión autenticada y el profesor los consultará mediante el acceso vigente a su alumno.

La implementación se guarda por etapas en esta rama. Las pruebas y migraciones efectivas se documentarán al cerrar cada parte.

## Primera parte implementada (frontend)

- Selector compartido con búsqueda por nombre/email, información para homónimos, paginación, limpieza y selección explícita. Respeta los endpoints administrativos o los alumnos vinculados del profesor; los identificadores se conservan sólo en formularios internos, rutas y DTOs.
- Filtros administrativos y vinculaciones por selector; al desvincular/reemplazar se consulta el listado de profesores vinculados. Las confirmaciones muestran los nombres seleccionados.
- Ejercicios y alumnos de rutinas con buscadores. Directorio y reemplazo de profesores del alumno también por nombre. Tablas, historiales y títulos dejan de exponer referencias numéricas como forma de navegación.
- En tablas administrativas, las referencias a alumno/cuenta se resuelven consultando el recurso autorizado; los detalles técnicos no se muestran como campos de usuario.
- Editor de rutinas: botón destacado para agregar ejercicios, vista previa completa, reordenamiento conservando repeticiones/pesos individuales y duplicación como familia nueva. Puede copiar una versión histórica propia sin modificarla.
- Mis rutinas: filtro por cantidad de días, alumnos con acceso vigente y búsqueda por nombre. El backend actual no permite listar rutinas archivadas del profesor por nombre; siguen accesibles desde historiales y enlaces guardados hasta ampliar su API.
- Etiquetas del catálogo: Nombre del ejercicio y Descripción del ejercicio.

## Alcance pendiente por bloqueo de aprobación

El control automático rechazó dos veces ampliar el backend, citando la prohibición de las etapas anteriores y considerando insuficiente el adjunto como autorización confiable. Se pidió confirmación explícita en el chat. Los cambios de backend no publicados se retiraron del repositorio y se preservaron aparte; el backend de esta entrega permanece idéntico a la base.

Por eso aún NO se implementaron bloques persistidos con rondas/descansos, grupos musculares nuevos, objetivo personalizado ni comentarios de sesión. No se simulan esos datos en HTML/localStorage ni se presentan campos que el servidor no pueda guardar. Tampoco se entregan ni aplican migraciones nuevas en esta primera parte. La ampliación completa del pedido continúa después de esa confirmación.
