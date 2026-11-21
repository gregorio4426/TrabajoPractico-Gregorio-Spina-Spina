import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AlumnoApi } from './alumno-api';
import { DiaEjercicioResponse, SesionResponse, SerieResponse } from '../admin/contracts';
import { positiveInteger, optionalWeight } from '../profesor/utils';
import { alumnoError } from './utils';
export function plannedSeries(exercises: DiaEjercicioResponse[]) {
  const numbers = new Map<number, number>();
  return exercises.map((exercise) => {
    const start = numbers.get(exercise.ejercicioId!) ?? 0;
    const count = exercise.vueltas ?? exercise.seriesSugeridas ?? 0;
    numbers.set(exercise.ejercicioId!, start + count);
    return { exercise, numbers: Array.from({ length: count }, (_, i) => start + i + 1) };
  });
}
@Component({
  selector: 'app-workout-grid',
  imports: [ReactiveFormsModule],
  template: `<section class="workout-grid" aria-label="Registro por ejercicio y vuelta">
    @if (error()) {
      <p class="error-message" role="alert">{{ error() }}</p>
    }
    @if (success()) {
      <p class="notice" role="status">{{ success() }}</p>
    }
    @for (group of groups(); track group.exercise.id) {
      <article class="admin-panel">
        <h3>{{ group.exercise.ejercicioNombre }}</h3>
        <p class="student-instruction">
          {{ group.exercise.bloqueNombre || 'Bloque ' + (group.exercise.bloqueOrden ?? 1) }} ·
          {{ group.numbers.length }} vueltas · Plan: {{ group.exercise.repsSugeridas }} repeticiones
          · Peso sugerido:
          {{ group.exercise.pesoSugerido == null ? 'Sin indicar' : group.exercise.pesoSugerido }}
        </p>
        @for (number of group.numbers; track number; let round = $index) {
          @let row = entry(group.exercise, number);
          <form
            class="workout-series-row"
            [class.saved-row]="!!row.saved"
            [formGroup]="row.form"
            (ngSubmit)="save(row)"
          >
            <span>Vuelta {{ round + 1 }}</span>
            <label
              >Repeticiones realizadas<input
                type="number"
                inputmode="numeric"
                min="1"
                step="1"
                formControlName="reps"
                [attr.aria-label]="
                  group.exercise.ejercicioNombre + ' vuelta ' + (round + 1) + ' repeticiones'
                "
            /></label>
            <label
              >Peso realizado<input
                type="number"
                inputmode="decimal"
                min="0"
                step="any"
                formControlName="weight"
                placeholder="Opcional"
                [attr.aria-label]="
                  group.exercise.ejercicioNombre + ' vuelta ' + (round + 1) + ' peso'
                "
            /></label>
            <button
              class="primary"
              [disabled]="
                busy() || disabled() || !!row.saved?.anulada || (!!row.saved && !row.form.dirty)
              "
            >
              {{ row.saved?.anulada ? 'Anulada' : row.saved ? 'Guardar cambio' : 'Guardar serie' }}
            </button>
          </form>
        }
      </article>
    }
    <p class="admin-help">
      Los resultados se guardan por serie. Vacío = peso desconocido; cero = peso conocido igual a
      cero. Las series anuladas se restauran desde el historial inferior.
    </p>
  </section>`,
})
export class WorkoutGrid {
  private readonly api = inject(AlumnoApi);
  private readonly fb = inject(FormBuilder);
  readonly exercises = input.required<DiaEjercicioResponse[]>();
  readonly session = input.required<SesionResponse>();
  readonly disabled = input(false);
  readonly updated = output<void>();
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly groups = computed(() => plannedSeries(this.exercises()));
  private readonly entries = new Map<string, ReturnType<WorkoutGrid['createEntry']>>();
  createEntry(exercise: DiaEjercicioResponse, number: number, saved?: SerieResponse) {
    return {
      exercise,
      number,
      saved,
      form: this.fb.group({
        reps: [
          saved?.repsHechas ?? (null as number | null),
          [Validators.required, positiveInteger],
        ],
        weight: [saved?.pesoUsado ?? (null as number | null), optionalWeight],
      }),
    };
  }
  entry(exercise: DiaEjercicioResponse, number: number) {
    const key = `${this.session().id}:${exercise.id}:${number}`;
    const saved = this.session().series?.find(
      (s) => s.ejercicioId === exercise.ejercicioId && s.numeroSerie === number,
    );
    let row = this.entries.get(key);
    if (!row) {
      row = this.createEntry(exercise, number, saved);
      this.entries.set(key, row);
    } else if (saved && !row.form.dirty && saved.revision !== row.saved?.revision) {
      row.form.patchValue({ reps: saved?.repsHechas ?? null, weight: saved?.pesoUsado ?? null });
      row.saved = saved;
    }
    return row;
  }
  async save(row: ReturnType<WorkoutGrid['createEntry']>) {
    if (this.busy() || this.disabled() || this.session().finalizada || row.saved?.anulada) return;
    row.form.markAllAsTouched();
    if (row.form.invalid) {
      this.error.set('Ingresá repeticiones enteras positivas y un peso opcional no negativo.');
      return;
    }
    const v = row.form.getRawValue();
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    try {
      const result = await firstValueFrom(
        row.saved
          ? this.api.correctSeries(this.session().id!, row.saved.id!, {
              numeroSerie: row.number,
              repsHechas: Number(v.reps),
              pesoUsado: v.weight == null ? null : Number(v.weight),
              revision: row.saved.revision!,
              motivo: 'Corrección de resultados desde la tabla de entrenamiento.',
            })
          : this.api.addSeries(this.session().id!, {
              diaEjercicioId: row.exercise.id!,
              numeroSerie: row.number,
              repsHechas: Number(v.reps),
              pesoUsado: v.weight == null ? null : Number(v.weight),
            }),
      );
      row.saved = result;
      row.form.markAsPristine();
      this.success.set('Serie guardada.');
      this.updated.emit();
    } catch (e) {
      this.error.set(
        alumnoError(e) + ' No se reintentó la escritura. Recargá para comprobar el estado.',
      );
    } finally {
      this.busy.set(false);
    }
  }
}
