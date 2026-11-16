# Revisión de costos y esfuerzo — semana 2026-W46 (9 al 15 de noviembre de 2026)

Skill: `revisor-costos` · Fuente: export CSV de Plane (proyecto IMP) + `equipo.md` · Generado: 2026-11-15

## 1. Resumen
| Indicador | Valor | Tipo |
|---|---|---|
| Tareas en el export | 13 (IMP-2 a IMP-14; IMP-1 fue eliminado por el equipo) | Observado |
| Estado | 13 Done · 0 pendientes | Observado |
| Story points totales / terminados | 87 / 87 | Observado |
| Tareas con responsable asignado | 13 de 13 (87 pts de 87) | Observado |
| Tareas SIN_RESPONSABLE | 0 (0 pts) | Observado |
| Horas registradas en Plane | 0 h (campo Total Time Logged = 0h 0m) | Observado |
| Costo de infraestructura | USD 0 | Informado por el equipo |

## 2. Tarifa y supuestos (reglas explícitas del curso)
- Tarifa: USD 3.000 por mes por integrante y 160 h al mes (diapositiva 7 del material del curso).
- Costo por hora = 3.000 ÷ 160 = **USD 18,75**.
- Costo semanal de referencia por integrante = 3.000 × 12 ÷ 52 = **USD 692,31**.
- Dedicación semanal declarada por el equipo: Facu 8 h, Nico 15 h, Giorgio 6 h (total 29 h).
- Los puntos de historia **no** se convierten a horas ni a dinero: no hay una regla del curso para esa conversión.

## 3. Costo cargado (dedicación declarada)
Fórmula: horas por semana × USD 18,75.

| Integrante | Horas/sem | Costo semanal (USD) |
|---|---|---|
| Facu | 8 | 150,00 |
| Nico | 15 | 281,25 |
| Giorgio | 6 | 112,50 |
| **Equipo** | **29** | **543,75** |

## 4. Capacidad no utilizada (referencia contractual)
- Capacidad de referencia: 3 integrantes × USD 692,31 = **USD 2.076,92 por semana** (≈ 36.9 h por persona por semana).
- Capacidad no utilizada = 2.076,92 − 543,75 = **USD 1.533,17 por semana** (74 % de la referencia).
- Supuesto: se compara contra una dedicación completa que el TP no exige. Es un dato de referencia, no un desperdicio real.

## 5. Costo de lo terminado vs. costo cargado
**No calculable de forma confiable.** Las 13 tareas figuran como Done, pero:
- no hay horas registradas por tarea;
- todas fueron creadas y cerradas en Plane el 15/11/2026 entre las 20:56 y las 21:06 (carga retroactiva), mientras que sus fechas de inicio/cierre van de 2026-05-19 a 2026-11-10;
- por lo tanto no se pueden atribuir los 87 puntos a una semana concreta.
Referencia puramente aritmética (no usar para decidir): 543,75 ÷ 87 pts = USD 6,25 por punto, que supone que todo el trabajo se hizo en una semana, algo que los datos contradicen.

## 6. Calidad de datos (por tarea, en `tareas-normalizadas.json`)
| Marca | Tareas | Detalle |
|---|---|---|
| SIN_RESPONSABLE | 0 | Todas las tareas tienen responsable (Facu 16 pts, Nico 50 pts, Giorgio 21 pts). Las asignaciones se cargaron el 15/11, sin historial |
| SIN_DESCRIPCION / SIN_CRITERIOS_ACEPTACION | 13 | Ninguna tarea tiene descripción |
| SIN_MODULO / SIN_PRIORIDAD | 13 | Sin módulo ni prioridad. Etiquetas cargadas en las 13 tareas (backend, frontend, seguridad) |
| ESTIMACION_ALTA (13 pts) | 2 | IMP-10 y IMP-12 |
| COMPLETADA_AL_CREAR | 13 | Creadas y cerradas en el mismo instante |
| SIN_HORAS_REGISTRADAS | 13 | No hay desvío de estimación calculable |
| SIN_ESTIMACION | 0 | Todas tienen puntos |
- Duplicados: no se detectaron.
- Bloqueos: ninguno registrado (no hay relaciones ni comentarios).
- Ciclo: Plane exporta "Sprint 1" del **2026-11-09 al 2026-11-23**; `equipo.md` dice del 9 al 22 de noviembre (1 día de diferencia, posiblemente fin de ciclo inclusivo vs. exclusivo).
- Comparación con histórico: no existe reporte anterior comparable (primera corrida).

## 7. Hallazgos
Formato: qué pasó → cuánto costó → qué hacer → ahorro por semana.

1. **Responsables cargados en las 13 tareas** (Facu 16 pts, Nico 50 pts, Giorgio 21 pts de 87). → Ahora el esfuerzo puede atribuirse por tarea, pero las asignaciones se hicieron retroactivamente el 15/11 y no hay horas registradas: no se calcula costo ni productividad por persona. → IMP-6 a IMP-9 (junio de 2026) están asignadas a Giorgio, mientras que en git esos commits son de Tomás; `equipo.md` indica que no se reasignan, así que conviene aclararlo en la entrega. → Ahorro: USD 0, mejora la trazabilidad.
2. **Tablero cargado retroactivamente.** → Velocidad, desvío y costo por punto no son confiables esta semana. → Desde ahora crear el ticket antes de codear y moverlo de estado cuando cambia el trabajo. → Ahorro: no cuantificable con estos datos.
3. **Capacidad de referencia sin usar: USD 1.533,17 por semana.** → Es costo de referencia, no gasto real. → Confirmar con el profesor si el TP evalúa contra dedicación completa; si no, ignorar. → Ahorro: n/a.
4. **Dos tareas de 13 pts (IMP-10 e IMP-12).** → Riesgo de estimación; IMP-12 mezcla dos paneles (ADMIN y PROFESOR). → Dividir en tareas de hasta 8 pts en lo que quede por cargar. → Ahorro: n/a.
5. **Sin horas registradas.** → No hay desvío de estimación. → Si el curso lo pide, activar time tracking en el proyecto (hoy `is_time_tracking_enabled=false`). → Ahorro: n/a.
6. **Infraestructura: USD 0.** → No hay infra paga con alternativa gratuita para recortar. → Mantener MySQL local, GitHub y Plane en plan gratuito.

## 8. Límites del análisis
Muestra de 1 semana, 13 tareas y 3 personas. No se infiere productividad individual.
