# Análisis de Brooks — semana 2026-W46 (9 al 15 de noviembre de 2026)

Skill: `analista-brooks` · Fuentes: Plane (export CSV), `equipo.md` y, sólo para bus factor, el historial de commits del repositorio · Generado: 2026-11-15

## 1. Alcance
- Equipo: 3 personas (Facu, Nico, Giorgio). Giorgio reemplaza a Tomás de forma administrativa; los commits históricos de Tomás no se reasignan.
- Proyecto IMPULSO. Entrega final: **23/11/2026**, a 8 días de esta corrida.
- Datos: 13 tareas de Plane (IMP-2 a IMP-14), 87 pts, todas Done.

## 2. Evidencia (sólo lo respaldado por datos)
| Métrica | Valor |
|---|---|
| Tareas terminadas / pendientes | 13 / 0 |
| Trabajo en curso (WIP) | 0 |
| Tareas bloqueadas, dependencias, cambios de responsable | 0 registrados (sin relaciones ni comentarios) |
| Tareas con múltiples responsables | 0 (cada tarea tiene un único responsable) |
| Handoffs y retrabajo | no medibles: no hay historial de estados |
| Duración del ciclo | Plane: 2026-11-09 a 2026-11-23; `equipo.md`: 9 a 22 de nov |
| Ruta crítica | **no calculable**: Plane no tiene relaciones entre tareas |

## 3. Canales de comunicación
Fórmula: n × (n − 1) ÷ 2.

| Equipo | Personas | Canales |
|---|---|---|
| Actual | 3 | 3 |
| Simulación: sumar 1 persona | 4 | 6 |
| Simulación: sumar 2 personas | 5 | 10 |

Sumar 1 persona duplica los canales (de 3 a 6); sumar 2 los triplica (a 10). Es aritmética pura, no una predicción de demora.

## 4. Bus factor
Fuente distinta de Plane: `git log` del repositorio hasta el commit 9f1b029 (2026-11-10), sin merges y agrupando identidades por email.

| Ventana | Commits | Distribución |
|---|---|---|
| Últimos 28 días (desde 2026-10-18) | 21 | Nico 21 (100 %) |
| Historial completo | 89 | Nico 54 · Tomás 25 · Facu 10 |

- Evidencia: en el último mes todos los commits del repositorio son de una persona. Bus factor observado en el código = **1**.
- Límites: un commit no mide esfuerzo ni valor; los commits de Tomás no se atribuyen a Giorgio, así que **el repositorio no permite medir la contribución de Giorgio**. Puede haber trabajo fuera de commits (diseño, pruebas, documentación, gestión) que acá no aparece.
- En Plane las asignaciones (Nico 50 pts, Giorgio 21 pts, Facu 16 pts) se cargaron el 15/11, sin historial. IMP-6 a IMP-9 (junio) figuran de Giorgio, pero los commits de esa época son de Tomás; no se reasignan (equipo.md).

## 5. Interpretación (no es causalidad)
- Con 8 días hasta la entrega y 0 tareas pendientes cargadas, el tablero no muestra trabajo por delante. Eso puede significar que todo terminó o que falta cargar lo que resta (README final, reportes de gestión, presentación, etc.).
- La Ley de Brooks plantea que sumar gente a un proyecto atrasado lo atrasa más por incorporación y coordinación. **Los datos no muestran un proyecto atrasado**, y esta muestra (3 personas, 1 semana) no permite afirmar que la ley se cumpla en este equipo.
- El reemplazo de Tomás por Giorgio no implica por sí solo más capacidad: la incorporación tiene costo de aprendizaje y Plane no tiene datos para medirlo.

## 6. Recomendaciones para el próximo ciclo
1. No sumar personas antes de la entrega del 23/11: a 8 días, el costo de incorporación supera cualquier beneficio posible.
2. Mantener los responsables al día: asignar cada tarea nueva al crearla (verificable: 0 tareas SIN_RESPONSABLE en el próximo export).
3. Cargar en Plane las tareas que quedan hasta la entrega, con responsable y estimación (verificable: pendientes > 0 y con dueño).
4. Repartir el trabajo restante para que más de una persona haga commits en el repo, y reducir el bus factor de 1 a 2 (verificable con `git log` en 2 semanas).
5. Crear relaciones entre tareas en Plane para poder calcular la ruta crítica.

## 7. Límites del análisis
Muestra pequeña (13 tareas, 3 personas), período de una sola semana y carga retroactiva del tablero. No se estima productividad individual ni se comparan integrantes.
