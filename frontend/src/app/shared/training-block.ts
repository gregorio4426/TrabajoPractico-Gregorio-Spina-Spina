import { Component, computed, input } from '@angular/core';
import { DiaEjercicioResponse } from '../admin/contracts';
/** Presentation of the immutable prescription; it never derives performed results. */
@Component({
  selector: 'app-training-block',
  template: `@if (row().bloqueOrden != null) {
    <div class="training-block-header">
      <h4>{{ row().bloqueNombre || type() }}{{ row().bloqueNombre ? ' · ' + type() : '' }}</h4>
      <p>
        {{ row().vueltas }} vueltas · Descanso entre vueltas:
        {{
          row().descansoVueltaSegundos == null ? 'Sin indicar' : row().descansoVueltaSegundos + ' s'
        }}
      </p>
    </div>
  }`,
})
export class TrainingBlock {
  readonly row = input.required<DiaEjercicioResponse>();
  readonly exercises = input.required<DiaEjercicioResponse[]>();
  readonly type = computed(() => {
    const count = this.exercises().filter((e) => e.bloqueOrden === this.row().bloqueOrden).length;
    return count === 1 ? 'Series' : count === 2 ? 'Biserie' : 'Circuito';
  });
}
