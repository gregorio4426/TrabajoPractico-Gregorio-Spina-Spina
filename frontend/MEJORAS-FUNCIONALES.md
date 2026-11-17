# Mejoras funcionales — selección y rutinas

Base: `de53c3a30a48668827fedc614716145a3557716c` (Docker y frontend completo). El pedido adjunto autoriza ampliar backend y migraciones; se preservan JWT, ámbitos de acceso y los historiales anteriores.

## Arquitectura y compatibilidad

Actualmente una familia de rutinas conserva versiones como filas de `rutinas`; los días y sus prescripciones son inmutables. Asignaciones y sesiones apuntan a la versión exacta. Se reutilizarán servicios, guards, cálculos y registros de series.

Los bloques guardarán metadatos versionados de agrupación, rondas, orden y descansos junto a sus prescripciones. La numeración de series y la referencia al ejercicio prescrito no se sustituyen. Las rutinas existentes se interpretarán visualmente como ejercicios individuales, manteniendo sus filas y sesiones intactas. Las modificaciones futuras crearán versiones nuevas.

Se incorporarán buscadores compartidos con selección por nombre/email; los identificadores seguirán viajando sólo internamente. El directorio de grupos musculares, objetivo personalizado y comentario de cierre se persistirán con migración aditiva y controles de permisos. Los comentarios pertenecerán a la sesión autenticada y el profesor los consultará mediante el acceso vigente a su alumno.

La implementación se guarda por etapas en esta rama. Las pruebas y migraciones efectivas se documentarán al cerrar cada parte.
