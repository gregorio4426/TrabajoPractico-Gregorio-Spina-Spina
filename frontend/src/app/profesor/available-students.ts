import { Component, inject, output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ProfesorApi } from './profesor-api';
import { Page } from '../admin/admin-api';
import { profesorError } from './utils';
export interface AvailableStudent {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
}
@Component({
  selector: 'app-available-students',
  imports: [ReactiveFormsModule],
  template: ` <section class="admin-panel">
    <h2>Alumnos sin asignar</h2>
    <p class="admin-help">
      Podés vincular alumnos activos sin profesor. Sus perfiles e historiales quedan disponibles
      después de la vinculación.
    </p>
    <button class="secondary" (click)="show()" [disabled]="busy()">
      {{ opened() ? 'Actualizar disponibles' : 'Buscar alumnos disponibles' }}
    </button>
    @if (opened()) {
      <form class="admin-filters" (submit)="$event.preventDefault(); search()">
        <label>Buscar nombre o email<input type="search" [formControl]="q" maxlength="150" /></label
        ><button class="secondary" [disabled]="busy()">Buscar</button>
      </form>
      @if (error()) {
        <p class="error-message" role="alert">{{ error() }}</p>
      }
      @if (success()) {
        <p class="notice" role="status">{{ success() }}</p>
      }
      @if (busy()) {
        <p role="status">Consultando…</p>
      }
      @for (student of data()?.contenido ?? []; track student.id) {
        <article class="admin-kicker">
          <p>
            <strong>{{ student.nombre }} {{ student.apellido }}</strong> · {{ student.email }}
            <span class="student-badge">Sin asignar</span>
          </p>
          <button class="primary" (click)="link(student)" [disabled]="busy()">
            Sumar a mis alumnos
          </button>
        </article>
      } @empty {
        @if (!busy()) {
          <p class="admin-empty">No hay alumnos disponibles con estos filtros.</p>
        }
      }
      @if (data(); as page) {
        <nav class="admin-pagination" aria-label="Páginas de alumnos disponibles">
          <button
            class="secondary"
            (click)="load(page.pagina - 1)"
            [disabled]="busy() || page.pagina === 0"
          >
            Anterior</button
          ><span>{{ page.totalElementos }} alumnos · Página {{ page.pagina + 1 }}</span
          ><button
            class="secondary"
            (click)="load(page.pagina + 1)"
            [disabled]="busy() || page.pagina + 1 >= page.totalPaginas"
          >
            Siguiente
          </button>
        </nav>
      }
    }
  </section>`,
})
export class AvailableStudents {
  private readonly api = inject(ProfesorApi);
  readonly linked = output<void>();
  readonly q = new FormControl('', { nonNullable: true });
  readonly opened = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly data = signal<Page<AvailableStudent> | null>(null);
  private query = '';
  show() {
    this.opened.set(true);
    this.search();
  }
  search() {
    this.query = this.q.value.trim();
    void this.load(0);
  }
  async load(page: number) {
    this.busy.set(true);
    this.error.set('');
    try {
      this.data.set(await firstValueFrom(this.api.availableStudents(this.query, page)));
    } catch (e) {
      this.error.set(profesorError(e));
      this.data.set(null);
    } finally {
      this.busy.set(false);
    }
  }
  async link(student: AvailableStudent) {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    try {
      await firstValueFrom(this.api.linkAvailableStudent(student.id));
      this.success.set(student.nombre + ' ya está en Mis alumnos.');
      this.linked.emit();
      await this.load(0);
    } catch (e) {
      this.error.set(profesorError(e));
    } finally {
      this.busy.set(false);
    }
  }
}
