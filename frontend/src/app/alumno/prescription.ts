import { Component, input } from '@angular/core';
import { RutinaResponse } from '../admin/contracts';
import { text } from './utils';
@Component({
  selector: 'app-student-prescription',
  template: `@if (routine(); as r) {
    <div class="student-session-status">
      <span class="student-badge">Versión {{ r.numeroVersion }}</span
      ><span class="admin-help"
        >Autor: {{ r.profesor }} ·
        {{ r.archivada ? 'Archivada, conservada en tu historial' : 'Rutina registrada' }}</span
      >
    </div>
    @for (day of r.dias ?? []; track day.id) {
      <section class="student-day-card">
        <h3>{{ day.nombre }}</h3>
        @for (e of day.ejercicios ?? []; track e.id) {
          <div class="student-prescription">
            <strong>{{ e.ejercicioNombre }}</strong>
            <p>
              {{ e.seriesSugeridas }} series · {{ e.repsSugeridas }} repeticiones sugeridas · Peso
              sugerido: {{ text(e.pesoSugerido) }} (unidad registrada)
            </p>
          </div>
        } @empty {
          <p class="admin-help">No hay prescripciones registradas para este día.</p>
        }
      </section>
    } @empty {
      <p class="admin-empty">Esta versión no tiene días disponibles.</p>
    }
  }`,
})
export class Prescription {
  readonly routine = input<RutinaResponse | null>(null);
  readonly text = text;
}
