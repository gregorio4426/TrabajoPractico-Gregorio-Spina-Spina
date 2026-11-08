import { volumeCoverage } from '../shared/statistics-values';
import { statisticsPeriodError } from '../shared/statistics-period';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AdminApi } from './admin-api';
import { DashboardAdmin } from './contracts';
import { adminError, text } from './admin-utils';
@Component({
  selector: 'app-admin-dashboard',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './dashboard.html',
})
export class Dashboard {
  private readonly api = inject(AdminApi);
  readonly data = signal<DashboardAdmin | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly filters = inject(FormBuilder).nonNullable.group({ desde: '', hasta: '' });
  readonly text = text;
  readonly volumeCoverage = volumeCoverage;
  private request = 0;
  constructor() {
    void this.load();
  }
  async load() {
    const dates = this.filters.getRawValue();
    const periodError = statisticsPeriodError(dates.desde, dates.hasta);
    if (periodError) {
      this.error.set(periodError);
      return;
    }
    const version = ++this.request;
    this.loading.set(true);
    this.error.set('');
    this.data.set(null);
    try {
      const result = await firstValueFrom(this.api.dashboard(dates));
      if (version === this.request) this.data.set(result);
    } catch (error) {
      if (version === this.request) {
        this.data.set(null);
        this.error.set(adminError(error));
      }
    } finally {
      if (version === this.request) this.loading.set(false);
    }
  }
}
