import { EntityPicker, entityOption } from '../shared/entity-picker';
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
  imports: [ReactiveFormsModule, RouterLink, Confirm, EntityPicker],
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
  readonly copyVersion = Number(this.route.snapshot.queryParamMap?.get('version')) || null;
  readonly copyFrom = Number(this.route.snapshot.queryParamMap?.get('copiar')) || null;
  readonly editing = this.route.snapshot.paramMap.has('id');
  readonly exercises = signal<D.EjercicioResponse[]>([]);
  readonly loading = signal(true);
  readonly pending = signal(false);
  readonly error = signal('');
  readonly ready = signal(false);
  readonly exerciseOptions = () => this.exercises().map(entityOption);
  exercise(initial?: D.DiaEjercicioResponse) {
    return this.fb.group({
      bloqueOrden: [initial?.bloqueOrden ?? (null as number | null)],
      bloqueNombre: [initial?.bloqueNombre ?? '', Validators.maxLength(100)],
      vueltas: [initial?.vueltas ?? (null as number | null)],
      descansoVueltaSegundos: [
        initial?.descansoVueltaSegundos ?? (null as number | null),
        optionalWeight,
      ],
      descansoEjercicioSegundos: [
        initial?.descansoEjercicioSegundos ?? (null as number | null),
        optionalWeight,
      ],
      ejercicioId: [initial?.ejercicioId ?? null, [Validators.required, positiveInteger]],
      seriesSugeridas: [initial?.seriesSugeridas ?? 1, [Validators.required, positiveInteger]],
      repsSugeridas: [initial?.repsSugeridas ?? 1, [Validators.required, positiveInteger]],
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
      if (this.editing || this.copyFrom) {
        const sourceId = this.copyFrom ?? this.id;
        if (!Number.isSafeInteger(sourceId) || sourceId < 1) throw new Error('ID inválido');
        const routine = await firstValueFrom(
          this.copyFrom && this.copyVersion
            ? this.api.version(sourceId, this.copyVersion)
            : this.api.routine(sourceId),
        );
        if (
          routine.profesorId !== this.auth.session()?.perfilId ||
          (this.editing && routine.archivada)
        ) {
          this.error.set('Solo podés editar tus propias rutinas no archivadas.');
          return;
        }
        this.form.patchValue({
          nombre: this.copyFrom
            ? (routine.nombre ?? '').slice(0, 247) + ' (copia)'
            : (routine.nombre ?? ''),
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
    const rows = this.days.at(day).controls.ejercicios;
    if (rows.controls.some((e) => e.controls.bloqueOrden.value !== null)) this.addBlock(day, 1);
    else rows.push(this.exercise());
  }
  blockRows(day: number, block: number) {
    return this.days
      .at(day)
      .controls.ejercicios.controls.filter((e) => e.controls.bloqueOrden.value === block);
  }
  firstInBlock(day: number, row: number) {
    const rows = this.days.at(day).controls.ejercicios.controls;
    const block = rows[row]!.controls.bloqueOrden.value;
    return block !== null && rows.findIndex((e) => e.controls.bloqueOrden.value === block) === row;
  }
  blockType(day: number, block: number | null) {
    const count = block === null ? 1 : this.blockRows(day, block).length;
    return count === 1 ? 'Series' : count === 2 ? 'Biserie' : 'Circuito';
  }
  addBlock(day: number, count: number) {
    const rows = this.days.at(day).controls.ejercicios;
    let next = Math.max(0, ...rows.controls.map((e) => e.controls.bloqueOrden.value ?? 0));
    for (const row of rows.controls)
      if (row.controls.bloqueOrden.value === null)
        row.patchValue({ bloqueOrden: ++next, vueltas: row.controls.seriesSugeridas.value });
    const block = next + 1;
    for (let i = 0; i < count; i++) {
      const row = this.exercise();
      row.patchValue({ bloqueOrden: block, vueltas: 1 });
      rows.push(row);
    }
  }
  addToBlock(day: number, block: number | null) {
    if (block === null) return;
    const rows = this.days.at(day).controls.ejercicios;
    const existing = this.blockRows(day, block),
      last = existing.at(-1)!;
    const row = this.exercise();
    row.patchValue({
      bloqueOrden: block,
      bloqueNombre: last.controls.bloqueNombre.value,
      vueltas: last.controls.vueltas.value,
      seriesSugeridas: last.controls.seriesSugeridas.value,
      descansoVueltaSegundos: last.controls.descansoVueltaSegundos.value,
    });
    rows.insert(rows.controls.indexOf(last) + 1, row);
  }
  syncBlock(day: number, index: number) {
    const source = this.days.at(day).controls.ejercicios.at(index);
    const block = source.controls.bloqueOrden.value;
    if (block === null) return;
    for (const row of this.blockRows(day, block))
      row.patchValue({
        bloqueNombre: source.controls.bloqueNombre.value,
        vueltas: source.controls.seriesSugeridas.value,
        seriesSugeridas: source.controls.seriesSugeridas.value,
        descansoVueltaSegundos: source.controls.descansoVueltaSegundos.value,
      });
  }
  moveBlock(day: number, block: number | null, offset: number) {
    if (block === null) return;
    const rows = this.days.at(day).controls.ejercicios;
    const group = this.blockRows(day, block),
      first = rows.controls.indexOf(group[0]!),
      last = first + group.length - 1;
    const neighbor = offset < 0 ? first - 1 : last + 1;
    if (neighbor < 0 || neighbor >= rows.length) return;
    const other = rows.at(neighbor).controls.bloqueOrden.value;
    const adjacent = other === null ? [rows.at(neighbor)] : this.blockRows(day, other);
    const start = offset < 0 ? rows.controls.indexOf(adjacent[0]!) : first;
    const ordered = offset < 0 ? [...group, ...adjacent] : [...adjacent, ...group];
    for (let i = 0; i < ordered.length; i++) rows.removeAt(start);
    ordered.forEach((r, i) => rows.insert(start + i, r));
  }
  removeBlock(day: number, block: number | null) {
    if (block === null) return;
    const rows = this.days.at(day).controls.ejercicios;
    for (let i = rows.length - 1; i >= 0; i--)
      if (rows.at(i).controls.bloqueOrden.value === block) rows.removeAt(i);
  }
  removeExercise(day: number, index: number) {
    this.days.at(day).controls.ejercicios.removeAt(index);
  }
  exerciseName(value: unknown) {
    return this.exercises().find((e) => e.id === Number(value))?.nombre ?? 'Seleccionar ejercicio';
  }
  moveExercise(day: number, index: number, offset: number) {
    const list = this.days.at(day).controls.ejercicios,
      target = index + offset;
    if (target < 0 || target >= list.length) return;
    const row = list.at(index);
    if (row.controls.bloqueOrden.value !== list.at(target).controls.bloqueOrden.value) return;
    list.removeAt(index);
    list.insert(target, row);
  }
  confirm() {
    this.form.markAllAsTouched();
    this.error.set('');
    if (this.pending() || !this.ready()) return;
    if (
      this.form.invalid ||
      this.days.controls.some((d) =>
        d.controls.ejercicios.controls.some((e) =>
          [
            e.controls.descansoEjercicioSegundos.value,
            e.controls.descansoVueltaSegundos.value,
          ].some((v) => v !== null && !Number.isSafeInteger(Number(v))),
        ),
      ) ||
      this.days.controls.some((d) => d.controls.ejercicios.length === 0)
    ) {
      this.error.set(
        'Revisá los campos: se requiere al menos un día y un ejercicio por día; órdenes de día únicos, ejercicios seleccionados, series/repeticiones enteras positivas y descansos opcionales enteros no negativos.',
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
        ejercicios: d.ejercicios.map((e, index) => ({
          ...(e.bloqueOrden === null
            ? {}
            : {
                bloqueOrden: d.ejercicios.findIndex((row) => row.bloqueOrden === e.bloqueOrden) + 1,
                bloqueNombre: e.bloqueNombre?.trim() || null,
                vueltas: Number(e.seriesSugeridas),
                ordenEnBloque: d.ejercicios
                  .slice(0, index + 1)
                  .filter((row) => row.bloqueOrden === e.bloqueOrden).length,
                descansoEjercicioSegundos:
                  e.descansoEjercicioSegundos === null ? null : Number(e.descansoEjercicioSegundos),
                descansoVueltaSegundos:
                  e.descansoVueltaSegundos === null ? null : Number(e.descansoVueltaSegundos),
              }),
          ejercicioId: Number(e.ejercicioId),
          seriesSugeridas: Number(e.seriesSugeridas),
          repsSugeridas: Number(e.repsSugeridas),
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
