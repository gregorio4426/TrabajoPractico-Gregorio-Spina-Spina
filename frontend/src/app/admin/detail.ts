import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { AdminApi, People, Resource } from './admin-api';
import { adminError, at, text } from './admin-utils';
import { CATALOG } from './catalog';
import { Editor, Operation } from './editor';
import { RecordView } from './record-view';
@Component({
  selector: 'app-admin-detail',
  imports: [RouterLink, Editor, RecordView],
  templateUrl: './detail.html',
})
export class Detail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(AdminApi);
  readonly type = this.route.snapshot.data['resource'] as Resource;
  readonly config = CATALOG[this.type];
  readonly people = this.type as People;
  readonly creating = this.route.snapshot.data['creating'] === true;
  readonly id = signal(0);
  readonly data = signal<unknown>(null);
  readonly audit = signal<unknown>(null);
  readonly error = signal('');
  readonly success = signal('');
  readonly loading = signal(false);
  readonly operation = signal<Operation | null>(this.creating ? 'create' : null);
  readonly at = at;
  readonly text = text;
  private request = 0;
  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe((params) => {
      this.id.set(Number(params.get('id')));
      if (!this.creating) void this.load();
    });
  }
  async load() {
    if (!Number.isSafeInteger(this.id()) || this.id() < 1) {
      this.error.set('El ID del registro no es válido.');
      return;
    }
    const version = ++this.request;
    this.loading.set(true);
    this.error.set('');
    this.operation.set(null);
    this.audit.set(null);
    try {
      const data = await firstValueFrom(this.api.get<unknown>(`${this.type}/${this.id()}`));
      if (version === this.request) this.data.set(data);
    } catch (error) {
      if (version === this.request) {
        this.data.set(null);
        this.error.set(adminError(error));
      }
    } finally {
      if (version === this.request) this.loading.set(false);
    }
  }
  open(operation: Operation) {
    this.success.set('');
    this.operation.set(operation);
  }
  async saved() {
    if (this.creating) {
      await this.router.navigate(['/admin', this.type], { queryParams: { q: '', creado: '1' } });
      return;
    }
    await this.load();
    if (!this.error()) this.success.set('Cambio guardado correctamente.');
  }
  cancel() {
    if (this.creating) void this.router.navigate(['/admin', this.type]);
    else this.operation.set(null);
  }
  async loadAudit() {
    this.error.set('');
    try {
      this.audit.set(
        await firstValueFrom(this.api.get<unknown>(`sesiones/${this.id()}/auditoria`)),
      );
    } catch (error) {
      this.error.set(adminError(error));
    }
  }
}
