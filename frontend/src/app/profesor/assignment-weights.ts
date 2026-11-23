import { Component, effect, inject, input } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule } from '@angular/forms';
import * as D from '../admin/contracts';
import { isTimed } from '../shared/measurement';
import { optionalPositiveInteger, optionalWeight } from './utils';
@Component({
  selector: 'app-assignment-weights',
  imports: [ReactiveFormsModule],
  template: ` <section class="admin-panel" [formGroup]="form">
    <h3>Personalizar para este alumno</h3>
    <p class="admin-help">
      Son opcionales y pertenecen solo a esta asignación. Podés dejar los campos vacíos para usar la referencia de la rutina. No modifican la plantilla ni otros alumnos.
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
          @if (names[i]?.timed) { <label>Tiempo sugerido (segundos)<input type="number" min="1" step="1" formControlName="segundosSugeridos" placeholder="Usar referencia de la rutina" /></label> }
          @else { <label>Repeticiones sugeridas<input type="number" min="1" step="1" formControlName="repsSugeridas" placeholder="Usar referencia de la rutina" /></label> }
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
  names: Array<{ day: string; exercise: string; timed: boolean }> = [];
  row(id: number, peso: number | null, reps: number|null = null, seconds: number|null = null) {
    return this.fb.group({ diaEjercicioId: [id], pesoSugerido: [peso, optionalWeight], repsSugeridas:[reps, optionalPositiveInteger], segundosSugeridos:[seconds, optionalPositiveInteger] });
  }
  constructor() {
    effect(() => {
      const r = this.routine(),
        existing = this.existing();
      this.rows.clear();
      this.names = [];
      for (const day of r.dias ?? [])
        for (const e of day.ejercicios ?? []) {
          this.names.push({ day: day.nombre ?? 'Día', exercise: e.ejercicioNombre ?? 'Ejercicio', timed: isTimed(e) });
          this.rows.push(this.row(e.id!, existing ? (e.pesoSugerido ?? null) : null, existing ? e.repsPersonalizadas ?? null : null, existing ? e.segundosPersonalizados ?? null : null));
        }
    });
  }
  reset() {
    for (const row of this.rows.controls) { row.controls.pesoSugerido.reset(null); row.controls.repsSugeridas.reset(null); row.controls.segundosSugeridos.reset(null); }
  }
  payload() {
    return {
      pesos: this.rows.getRawValue().map((r) => ({
        diaEjercicioId: Number(r.diaEjercicioId),
        ...(r.repsSugeridas == null ? {} : {repsSugeridas:Number(r.repsSugeridas)}),
        ...(r.segundosSugeridos == null ? {} : {segundosSugeridos:Number(r.segundosSugeridos)}),
        pesoSugerido:
          r.pesoSugerido === null || String(r.pesoSugerido) === '' ? null : Number(r.pesoSugerido),
      })),
      revision: this.routine().asignacionRevision ?? null,
    };
  }
}
