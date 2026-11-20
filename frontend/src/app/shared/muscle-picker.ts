import {
  Component,
  effect,
  forwardRef,
  inject,
  input,
  output,
  signal,
  untracked,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormControl,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
} from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { SearchSelect } from './search-select';
import { ProfesorApi } from '../profesor/profesor-api';
import { AuthService } from '../core/auth.service';
import { profesorError } from '../profesor/utils';
export interface MuscleOption {
  id: number;
  nombre: string;
}
@Component({
  selector: 'app-muscle-picker',
  imports: [SearchSelect, ReactiveFormsModule],
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MusclePicker), multi: true },
  ],
  template: ` <app-search-select
      [label]="label()"
      placeholder="Buscar grupo muscular"
      [options]="choices()"
      [allowCreate]="allowCreate() && auth.session()?.rol === 'PROFESOR'"
      creationLabel="grupo muscular"
      [busy]="loading()"
      [formControl]="control"
      (createRequested)="create($event)"
    />
    @if (error()) {
      <p class="error-message" role="alert">{{ error() }}</p>
      <button type="button" class="secondary" (click)="load()">Reintentar grupos musculares</button>
    }`,
})
export class MusclePicker implements ControlValueAccessor {
  private readonly api = inject(ProfesorApi);
  readonly auth = inject(AuthService);
  readonly label = input('Grupo muscular');
  readonly allowCreate = input(true);
  readonly supplied = input<MuscleOption[] | undefined>(undefined);
  readonly created = output<MuscleOption>();
  readonly groups = signal<MuscleOption[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly control = new FormControl('', { nonNullable: true });
  readonly choices = () => this.groups().map((g) => ({ value: g.id, label: g.nombre }));
  private touched = () => {};
  constructor() {
    effect(() => {
      const supplied = this.supplied();
      if (supplied !== undefined) this.groups.set(supplied);
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
      this.groups.set(await firstValueFrom(this.api.muscleGroups()));
    } catch (e) {
      this.error.set(profesorError(e));
    } finally {
      this.loading.set(false);
    }
  }
  async create(nombre: string) {
    if (this.loading() || this.auth.session()?.rol !== 'PROFESOR') return;
    this.loading.set(true);
    this.error.set('');
    try {
      const group = await firstValueFrom(this.api.createMuscleGroup(nombre));
      this.groups.update((gs) => [...gs.filter((g) => g.id !== group.id), group]);
      this.control.setValue(String(group.id));
      this.created.emit(group);
    } catch (e) {
      this.error.set(profesorError(e));
    } finally {
      this.loading.set(false);
    }
  }
}
