import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { combineLatest } from 'rxjs';
import { Progress } from '../profesor/progress';
import { PROGRESS_SOURCE, PROGRESS_ERROR } from '../profesor/progress-source';
import { AdminApi } from './admin-api';
import { adminError } from './admin-utils';
@Component({
  selector: 'app-admin-progress',
  imports: [Progress, RouterLink],
  providers: [
    { provide: PROGRESS_SOURCE, useExisting: AdminApi },
    { provide: PROGRESS_ERROR, useValue: adminError },
  ],
  template: `<section class="page-intro">
      <span class="eyebrow muted">ENTRENAMIENTO REGISTRADO</span>
      <h1>Estadísticas{{ studentId() ? ' del alumno #' + studentId() : ' del gimnasio' }}.</h1>
      <p>
        Resultados del backend por fecha efectiva. Consultá cada sesión desde los valores exactos de
        su evolución.
      </p>
    </section>
    @if (invalid()) {
      <p class="error-message" role="alert">El ID del alumno no es válido.</p>
    } @else {
      <a class="admin-link" routerLink="/admin/sesiones" [queryParams]="{ alumnoId: studentId() }"
        >Consultar historial de sesiones</a
      >
      <app-teacher-progress
        [studentId]="studentId()"
        [period]="period()"
        sessionBase="/admin/sesiones"
      />
    }`,
})
export class AdminProgress {
  readonly studentId = signal<number | null>(null);
  readonly invalid = signal(false);
  readonly period = signal({ desde: '', hasta: '' });
  constructor() {
    const route = inject(ActivatedRoute);
    combineLatest([route.paramMap, route.queryParamMap])
      .pipe(takeUntilDestroyed())
      .subscribe(([params, query]) => {
        const raw = params.get('id'),
          id = raw === null ? null : Number(raw);
        this.invalid.set(id !== null && (!Number.isSafeInteger(id) || id < 1));
        this.studentId.set(id);
        this.period.set({ desde: query.get('desde') ?? '', hasta: query.get('hasta') ?? '' });
      });
  }
}
