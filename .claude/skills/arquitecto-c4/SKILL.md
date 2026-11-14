---
name: arquitecto-c4
description: Analiza cambios del backend Spring Boot y frontend Angular y mantiene diagramas C4.
---

# Arquitecto C4

## Objetivo
Mantener una vista C4 verificable de Impulso y actualizarla a partir de cambios
reales en el código, evitando regenerar diagramas sin necesidad.

## Alcance técnico observado
- Backend: aplicación Spring Boot 3.4.5 / Java 21; API REST, Spring Security con
  JWT, Spring Data JPA/Hibernate y MySQL.
- Frontend: Angular 22; aplicación standalone con rutas lazy-loaded por rol,
  servicios HTTP y autenticación JWT.
- Roles del producto: ADMIN, PROFESOR y ALUMNO.

## Procedimiento inicial
1. Inspeccionar estructura y configuración del backend y frontend.
2. Identificar actores, límites del sistema, contenedores, componentes y relaciones.
3. Crear/actualizar los diagramas C4 en `docs/arquitectura/`.
4. Guardar el último commit analizado en `docs/arquitectura/estado-arquitectura.json`.
5. Si no hay `.git` o no se puede obtener el commit, registrar `commit: null` y
   explicar que el análisis se basó en el snapshot del código.
6. En ejecuciones siguientes, comparar `git diff` desde el commit registrado.
7. Actualizar sólo diagramas afectados y agregar una entrada al changelog.
8. Validar sintaxis Mermaid y que cada elemento tenga respaldo en código/configuración.

## Archivos de salida
- `docs/arquitectura/c4-contexto.mmd`
- `docs/arquitectura/c4-contenedores.mmd`
- `docs/arquitectura/c4-componentes-backend.mmd`
- `docs/arquitectura/c4-componentes-frontend.mmd`
- `docs/arquitectura/estado-arquitectura.json`
- `docs/arquitectura/CHANGELOG.md`

## Reglas de evidencia
- No dibujar servicios externos, colas, microservicios o infraestructura no comprobada.
- No inferir despliegue productivo a partir de configuración local.
- No confundir el repositorio Git con el sistema desplegado.
- Registrar incertidumbres y elementos no observados.
- Mantener al menos dos actualizaciones reales posteriores para demostrar evolución;
  no fabricar cambios ni commits para cumplir el requisito.
