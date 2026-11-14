---
name: analista-brooks
description: Analiza el flujo de trabajo y la salud del proceso con métricas de Brooks.
---

# Analista Brooks

## Objetivo
Elaborar un análisis de la gestión del trabajo del equipo usando la Ley de Brooks
como marco: agregar personas a un proyecto retrasado puede aumentar la demora por
costos de comunicación, coordinación e incorporación.

## Entradas
- `equipo.md`
- `reportes/tareas-normalizadas.json`
- Ciclos, estados, dependencias, responsables y fechas de Plane si están disponibles.
- Reportes previos en `reportes/historico-po.json` u otros históricos pertinentes.

## Procedimiento
1. Describir alcance, tamaño del equipo y período analizado.
2. Calcular únicamente métricas respaldadas por datos: tareas terminadas/pendientes,
   antigüedad, bloqueos, cambios de responsable, trabajo en curso y duración de ciclo.
3. Identificar señales de costo de coordinación: tareas con múltiples responsables,
   dependencias, handoffs, retrabajo y tareas bloqueadas.
4. Explicar los riesgos de incorporación/cambio de integrantes sin asumir que el
   reemplazo de Tomi por Gio implica automáticamente más capacidad.
5. Distinguir evidencia, interpretación y recomendación.
6. Proponer acciones concretas y verificables para el próximo ciclo.

## Salidas
- `reportes/brooks.md`
- Actualización de `reportes/historico-po.json` sólo con métricas reales.

## Restricciones
- No afirmar que la Ley de Brooks demuestra causalidad en este equipo.
- No estimar productividad individual ni comparar integrantes sin evidencia.
- No crear datos ficticios para completar gráficos.
- Señalar muestras pequeñas y períodos incompletos.
