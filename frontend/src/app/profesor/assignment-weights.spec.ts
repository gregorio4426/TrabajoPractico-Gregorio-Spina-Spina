import { TestBed } from '@angular/core/testing';
import { afterEach, describe, it, expect } from 'vitest';
import { AssignmentWeights } from './assignment-weights';
describe('per-assignment suggested weights', () => {
  afterEach(() => TestBed.resetTestingModule());
  const routine = {
    asignacionRevision: 2,
    dias: [
      {
        nombre: 'Día 1',
        ejercicios: [
          { id: 5, ejercicioNombre: 'Press', pesoSugerido: 60 },
          { id: 6, ejercicioNombre: 'Remo', pesoSugerido: 0 },
        ],
      },
    ],
  };
  it('starts blank when assigning a template, including legacy templates with stored weights', () => {
    const f = TestBed.createComponent(AssignmentWeights);
    f.componentRef.setInput('routine', routine);
    f.detectChanges();
    expect(f.componentInstance.payload().pesos.map((p) => p.pesoSugerido)).toEqual([null, null]);
  });
  it('recovers saved weights and preserves zero separately from absent weight', () => {
    const f = TestBed.createComponent(AssignmentWeights);
    f.componentRef.setInput('routine', routine);
    f.componentRef.setInput('existing', true);
    f.detectChanges();
    const c = f.componentInstance;
    expect(c.payload()).toEqual({
      revision: 2,
      pesos: [
        { diaEjercicioId: 5, pesoSugerido: 60 },
        { diaEjercicioId: 6, pesoSugerido: 0 },
      ],
    });
    c.rows.at(0).controls.pesoSugerido.setValue(-1);
    expect(c.form.invalid).toBe(true);
    c.reset();
    expect(c.payload().pesos.map((p) => p.pesoSugerido)).toEqual([null, null]);
  });
  it('recovers personal repetition and time targets and leaves template defaults untouched', () => {
    const f = TestBed.createComponent(AssignmentWeights);
    const source = {
      asignacionRevision: 3,
      dias: [
        {
          nombre: 'Día 1',
          ejercicios: [
            { id: 5, repsSugeridas: 12, repsPersonalizadas: 10, pesoSugerido: 60 },
            { id: 6, tipoMedicion: 'SEGUNDOS', segundosSugeridos: 45, segundosPersonalizados: 60 },
          ],
        },
      ],
    };
    f.componentRef.setInput('routine', source);
    f.componentRef.setInput('existing', true);
    f.detectChanges();
    expect(f.componentInstance.payload().pesos[0]).toMatchObject({
      repsSugeridas: 10,
      pesoSugerido: 60,
    });
    expect(f.componentInstance.payload().pesos[1]).toMatchObject({ segundosSugeridos: 60 });
    f.componentInstance.rows.at(0).controls.repsSugeridas.setValue(9);
    expect(source.dias[0]!.ejercicios[0]!.repsSugeridas).toBe(12);
    f.componentInstance.rows.at(1).controls.segundosSugeridos.setValue(0);
    expect(f.componentInstance.form.invalid).toBe(true);
  });
});
