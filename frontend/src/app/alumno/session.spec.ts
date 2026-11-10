import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, it, expect } from 'vitest';
import { StudentSession } from './session';
import { SESSION_KEY } from '../core/auth.service';
import { testSession } from '../core/auth.fixtures';
import { SesionResponse } from '../admin/contracts';
const seed: SesionResponse = {
  id: 7,
  alumnoId: 1,
  rutinaId: 3,
  numeroVersion: 2,
  diaRutinaId: 15,
  revision: 4,
  fecha: '2026-11-01',
  finalizada: false,
  series: [],
  seriesEsperadas: 2,
  seriesRealizadas: 0,
};
const row = {
  id: 9,
  ejercicioId: 3,
  diaEjercicioId: 20,
  numeroSerie: 1,
  repsHechas: 8,
  pesoUsado: 20,
  completada: true,
  revision: 2,
};
describe('ALUMNO session recovery and revisions', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    localStorage.setItem(SESSION_KEY, JSON.stringify(testSession('ALUMNO')));
    TestBed.configureTestingModule({
      imports: [StudentSession],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '7' })) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
    localStorage.clear();
  });
  async function flushSession(data = seed) {
    http.expectOne('/api/sesiones/7').flush(data);
    await Promise.resolve();
    if (data.alumnoId === 1) {
      http
        .expectOne('/api/rutinas/3/versiones/2')
        .flush({
          dias: [
            {
              id: 15,
              nombre: 'Día original',
              ejercicios: [
                {
                  id: 20,
                  ejercicioId: 3,
                  ejercicioNombre: 'Press',
                  seriesSugeridas: 2,
                  repsSugeridas: 8,
                  pesoSugerido: 20,
                },
              ],
            },
          ],
        });
      http.expectOne('/api/sesiones/7/auditoria').flush([]);
    }
    await Promise.resolve();
  }
  async function setup(data = seed) {
    const fixture = TestBed.createComponent(StudentSession);
    await flushSession(data);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }
  it('resumes from the server and reads the exact version rather than the current routine', async () => {
    const fixture = await setup({ ...seed, series: [row] });
    expect(fixture.componentInstance.data()?.series?.length).toBe(1);
    expect(fixture.componentInstance.day()?.nombre).toBe('Día original');
    http.expectNone('/api/rutinas/me/activa');
  });
  it('does not request prescriptions or audits for a foreign session', async () => {
    const fixture = await setup({ ...seed, alumnoId: 99 });
    expect(fixture.componentInstance.data()).toBeNull();
    expect(fixture.componentInstance.error()).toContain('propias');
    http.expectNone('/api/rutinas/3/versiones/2');
  });
  it('never prefills actual repetitions or weights from suggested values', async () => {
    const fixture = await setup();
    const component = fixture.componentInstance;
    component.choose(component.day()!.ejercicios![0]!);
    http
      .expectOne('/api/sesiones/me/ejercicios/3/ultima')
      .flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.addForm.controls.numeroSerie.value).toBe(1);
    expect(component.addForm.controls.repsHechas.value).toBeNull();
    expect(component.addForm.controls.pesoUsado.value).toBeNull();
  });
  it('keeps a blank weight null when writing and refreshes confirmed records', async () => {
    const fixture = await setup();
    const component = fixture.componentInstance;
    component.choose(component.day()!.ejercicios![0]!);
    http
      .expectOne('/api/sesiones/me/ejercicios/3/ultima')
      .flush({}, { status: 404, statusText: 'Not Found' });
    component.addForm.patchValue({ numeroSerie: 1, repsHechas: 8, pesoUsado: null });
    const pending = component.save();
    const req = http.expectOne('/api/sesiones/7/series');
    expect(req.request.body).toEqual({
      diaEjercicioId: 20,
      numeroSerie: 1,
      repsHechas: 8,
      pesoUsado: null,
    });
    req.flush(row);
    await Promise.resolve();
    await flushSession({ ...seed, series: [row], revision: 5 });
    await pending;
    expect(component.success()).toContain('registrada');
  });
  it('uses the series revision for correction and never retries a 409 automatically', async () => {
    const fixture = await setup({ ...seed, series: [row] });
    const component = fixture.componentInstance;
    component.editSeries(row, 'correct');
    component.correction.patchValue({ repsHechas: 9, motivo: 'Corregir anotación' });
    const pending = component.save();
    const req = http.expectOne('/api/sesiones/7/series/9');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.revision).toBe(2);
    req.flush({}, { status: 409, statusText: 'Conflict' });
    await pending;
    expect(component.error()).toContain('Recargá');
    http.expectNone('/api/sesiones/7');
  });
  it('uses the session revision, not the series revision, for effective date changes', async () => {
    const fixture = await setup({ ...seed, series: [row] });
    const component = fixture.componentInstance;
    component.editDate();
    component.dateForm.setValue({ fecha: '2026-10-31', motivo: 'Fecha corregida' });
    const pending = component.save();
    const req = http.expectOne('/api/sesiones/7/correccion');
    expect(req.request.body.revision).toBe(4);
    req.flush({}, { status: 409, statusText: 'Conflict' });
    await pending;
  });
  it('blocks empty finalization before a request is sent', async () => {
    const fixture = await setup();
    fixture.componentInstance.ask('finish');
    expect(fixture.componentInstance.error()).toContain('al menos una serie');
    http.expectNone('/api/sesiones/7/completar');
  });
  it('does not offer annulment of the last valid series in a closed session', async () => {
    const fixture = await setup({ ...seed, finalizada: true, series: [row] });
    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
    ) as HTMLButtonElement[];
    expect(buttons.find((b) => b.textContent?.includes('Anular serie #9'))?.disabled).toBe(true);
  });
});
