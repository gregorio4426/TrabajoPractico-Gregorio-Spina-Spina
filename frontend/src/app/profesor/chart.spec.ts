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

import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Chart } from './chart';
import { afterEach } from 'vitest';
describe('Chart rendering and missing data', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('splits lines at missing or unsafe values and draws known zero', () => {
    TestBed.configureTestingModule({ imports: [Chart], providers: [provideRouter([])] });
    const f = TestBed.createComponent(Chart);
    f.componentRef.setInput('title', 'Carga');
    f.componentRef.setInput('points', [
      { label: 'A', value: '0' },
      { label: 'B', value: null },
      { label: 'C', value: '12.5' },
      { label: 'D', value: '12345678901234567890.123' },
    ]);
    f.detectChanges();
    expect(f.componentInstance.segments()).toHaveLength(2);
    expect(f.nativeElement.querySelectorAll('circle')).toHaveLength(2);
    expect(f.nativeElement.textContent).toContain('12345678901234567890.123');
    expect(f.nativeElement.textContent).toContain('Sin dato');
    expect(f.componentInstance.plotted()[0]?.y).toBe(180);
  });
  it('keeps zero-frequency marks visible and provides exact session links and coverage', () => {
    TestBed.configureTestingModule({ imports: [Chart], providers: [provideRouter([])] });
    const f = TestBed.createComponent(Chart);
    f.componentRef.setInput('title', 'Volumen');
    f.componentRef.setInput('kind', 'bar');
    f.componentRef.setInput('points', [
      {
        label: 'Sesión #17',
        value: '0',
        link: '/admin/sesiones/17',
        note: 'parcial · 1 serie sin peso',
      },
    ]);
    f.detectChanges();
    expect(f.nativeElement.querySelectorAll('circle')).toHaveLength(1);
    expect(f.nativeElement.querySelector('a').getAttribute('href')).toBe('/admin/sesiones/17');
    expect(f.nativeElement.textContent).toContain('parcial · 1 serie sin peso');
  });
  it('rejects non-decimal input instead of interpreting spaces or exponent strings as data', () => {
    expect(chartNumber(' ')).toBeNull();
    expect(chartNumber('1e12')).toBeNull();
  });
});
