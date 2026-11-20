import { TestBed } from '@angular/core/testing';
import { afterEach, describe, it, expect } from 'vitest';
import { SearchSelect } from './search-select';
describe('searchable selection', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('filters case and accents, chooses by keyboard and prevents form submission', () => {
    const f = TestBed.createComponent(SearchSelect),
      c = f.componentInstance;
    f.componentRef.setInput('options', [
      { value: 1, label: 'Press de banca' },
      { value: 2, label: 'Remó con barra' },
    ]);
    f.detectChanges();
    const emitted: string[] = [];
    c.registerOnChange((v) => emitted.push(v));
    c.type('REMO');
    expect(c.matches().map((x) => x.value)).toEqual([2]);
    const enter = new KeyboardEvent('keydown', { key: 'Enter', cancelable: true });
    c.key(enter);
    expect(enter.defaultPrevented).toBe(true);
    expect(c.value()).toBe(2);
    expect(emitted.at(-1)).toBe('2');
    expect(c.opened()).toBe(false);
  });
  it('offers creation only when no equivalent name exists and honors disabled state', () => {
    const f = TestBed.createComponent(SearchSelect),
      c = f.componentInstance;
    f.componentRef.setInput('options', [{ value: 1, label: 'Tríceps' }]);
    f.componentRef.setInput('allowCreate', true);
    f.detectChanges();
    c.type('TRICEPS');
    expect(c.canOfferCreate()).toBe(false);
    c.type('Movilidad');
    expect(c.canOfferCreate()).toBe(true);
    let name = '';
    c.createRequested.subscribe((v) => (name = v));
    c.create();
    expect(name).toBe('Movilidad');
    c.setDisabledState(true);
    c.open();
    expect(c.opened()).toBe(false);
  });
});
