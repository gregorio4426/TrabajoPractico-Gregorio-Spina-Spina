import { describe, it, expect } from 'vitest';
import { chartNumber } from './chart';
describe('Real chart data conversion', () => {
  it('keeps known zero and excludes absent values', () => {
    expect(chartNumber('0')).toBe(0);
    expect(chartNumber(null)).toBeNull();
    expect(chartNumber(undefined)).toBeNull();
    expect(chartNumber('')).toBeNull();
  });
  it('does not silently plot unsafe large decimals', () => {
    expect(chartNumber('12345678901234567890.123')).toBeNull();
    expect(chartNumber('NaN')).toBeNull();
    expect(chartNumber('Infinity')).toBeNull();
    expect(chartNumber('27.125')).toBe(27.125);
  });
});
