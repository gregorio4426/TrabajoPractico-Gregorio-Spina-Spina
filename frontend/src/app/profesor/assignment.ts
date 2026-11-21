import { Component, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ProfesorApi } from './profesor-api';
import { AuthService } from '../core/auth.service';
import * as D from '../admin/contracts';
import { AssignmentWeights } from './assignment-weights';
import { profesorError } from './utils';
@Component({
  selector: 'app-assignment-editor',
  imports: [RouterLink, AssignmentWeights],
  template: `
    <a class="admin-link" routerLink="/profesor/alumnos">← Volver a alumnos</a>
    <h1>Pesos de la asignación</h1>
    @if (loading()) {
      <p class="notice" role="status">Cargando asignación…</p>
    }
    @if (error()) {
      <p class="error-message" role="alert">{{ error() }}</p>
      <button class="secondary" (click)="load()">Recargar asignación</button>
    }
    @if (success()) {
      <p class="notice" role="status">{{ success() }}</p>
    }
    @if (data(); as r) {
      <h2>{{ r.alumnoNombre }} · {{ r.nombre }} · Versión {{ r.numeroVersion }}</h2>
      @if (r.activa && r.profesorId === auth.session()?.perfilId) {
        <fieldset [disabled]="pending()" class="teacher-fieldset">
          <app-assignment-weights #weights [routine]="r" [existing]="true" />
          <button class="primary" (click)="ask()">Guardar pesos individuales</button>
        </fieldset>
      } @else {
        <p class="notice">
          Solo el autor puede editar los pesos de una asignación activa de un alumno vinculado.
        </p>
      }
    }
  `,
})
export class AssignmentEditor {
  private readonly api = inject(ProfesorApi);
  readonly auth = inject(AuthService);
  readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly data = signal<D.RutinaResponse | null>(null);
  readonly loading = signal(false);
  readonly pending = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly weights = viewChild<AssignmentWeights>('weights');
  constructor() {
    void this.load();
  }
  async load() {
    this.data.set(null);
    this.loading.set(true);
    this.error.set('');
    try {
      this.data.set(await firstValueFrom(this.api.assignment(this.id)));
    } catch (e) {
      this.error.set(profesorError(e));
    } finally {
      this.loading.set(false);
    }
  }
  ask() {
    if (this.pending() || !this.weights()) return;
    this.weights()!.form.markAllAsTouched();
    if (this.weights()!.form.invalid) {
      this.error.set('Los pesos deben ser finitos y no negativos, o quedar vacíos.');
      return;
    }
    void this.save();
  }
  async save() {
    if (this.pending() || !this.weights()) return;
    this.pending.set(true);
    this.error.set('');
    this.success.set('');
    try {
      const result = await firstValueFrom(
        this.api.updateAssignmentWeights(this.id, this.weights()!.payload()),
      );
      this.data.set(result);
      this.success.set('Pesos individuales guardados.');
    } catch (e) {
      this.error.set(profesorError(e));
    } finally {
      this.pending.set(false);
    }
  }
}
