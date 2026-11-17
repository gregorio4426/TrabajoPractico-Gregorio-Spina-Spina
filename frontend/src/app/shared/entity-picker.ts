import {
  Component,
  ElementRef,
  effect,
  forwardRef,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormControl,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
} from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { API_BASE } from '../core/api';
import { adminError } from '../admin/admin-utils';
export interface EntityOption {
  value: number;
  label: string;
  detail?: string;
}
export type EntityKind = 'alumnos' | 'profesores' | 'rutinas' | 'ejercicios' | 'usuarios';
export function entityOption(row: any): EntityOption {
  const p = row.perfil ?? row;
  return {
    value: Number(p.id),
    label: [p.nombre, p.apellido].filter(Boolean).join(' ') || row.email || 'Registro',
    detail: [row.cuenta?.email ?? p.email, p.especialidad, p.grupoMuscular, row.profesor]
      .filter(Boolean)
      .join(' · '),
  };
}
@Component({
  selector: 'app-entity-picker',
  imports: [ReactiveFormsModule],
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => EntityPicker), multi: true },
  ],
  template: `<div class="entity-picker" (keydown.escape)="close($event)">
    <button
      type="button"
      class="secondary entity-picker-trigger"
      #trigger
      [disabled]="disabled()"
      (click)="toggle()"
      [attr.aria-expanded]="opened()"
      [attr.aria-label]="label() + ': ' + (selected()?.label ?? 'Seleccionar')"
    >
      {{ selected()?.label ?? 'Seleccionar ' + label().toLowerCase() }}
      <span aria-hidden="true">⌕</span>
    </button>
    @if (selected() && !disabled()) {
      <button
        type="button"
        class="secondary"
        (click)="clear()"
        [attr.aria-label]="'Quitar selección de ' + label()"
      >
        Quitar selección
      </button>
    }
    @if (opened()) {
      <section class="entity-picker-panel" [attr.aria-label]="'Seleccionar ' + label()">
        <label
          >{{ 'Buscar ' + label().toLowerCase()
          }}<input
            type="search"
            [formControl]="search"
            (input)="load(0)"
            (keydown.enter)="$event.preventDefault(); load(0)"
            placeholder="Nombre, apellido o email"
        /></label>
        @if (loading()) {
          <p role="status">Buscando registros…</p>
        }
        @if (error()) {
          <p class="error-message" role="alert">{{ error() }}</p>
          <button type="button" class="secondary" (click)="load(0)">Reintentar búsqueda</button>
        }
        @for (item of rows(); track item.value) {
          <button
            type="button"
            class="entity-picker-option"
            (click)="choose(item)"
            [attr.aria-pressed]="value === item.value"
          >
            <strong>{{ item.label }}</strong>
            @if (item.detail) {
              <small>{{ item.detail }}</small>
            }
          </button>
        } @empty {
          @if (!loading() && !error()) {
            <p class="admin-empty">No hay coincidencias.</p>
          }
        }
        <div class="admin-actions">
          <button
            type="button"
            class="secondary"
            (click)="load(page() - 1)"
            [disabled]="loading() || page() === 0"
          >
            Resultados anteriores</button
          ><button
            type="button"
            class="secondary"
            (click)="load(page() + 1)"
            [disabled]="loading() || page() + 1 >= pages()"
          >
            Más resultados</button
          ><button type="button" class="secondary" (click)="close()">Cerrar listado</button>
        </div>
      </section>
    }
  </div>`,
})
export class EntityPicker implements ControlValueAccessor {
  readonly parentStudentId = input<number | null>(null);
  readonly kind = input<EntityKind>('alumnos');
  readonly label = input('Registro');
  readonly options = input<EntityOption[] | undefined>(undefined);
  private readonly trigger = viewChild<ElementRef<HTMLButtonElement>>('trigger');
  close(event?: Event) {
    event?.preventDefault();
    event?.stopPropagation();
    this.opened.set(false);
    this.trigger()?.nativeElement.focus();
  }
  private http = inject(HttpClient);
  private base = inject(API_BASE);
  private auth = inject(AuthService);
  readonly search = new FormControl('', { nonNullable: true });
  readonly opened = signal(false);
  readonly disabled = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly selected = signal<EntityOption | null>(null);
  readonly rows = signal<EntityOption[]>([]);
  readonly page = signal(0);
  readonly pages = signal(0);
  value: number | null = null;
  private seq = 0;
  private change: (v: string) => void = () => {};
  private touched = () => {};
  constructor() {
    effect(() => {
      const choices = this.options();
      if (choices && this.value)
        this.selected.set(choices.find((o) => o.value === this.value) ?? this.selected());
    });
  }
  writeValue(v: unknown) {
    this.value = v == null || v === '' ? null : Number(v);
    this.selected.set(this.options()?.find((o) => o.value === this.value) ?? null);
    if (Number.isSafeInteger(this.value) && Number(this.value) > 0 && !this.options())
      void this.resolve();
  }
  registerOnChange(fn: (v: string) => void) {
    this.change = fn;
  }
  registerOnTouched(fn: () => void) {
    this.touched = fn;
  }
  setDisabledState(v: boolean) {
    this.disabled.set(v);
  }
  toggle() {
    this.touched();
    this.opened.update((v) => !v);
    if (this.opened()) void this.load(0);
  }
  choose(option: EntityOption) {
    this.value = option.value;
    this.selected.set(option);
    this.change(String(option.value));
    this.touched();
    this.close();
  }
  clear() {
    this.value = null;
    this.selected.set(null);
    this.change('');
    this.touched();
  }
  private path() {
    if (
      this.auth.session()?.rol === 'ADMIN' &&
      this.kind() === 'profesores' &&
      this.parentStudentId()
    )
      return '/admin/alumnos/' + this.parentStudentId() + '/profesores';
    return this.auth.session()?.rol === 'ADMIN' && this.kind() !== 'ejercicios'
      ? '/admin/' + this.kind()
      : this.kind() === 'alumnos'
        ? '/profesores/me/alumnos'
        : '/' + this.kind();
  }
  private async resolve() {
    const value = this.value;
    try {
      const row = await firstValueFrom(this.http.get<any>(this.base + this.path() + '/' + value));
      if (this.value === value) this.selected.set(entityOption(row));
    } catch {
      /* Search remains available if a previously selected record is no longer accessible. */
    }
  }
  async load(page = 0) {
    const seq = ++this.seq;
    this.loading.set(true);
    this.error.set('');
    this.rows.set([]);
    this.page.set(page);
    try {
      let options = this.options();
      const q = this.search.value.trim().toLocaleLowerCase('es');
      if (!options) {
        const remote = this.path().startsWith('/admin/');
        const params = remote
          ? new HttpParams().set('q', q).set('pagina', page).set('tamanio', 10)
          : new HttpParams();
        const result = await firstValueFrom(
          this.http.get<any>(this.base + this.path(), { params }),
        );
        if (seq !== this.seq) return;
        if (remote) {
          this.rows.set(result.contenido.map(entityOption));
          this.pages.set(result.totalPaginas);
          return;
        }
        options = result.map(entityOption);
      }
      const filtered = options!.filter(
        (o) => !q || (o.label + ' ' + (o.detail ?? '')).toLocaleLowerCase('es').includes(q),
      );
      if (seq === this.seq) {
        this.pages.set(Math.ceil(filtered.length / 10));
        this.rows.set(filtered.slice(page * 10, page * 10 + 10));
      }
    } catch (e) {
      if (seq === this.seq) this.error.set(adminError(e));
    } finally {
      if (seq === this.seq) this.loading.set(false);
    }
  }
}
