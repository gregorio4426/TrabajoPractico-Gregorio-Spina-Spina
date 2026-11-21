import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { Icon } from '../shared/icon';
import { ExercisePicker } from '../shared/exercise-picker';
import { entityOption } from '../shared/entity-picker';
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
  imports: [ReactiveFormsModule, Confirm, ExercisePicker, DragDropModule, Icon],
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
        this.reorderDays();
        for (const day of this.days.controls) {
          let next = Math.max(
            0,
            ...day.controls.ejercicios.controls.map((r) => r.controls.bloqueOrden.value ?? 0),
          );
          for (const row of day.controls.ejercicios.controls)
            if (row.controls.bloqueOrden.value === null)
              row.patchValue({ bloqueOrden: ++next, vueltas: row.controls.seriesSugeridas.value });
        }
      } else this.addDay();
      this.ready.set(true);
    } catch (e) {
      this.error.set(profesorError(e));
    } finally {
      this.loading.set(false);
    }
  }
  exerciseLists(day: number) {
    return this.blocks(day).map((_, i) => 'exercises-' + day + '-' + i);
  }
  deleteDay(day: number) {
    this.requestRemoval(
      () => this.removeDay(day),
      !!this.days.at(day).controls.nombre.value ||
        this.days.at(day).controls.ejercicios.controls.some((r) => !!r.controls.ejercicioId.value),
    );
  }
  deleteBlock(day: number, index: number) {
    const b = this.blocks(day)[index]!;
    this.requestRemoval(
      () => {
        for (const r of b.rows) {
          const rows = this.days.at(day).controls.ejercicios;
          rows.removeAt(rows.controls.indexOf(r));
        }
      },
      b.rows.some((r) => !!r.controls.ejercicioId.value) ||
        !!b.rows[0]!.controls.bloqueNombre.value,
    );
  }
  deleteExercise(day: number, index: number) {
    const row = this.days.at(day).controls.ejercicios.at(index);
    this.requestRemoval(() => {
      if (this.blocks(day).find((b) => b.rows.includes(row))!.rows.length === 1) {
        row.patchValue({ ejercicioId: null, repsSugeridas: 1, descansoEjercicioSegundos: null });
        this.placeholders.add(row);
      } else this.removeExercise(day, index);
    }, !!row.controls.ejercicioId.value);
  }
  readonly placeholders = new WeakSet<object>();
  readonly movement = signal('');
  private removal: (() => void) | null = null;
  readonly removing = signal(false);
  isEmpty(row: ReturnType<RoutineEditor['exercise']>) {
    return this.placeholders.has(row) && !row.controls.ejercicioId.value;
  }
  emptyBlock(day: number) {
    const rows = this.days.at(day).controls.ejercicios;
    const row = this.exercise();
    row.patchValue({
      bloqueOrden: Math.max(0, ...rows.controls.map((r) => r.controls.bloqueOrden.value ?? 0)) + 1,
      vueltas: 1,
    });
    rows.push(row);
    this.placeholders.add(row);
    this.form.markAsDirty();
  }
  reorderDays() {
    this.days.controls.forEach((d, i) => d.controls.numeroOrden.setValue(i + 1));
  }
  requestRemoval(action: () => void, hasData: boolean) {
    if (!hasData) {
      action();
      return;
    }
    this.removal = action;
    this.removing.set(true);
    this.confirmation().open();
  }
  acceptRemoval() {
    this.removal?.();
    this.removal = null;
    this.removing.set(false);
    this.form.markAsDirty();
  }
  dropBlock(
    day: number,
    event: Pick<CdkDragDrop<unknown>, 'previousIndex' | 'currentIndex' | 'item'>,
  ) {
    const groups = this.blocks(day);
    const [moved] = groups.splice(event.previousIndex, 1);
    groups.splice(event.currentIndex, 0, moved!);
    const rows = this.days.at(day).controls.ejercicios;
    rows.clear();
    groups.forEach((b, i) =>
      b.rows.forEach((r) => {
        r.controls.bloqueOrden.setValue(i + 1);
        rows.push(r);
      }),
    );
    this.form.markAsDirty();
    this.movement.set('Bloque reorganizado.');
  }
  dropExercise(
    day: number,
    targetIndex: number,
    event: Pick<CdkDragDrop<unknown>, 'previousIndex' | 'currentIndex' | 'item'>,
  ) {
    const row = event.item.data as ReturnType<RoutineEditor['exercise']>;
    const rows = this.days.at(day).controls.ejercicios;
    const groups = this.blocks(day);
    const target = groups[targetIndex]!;
    const anchor = target.rows[0]!;
    const destination = target.rows.filter((r) => r !== row && !this.isEmpty(r));
    destination.splice(event.currentIndex, 0, row);
    row.patchValue({
      bloqueOrden: anchor.controls.bloqueOrden.value ?? targetIndex + 1,
      bloqueNombre: anchor.controls.bloqueNombre.value,
      seriesSugeridas: anchor.controls.seriesSugeridas.value,
      vueltas: anchor.controls.seriesSugeridas.value,
      descansoVueltaSegundos: anchor.controls.descansoVueltaSegundos.value,
    });
    const ordered = groups.flatMap((b, i) =>
      i === targetIndex ? destination : b.rows.filter((r) => r !== row),
    );
    rows.clear();
    ordered.forEach((r) => rows.push(r));
    this.form.markAsDirty();
    this.movement.set('Ejercicio reorganizado.');
  }
  keyboardMove(event: KeyboardEvent, day: number, index: number, block: boolean) {
    if (!['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(event.key)) return;
    event.preventDefault();
    if (block) this.moveBlockAt(day, index, event.key === 'ArrowUp' ? -1 : 1);
    else if (event.key === 'ArrowLeft' || event.key === 'ArrowRight')
      this.moveToBlock(day, index, event.key === 'ArrowLeft' ? -1 : 1);
    else this.moveExercise(day, index, event.key === 'ArrowUp' ? -1 : 1);
    this.form.markAsDirty();
    this.movement.set('Orden actualizado.');
  }
  addDay(after = this.days.length - 1) {
    const day = this.day(undefined, this.days.length + 1);
    const row = this.exercise();
    row.patchValue({ bloqueOrden: 1, vueltas: 1 });
    this.placeholders.add(row);
    day.controls.ejercicios.push(row);
    this.days.insert(after + 1, day);
    this.reorderDays();
    setTimeout(() =>
      document
        .getElementById('routine-day-' + (after + 1))
        ?.scrollIntoView?.({ behavior: 'smooth', block: 'start' }),
    );
  }
  removeDay(index: number) {
    this.days.removeAt(index);
    this.reorderDays();
  }
  addExercise(day: number) {
    const rows = this.days.at(day).controls.ejercicios;
    if (rows.controls.some((e) => e.controls.bloqueOrden.value !== null))
      this.addToBlock(day, rows.at(-1).controls.bloqueOrden.value);
    else rows.push(this.exercise());
  }
  blocks(day: number) {
    const result: Array<{
      key: unknown;
      id: unknown;
      first: number;
      indexes: number[];
      rows: Array<ReturnType<RoutineEditor['exercise']>>;
    }> = [];
    this.days.at(day).controls.ejercicios.controls.forEach((row, index) => {
      const id = row.controls.bloqueOrden.value ?? row;
      let block = result.find((b) => b.id === id);
      if (!block) {
        block = { key: row, id, first: index, indexes: [], rows: [] };
        result.push(block);
      }
      block.indexes.push(index);
      block.rows.push(row);
    });
    return result;
  }
  exerciseCreated(exercise: D.EjercicioResponse) {
    this.exercises.update((es) => [...es.filter((e) => e.id !== exercise.id), exercise]);
  }
  moveToBlock(day: number, index: number, offset: number) {
    const list = this.days.at(day).controls.ejercicios,
      row = list.at(index);
    const groups = this.blocks(day),
      current = groups.findIndex((b) => b.rows.includes(row));
    const target = groups[current + offset];
    if (!target) return;
    // Normalize legacy single exercises before grouping so their order remains explicit.
    let next = Math.max(0, ...list.controls.map((r) => r.controls.bloqueOrden.value ?? 0));
    for (const r of list.controls)
      if (r.controls.bloqueOrden.value === null)
        r.patchValue({ bloqueOrden: ++next, vueltas: r.controls.seriesSugeridas.value });
    const anchor = target.rows.at(-1)!,
      source = target.rows[0]!;
    list.removeAt(index);
    row.patchValue({
      bloqueOrden: source.controls.bloqueOrden.value,
      bloqueNombre: source.controls.bloqueNombre.value,
      vueltas: source.controls.seriesSugeridas.value,
      seriesSugeridas: source.controls.seriesSugeridas.value,
      descansoVueltaSegundos: source.controls.descansoVueltaSegundos.value,
    });
    list.insert(list.controls.indexOf(anchor) + 1, row);
  }
  convertAndAdd(day: number, index: number) {
    const rows = this.days.at(day).controls.ejercicios;
    let next = Math.max(0, ...rows.controls.map((r) => r.controls.bloqueOrden.value ?? 0));
    for (const row of rows.controls)
      if (row.controls.bloqueOrden.value === null)
        row.patchValue({ bloqueOrden: ++next, vueltas: row.controls.seriesSugeridas.value });
    this.addToBlock(day, rows.at(index).controls.bloqueOrden.value);
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
    const empty = rows.length === 1 ? rows.at(0) : null;
    if (
      empty &&
      empty.pristine &&
      (empty.controls.bloqueOrden.value === null || this.isEmpty(empty)) &&
      !empty.controls.ejercicioId.value &&
      empty.controls.seriesSugeridas.value === 1 &&
      empty.controls.repsSugeridas.value === 1
    )
      rows.clear();
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
    if (existing.length === 1 && this.isEmpty(last)) {
      this.placeholders.delete(last);
      return;
    }
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
  updateBlock(
    day: number,
    index: number,
    field: 'bloqueNombre' | 'seriesSugeridas' | 'descansoVueltaSegundos',
    value: string,
  ) {
    const row = this.days.at(day).controls.ejercicios.at(index);
    row
      .get(field)!
      .setValue(field === 'bloqueNombre' ? value : value === '' ? null : Number(value));
    this.syncBlock(day, index);
  }
  syncBlock(day: number, index: number) {
    const source = this.days.at(day).controls.ejercicios.at(index);
    if (source.controls.bloqueOrden.value === null) {
      const rows = this.days.at(day).controls.ejercicios;
      let next = Math.max(0, ...rows.controls.map((r) => r.controls.bloqueOrden.value ?? 0));
      for (const row of rows.controls)
        if (row.controls.bloqueOrden.value === null)
          row.patchValue({ bloqueOrden: ++next, vueltas: row.controls.seriesSugeridas.value });
    }
    const block = source.controls.bloqueOrden.value!;
    const header = this.blockRows(day, block)[0]!;
    for (const row of this.blockRows(day, block))
      row.patchValue({
        bloqueNombre: header.controls.bloqueNombre.value,
        vueltas: header.controls.seriesSugeridas.value,
        seriesSugeridas: header.controls.seriesSugeridas.value,
        descansoVueltaSegundos: header.controls.descansoVueltaSegundos.value,
      });
  }
  moveBlockAt(day: number, index: number, offset: number) {
    const rows = this.days.at(day).controls.ejercicios;
    let next = Math.max(0, ...rows.controls.map((r) => r.controls.bloqueOrden.value ?? 0));
    for (const row of rows.controls)
      if (row.controls.bloqueOrden.value === null)
        row.patchValue({ bloqueOrden: ++next, vueltas: row.controls.seriesSugeridas.value });
    this.moveBlock(day, rows.at(index).controls.bloqueOrden.value, offset);
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
        'Completá los nombres, la descripción y al menos un ejercicio en cada bloque. Las vueltas y repeticiones deben ser enteros positivos; los descansos opcionales, enteros no negativos.',
      );
      return;
    }
    void this.save();
  }
  payload(): D.RutinaRequest {
    const v = this.form.getRawValue();
    return {
      nombre: v.nombre!.trim(),
      descripcion: v.descripcion!.trim(),
      dias: v.dias.map((d, dayIndex) => ({
        nombre: d.nombre!.trim(),
        numeroOrden: dayIndex + 1,
        ejercicios: d.ejercicios.map((e, index) => ({
          ...(e.bloqueOrden === null
            ? {}
            : {
                bloqueOrden: [...new Set(d.ejercicios.map(row => row.bloqueOrden))].indexOf(e.bloqueOrden) + 1,
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
