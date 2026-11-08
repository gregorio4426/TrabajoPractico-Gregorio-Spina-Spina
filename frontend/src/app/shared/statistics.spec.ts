import { describe, it, expect } from 'vitest';
import { statisticsPeriodError as error } from './statistics-period';
import { volumeCoverage } from './statistics-values';
describe('Backend statistics period and coverage', () => {
  it('accepts inclusive one-day and leap-year 366-day windows', () => {
    expect(error('2024-01-01', '2024-12-31')).toBe('');
    expect(error('2024-02-29', '2024-02-29')).toBe('');
    expect(error('2024-01-01', '2025-01-01')).toContain('366');
  });
  it('rejects impossible dates, inverted ranges and unsupported years', () => {
    expect(error('2025-02-29', '2025-03-01')).toContain('calendario');
    expect(error('2026-11-08', '2026-11-01')).toContain('anterior o igual');
    expect(error('0999-01-01', '1000-01-30')).toContain('rango');
    expect(error('1000-01-01', '1000-01-29')).toContain('rango');
  });
  it('resolves omitted dates like the backend, using UTC instead of local time', () => {
    expect(error('', '', '2026-11-08')).toBe('');
    expect(error('2025-01-01', '', '2026-11-08')).toContain('366');
    expect(error('', '1000-01-30')).toBe('');
  });
  it('preserves zero, unknown and incomplete volumes without deriving totals', () => {
    expect(volumeCoverage({ volumenRegistrado: null })).toBe('(sin pesos conocidos)');
    expect(volumeCoverage({ volumenRegistrado: '0', volumenCompleto: true })).toBe('(completo)');
    expect(
      volumeCoverage({ volumenRegistrado: '0', volumenCompleto: false, seriesSinPeso: 2 }),
    ).toContain('parcial · 2 series');
    expect(
      volumeCoverage({ volumenRegistrado: '90071992547409999.123', volumenCompleto: true }),
    ).toBe('(completo)');
  });
});
