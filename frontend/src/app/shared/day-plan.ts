import { suggested, unitLabel } from './measurement';
import { Component, computed, input, output } from '@angular/core';
import * as D from '../admin/contracts';
import { text } from '../admin/admin-utils';
export function prescriptionBlocks(exercises: D.DiaEjercicioResponse[]) {
  const groups: Array<{
    key: string;
    name: string;
    rounds: number | null | undefined;
    rest: number | null | undefined;
    entries: Array<{ exercise: D.DiaEjercicioResponse; index: number }>;
  }> = [];
  exercises.forEach((exercise, index) => {
    const key = exercise.bloqueOrden == null ? 'single:' + index : 'block:' + exercise.bloqueOrden;
    let block = groups.find((b) => b.key === key);
    if (!block) {
      block = {
        key,
        name: exercise.bloqueNombre ?? '',
        rounds: exercise.vueltas ?? exercise.seriesSugeridas,
        rest: exercise.descansoVueltaSegundos,
        entries: [],
      };
      groups.push(block);
    }
    block.entries.push({ exercise, index });
  });
  return groups;
}
@Component({
  selector: 'app-day-plan',
  template: ` @for (block of blocks(); track block.key; let bi = $index) {
      <section class="routine-block" [attr.aria-label]="block.name || 'Bloque ' + (bi + 1)">
        <h4>{{ block.name || 'Bloque ' + (bi + 1) }} · {{ block.rounds ?? '' }} vueltas</h4>
        @if (block.entries.length > 1) {
          <p class="admin-help">
            Una vuelta incluye todos los ejercicios de este bloque, en este orden.
          </p>
        }
        @if (block.rest != null) {
          <p class="admin-help">Descanso entre vueltas: {{ block.rest }} s</p>
        }
        @for (entry of block.entries; track entry.index) {
          <div class="student-prescription">
            <strong>{{ entry.exercise.ejercicioNombre }}</strong>
            @if (suggested(entry.exercise) != null) {
              <p>
                {{ suggested(entry.exercise) }}
                {{ suggested(entry.exercise) == null ? '' : unitLabel(entry.exercise) }}
                @if (block.entries.length > 1) {
                  por vuelta
                }
              </p>
            }
            @if (showWeights() && entry.exercise.pesoSugerido != null) {
              <p>Peso sugerido: {{ text(entry.exercise.pesoSugerido) }}</p>
            }
            @if (entry.exercise.descansoEjercicioSegundos != null) {
              <p>
                Descanso después del ejercicio: {{ entry.exercise.descansoEjercicioSegundos }} s
              </p>
            }
            @if (selectable()) {
              <button
                type="button"
                class="primary"
                [disabled]="disabled()"
                (click)="chosen.emit(entry.exercise)"
              >
                Registrar {{ entry.exercise.ejercicioNombre }} · posición {{ entry.index + 1 }}
              </button>
            }
          </div>
        }
      </section>
    } @empty {
      <p class="admin-empty">No hay ejercicios registrados para este día.</p>
    }`,
})
export class DayPlan {
  readonly suggested = suggested;
  readonly unitLabel = unitLabel;
  readonly exercises = input<D.DiaEjercicioResponse[]>([]);
  readonly showWeights = input(false);
  readonly selectable = input(false);
  readonly disabled = input(false);
  readonly chosen = output<D.DiaEjercicioResponse>();
  readonly text = text;
  readonly blocks = computed(() => prescriptionBlocks(this.exercises()));
}
