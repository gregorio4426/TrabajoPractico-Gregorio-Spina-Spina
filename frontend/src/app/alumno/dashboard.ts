import { statisticsPeriodError } from '../shared/statistics-period';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AlumnoApi } from './alumno-api';
import * as D from '../admin/contracts';
import { StudentProgress } from './progress';
import { alumnoError, isClosed, text } from './utils';
@Component({
  selector: 'app-student-dashboard',
  imports: [RouterLink, StudentProgress],
  template: `<section class="page-intro">
      <span class="eyebrow muted">MI ENTRENAMIENTO</span>
      <h1>Tu próximo paso empieza acá<span class="accent">.</span></h1>
      <p>Entrená a tu ritmo y llevá el registro real de cada avance.</p>
    </section>

    @if (loading()) {
      <p class="notice" role="status">Consultando tus datos…</p>
    }
    @if (error()) {
      <p class="error-message" role="alert">
        {{ error() }} <button class="secondary" (click)="load()">Reintentar</button>
      </p>
    }
    @if (data(); as d) {
      <section class="student-welcome">
        <div>
          <span class="eyebrow">TU RUTINA ACTUAL</span>
          <h2>{{ d.rutinaActiva?.rutinaNombre ?? 'Todavía no tenés rutina activa' }}</h2>
          <p>
            {{
              d.rutinaActiva
                ? 'Tu plan está listo. Vamos a entrenar.'
                : 'Elegí un profesor para organizar tu entrenamiento.'
            }}
          </p>
          <a class="primary" routerLink="/alumno/rutina">Ver mi rutina y entrenar</a>
        </div>
        <span class="student-welcome-mark" aria-hidden="true">01</span>
      </section>
      @if (open().length) {
        <section class="admin-panel">
          <h2>Retomá tu entrenamiento</h2>
          <p class="admin-help">Continuá desde donde quedaste.</p>
          <div class="teacher-history-list">
            @for (s of open(); track s.id) {
              <div>
                <strong>{{ s.rutinaNombre }} · {{ s.diaRutinaNombre }}</strong>
                <p>
                  {{ s.fechaEfectiva ?? s.fecha }} · {{ s.seriesRealizadas }} series registradas
                </p>
                <a class="primary" [routerLink]="['/alumno/sesiones', s.id]"
                  >Continuar {{ s.diaRutinaNombre }} · {{ s.fechaEfectiva ?? s.fecha }}</a
                >
              </div>
            }
          </div>
        </section>
      }

      <app-student-progress [period]="period()" [showDates]="false" [compact]="true" />
    }`,
})
export class StudentDashboard {
  private readonly api = inject(AlumnoApi);
  readonly data = signal<D.DashboardAlumno | null>(null);
  readonly open = signal<D.SesionResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly period = signal<{ desde: string; hasta: string } | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({ desde: '', hasta: '' });
  readonly text = text;
  private seq = 0;
  constructor() {
    void this.load();
  }
  async load() {
    const query = this.form.getRawValue();
    const periodError = statisticsPeriodError(query.desde, query.hasta);
    if (periodError) {
      this.error.set(periodError);
      return;
    }
    const seq = ++this.seq;
    this.data.set(null);
    this.open.set([]);
    this.loading.set(true);
    this.error.set('');
    try {
      const [data, sessions] = await Promise.all([
        firstValueFrom(this.api.dashboard(query)),
        firstValueFrom(this.api.sessions()),
      ]);
      if (seq !== this.seq) return;
      this.period.set({
        desde: data.periodo?.desde ?? query.desde,
        hasta: data.periodo?.hasta ?? query.hasta,
      });
      this.open.set(sessions.filter((s) => !isClosed(s)));
      this.data.set(data);
    } catch (e) {
      if (seq === this.seq) this.error.set(alumnoError(e));
    } finally {
      if (seq === this.seq) this.loading.set(false);
    }
  }
}
