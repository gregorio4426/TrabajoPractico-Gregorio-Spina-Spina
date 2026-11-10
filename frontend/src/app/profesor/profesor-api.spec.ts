import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, it, expect } from 'vitest';
import { ProfesorApi } from './profesor-api';
describe('PROFESOR HTTP contracts', () => {
  let api: ProfesorApi;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ProfesorApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });
  it('loads only the linked students, without invented pagination parameters', () => {
    api.students().subscribe();
    const r = http.expectOne('/api/profesores/me/alumnos');
    expect(r.request.params.keys()).toEqual([]);
    r.flush([]);
  });
  it('uses the real teacher dashboard and explicit date range', () => {
    api.dashboard({ desde: '2026-11-01', hasta: '2026-11-07' }).subscribe();
    const r = http.expectOne((r) => r.url === '/api/profesores/me/dashboard');
    expect(r.request.params.get('desde')).toBe('2026-11-01');
    r.flush({});
  });
  it('scopes student statistics to the currently authorized teacher', () => {
    api.evolution(9, 3, { pagina: 2, tamanio: 30 }).subscribe();
    const r = http.expectOne(
      (r) => r.url === '/api/profesores/me/alumnos/9/estadisticas/ejercicios/3/evolucion',
    );
    expect(r.request.params.get('pagina')).toBe('2');
    r.flush({ contenido: [] });
  });
  it('archives a routine with DELETE and no invented physical deletion body', () => {
    api.archive(7).subscribe();
    const r = http.expectOne('/api/rutinas/7');
    expect(r.request.method).toBe('DELETE');
    r.flush(null, { status: 204, statusText: 'No Content' });
  });
  it('assigns own routine to a linked student using the documented POST route', () => {
    api.assign(9, 7).subscribe();
    const r = http.expectOne('/api/rutinas/asignar/alumno/9/rutina/7');
    expect(r.request.method).toBe('POST');
    r.flush({ id: 1 });
  });
  it('loads an exact historical routine version', () => {
    api.version(7, 2).subscribe();
    http.expectOne('/api/rutinas/7/versiones/2').flush({ numeroVersion: 2 });
  });
});
