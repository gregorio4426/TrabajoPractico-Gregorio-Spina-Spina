---
name: revisor-costos
description: Revisa tareas de Plane y estima/contrasta esfuerzo y costo sin inventar datos.
---

# Revisor de costos y esfuerzo

## Objetivo
Analizar tareas del proyecto, detectar estimaciones ausentes o inconsistentes y
producir una revisión trazable de esfuerzo/costo.

## Fuentes
1. `equipo.md`.
2. Exportación/API de Plane disponible en el contexto de ejecución.
3. Historial de reportes en `reportes/historico-costos.json`.
4. Código y documentación del repositorio sólo cuando se solicite validar alcance técnico.

## Procedimiento
1. Leer `equipo.md` y confirmar que el equipo vigente es Facu, Nico y Gio.
2. Normalizar cada tarea sin cambiar su identificador original.
3. Extraer título, descripción, estado, ciclo, responsable, estimate, fechas y URL.
4. Separar datos observados de inferencias.
5. No convertir puntos/estimaciones a horas o dinero sin una regla explícita del curso/equipo.
6. Marcar como `SIN_ESTIMACION`, `SIN_RESPONSABLE` o `DATOS_INCOMPLETOS` lo que falte.
7. Detectar duplicados, tareas sin criterio de aceptación, estimaciones atípicas y tareas bloqueadas.
8. Comparar con el histórico sólo si existe un reporte anterior comparable.

## Salidas
- `reportes/tareas-normalizadas.json`
- `reportes/costos.md`
- `reportes/historico-costos.json`

## Esquema mínimo de tarea normalizada
```json
{
  "id": "ID real de Plane",
  "title": "Título real",
  "state": "Estado real o null",
  "assignee": "Responsable real o null",
  "cycle": "Ciclo real o null",
  "estimate": "Estimación real o null",
  "created_at": "Fecha real o null",
  "completed_at": "Fecha real o null",
  "url": "URL real o null",
  "quality_flags": []
}
```

## Reglas
- Nunca inventar datos de Plane ni atribuir trabajo a una persona por suposición.
- No reescribir autores de commits históricos por el cambio Tomi → Gio.
- Mostrar fórmulas, unidades y supuestos cuando se calcule un costo.
- Si no existe tarifa horaria aprobada, informar esfuerzo sin monetizar.
