import { volumeCoverage } from '../shared/statistics-values';
import { statisticsPeriodError } from '../shared/statistics-period';
import { PROGRESS_SOURCE, PROGRESS_ERROR } from './progress-source';
import {
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { StatPage } from './profesor-api';
import * as D from '../admin/contracts';
import { Chart, ChartPoint } from './chart';
import { isDenied, text } from './utils';
@Component({
  selector: 'app-teacher-progress',
  imports: [ReactiveFormsModule, Chart],
  templateUrl: './progress.html',
})
export class Progress {
  private readonly api = inject(PROGRESS_SOURCE);
  private readonly failure = inject(PROGRESS_ERROR);
  readonly studentId = input<number | null>(null);
  readonly period = input<{ desde: string; hasta: string } | null>(null);
  readonly showDates = input(true);
  readonly sessionBase = input('/profesor/sesiones');
  readonly denied = output<void>();
  readonly form = inject(FormBuilder).nonNullable.group({ desde: '', hasta: '' });
  readonly text = text;
  readonly volumeCoverage = volumeCoverage;
  readonly summary = signal<D.ResumenEstadistico | null>(null);
  readonly activity = signal<StatPage<D.ActividadDia> | null>(null);
  readonly exercises = signal<StatPage<D.EjercicioEstadistica> | null>(null);
  readonly evolution = signal<StatPage<D.EvolucionEjercicio> | null>(null);
  readonly selected = signal<number | null>(null);
  private readonly pending = signal(new Set<string>());
  readonly loading = computed(() => this.pending().size > 0);
  private activitySeq = 0;
  private exerciseSeq = 0;
  private busy(key: string, value: boolean) {
    this.pending.update((current) => {
      const next = new Set(current);
      if (value) next.add(key);
      else next.delete(key);
      return next;
    });
  }
  readonly error = signal('');
  private seq = 0;
  private evoSeq = 0;
  private applied = { desde: '', hasta: '' };
  constructor() {
    effect(() => {
      const id = this.studentId(),
        period = this.period();
      untracked(() => {
        // A scope change must clear earlier protected results even when the next filter is invalid.
        ++this.seq;
        ++this.evoSeq;
        this.pending.set(new Set());
        this.summary.set(null);
        this.activity.set(null);
        this.exercises.set(null);
        this.evolution.set(null);
        this.selected.set(null);
        if (period) this.form.setValue(period);
        void this.load(id);
      });
    });
  }
  async load(id = this.studentId()) {
    const query = this.form.getRawValue();
    const periodError = statisticsPeriodError(query.desde, query.hasta);
    if (periodError) {
      this.error.set(periodError);
      return;
    }
    const seq = ++this.seq;
    ++this.evoSeq;
    this.pending.set(new Set(['load']));
    this.error.set('');
    this.summary.set(null);
    this.activity.set(null);
    this.exercises.set(null);
    this.evolution.set(null);
    this.selected.set(null);
    try {
      const [summary, activity, exercises] = await Promise.all([
        firstValueFrom(this.api.summary(id, query)),
        firstValueFrom(this.api.activity(id, { ...query, pagina: 0, tamanio: 30 })),
        firstValueFrom(this.api.exerciseStats(id, { ...query, pagina: 0, tamanio: 20 })),
      ]);
      if (seq !== this.seq) return;
      this.applied = {
        desde: summary.periodo?.desde ?? query.desde,
        hasta: summary.periodo?.hasta ?? query.hasta,
      };
      this.summary.set(summary);
      this.activity.set(activity);
      this.exercises.set(exercises);
    } catch (e) {
      if (seq === this.seq) {
        this.error.set(this.failure(e));
        if (isDenied(e)) this.denied.emit();
      }
    } finally {
      if (seq === this.seq) this.busy('load', false);
    }
  }
  async activityPage(page: number) {
    this.busy('activity', true);
    this.error.set('');
    const seq = this.seq,
      request = ++this.activitySeq;
    try {
      const value = await firstValueFrom(
        this.api.activity(this.studentId(), { ...this.applied, pagina: page, tamanio: 30 }),
      );
      if (seq === this.seq && request === this.activitySeq) this.activity.set(value);
    } catch (e) {
      if (seq === this.seq && request === this.activitySeq) this.handle(e);
    } finally {
      if (seq === this.seq && request === this.activitySeq) this.busy('activity', false);
    }
  }
  async exercisePage(page: number) {
    this.busy('exercise', true);
    this.error.set('');
    const seq = this.seq,
      request = ++this.exerciseSeq;
    try {
      const value = await firstValueFrom(
        this.api.exerciseStats(this.studentId(), { ...this.applied, pagina: page, tamanio: 20 }),
      );
      if (seq === this.seq && request === this.exerciseSeq) {
        this.exercises.set(value);
        this.selected.set(null);
        this.evolution.set(null);
        ++this.evoSeq;
        this.busy('evolution', false);
      }
    } catch (e) {
      if (seq === this.seq && request === this.exerciseSeq) this.handle(e);
    } finally {
      if (seq === this.seq && request === this.exerciseSeq) this.busy('exercise', false);
    }
  }
  async chooseExercise(id: number, page = 0) {
    this.selected.set(id);
    this.evolution.set(null);
    this.busy('evolution', true);
    this.error.set('');
    const seq = ++this.evoSeq;
    try {
      const value = await firstValueFrom(
        this.api.evolution(this.studentId(), id, { ...this.applied, pagina: page, tamanio: 30 }),
      );
      if (seq === this.evoSeq) this.evolution.set(value);
    } catch (e) {
      if (seq === this.evoSeq) this.handle(e);
    } finally {
      if (seq === this.evoSeq) this.busy('evolution', false);
    }
  }
  private handle(e: unknown) {
    this.error.set(this.failure(e));
    if (isDenied(e)) {
      ++this.seq;
      ++this.evoSeq;
      this.summary.set(null);
      this.activity.set(null);
      this.exercises.set(null);
      this.evolution.set(null);
      this.pending.set(new Set());
      this.selected.set(null);
      this.denied.emit();
    }
  }
  activityPoints(): ChartPoint[] {
    return (this.activity()?.contenido ?? []).map((p) => ({
      label: p.fecha ?? 'Sin fecha',
      value: p.datos?.entrenamientosConDatosValidos,
      note: `Sesiones abiertas: ${text(p.datos?.sesionesAbiertas)} · Finalizadas sin datos válidos: ${text(p.datos?.sesionesFinalizadasSinDatosValidos)}`,
    }));
  }
  frequencyPoints(): ChartPoint[] {
    const f = this.summary()?.frecuencia;
    return f
      ? [
          { label: 'Entrenamientos por semana', value: f.entrenamientosPorSemana },
          { label: 'Días entrenados por semana', value: f.diasEntrenadosPorSemana },
        ]
      : [];
  }
  evolutionPoints(
    metric: 'pesoMaximo' | 'repeticionesTotales' | 'volumenRegistrado',
  ): ChartPoint[] {
    return (this.evolution()?.contenido ?? []).map((p) => ({
      label: `${p.fechaEfectiva} · sesión #${p.sesionId} · alumno #${p.alumnoId} · ${p.nombreHistorico}`,
      value: p.datos?.[metric],
      link: p.sesionId ? `${this.sessionBase()}/${p.sesionId}` : undefined,
      note: [
        p.fechaOriginal && p.fechaOriginal !== p.fechaEfectiva
          ? `Fecha original: ${p.fechaOriginal}`
          : '',
        metric === 'volumenRegistrado' ? volumeCoverage(p.datos) : '',
      ]
        .filter(Boolean)
        .join(' · '),
    }));
  }
}
