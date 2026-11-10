import { Component, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { ProfesorApi } from './profesor-api';
import * as D from '../admin/contracts';
import { LEVELS, OBJECTIVES } from '../admin/catalog';
import { RecordView } from '../admin/record-view';
import { Confirm } from './confirm';
import { Progress } from './progress';
import { profesorError, text } from './utils';
@Component({
  selector: 'app-teacher-student',
  imports: [RouterLink, ReactiveFormsModule, RecordView, Confirm, Progress],
  templateUrl: './student.html',
})
export class Student {
  private readonly api = inject(ProfesorApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly id = signal(0);
  readonly data = signal<D.AlumnoResponse | null>(null);
  readonly active = signal<D.RutinaResponse | null>(null);
  readonly history = signal<D.RutinaResponse[]>([]);
  readonly sessions = signal<D.SesionResponse[]>([]);
  readonly error = signal('');
  readonly success = signal('');
  readonly loading = signal(false);
  readonly pending = signal(false);
  readonly tab = signal<'perfil' | 'rutinas' | 'sesiones' | 'progreso'>('perfil');
  readonly text = text;
  readonly levels = LEVELS;
  readonly objectives = OBJECTIVES;
  readonly action = signal<'follow' | 'unlink'>('follow');
  private seq = 0;
  readonly form = inject(FormBuilder).nonNullable.group({
    nivel: ['', Validators.required],
    objetivo: ['', Validators.required],
  });
  private readonly confirm = viewChild.required<Confirm>('confirmation');
  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.id.set(Number(params.get('id')));
      void this.load();
    });
  }
  async load() {
    const seq = ++this.seq;
    this.clear();
    this.loading.set(true);
    this.error.set('');
    if (!Number.isSafeInteger(this.id()) || this.id() < 1) {
      this.error.set('El ID del alumno no es válido.');
      this.loading.set(false);
      return;
    }
    try {
      const profile = await firstValueFrom(this.api.student(this.id()));
      if (seq !== this.seq) return;
      const [active, history, sessions] = await Promise.all([
        firstValueFrom(this.api.active(this.id())).catch((e: unknown) => {
          if (e instanceof HttpErrorResponse && e.status === 404) return null;
          throw e;
        }),
        firstValueFrom(this.api.history(this.id())),
        firstValueFrom(this.api.sessions(this.id())),
      ]);
      if (seq !== this.seq) return;
      this.data.set(profile);
      this.active.set(active);
      this.history.set(history);
      this.sessions.set(sessions);
      this.form.setValue({ nivel: profile.nivel ?? '', objetivo: profile.objetivo ?? '' });
    } catch (e) {
      if (seq === this.seq) {
        this.clear();
        this.error.set(profesorError(e));
      }
    } finally {
      if (seq === this.seq) this.loading.set(false);
    }
  }
  clear() {
    this.data.set(null);
    this.active.set(null);
    this.history.set([]);
    this.sessions.set([]);
  }
  lostAccess() {
    ++this.seq;
    this.clear();
    this.error.set(
      'Ya no tenés una vinculación vigente con este alumno. Volvé al listado de alumnos.',
    );
  }
  ask(action: 'follow' | 'unlink') {
    this.form.markAllAsTouched();
    if (this.pending() || (action === 'follow' && this.form.invalid)) return;
    this.action.set(action);
    this.confirm().open();
  }
  async save() {
    if (this.pending()) return;
    this.pending.set(true);
    this.error.set('');
    this.success.set('');
    try {
      if (this.action() === 'unlink') {
        await firstValueFrom(this.api.unlink(this.id()));
        this.clear();
        await this.router.navigateByUrl('/profesor/alumnos');
      } else {
        const v = this.form.getRawValue();
        await firstValueFrom(
          this.api.follow(this.id(), {
            nivel: v.nivel as D.SeguimientoAlumnoRequest['nivel'],
            objetivo: v.objetivo as D.SeguimientoAlumnoRequest['objetivo'],
          }),
        );
        await this.load();
        if (!this.error()) this.success.set('Seguimiento actualizado correctamente.');
      }
    } catch (e) {
      this.clear();
      this.error.set(profesorError(e));
    } finally {
      this.pending.set(false);
    }
  }
}
