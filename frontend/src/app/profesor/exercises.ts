import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ProfesorApi } from './profesor-api';
import * as D from '../admin/contracts';
import { Confirm } from './confirm';
import { notBlank, profesorError, text } from './utils';
export const MUSCLES = ['PECHO', 'ESPALDA', 'PIERNAS', 'BRAZOS', 'HOMBROS', 'ABDOMINALES'] as const;
@Component({
  selector: 'app-teacher-exercises',
  imports: [ReactiveFormsModule, Confirm],
  templateUrl: './exercises.html',
})
export class Exercises {
  private readonly api = inject(ProfesorApi);
  private readonly fb = inject(FormBuilder);
  private readonly confirmation = viewChild.required<Confirm>('confirmation');
  readonly groups = MUSCLES;
  readonly text = text;
  readonly data = signal<D.EjercicioResponse[]>([]);
  readonly editing = signal(false);
  readonly selected = signal<D.EjercicioResponse | null>(null);
  readonly action = signal<'save' | 'delete'>('save');
  readonly pending = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly page = signal(0);
  private seq = 0;
  readonly filters = this.fb.nonNullable.group({ nombre: '', grupoMuscular: '' });
  private readonly applied = signal(this.filters.getRawValue());
  readonly visible = computed(() => {
    const name = this.applied().nombre.trim().toLocaleLowerCase('es');
    return this.data().filter((e) => !name || e.nombre?.toLocaleLowerCase('es').includes(name));
  });
  readonly rows = computed(() => this.visible().slice(this.page() * 20, this.page() * 20 + 20));
  readonly pages = computed(() => Math.ceil(this.visible().length / 20));
  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, notBlank, Validators.maxLength(60)]],
    descripcion: ['', [Validators.required, notBlank, Validators.maxLength(255)]],
    grupoMuscular: ['', [Validators.required]],
  });
  constructor() {
    void this.load();
  }
  async load() {
    const seq = ++this.seq;
    this.error.set('');
    this.loading.set(true);
    this.data.set([]);
    this.page.set(0);
    const q = this.filters.getRawValue();
    this.applied.set(q);
    try {
      const data = await firstValueFrom(
        this.api.exercises(
          q.grupoMuscular ? { grupoMuscular: q.grupoMuscular } : { nombre: q.nombre.trim() },
        ),
      );
      if (seq === this.seq) this.data.set(data);
    } catch (e) {
      if (seq === this.seq) this.error.set(profesorError(e));
    } finally {
      if (seq === this.seq) this.loading.set(false);
    }
  }
  open(exercise: D.EjercicioResponse | null) {
    this.selected.set(exercise);
    this.editing.set(true);
    this.success.set('');
    this.error.set('');
    this.form.setValue({
      nombre: exercise?.nombre ?? '',
      descripcion: exercise?.descripcion ?? '',
      grupoMuscular: exercise?.grupoMuscular ?? '',
    });
  }
  ask(action: 'save' | 'delete', exercise?: D.EjercicioResponse) {
    if (this.pending()) return;
    this.action.set(action);
    this.error.set('');
    if (action === 'delete') {
      this.selected.set(exercise!);
      this.editing.set(false);
    } else {
      this.form.markAllAsTouched();
      if (this.form.invalid) {
        this.error.set(
          'Revisá nombre (hasta 60 caracteres), descripción (hasta 255) y grupo muscular.',
        );
        return;
      }
    }
    this.confirmation().open();
  }
  async save() {
    if (this.pending()) return;
    this.pending.set(true);
    this.error.set('');
    this.success.set('');
    try {
      if (this.action() === 'delete')
        await firstValueFrom(this.api.deleteExercise(this.selected()!.id!));
      else {
        const v = this.form.getRawValue();
        const body: D.EjercicioRequest = {
          nombre: v.nombre.trim(),
          descripcion: v.descripcion.trim(),
          grupoMuscular: v.grupoMuscular as D.EjercicioRequest['grupoMuscular'],
        };
        await firstValueFrom(
          this.selected()
            ? this.api.updateExercise(this.selected()!.id!, body)
            : this.api.createExercise(body),
        );
      }
      this.editing.set(false);
      await this.load();
      if (!this.error())
        this.success.set(
          this.action() === 'delete'
            ? 'Ejercicio eliminado del catálogo.'
            : 'Ejercicio guardado correctamente.',
        );
    } catch (e) {
      this.error.set(profesorError(e));
    } finally {
      this.pending.set(false);
    }
  }
}
