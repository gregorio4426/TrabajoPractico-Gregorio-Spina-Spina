import { describe, it, expect } from 'vitest';
import { progressValues, monday } from './progress-values';
import { dateAR, suggested, performed } from '../shared/measurement';
const workout = (date: string, id = 1) => ({
  id,
  fecha: date,
  finalizada: true,
  seriesRealizadas: 1,
});
describe('friendly personal progress from actual training', () => {
  it('uses Monday to Sunday, counts sessions separately from distinct days and ignores open sessions', () => {
    const result = progressValues(
      [
        workout('2026-11-23'),
        workout('2026-11-23', 2),
        workout('2026-11-22', 3),
        { ...workout('2026-11-24'), finalizada: false },
      ],
      '2026-11-24',
    );
    expect(result.weekCount).toBe(2);
    expect(result.days.map((d) => d.trained)).toEqual([
      true,
      false,
      false,
      false,
      false,
      false,
      false,
    ]);
    expect(monday('2026-11-22')).toBe('2026-11-16');
  });
  it('counts timed workouts, excludes annulled-only, missing-date and future records', () => {
    const result = progressValues(
      [
        {
          fecha: '2026-11-23',
          finalizada: true,
          series: [
            { completada: true, numeroSerie: 1, tipoMedicion: 'SEGUNDOS', segundosHechos: 45 },
          ],
        },
        {
          fecha: '2026-11-23',
          finalizada: true,
          series: [{ completada: true, numeroSerie: 1, repsHechas: 10, anulada: true }],
        },
        workout('2026-11-30'),
        workout(''),
      ],
      '2026-11-23',
    );
    expect(result.weekCount).toBe(1);
    expect(result.monthCount).toBe(1);
  });
  it('counts only consecutive weeks and permits the current unfinished week to have no training yet', () => {
    expect(
      progressValues(
        [workout('2026-11-17'), workout('2026-11-11'), workout('2026-11-03')],
        '2026-11-23',
      ).streak,
    ).toBe(3);
    expect(
      progressValues([workout('2026-11-17'), workout('2026-11-03')], '2026-11-23').streak,
    ).toBe(1);
  });
  it('keeps units and personal overrides distinct, and formats calendar dates without UTC conversion', () => {
    expect(
      suggested({
        tipoMedicion: 'SEGUNDOS',
        segundosSugeridos: 45,
        segundosPersonalizados: 60,
        repsSugeridas: null,
      }),
    ).toBe(60);
    expect(suggested({ repsSugeridas: 12, repsPersonalizadas: 10 })).toBe(10);
    expect(performed({ tipoMedicion: 'SEGUNDOS', segundosHechos: 44, repsHechas: null })).toBe(44);
    expect(dateAR('2026-11-23T00:00:00Z')).toBe('23/11/2026');
  });
});
