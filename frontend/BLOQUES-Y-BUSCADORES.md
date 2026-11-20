# Bloques completos y buscadores con creación

## Diagnóstico previo

El backend ya persistía orden de bloque, posiciones, vueltas, nombres y descansos en las prescripciones inmutables. El problema principal estaba en el editor: presentaba ejercicios planos, repetía series en cada ejercicio, dibujaba el encabezado dentro del primero y no ofrecía mover ejercicios entre bloques. Eso ocultaba la diferencia entre repetir el bloque completo y hacer series independientes.

Se mantiene el contrato versionado existente. Para compatibilidad, `seriesSugeridas` de cada prescripción coincide con las vueltas del bloque; no representa una instrucción de realizar todas las series de un ejercicio antes del siguiente. No se agrega una migración: las columnas necesarias ya existen desde 006 y los pesos individuales desde 007.

## Implementación

- Bloques como secciones independientes, con un solo control de vueltas compartidas; una sección con un ejercicio se presenta como serie individual, dos como biserie y tres o más como circuito.
- Agregar, quitar y reordenar bloques/ejercicios, mover ejercicios entre bloques y combinar series individuales con circuitos. Al mover, se conservan repeticiones y descanso del ejercicio y se adopta la configuración de vueltas/descanso del bloque destino.
- Vista previa y visualización compartida para profesor, alumno, historial y sesión: repeticiones por vuelta, instrucciones de completar todos los ejercicios en orden y descanso opcional.
- Selector reutilizable con búsqueda reactiva, normalización de nombres, teclado, estados de carga/error, foco y opciones de creación. No se agregan librerías.
- Grupos musculares con selector buscable y creación inline, tanto en el catálogo como dentro del alta de un ejercicio en la rutina.
- Alta de ejercicios en un diálogo sin salir del editor. La descripción ahora es opcional; su ausencia se guarda como cadena vacía, compatible con la columna existente. Se valida longitud máxima y se mantienen grupo obligatorio, nombres únicos y permisos PROFESOR.
- La creación refresca el catálogo y selecciona el ejercicio. Se comprueba el nombre normalizado antes de crear; el backend conserva la protección de unicidad para concurrencia.
- Se mantienen JWT, tres roles, versiones históricas y pesos individuales por asignación. Las plantillas no incluyen pesos sugeridos.

La revisión de textos no encontró «Crear artículo» en la rama actual. Se unifican los textos del catálogo y se conserva «Categoría de objetivo» donde se refiere al objetivo del alumno, no al grupo muscular. Para ver la actualización en Docker hay que reconstruir las imágenes; los volúmenes con 007 no requieren SQL adicional.

## Verificación

- Backend: **217 pruebas aprobadas sin omisiones**, con MySQL habilitado; incluye descripción opcional, permisos, unicidad e historial.
- Angular: compilación de producción y **121 pruebas unitarias/HTTP aprobadas**. Cubren teclado, reutilización de ejercicios existentes, descripción opcional, movimientos entre bloques y conservación del nombre al cambiar vueltas y descansos.
- Chromium: **57 aprobadas**, con Angular en configuración de producción: 49 contra Spring Boot/MySQL reales y 8 de interfaz aislada. Regresión ADMIN/PROFESOR/ALUMNO y revisión responsive en 1440/820/360 px.
- Aceptación específica a **390 px desde el inicio**: crear tres ejercicios en el editor (uno con grupo nuevo y sin descripciones), configurar Bloque A con tres vueltas, repeticiones 12/10/15 y descansos; guardar, salir, volver a editar y comprobar exactamente nombre, vueltas, ejercicios, orden y valores. Añadir un cuarto ejercicio, moverlo a otro bloque, guardar versión nueva y verificar que la anterior siga intacta. Asignar a dos alumnos con pesos 60/40 y comprobar agrupación y vueltas en la interfaz del alumno.
- Tres imágenes Docker construidas. La adaptación privada de proxy Cloud queda fuera del repositorio; no se reemplazó el stack Compose existente.

La primera ejecución detectó pérdida del nombre del bloque por leer controles antes de actualizar su valor; se corrigió tomando el valor del evento y sincronizando desde el encabezado único. La aceptación completa volvió a pasar después de la corrección.

No se hizo merge a master. No se agregan migraciones y no se eliminan datos. Los backups, credenciales, informes y capturas quedan fuera de Git.
