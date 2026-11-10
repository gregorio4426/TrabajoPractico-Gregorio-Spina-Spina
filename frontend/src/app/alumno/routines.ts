import { Component, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AlumnoApi } from './alumno-api';
import * as D from '../admin/contracts';
import { Confirm } from '../profesor/confirm';
import { Prescription } from './prescription';
import { alumnoError, isClosed, localDate, pastOrToday, text, utcDate } from './utils';
@Component({
  selector: 'app-student-routines',
  imports: [RouterLink, ReactiveFormsModule, Confirm, Prescription],
  templateUrl: './routines.html',
})
export class StudentRoutines {
  private readonly api = inject(AlumnoApi);
  private readonly router = inject(Router);
  readonly historyMode = inject(ActivatedRoute).snapshot.data['history'] === true;
  private readonly confirmation = viewChild.required<Confirm>('confirmation');
  readonly active = signal<D.RutinaResponse | null>(null);
  readonly history = signal<D.RutinaResponse[]>([]);
  readonly open = signal<D.SesionResponse[]>([]);
  readonly selected = signal<D.RutinaResponse | null>(null);
  readonly day = signal<D.DiaRutinaResponse | null>(null);
  readonly loading = signal(false);
  readonly pending = signal(false);
  readonly error = signal('');
  readonly text = text;
  readonly today = utcDate();
  readonly form = inject(FormBuilder).nonNullable.group({
    fecha: [localDate(), [Validators.required, pastOrToday]],
  });
  private seq = 0;
  constructor() {
    void this.load();
  }
  async load() {
    const seq = ++this.seq;
    this.loading.set(true);
    this.error.set('');
    this.active.set(null);
    this.history.set([]);
    this.open.set([]);
    this.selected.set(null);
    try {
      const [active, history, sessions] = await Promise.all([
        firstValueFrom(this.api.active()).catch((e: unknown) => {
          if (e instanceof HttpErrorResponse && e.status === 404) return null;
          throw e;
        }),
        firstValueFrom(this.api.history()),
        firstValueFrom(this.api.sessions()),
      ]);
      if (seq !== this.seq) return;
      this.active.set(active);
      this.history.set(history);
      this.open.set(sessions.filter((s) => !isClosed(s)));
    } catch (e) {
      if (seq === this.seq) this.error.set(alumnoError(e));
    } finally {
      if (seq === this.seq) this.loading.set(false);
    }
  }
  openForDay(id: number | null | undefined) {
    return this.open().filter((s) => s.diaRutinaId === id);
  }
  ask(day: D.DiaRutinaResponse) {
    this.form.markAllAsTouched();
    this.error.set('');
    if (this.pending()) return;
    if (
      this.form.invalid ||
      !Number.isSafeInteger(day.id) ||
      !this.active()?.dias?.some((d) => d.id === day.id)
    ) {
      this.error.set('Seleccioná un día de tu rutina activa y una fecha válida no futura.');
      return;
    }
    this.day.set(day);
    this.confirmation().open();
  }
  async start() {
    if (this.pending() || !this.day()?.id) return;
    this.pending.set(true);
    this.error.set('');
    try {
      const session = await firstValueFrom(
        this.api.start({ diaRutinaId: this.day()!.id!, fecha: this.form.controls.fecha.value }),
      );
      await this.router.navigate(['/alumno/sesiones', session.id]);
    } catch (e) {
      this.error.set(
        alumnoError(e) +
          ' No se repetirá el inicio automáticamente. Revisá tus sesiones abiertas antes de volver a intentar.',
      );
    } finally {
      this.pending.set(false);
    }
  }
}
