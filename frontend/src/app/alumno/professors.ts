import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AlumnoApi } from './alumno-api';
import * as D from '../admin/contracts';
import { Confirm } from '../profesor/confirm';
import { positiveInteger } from '../profesor/utils';
import { alumnoError } from './utils';
@Component({
  selector: 'app-student-professors',
  imports: [ReactiveFormsModule, Confirm],
  templateUrl: './professors.html',
})
export class Professors {
  private readonly api = inject(AlumnoApi);
  private readonly confirmation = viewChild.required<Confirm>('confirmation');
  private readonly fb = inject(FormBuilder);
  readonly profile = signal<D.AlumnoResponse | null>(null);
  readonly directory = signal<D.ProfesorResponse[]>([]);
  readonly loading = signal(false);
  readonly pending = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly action = signal<'link' | 'unlink' | 'replace'>('link');
  readonly selected = signal<D.ProfesorResponse | D.ProfesorResumenResponse | null>(null);
  readonly page = signal(0);
  private seq = 0;
  readonly search = this.fb.nonNullable.control('');
  private readonly q = signal('');
  readonly form = this.fb.nonNullable.group({
    anterior: ['', [Validators.required, positiveInteger]],
    nuevo: ['', [Validators.required, positiveInteger]],
  });
  readonly filtered = computed(() =>
    this.directory().filter((p) =>
      `${p.nombre} ${p.apellido} ${p.email} ${p.especialidad}`
        .toLocaleLowerCase('es')
        .includes(this.q().trim().toLocaleLowerCase('es')),
    ),
  );
  readonly visible = computed(() => this.filtered().slice(this.page() * 20, this.page() * 20 + 20));
  readonly pages = computed(() => Math.ceil(this.filtered().length / 20));
  constructor() {
    this.search.valueChanges.pipe(takeUntilDestroyed()).subscribe((value) => {
      this.q.set(value);
      this.page.set(0);
    });
    void this.load();
  }
  linked(id: number | null | undefined) {
    return this.profile()?.profesores?.some((p) => p.id === id) ?? false;
  }
  async load() {
    const seq = ++this.seq;
    this.profile.set(null);
    this.directory.set([]);
    this.page.set(0);
    this.error.set('');
    this.loading.set(true);
    try {
      const [profile, directory] = await Promise.all([
        firstValueFrom(this.api.profile()),
        firstValueFrom(this.api.professors()),
      ]);
      if (seq === this.seq) {
        this.profile.set(profile);
        this.directory.set(directory);
      }
    } catch (e) {
      if (seq === this.seq) this.error.set(alumnoError(e));
    } finally {
      if (seq === this.seq) this.loading.set(false);
    }
  }
  ask(
    action: 'link' | 'unlink' | 'replace',
    professor?: D.ProfesorResponse | D.ProfesorResumenResponse,
  ) {
    if (this.pending()) return;
    this.error.set('');
    this.action.set(action);
    if (action === 'replace') {
      this.form.markAllAsTouched();
      if (
        this.form.invalid ||
        this.form.controls.anterior.value === this.form.controls.nuevo.value
      ) {
        this.error.set(
          'Seleccioná un profesor vinculado y otro profesor diferente para el reemplazo.',
        );
        return;
      }
    } else this.selected.set(professor!);
    this.confirmation().open();
  }
  async save() {
    if (this.pending() || !this.profile()?.id) return;
    this.pending.set(true);
    this.error.set('');
    this.success.set('');
    try {
      await firstValueFrom(
        this.action() === 'link'
          ? this.api.link(this.selected()!.id!)
          : this.action() === 'unlink'
            ? this.api.unlink(this.selected()!.id!)
            : this.api.replace(
                this.profile()!.id!,
                Number(this.form.controls.anterior.value),
                Number(this.form.controls.nuevo.value),
              ),
      );
      await this.load();
      this.form.reset();
      if (!this.error())
        this.success.set('Vinculación actualizada. Tus rutinas y entrenamientos se conservan.');
    } catch (e) {
      this.error.set(
        e instanceof HttpErrorResponse && e.status === 409
          ? 'No se pudo actualizar el vínculo. Revisá que el profesor esté activo y vigente y recargá tus profesores antes de reintentar.'
          : alumnoError(e),
      );
    } finally {
      this.pending.set(false);
    }
  }
  message() {
    return this.action() === 'replace'
      ? `Se reemplazará solo el vínculo con el profesor #${this.form.controls.anterior.value} por #${this.form.controls.nuevo.value}. Los demás profesores y tu historial se conservan.`
      : `Se ${this.action() === 'link' ? 'agregará' : 'terminará'} tu vínculo con ${this.selected()?.nombre} ${this.selected()?.apellido} (#${this.selected()?.id}). La rutina y el historial se conservan. El profesor desvinculado pierde acceso a tus datos.`;
  }
}
