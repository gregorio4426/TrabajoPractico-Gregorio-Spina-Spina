import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProfesorApi } from './profesor-api';
import { AuthService } from '../core/auth.service';
import { RutinaResponse } from '../admin/contracts';
import { profesorError } from './utils';
@Component({
  selector: 'app-teacher-routines',
  imports: [ReactiveFormsModule, RouterLink],
  template: `<div class="page-intro admin-kicker">
      <div>
        <span class="eyebrow muted">PLANIFICACIÓN</span>
        <h1>Rutinas del gimnasio<span class="accent">.</span></h1>
      </div>
      <a class="primary" routerLink="/profesor/rutinas/nueva">Crear rutina</a>
    </div>
    <p class="admin-help">
      El catálogo compartido devuelve solo familias no archivadas. Solo podés modificar y asignar
      tus propias rutinas. Las archivadas se pueden consultar por su ID o desde un historial.
    </p>
    <form class="admin-filters" [formGroup]="filters" (ngSubmit)="load()">
      <label>Buscar rutina<input type="search" formControlName="q" /></label
      ><label
        >Autoría<select formControlName="scope">
          <option value="own">Mis rutinas</option>
          <option value="all">Todas las disponibles</option>
        </select></label
      ><button class="secondary" [disabled]="loading()">Recargar catálogo</button>
    </form>
    <form class="admin-filters" (submit)="$event.preventDefault(); lookup()">
      <label
        >Consultar ID de rutina<input
          type="number"
          min="1"
          step="1"
          [formControl]="filters.controls.id" /></label
      ><button class="secondary">Consultar por ID</button>
    </form>
    @if (error()) {
      <p class="error-message" role="alert">{{ error() }}</p>
    }
    @if (loading()) {
      <p class="notice" role="status">Cargando catálogo de rutinas…</p>
    }
    @if (!loading() && !error()) {
      <p class="admin-help">
        {{ filtered().length }} rutinas. Filtros y paginación locales sobre el catálogo recibido.
      </p>
      <div class="teacher-history-list">
        @for (r of visible(); track r.id) {
          <div>
            <strong>{{ r.nombre }} · versión {{ r.numeroVersion }}</strong>
            <p>
              {{ r.descripcion }} · Autor: {{ r.profesor }}
              {{ r.profesorId === auth.session()?.perfilId ? '(vos)' : '' }}
            </p>
            <a class="admin-link" [routerLink]="['/profesor/rutinas', r.id]"
              >Ver rutina #{{ r.id }}</a
            >
          </div>
        } @empty {
          <p class="admin-empty">No hay rutinas con estos filtros.</p>
        }
      </div>
      <nav class="admin-pagination" aria-label="Páginas de rutinas">
        <span>Página {{ pages() ? page() + 1 : 0 }} de {{ pages() }}</span>
        <div>
          <button class="secondary" (click)="page.set(page() - 1)" [disabled]="page() === 0">
            Anterior</button
          ><button
            class="secondary"
            (click)="page.set(page() + 1)"
            [disabled]="page() + 1 >= pages()"
          >
            Siguiente
          </button>
        </div>
      </nav>
    }`,
})
export class Routines {
  private readonly api = inject(ProfesorApi);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);
  readonly data = signal<RutinaResponse[]>([]);
  readonly error = signal('');
  readonly loading = signal(false);
  readonly page = signal(0);
  readonly filters = inject(FormBuilder).nonNullable.group({ q: '', scope: 'own', id: '' });
  private readonly criteria = signal(this.filters.getRawValue());
  private seq = 0;
  readonly filtered = computed(() => {
    const c = this.criteria(),
      q = c.q.trim().toLocaleLowerCase('es');
    return this.data().filter(
      (r) =>
        (c.scope === 'all' || r.profesorId === this.auth.session()?.perfilId) &&
        (!q || `${r.nombre} ${r.descripcion} ${r.profesor}`.toLocaleLowerCase('es').includes(q)),
    );
  });
  readonly pages = computed(() => Math.ceil(this.filtered().length / 20));
  readonly visible = computed(() => this.filtered().slice(this.page() * 20, this.page() * 20 + 20));
  constructor() {
    this.filters.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.criteria.set(this.filters.getRawValue());
      this.page.set(0);
    });
    void this.load();
  }
  lookup() {
    const id = Number(this.filters.controls.id.value);
    if (Number.isSafeInteger(id) && id > 0) void this.router.navigate(['/profesor/rutinas', id]);
    else this.error.set('Ingresá un ID entero positivo.');
  }
  async load() {
    const seq = ++this.seq;
    this.data.set([]);
    this.error.set('');
    this.loading.set(true);
    this.page.set(0);
    try {
      const data = await firstValueFrom(this.api.routines());
      if (seq === this.seq) this.data.set(data);
    } catch (e) {
      if (seq === this.seq) this.error.set(profesorError(e));
    } finally {
      if (seq === this.seq) this.loading.set(false);
    }
  }
}
