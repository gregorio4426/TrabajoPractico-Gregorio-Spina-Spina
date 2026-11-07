import { Component, inject, signal, viewChild } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  ReactiveFormsModule,
  Validators,
  ValidatorFn,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ProfesorApi } from './profesor-api';
import { AuthService } from '../core/auth.service';
import * as D from '../admin/contracts';
import { Confirm } from './confirm';
import { notBlank, positiveInteger, optionalWeight, profesorError } from './utils';
export const uniqueDayOrder: ValidatorFn = (control) => {
  const orders = (control as FormArray).controls.map((day) =>
    Number(day.get('numeroOrden')?.value),
  );
  return new Set(orders).size === orders.length ? null : { duplicateOrder: true };
};
@Component({
  selector: 'app-teacher-routine-editor',
  imports: [ReactiveFormsModule, RouterLink, Confirm],
  templateUrl: './routine-editor.html',
})
export class RoutineEditor {
  private readonly api = inject(ProfesorApi);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly confirmation = viewChild.required<Confirm>('confirmation');
  readonly id = Number(this.route.snapshot.paramMap.get('id'));
  readonly editing = this.route.snapshot.paramMap.has('id');
  readonly exercises = signal<D.EjercicioResponse[]>([]);
  readonly loading = signal(true);
  readonly pending = signal(false);
  readonly error = signal('');
  readonly ready = signal(false);
  exercise(initial?: D.DiaEjercicioResponse) {
    return this.fb.group({
      ejercicioId: [initial?.ejercicioId ?? null, [Validators.required, positiveInteger]],
      seriesSugeridas: [initial?.seriesSugeridas ?? 1, [Validators.required, positiveInteger]],
      repsSugeridas: [initial?.repsSugeridas ?? 1, [Validators.required, positiveInteger]],
      pesoSugerido: [initial?.pesoSugerido ?? null, optionalWeight],
    });
  }
  day(initial?: D.DiaRutinaResponse, order = 1) {
    return this.fb.group({
      nombre: [initial?.nombre ?? '', [Validators.required, notBlank, Validators.maxLength(255)]],
      numeroOrden: [initial?.numeroOrden ?? order, [Validators.required, positiveInteger]],
      ejercicios: new FormArray(
        (initial?.ejercicios ?? []).map((e) => this.exercise(e)),
        Validators.minLength(1),
      ),
    });
  }
  readonly form = this.fb.group({
    nombre: ['', [Validators.required, notBlank, Validators.maxLength(255)]],
    descripcion: ['', [Validators.required, notBlank, Validators.maxLength(255)]],
    dias: new FormArray<ReturnType<RoutineEditor['day']>>(
      [],
      [Validators.required, Validators.minLength(1), uniqueDayOrder],
    ),
  });
  get days() {
    return this.form.controls.dias;
  }
  constructor() {
    void this.load();
  }
  async load() {
    this.loading.set(true);
    this.error.set('');
    this.ready.set(false);
    try {
      const exercises = await firstValueFrom(this.api.exercises());
      this.exercises.set(exercises);
      this.days.clear();
      if (this.editing) {
        if (!Number.isSafeInteger(this.id) || this.id < 1) throw new Error('ID inválido');
        const routine = await firstValueFrom(this.api.routine(this.id));
        if (routine.profesorId !== this.auth.session()?.perfilId || routine.archivada) {
          this.error.set('Solo podés editar tus propias rutinas no archivadas.');
          return;
        }
        this.form.patchValue({
          nombre: routine.nombre ?? '',
          descripcion: routine.descripcion ?? '',
        });
        for (const day of routine.dias ?? []) this.days.push(this.day(day));
      } else this.addDay();
      this.ready.set(true);
    } catch (e) {
      this.error.set(profesorError(e));
    } finally {
      this.loading.set(false);
    }
  }
  addDay() {
    const next =
      Math.max(0, ...this.days.controls.map((d) => Number(d.controls.numeroOrden.value))) + 1;
    const day = this.day(undefined, next);
    day.controls.ejercicios.push(this.exercise());
    this.days.push(day);
  }
  removeDay(index: number) {
    this.days.removeAt(index);
  }
  addExercise(day: number) {
    this.days.at(day).controls.ejercicios.push(this.exercise());
  }
  removeExercise(day: number, index: number) {
    this.days.at(day).controls.ejercicios.removeAt(index);
  }
  confirm() {
    this.form.markAllAsTouched();
    this.error.set('');
    if (this.pending() || !this.ready()) return;
    if (this.form.invalid || this.days.controls.some((d) => d.controls.ejercicios.length === 0)) {
      this.error.set(
        'Revisá los campos: se requiere al menos un día y un ejercicio por día; órdenes de día únicos, IDs válidos, series/repeticiones enteras positivas y pesos opcionales finitos no negativos.',
      );
      return;
    }
    this.confirmation().open();
  }
  payload(): D.RutinaRequest {
    const v = this.form.getRawValue();
    return {
      nombre: v.nombre!.trim(),
      descripcion: v.descripcion!.trim(),
      dias: v.dias.map((d) => ({
        nombre: d.nombre!.trim(),
        numeroOrden: Number(d.numeroOrden),
        ejercicios: d.ejercicios.map((e) => ({
          ejercicioId: Number(e.ejercicioId),
          seriesSugeridas: Number(e.seriesSugeridas),
          repsSugeridas: Number(e.repsSugeridas),
          pesoSugerido:
            e.pesoSugerido === null || String(e.pesoSugerido) === ''
              ? null
              : Number(e.pesoSugerido),
        })),
      })),
    };
  }
  async save() {
    if (this.pending()) return;
    this.pending.set(true);
    this.error.set('');
    try {
      const result = await firstValueFrom(
        this.editing
          ? this.api.updateRoutine(this.id, this.payload())
          : this.api.createRoutine(this.payload()),
      );
      await this.router.navigate(['/profesor/rutinas', result.id], {
        queryParams: { guardada: result.numeroVersion },
      });
    } catch (e) {
      this.error.set(profesorError(e));
    } finally {
      this.pending.set(false);
    }
  }
}
