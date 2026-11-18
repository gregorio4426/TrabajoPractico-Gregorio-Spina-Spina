import { Component, effect, inject, input } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule } from '@angular/forms';
import * as D from '../admin/contracts';
import { optionalWeight } from './utils';
@Component({
  selector: 'app-assignment-weights',
  imports: [ReactiveFormsModule],
  template: ` <section class="admin-panel" [formGroup]="form">
    <h3>Pesos individuales del alumno</h3>
    <p class="admin-help">
      Son opcionales y pertenecen solo a esta asignación. Vacío significa sin peso sugerido; cero es
      un peso conocido. No modifican la plantilla ni otros alumnos.
    </p>
    <div formArrayName="pesos">
      @for (row of rows.controls; track row.controls.diaEjercicioId.value; let i = $index) {
        <div
          class="admin-form"
          [formGroupName]="i"
          role="group"
          [attr.aria-label]="names[i]?.day + ' · ' + names[i]?.exercise + ' · posición ' + (i + 1)"
        >
          <p>{{ names[i]?.day }} · {{ names[i]?.exercise }}</p>
          <label
            >Peso sugerido<input type="number" min="0" step="any" formControlName="pesoSugerido"
          /></label>
        </div>
      } @empty {
        <p>No hay ejercicios para configurar.</p>
      }
    </div>
  </section>`,
})
export class AssignmentWeights {
  private readonly fb = inject(FormBuilder);
  readonly routine = input.required<D.RutinaResponse>();
  readonly existing = input(false);
  readonly rows = new FormArray<ReturnType<AssignmentWeights['row']>>([]);
  readonly form = this.fb.group({ pesos: this.rows });
  names: Array<{ day: string; exercise: string }> = [];
  row(id: number, peso: number | null) {
    return this.fb.group({ diaEjercicioId: [id], pesoSugerido: [peso, optionalWeight] });
  }
  constructor() {
    effect(() => {
      const r = this.routine(),
        existing = this.existing();
      this.rows.clear();
      this.names = [];
      for (const day of r.dias ?? [])
        for (const e of day.ejercicios ?? []) {
          this.names.push({ day: day.nombre ?? 'Día', exercise: e.ejercicioNombre ?? 'Ejercicio' });
          this.rows.push(this.row(e.id!, existing ? (e.pesoSugerido ?? null) : null));
        }
    });
  }
  reset() {
    for (const row of this.rows.controls) row.controls.pesoSugerido.reset(null);
  }
  payload() {
    return {
      pesos: this.rows.getRawValue().map((r) => ({
        diaEjercicioId: Number(r.diaEjercicioId),
        pesoSugerido:
          r.pesoSugerido === null || String(r.pesoSugerido) === '' ? null : Number(r.pesoSugerido),
      })),
      revision: this.routine().asignacionRevision ?? null,
    };
  }
}
