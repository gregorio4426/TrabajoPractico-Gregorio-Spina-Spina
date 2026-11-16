# Reporte del Product Owner — semana 2026-W46

Skill: `product-owner` (con costos y Brooks) · Fuente: Plane + `equipo.md` · Generado: 2026-11-15 · Versión gráfica: `reporte-po.html`

## Semáforo de entrega: VERDE (formal, no concluyente)
- Cálculo: puntos pendientes ÷ velocidad promedio, contra las semanas que quedan.
- Puntos pendientes = **0**; semanas hasta la entrega (23/11/2026) ≈ 1.1.
- Con 0 pendientes el resultado es "En camino", pero **no es concluyente**: el tablero no tiene trabajo pendiente cargado y la velocidad promedio no es calculable (las 13 tareas se cerraron el mismo día por carga retroactiva).
- Para que el semáforo sea útil hay que cargar lo que realmente falta hasta la entrega.

## Resumen
- 13 tareas / 87 pts, todas Done. 13 ítems de 3 a 13 pts.
- Costo semanal por dedicación declarada: USD 543,75 (ver `costos.md`). Infraestructura: USD 0.
- Riesgo principal: bus factor 1 en el código del último mes y tablero cargado retroactivamente (ver `brooks.md`). Responsables: Facu 16 pts, Nico 50 pts, Giorgio 21 pts.

## Alerta de tablero quieto
- **3 días hábiles sin cambios de estado:** No dispara hoy: último cambio el 15/11 (domingo). Dispara el miércoles 18/11 si no hay movimientos.
- **Tareas en curso sin moverse hace 5+ días:** No aplica: 0 tareas en curso.
- **Menos de 2 terminadas en la semana:** No dispara: 13 tareas terminadas (todas el 15/11, por carga retroactiva).
- **Integrantes sin tareas movidas ni commits:** Los 3 integrantes tienen tareas asignadas, pero las asignaciones son del 15/11 y sin historial. Commits desde 18/10: sólo Nico (según git).

## Backlog por etiqueta (Plane no tiene módulos cargados)
- **backend:** IMP-2 (5 pts), IMP-3 (5 pts), IMP-4 (3 pts), IMP-6 (3 pts), IMP-7 (5 pts), IMP-8 (8 pts), IMP-10 (13 pts) = 42 pts
- **seguridad:** IMP-5 (3 pts), IMP-9 (5 pts) = 8 pts
- **frontend:** IMP-11 (8 pts), IMP-12 (13 pts), IMP-13 (8 pts), IMP-14 (8 pts) = 37 pts

## Revisión de historias
Ninguna tarea tiene descripción ni criterios de aceptación, y todas se redactaron como tareas técnicas, sin la forma "Como [usuario], quiero [capacidad], para [beneficio]". Las de frontend (IMP-11 a IMP-14) y los endpoints (IMP-6, 7, 8) tienen valor de usuario claro; las de base técnica (IMP-2, 3, 4, 5, 9) no explicitan el beneficio.

### Refinamientos propuestos (no se tocó el backlog real)
- **IMP-12 (13 pts)** → separar en "Panel ADMIN" y "Panel PROFESOR". Ejemplo de historia: *Como profesor, quiero ver y editar las rutinas de mis alumnos, para ajustar su entrenamiento.* Criterio: **Dado** un profesor autenticado, **cuando** abre un alumno vinculado, **entonces** ve su rutina activa y puede modificarla.
- **IMP-10 (13 pts)** → dividir por etapa (seguridad, historial, administración, dashboards). Duró 30 días (2026-10-01 a 2026-10-30).
- **IMP-13** → *Como alumno, quiero registrar mi sesión desde el celular, para llevar mi progreso sin papel.* Criterio: **Dado** un alumno con rutina asignada, **cuando** completa una serie en el móvil, **entonces** queda en su historial.

## Prioridad recomendada para lo que falta (criterio: valor, urgencia, riesgo, dependencias)
1. Aclarar la asignación de IMP-6 a IMP-9 (hechas en junio por Tomás, asignadas a Giorgio) para no confundir la evaluación.
2. Cargar el trabajo restante hasta el 23/11 (urgencia alta; sin esto el semáforo no informa).
3. Cerrar documentación y presentación final (valor para la evaluación).
4. Completar descripción y criterios de aceptación en las tareas futuras (valor para el equipo).
Son recomendaciones: las prioridades acordadas con el profesor no figuran en los datos.

## Discrepancias a resolver
- Ciclo "Sprint 1": Plane exporta 9 al 23/11; `equipo.md` dice 9 al 22/11 (1 día de diferencia).
- IMP-6 a IMP-9 (junio de 2026) figuran asignadas a Giorgio; en git esos commits son de Tomás Spina y `equipo.md` indica que no se reasignan. Es una decisión del equipo; el reporte respeta la asignación de Plane y lo señala.

## Límites
Sin feedback de usuarios ni métricas de negocio. 1 semana, 13 tareas. Carga retroactiva.
