import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, it, expect } from 'vitest';
import { Dashboard } from './dashboard';
describe('ADMIN dashboard data semantics', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }),
  );
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
  });
  it('preserves exact decimal strings and distinguishes unknown volume from zero', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    TestBed.inject(HttpTestingController)
      .expectOne('/api/admin/dashboard')
      .flush({
        estadoActual: { cuentasActivas: 0 },
        periodo: { desde: '2026-10-01', hasta: '2026-10-30' },
        entrenamientos: { sesionesRegistradas: 0, volumenRegistrado: null, volumenCompleto: false },
        frecuencia: { entrenamientosPorSemana: '12345678901234567890.1234' },
      });
    await Promise.resolve();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('12345678901234567890.1234');
    expect(fixture.nativeElement.textContent).toContain('Sin dato');
    expect(fixture.nativeElement.textContent).toContain('No hay sesiones en este período');
  });
  it('does not request an inverted period', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    TestBed.inject(HttpTestingController).expectOne('/api/admin/dashboard').flush({});
    await Promise.resolve();
    fixture.componentInstance.filters.setValue({ desde: '2026-11-07', hasta: '2026-10-01' });
    await fixture.componentInstance.load();
    expect(fixture.componentInstance.error()).toContain('anterior o igual');
    TestBed.inject(HttpTestingController).expectNone((r) => r.url === '/api/admin/dashboard');
  });
});
