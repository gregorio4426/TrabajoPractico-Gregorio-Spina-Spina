import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, it, expect } from 'vitest';
import { RoutineEditor } from './routine-editor';
import { SESSION_KEY } from '../core/auth.service';
import { testSession } from '../core/auth.fixtures';
describe('PROFESOR routine forms', () => {
  beforeEach(() => localStorage.setItem(SESSION_KEY, JSON.stringify(testSession('PROFESOR'))));
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
    localStorage.clear();
  });
  async function setup(edit = false, owner = 1) {
    TestBed.configureTestingModule({
      imports: [RoutineEditor],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap(edit ? { id: '7' } : {}) } },
        },
      ],
    });
    const fixture = TestBed.createComponent(RoutineEditor);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/ejercicios').flush([{ id: 3, nombre: 'Press', grupoMuscular: 'PECHO' }]);
    await Promise.resolve();
    if (edit) {
      http
        .expectOne('/api/rutinas/7')
        .flush({
          id: 7,
          profesorId: owner,
          archivada: false,
          nombre: 'Plan',
          descripcion: 'Real',
          dias: [],
        });
      await Promise.resolve();
    }
    fixture.detectChanges();
    return fixture.componentInstance;
  }
  it('requires a day and an exercise, never a trivial empty routine', async () => {
    const editor = await setup();
    expect(editor.days.length).toBe(1);
    expect(editor.form.invalid).toBe(true);
    editor.removeDay(0);
    expect(editor.form.invalid).toBe(true);
  });
  it('rejects duplicate day order numbers', async () => {
    const editor = await setup();
    editor.addDay();
    editor.days.at(1).controls.numeroOrden.setValue(1);
    expect(editor.days.hasError('duplicateOrder')).toBe(true);
  });
  it('preserves known zero and absent suggested weight separately', async () => {
    const editor = await setup();
    editor.form.patchValue({ nombre: ' Plan ', descripcion: ' Real ' });
    const day = editor.days.at(0);
    day.patchValue({ nombre: ' Día ', numeroOrden: 1 });
    day.controls.ejercicios
      .at(0)
      .patchValue({ ejercicioId: 3, seriesSugeridas: 2, repsSugeridas: 8, pesoSugerido: 0 });
    expect(editor.payload().dias[0]?.ejercicios[0]?.pesoSugerido).toBe(0);
    day.controls.ejercicios.at(0).controls.pesoSugerido.setValue(null);
    expect(editor.payload().dias[0]?.ejercicios[0]?.pesoSugerido).toBeNull();
    expect(editor.payload().nombre).toBe('Plan');
  });
  it('rejects fractional repetitions and negative suggested weight', async () => {
    const editor = await setup();
    const row = editor.days.at(0).controls.ejercicios.at(0);
    row.controls.repsSugeridas.setValue(1.5);
    row.controls.pesoSugerido.setValue(-1);
    expect(row.controls.repsSugeridas.invalid).toBe(true);
    expect(row.controls.pesoSugerido.invalid).toBe(true);
  });
  it('does not expose an editor for another author even with a manipulated URL', async () => {
    const editor = await setup(true, 99);
    expect(editor.ready()).toBe(false);
    expect(editor.error()).toContain('propias');
    TestBed.inject(HttpTestingController).expectNone((r) => r.method === 'PUT');
  });
});
