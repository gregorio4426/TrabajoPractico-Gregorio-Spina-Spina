import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, it, expect, vi } from 'vitest';
import { Progress } from './progress';
describe('PROFESOR progress requests', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [Progress],
      providers: [provideHttpClient(), provideHttpClientTesting()],
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
    http
      .expectOne('/api/profesores/me/alumnos/9/estadisticas')
      .flush({
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
