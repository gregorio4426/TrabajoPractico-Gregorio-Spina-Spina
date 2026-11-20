import {
  Component,
  computed,
  effect,
  forwardRef,
  input,
  output,
  signal,
  ElementRef,
  viewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import type { EntityOption } from './entity-picker';
export const normalizedName = (value: string) =>
  value.normalize('NFD').replace(/\p{M}/gu, '').trim().replace(/\s+/g, ' ').toLocaleLowerCase('es');
let nextSearchSelect = 0;
@Component({
  selector: 'app-search-select',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SearchSelect), multi: true },
  ],
  template: ` <div
    class="search-select"
    (keydown.escape)="opened() ? close($event) : null"
    (focusout)="leave($event)"
  >
    <label
      >{{ label()
      }}<input
        #field
        role="combobox"
        type="text"
        autocomplete="off"
        [value]="query()"
        [disabled]="disabled() || busy()"
        [placeholder]="placeholder()"
        aria-autocomplete="list"
        [attr.aria-expanded]="opened()"
        [attr.aria-controls]="listId"
        [attr.aria-activedescendant]="opened() && itemCount() ? listId + '-' + active() : null"
        (focus)="open()"
        (input)="type($any($event.target).value)"
        (keydown)="key($event)"
    /></label>
    @if (value() !== null && !disabled()) {
      <button
        type="button"
        class="secondary search-select-clear"
        [attr.aria-label]="'Quitar selección de ' + label()"
        (click)="clear()"
      >
        Quitar selección
      </button>
    }
    @if (opened()) {
      <div
        class="search-select-panel"
        [id]="listId"
        role="listbox"
        [attr.aria-label]="'Opciones de ' + label()"
      >
        @if (busy()) {
          <p role="status">Cargando opciones…</p>
        }
        @for (option of matches(); track option.value; let i = $index) {
          <button
            type="button"
            role="option"
            tabindex="-1"
            [id]="listId + '-' + i"
            [attr.aria-selected]="value() === option.value"
            [class.is-active]="active() === i"
            (mouseenter)="active.set(i)"
            (click)="choose(option)"
            (keydown)="key($event)"
          >
            <strong>{{ option.label }}</strong>
            @if (option.detail) {
              <small>{{ option.detail }}</small>
            }
          </button>
        } @empty {
          @if (!busy()) {
            <p class="admin-empty">No hay coincidencias.</p>
          }
        }
        @if (canOfferCreate()) {
          <button
            type="button"
            role="option"
            tabindex="-1"
            [id]="listId + '-' + matches().length"
            aria-selected="false"
            [class.is-active]="active() === matches().length"
            class="search-select-create"
            (click)="create()"
            (keydown)="key($event)"
          >
            Crear {{ creationLabel() }}: {{ query().trim() }}
          </button>
        }
        <button type="button" class="secondary" (click)="close()">Cerrar sugerencias</button>
      </div>
    }
  </div>`,
})
export class SearchSelect implements ControlValueAccessor {
  readonly label = input('Seleccionar');
  readonly placeholder = input('Escribí para buscar');
  readonly options = input<readonly EntityOption[]>([]);
  readonly allowCreate = input(false);
  readonly creationLabel = input('opción');
  readonly busy = input(false);
  readonly createRequested = output<string>();
  readonly listId = 'search-select-' + ++nextSearchSelect;
  readonly query = signal('');
  readonly opened = signal(false);
  readonly disabled = signal(false);
  readonly value = signal<number | null>(null);
  readonly active = signal(0);
  readonly matches = computed(() => {
    const q = normalizedName(this.query());
    return this.options().filter(
      (o) => !q || normalizedName(o.label + ' ' + (o.detail ?? '')).includes(q),
    );
  });
  readonly canOfferCreate = computed(
    () =>
      this.allowCreate() &&
      !!this.query().trim() &&
      !this.busy() &&
      !this.options().some((o) => normalizedName(o.label) === normalizedName(this.query())),
  );
  readonly itemCount = computed(() => this.matches().length + (this.canOfferCreate() ? 1 : 0));
  readonly field = viewChild.required<ElementRef<HTMLInputElement>>('field');
  focus() {
    if (!this.disabled()) this.field().nativeElement.focus();
    this.opened.set(false);
  }
  private change: (value: string) => void = () => {};
  private touched = () => {};
  constructor() {
    effect(() => {
      const value = this.value(),
        options = this.options();
      if (value !== null) {
        const option = options.find((o) => o.value === value);
        if (option) this.query.set(option.label);
      }
    });
  }
  writeValue(value: unknown) {
    const id = Number(value);
    this.value.set(
      value !== null && value !== '' && Number.isSafeInteger(id) && id > 0 ? id : null,
    );
    if (this.value() === null) this.query.set('');
  }
  registerOnChange(fn: (v: string) => void) {
    this.change = fn;
  }
  registerOnTouched(fn: () => void) {
    this.touched = fn;
  }
  setDisabledState(value: boolean) {
    this.disabled.set(value);
    if (value) this.opened.set(false);
  }
  open() {
    if (!this.disabled()) {
      this.opened.set(true);
      this.active.set(0);
    }
  }
  type(value: string) {
    this.query.set(value);
    this.value.set(null);
    this.change('');
    this.touched();
    this.open();
  }
  choose(option: EntityOption) {
    this.value.set(option.value);
    this.query.set(option.label);
    this.change(String(option.value));
    this.touched();
    this.opened.set(false);
    this.focus();
  }
  clear() {
    this.value.set(null);
    this.query.set('');
    this.change('');
    this.touched();
    this.open();
  }
  create() {
    if (!this.canOfferCreate()) return;
    this.opened.set(false);
    this.createRequested.emit(this.query().trim());
  }
  leave(event: FocusEvent) {
    if (!(event.currentTarget as HTMLElement).contains(event.relatedTarget as Node | null))
      this.close();
  }
  close(event?: Event) {
    event?.preventDefault();
    event?.stopPropagation();
    this.opened.set(false);
    this.touched();
  }
  key(event: KeyboardEvent) {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      event.stopPropagation();
      if (!this.opened()) this.open();
      else if (this.itemCount())
        this.active.set(
          (this.active() + (event.key === 'ArrowDown' ? 1 : -1) + this.itemCount()) %
            this.itemCount(),
        );
    } else if (event.key === 'Home' || event.key === 'End') {
      if (this.opened()) {
        event.preventDefault();
        this.active.set(event.key === 'Home' ? 0 : Math.max(0, this.itemCount() - 1));
      }
    } else if (event.key === 'Enter') {
      event.preventDefault();
      event.stopPropagation();
      if (!this.opened()) {
        this.open();
        return;
      }
      const option = this.matches()[this.active()];
      if (option) this.choose(option);
      else if (this.active() === this.matches().length) this.create();
    } else if (event.key === 'Tab') this.close();
    if (this.opened()) {
      this.field()
        .nativeElement.ownerDocument.getElementById(this.listId + '-' + this.active())
        ?.scrollIntoView?.({ block: 'nearest' });
    }
  }
}
