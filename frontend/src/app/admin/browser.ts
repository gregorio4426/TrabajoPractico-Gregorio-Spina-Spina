import { EntityPicker, entityOption } from '../shared/entity-picker';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormControl, FormRecord, ReactiveFormsModule } from '@angular/forms';
import { combineLatest, firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdminApi, Page, Resource, Query } from './admin-api';
import { CATALOG, label } from './catalog';
import { adminError, at, text, idOf } from './admin-utils';
import { RecordView } from './record-view';
@Component({
  selector: 'app-admin-browser',
  imports: [ReactiveFormsModule, RouterLink, RecordView, EntityPicker],
  templateUrl: './browser.html',
})
export class Browser {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(AdminApi);
  readonly type = this.route.snapshot.data['resource'] as Resource;
  readonly config = CATALOG[this.type];
  readonly created = this.route.snapshot.queryParamMap.get('creado') === '1';
  readonly sub = this.route.snapshot.data['sub'] as string | undefined;
  readonly parent = this.route.snapshot.data['parent'] as Resource | undefined;
  readonly filters =
    this.sub === 'versiones'
      ? []
      : this.config.filters.filter(
          (f) =>
            !(this.parent === 'alumnos' && f.key === 'alumnoId') &&
            !(this.parent === 'profesores' && f.key === 'profesorId'),
        );
  readonly form = new FormRecord<FormControl<string>>({});
  readonly data = signal<Page<unknown> | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly selected = signal<unknown>(null);
  readonly text = text;
  readonly label = label;
  readonly at = at;
  readonly idOf = idOf;
  readonly names = signal(new Map<string, string>());
  readonly id = signal(0);
  readonly page = signal(0);
  private request = 0;
  readonly isPeople = ['usuarios', 'alumnos', 'profesores'].includes(this.type);
  constructor() {
    for (const filter of this.filters)
      this.form.addControl(filter.key, new FormControl('', { nonNullable: true }));
    for (const [key, value] of Object.entries({
      orden: this.sub === 'versiones' ? 'numeroVersion' : 'id',
      direccion: 'asc',
      tamanio: '20',
    }))
      this.form.addControl(key, new FormControl(value, { nonNullable: true }));
    combineLatest([this.route.paramMap, this.route.queryParamMap])
      .pipe(takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(([params, query]) => {
        this.id.set(Number(params.get('id')));
        this.page.set(Math.max(0, Number(query.get('pagina')) || 0));
        for (const [key, control] of Object.entries(this.form.controls))
          control.setValue(
            query.get(key) ??
              (key === 'tamanio'
                ? '20'
                : key === 'orden'
                  ? this.sub === 'versiones'
                    ? 'numeroVersion'
                    : 'id'
                  : key === 'direccion'
                    ? 'asc'
                    : ''),
          );
        void this.load();
      });
  }
  async load() {
    const query: Query = { ...this.form.getRawValue(), pagina: this.page() };
    if (query['desde'] && query['hasta'] && String(query['desde']) > String(query['hasta'])) {
      this.error.set('La fecha desde debe ser anterior o igual a hasta.');
      return;
    }
    for (const f of this.filters)
      if (
        f.type === 'number' &&
        query[f.key] &&
        (!Number.isSafeInteger(Number(query[f.key])) || Number(query[f.key]) < 1)
      ) {
        this.error.set('Los IDs deben ser enteros positivos.');
        return;
      }
    const request = ++this.request;
    this.data.set(null);
    this.names.set(new Map());
    this.loading.set(true);
    this.error.set('');
    this.selected.set(null);
    try {
      const path = this.sub ? `${this.parent}/${this.id()}/${this.sub}` : this.type;
      const data = await firstValueFrom(this.api.get<Page<unknown>>(path, query));
      const names = new Map<string, string>();
      await Promise.all(
        data.contenido.flatMap((row) =>
          ['alumnoId', 'usuarioId', 'actorId'].map(async (key) => {
            const id = at(row, key);
            if (!id) return;
            const resource = key === 'alumnoId' ? 'alumnos' : 'usuarios';
            const cacheKey = key + ':' + id;
            if (names.has(cacheKey)) return;
            try {
              const person = await firstValueFrom(this.api.get<unknown>(resource + '/' + id));
              const p = at(person, 'perfil') ?? person;
              names.set(
                cacheKey,
                [at(p, 'nombre'), at(p, 'apellido')].filter(Boolean).join(' ') ||
                  String(at(p, 'email') ?? 'Cuenta'),
              );
            } catch {
              names.set(cacheKey, 'Registro no disponible');
            }
          }),
        ),
      );
      if (request === this.request) {
        this.names.set(names);
        this.data.set(data);
      }
    } catch (error) {
      if (request === this.request) {
        this.data.set(null);
        this.error.set(adminError(error));
      }
    } finally {
      if (request === this.request) this.loading.set(false);
    }
  }
  apply(page = 0) {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ...this.form.getRawValue(), pagina: page },
    });
  }
  displayCell(row: unknown, key: string) {
    if (key === 'vinculacion') { const professors = at(row, 'perfil.profesores'); return Array.isArray(professors) && professors.length ? 'Asignado' : 'Sin asignar'; }
    return key.endsWith('Id')
      ? (this.names().get(key + ':' + at(row, key)) ?? 'Registro')
      : text(at(row, key));
  }
  rowKey(row: unknown): string {
    return this.sub === 'versiones' ? String(at(row, 'rutinaVersionId')) : String(idOf(row));
  }
  detail(row: unknown) {
    if (this.type === 'auditoria' || this.sub === 'versiones') this.selected.set(row);
    else void this.router.navigate(['/admin', this.type, idOf(row)]);
  }
  readonly orders = this.sub === 'versiones' ? ['id', 'numeroVersion'] : this.config.orders;
}
