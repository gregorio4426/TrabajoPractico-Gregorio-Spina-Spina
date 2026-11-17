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
  async function setup(edit = false, owner = 1, copy = false) {
    TestBed.configureTestingModule({
      imports: [RoutineEditor],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(edit ? { id: '7' } : {}),
              queryParamMap: convertToParamMap(copy ? { copiar: '7', version: '1' } : {}),
            },
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(RoutineEditor);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/ejercicios').flush([{ id: 3, nombre: 'Press', grupoMuscular: 'PECHO' }]);
    await Promise.resolve();
    if (edit || copy) {
      http.expectOne(copy ? '/api/rutinas/7/versiones/1' : '/api/rutinas/7').flush({
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
  it('never copies legacy template weights into new template payloads or controls', async () => {
    const editor = await setup();
    const row = editor.exercise({
      ejercicioId: 3,
      repsSugeridas: 8,
      seriesSugeridas: 2,
      pesoSugerido: 60,
    });
    editor.days.at(0).controls.ejercicios.clear();
    editor.days.at(0).controls.ejercicios.push(row);
    expect(Object.keys(row.controls)).not.toContain('pesoSugerido');
    expect(editor.payload().dias[0]!.ejercicios[0]).not.toHaveProperty('pesoSugerido');
    row.controls.repsSugeridas.setValue(1.5);
    expect(row.invalid).toBe(true);
  });
  it('does not expose an editor for another author even with a manipulated URL', async () => {
    const editor = await setup(true, 99);
    expect(editor.ready()).toBe(false);
    expect(editor.error()).toContain('propias');
    TestBed.inject(HttpTestingController).expectNone((r) => r.method === 'PUT');
  });
  it('serializes reordered exercises without losing their individual prescriptions', async () => {
    const e = await setup();
    e.days.at(0).controls.ejercicios.at(0).patchValue({ ejercicioId: 3, repsSugeridas: 8 });
    e.addExercise(0);
    e.days.at(0).controls.ejercicios.at(1).patchValue({ ejercicioId: 3, repsSugeridas: 12 });
    e.moveExercise(0, 1, -1);
    expect(e.payload().dias[0]?.ejercicios.map((x) => x.repsSugeridas)).toEqual([12, 8]);
  });
  it('duplicates the selected historical version as a new routine instead of editing its family', async () => {
    const e = await setup(false, 1, true);
    expect(e.editing).toBe(false);
    expect(e.form.controls.nombre.value).toBe('Plan (copia)');
    expect(e.copyVersion).toBe(1);
  });
  it('persists biseries with shared rounds and individual reps and rests', async () => {
    const e = await setup();
    e.removeExercise(0, 0);
    e.addBlock(0, 2);
    const rows = e.days.at(0).controls.ejercicios;
    rows.at(0).patchValue({
      ejercicioId: 3,
      bloqueNombre: 'Fuerza',
      seriesSugeridas: 3,
      repsSugeridas: 8,

      descansoVueltaSegundos: 60,
      descansoEjercicioSegundos: 0,
    });
    e.syncBlock(0, 0);
    rows.at(1).patchValue({
      ejercicioId: 3,
      repsSugeridas: 12,

      descansoEjercicioSegundos: 20,
    });
    const result = e.payload().dias[0]!.ejercicios;
    expect(
      result.map((r) => [
        r.bloqueNombre,
        r.vueltas,
        r.seriesSugeridas,
        r.ordenEnBloque,
        r.descansoVueltaSegundos,
      ]),
    ).toEqual([
      ['Fuerza', 3, 3, 1, 60],
      ['Fuerza', 3, 3, 2, 60],
    ]);
    expect(result.map((r) => [r.repsSugeridas, r.descansoEjercicioSegundos])).toEqual([
      [8, 0],
      [12, 20],
    ]);
    e.moveExercise(0, 1, -1);
    expect(e.payload().dias[0]!.ejercicios.map((r) => r.repsSugeridas)).toEqual([12, 8]);
  });
  it('reorders entire blocks, adds exercises without an arbitrary circuit limit and removes groups', async () => {
    const e = await setup();
    e.removeExercise(0, 0);
    e.addBlock(0, 2);
    e.addBlock(0, 3);
    const rows = e.days.at(0).controls.ejercicios;
    const first = rows.at(0).controls.bloqueOrden.value!,
      second = rows.at(2).controls.bloqueOrden.value!;
    e.addToBlock(0, second);
    expect(e.blockRows(0, second)).toHaveLength(4);
    e.moveBlock(0, second, -1);
    expect(rows.at(0).controls.bloqueOrden.value).toBe(second);
    expect(e.payload().dias[0]!.ejercicios.map((r) => r.ordenEnBloque)).toEqual([1, 2, 3, 4, 1, 2]);
    e.removeBlock(0, first);
    expect(rows.length).toBe(4);
  });
});
