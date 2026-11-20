import {
  Component,
  ElementRef,
  effect,
  forwardRef,
  inject,
  input,
  output,
  signal,
  viewChild,
  untracked,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { SearchSelect, normalizedName } from './search-select';
import { MusclePicker } from './muscle-picker';
import { ProfesorApi } from '../profesor/profesor-api';
import { AuthService } from '../core/auth.service';
import { profesorError, notBlank } from '../profesor/utils';
import * as D from '../admin/contracts';
let nextExerciseDialog = 0;
@Component({
  selector: 'app-exercise-picker',
  imports: [SearchSelect, MusclePicker, ReactiveFormsModule],
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => ExercisePicker), multi: true },
  ],
  template: ` <app-search-select
      label="Ejercicio"
      placeholder="Escribí el nombre del ejercicio"
      [options]="choices()"
      [formControl]="control"
      [allowCreate]="allowCreate() && auth.session()?.rol === 'PROFESOR'"
      creationLabel="ejercicio"
      [busy]="loading()"
      (createRequested)="openCreate($event)"
    />
    @if (error()) {
      <p class="error-message" role="alert">{{ error() }}</p>
      <button type="button" class="secondary" (click)="load()">Reintentar ejercicios</button>
    }
    <dialog
      #dialog
      class="dialog exercise-create-dialog"
      [attr.aria-labelledby]="headingId"
      (cancel)="cancel($event)"
    >
      <h2 [id]="headingId">Crear ejercicio</h2>
      <p>El ejercicio quedará guardado y seleccionado en esta rutina.</p>
      <fieldset [formGroup]="form" [disabled]="saving()" class="teacher-fieldset admin-form">
        <label>Nombre del ejercicio<input formControlName="nombre" maxlength="60" /></label>
        @if (creating()) {
          <app-muscle-picker formControlName="grupoId" />
        }
        <label class="wide"
          >Descripción (opcional)<textarea
            formControlName="descripcion"
            maxlength="255"
            rows="3"
          ></textarea>
        </label>
        @if (createError()) {
          <p class="error-message wide" role="alert">{{ createError() }}</p>
        }
        <div class="admin-actions wide">
          <button type="button" class="primary" (click)="save()">
            {{ saving() ? 'Guardando ejercicio…' : 'Guardar ejercicio' }}</button
          ><button type="button" class="secondary" (click)="cancel()">Cancelar creación</button>
        </div>
      </fieldset>
    </dialog>`,
})
export class ExercisePicker implements ControlValueAccessor {
  private readonly api = inject(ProfesorApi);
  private readonly fb = inject(FormBuilder);
  readonly auth = inject(AuthService);
  readonly catalogue = input<D.EjercicioResponse[] | undefined>(undefined);
  readonly allowCreate = input(true);
  readonly created = output<D.EjercicioResponse>();
  readonly headingId = 'exercise-create-' + ++nextExerciseDialog;
  readonly creating = signal(false);
  readonly control = new FormControl('', { nonNullable: true });
  readonly exercises = signal<D.EjercicioResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly createError = signal('');
  readonly selector = viewChild.required(SearchSelect);
  readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly choices = () =>
    this.exercises().map((e) => ({
      value: e.id!,
      label: e.nombre ?? 'Ejercicio',
      detail: e.grupoMuscularNombre ?? e.grupoMuscular ?? '',
    }));
  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, notBlank, Validators.maxLength(60)]],
    grupoId: ['', Validators.required],
    descripcion: ['', Validators.maxLength(255)],
  });
  private touched = () => {};
  constructor() {
    effect(() => {
      const catalogue = this.catalogue();
      if (catalogue !== undefined) this.exercises.set(catalogue);
      else untracked(() => void this.load());
    });
  }
  writeValue(value: unknown) {
    this.control.setValue(value == null ? '' : String(value), { emitEvent: false });
  }
  registerOnChange(fn: (v: string) => void) {
    this.control.valueChanges.subscribe((v) => {
      fn(v);
      this.touched();
    });
  }
  registerOnTouched(fn: () => void) {
    this.touched = fn;
  }
  setDisabledState(value: boolean) {
    value ? this.control.disable({ emitEvent: false }) : this.control.enable({ emitEvent: false });
  }
  async load() {
    this.loading.set(true);
    this.error.set('');
    try {
      this.exercises.set(await firstValueFrom(this.api.exercises()));
    } catch (e) {
      this.error.set(profesorError(e));
    } finally {
      this.loading.set(false);
    }
  }
  openCreate(nombre: string) {
    if (this.auth.session()?.rol !== 'PROFESOR' || !this.allowCreate()) return;
    this.form.reset({ nombre, grupoId: '', descripcion: '' });
    this.createError.set('');
    this.creating.set(true);
    this.dialog().nativeElement.showModal();
  }
  cancel(event?: Event) {
    if (this.saving()) {
      event?.preventDefault();
      return;
    }
    this.dialog().nativeElement.close();
    this.creating.set(false);
    this.selector().focus();
  }
  selectCreated(exercise: D.EjercicioResponse) {
    this.exercises.update((es) => [...es.filter((e) => e.id !== exercise.id), exercise]);
    this.control.setValue(String(exercise.id));
    this.created.emit(exercise);
    this.dialog().nativeElement.close();
    this.creating.set(false);
    this.selector().focus();
  }
  async save() {
    if (this.saving()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.createError.set(
        'Completá el nombre y seleccioná un grupo muscular; la descripción es opcional.',
      );
      return;
    }
    this.saving.set(true);
    this.createError.set('');
    const v = this.form.getRawValue();
    try {
      const fresh = await firstValueFrom(this.api.exercises());
      const existing = fresh.find(
        (e) => normalizedName(e.nombre ?? '') === normalizedName(v.nombre),
      );
      if (existing) {
        this.selectCreated(existing);
        return;
      }
      this.selectCreated(
        await firstValueFrom(
          this.api.createExercise({
            nombre: v.nombre.trim(),
            grupoId: Number(v.grupoId),
            descripcion: v.descripcion.trim(),
          }),
        ),
      );
    } catch (e) {
      this.createError.set(profesorError(e));
    } finally {
      this.saving.set(false);
    }
  }
}
