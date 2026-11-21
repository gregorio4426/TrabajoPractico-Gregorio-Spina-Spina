import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { plannedSeries, WorkoutGrid } from './workout-grid';
describe('individual results for every exercise and round', () => {
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
  });
  function setup() {
    TestBed.configureTestingModule({
      imports: [WorkoutGrid],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(WorkoutGrid);
    fixture.componentRef.setInput('session', { id: 7, finalizada: false, series: [] });
    fixture.componentRef.setInput('exercises', [
      { id: 20, ejercicioId: 3, vueltas: 3, repsSugeridas: 12 },
    ]);
    fixture.detectChanges();
    return fixture;
  }
  it('numbers repeated exercises globally, without assuming matching results per round', () => {
    setup();
    expect(
      plannedSeries([
        { ejercicioId: 3, vueltas: 3 },
        { ejercicioId: 4, vueltas: 2 },
        { ejercicioId: 3, seriesSugeridas: 2 },
      ]).map((r) => r.numbers),
    ).toEqual([
      [1, 2, 3],
      [1, 2],
      [4, 5],
    ]);
  });
  it('writes distinct actual results, preserves unsent drafts and distinguishes zero from unknown', async () => {
    const f = setup(),
      c = f.componentInstance,
      http = TestBed.inject(HttpTestingController),
      exercise = c.exercises()[0]!;
    const a = c.entry(exercise, 1),
      b = c.entry(exercise, 2);
    a.form.patchValue({ reps: 12, weight: 0 });
    b.form.patchValue({ reps: 8, weight: 65 });
    b.form.markAsDirty();
    const save = c.save(a);
    const req = http.expectOne('/api/sesiones/7/series');
    expect(req.request.body).toEqual({
      diaEjercicioId: 20,
      numeroSerie: 1,
      repsHechas: 12,
      pesoUsado: 0,
    });
    req.flush({ id: 9, revision: 0, repsHechas: 12, pesoUsado: 0 });
    await save;
    expect(b.form.getRawValue()).toEqual({ reps: 8, weight: 65 });
    expect(c.entry(exercise, 1).saved?.id).toBe(9);
    b.form.controls.weight.setValue(null);
    const next = c.save(b);
    const r = http.expectOne('/api/sesiones/7/series');
    expect(r.request.body).toMatchObject({ numeroSerie: 2, repsHechas: 8, pesoUsado: null });
    r.flush({ id: 10, revision: 0 });
    await next;
  });
  it('recovers saved results and uses their revision for corrections; 409 never retries', async () => {
    const f = setup(),
      c = f.componentInstance,
      http = TestBed.inject(HttpTestingController);
    f.componentRef.setInput('session', {
      id: 7,
      series: [
        { id: 9, ejercicioId: 3, numeroSerie: 1, repsHechas: 10, pesoUsado: 60, revision: 4 },
      ],
    });
    f.detectChanges();
    const row = c.entry(c.exercises()[0]!, 1);
    expect(row.form.getRawValue()).toEqual({ reps: 10, weight: 60 });
    row.form.controls.reps.setValue(8);
    row.form.markAsDirty();
    const save = c.save(row);
    const r = http.expectOne('/api/sesiones/7/series/9');
    expect(r.request.method).toBe('PATCH');
    expect(r.request.body).toMatchObject({ revision: 4, repsHechas: 8 });
    r.flush({}, { status: 409, statusText: 'Conflict' });
    await save;
    expect(c.error()).toContain('No se reintentó');
    http.expectNone('/api/sesiones/7/series/9');
  });
});
