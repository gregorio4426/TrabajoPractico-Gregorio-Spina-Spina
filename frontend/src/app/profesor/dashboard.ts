import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ProfesorApi } from './profesor-api';
import { DashboardProfesor } from '../admin/contracts';
import { profesorError, text, validDates } from './utils';
import { Progress } from './progress';
@Component({
  selector: 'app-teacher-dashboard',
  imports: [RouterLink, ReactiveFormsModule, Progress],
  template: `<section class="page-intro">
      <span class="eyebrow muted">ESPACIO DEL PROFESOR</span>
      <h1>Acompañá cada avance<span class="accent">.</span></h1>
      <p>Tu equipo, tus planes y el entrenamiento registrado por tus alumnos.</p>
    </section>
    <form class="admin-filters" [formGroup]="form" (ngSubmit)="load()">
      <label>Desde<input type="date" formControlName="desde" /></label
      ><label>Hasta<input type="date" formControlName="hasta" /></label
      ><button class="primary" [disabled]="loading()">Actualizar dashboard</button>
    </form>
    @if (loading()) {
      <p class="notice" role="status">Cargando tu dashboard…</p>
    }
    @if (error()) {
      <p class="error-message" role="alert">
        {{ error() }} <button class="secondary" (click)="load()">Reintentar</button>
      </p>
    }
    @if (data(); as d) {
      <section class="admin-metrics" aria-label="Estado actual del profesor">
        <a routerLink="/profesor/alumnos"
          ><span>Alumnos vinculados</span><strong>{{ text(d.alumnosVinculados) }}</strong
          ><small>Vínculos actuales</small></a
        ><a routerLink="/profesor/rutinas"
          ><span>Rutinas propias</span><strong>{{ text(d.rutinasPropias) }}</strong
          ><small>{{ text(d.rutinasPropiasArchivadas) }} archivadas</small></a
        ><a routerLink="/profesor/alumnos"
          ><span>Asignaciones activas de tus alumnos</span
          ><strong>{{ text(d.asignacionesActivasDeSusAlumnos) }}</strong
          ><small>Incluye cualquier autor</small></a
        ><a routerLink="/profesor/progreso"
          ><span>Entrenamientos con datos válidos</span
          ><strong>{{ text(d.entrenamientos?.entrenamientosConDatosValidos) }}</strong
          ><small>{{ d.periodo?.desde }} → {{ d.periodo?.hasta }}</small></a
        >
      </section>
      <p class="admin-help">
        Vínculos, rutinas y asignaciones son recuentos actuales. Las estadísticas corresponden al
        período informado, sin inferir asistencia.
      </p>
    }
    @if (data()) {
      <app-teacher-progress [period]="period()" [showDates]="false" />
    }`,
})
export class TeacherDashboard {
  private readonly api = inject(ProfesorApi);
  readonly data = signal<DashboardProfesor | null>(null);
  readonly error = signal('');
  readonly loading = signal(false);
  readonly text = text;
  readonly period = signal<{ desde: string; hasta: string } | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({ desde: '', hasta: '' });
  private seq = 0;
  constructor() {
    void this.load();
  }
  async load() {
    const query = this.form.getRawValue();
    if (!validDates(query.desde, query.hasta)) {
      this.error.set('La fecha desde debe ser anterior o igual a hasta.');
      return;
    }
    const seq = ++this.seq;
    this.loading.set(true);
    this.error.set('');
    this.data.set(null);
    try {
      const data = await firstValueFrom(this.api.dashboard(query));
      if (seq === this.seq) {
        this.period.set({
          desde: data.periodo?.desde ?? query.desde,
          hasta: data.periodo?.hasta ?? query.hasta,
        });
        this.data.set(data);
      }
    } catch (e) {
      if (seq === this.seq) this.error.set(profesorError(e));
    } finally {
      if (seq === this.seq) this.loading.set(false);
    }
  }
}
