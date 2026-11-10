import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, it, expect, vi } from 'vitest';
import { Progress } from './progress';
describe('PROFESOR progress requests', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [Progress],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }),
  );
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
  });
  async function setup() {
    const fixture = TestBed.createComponent(Progress);
    fixture.componentRef.setInput('studentId', 9);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/profesores/me/alumnos/9/estadisticas').flush({
      periodo: { desde: '2026-11-01', hasta: '2026-11-07' },
      datos: { entrenamientosConDatosValidos: 0 },
      frecuencia: { entrenamientosPorSemana: '0.0000' },
    });
    http
      .expectOne((r) => r.url.endsWith('/actividad'))
      .flush({ contenido: [], pagina: 0, totalPaginas: 0 });
    http
      .expectOne((r) => r.url.endsWith('/ejercicios'))
      .flush({ contenido: [], pagina: 0, totalPaginas: 0 });
    await Promise.resolve();
    return fixture;
  }
  it('does not fabricate activity points for an empty period', async () => {
    const fixture = await setup();
    expect(fixture.componentInstance.activityPoints()).toEqual([]);
    expect(fixture.componentInstance.frequencyPoints()[0]?.value).toBe('0.0000');
  });
  it('keeps pagination pinned to the consulted period until dates are applied', async () => {
    const fixture = await setup();
    fixture.componentInstance.form.setValue({ desde: '2025-01-01', hasta: '2025-01-07' });
    const pending = fixture.componentInstance.activityPage(1);
    const req = TestBed.inject(HttpTestingController).expectOne((r) =>
      r.url.endsWith('/actividad'),
    );
    expect(req.request.params.get('desde')).toBe('2026-11-01');
    expect(req.request.params.get('pagina')).toBe('1');
    req.flush({ contenido: [], pagina: 1, totalPaginas: 2 });
    await pending;
  });
  it('clears protected chart data after a denied linkage request', async () => {
    const fixture = await setup();
    const spy = vi.spyOn(fixture.componentInstance.denied, 'emit');
    const pending = fixture.componentInstance.chooseExercise(3);
    TestBed.inject(HttpTestingController)
      .expectOne((r) => r.url.endsWith('/ejercicios/3/evolucion'))
      .flush({}, { status: 403, statusText: 'Forbidden' });
    await pending;
    expect(fixture.componentInstance.summary()).toBeNull();
    expect(fixture.componentInstance.evolution()).toBeNull();
    expect(spy).toHaveBeenCalled();
  });
});

describe('Shared statistics concurrency and exact point metadata', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [Progress],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }),
  );
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
  });
  async function setup() {
    const f = TestBed.createComponent(Progress);
    f.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/api/profesores/me/estadisticas')
      .flush({ periodo: { desde: '2026-11-01', hasta: '2026-11-07' } });
    http
      .expectOne((r) => r.url.endsWith('/actividad'))
      .flush({ contenido: [], pagina: 0, totalPaginas: 3 });
    http
      .expectOne((r) => r.url.endsWith('/ejercicios'))
      .flush({ contenido: [], pagina: 0, totalPaginas: 3 });
    await Promise.resolve();
    return f;
  }
  it('keeps the newest activity page when responses arrive out of order', async () => {
    const f = await setup(),
      p = f.componentInstance,
      h = TestBed.inject(HttpTestingController);
    const old = p.activityPage(1),
      latest = p.activityPage(2);
    const requests = h.match((r) => r.url.endsWith('/actividad'));
    requests[1]!.flush({ contenido: [{ fecha: '2026-11-07' }], pagina: 2 });
    await latest;
    requests[0]!.flush({ contenido: [{ fecha: '2026-11-02' }], pagina: 1 });
    await old;
    expect(p.activity()?.pagina).toBe(2);
    expect(p.loading()).toBe(false);
  });
  it('does not let an old page failure clear newer data, error or loading', async () => {
    const f = await setup(),
      p = f.componentInstance,
      h = TestBed.inject(HttpTestingController);
    const old = p.activityPage(1);
    const request = h.expectOne((r) => r.url.endsWith('/actividad'));
    p.form.setValue({ desde: '2026-10-01', hasta: '2026-10-07' });
    const newer = p.load();
    request.flush({}, { status: 403, statusText: 'Forbidden' });
    await old;
    expect(p.error()).toBe('');
    expect(p.loading()).toBe(true);
    h.expectOne((r) => r.url.endsWith('/estadisticas')).flush({
      periodo: { desde: '2026-10-01', hasta: '2026-10-07' },
    });
    h.expectOne((r) => r.url.endsWith('/actividad')).flush({ contenido: [], pagina: 0 });
    h.expectOne((r) => r.url.endsWith('/ejercicios')).flush({ contenido: [], pagina: 0 });
    await newer;
    expect(p.summary()?.periodo?.desde).toBe('2026-10-01');
    expect(p.loading()).toBe(false);
  });
  it('keeps exact API values, coverage, original date and role-specific session links', async () => {
    const f = await setup(),
      p = f.componentInstance;
    f.componentRef.setInput('sessionBase', '/alumno/sesiones');
    p.evolution.set({
      contenido: [
        {
          sesionId: 17,
          alumnoId: 9,
          fechaOriginal: '2026-11-01',
          fechaEfectiva: '2026-11-02',
          nombreHistorico: 'Nombre conservado',
          datos: {
            pesoMaximo: '0',
            volumenRegistrado: '12345678901234567.123',
            volumenCompleto: false,
            seriesSinPeso: 1,
          },
        },
      ],
      pagina: 0,
      tamanio: 30,
      totalElementos: 1,
      totalPaginas: 1,
    });
    const point = p.evolutionPoints('volumenRegistrado')[0]!;
    expect(point.value).toBe('12345678901234567.123');
    expect(point.note).toContain('parcial');
    expect(point.note).toContain('Fecha original: 2026-11-01');
    expect(point.link).toBe('/alumno/sesiones/17');
    expect(p.evolutionPoints('pesoMaximo')[0]?.value).toBe('0');
  });
  it('rejects an oversized window before issuing HTTP calls', async () => {
    const f = await setup();
    f.componentInstance.form.setValue({ desde: '2024-01-01', hasta: '2026-11-07' });
    await f.componentInstance.load();
    expect(f.componentInstance.error()).toContain('366');
    TestBed.inject(HttpTestingController).expectNone(() => true);
  });
});
