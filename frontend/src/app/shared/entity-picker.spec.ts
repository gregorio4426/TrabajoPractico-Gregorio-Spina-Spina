import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { EntityPicker } from './entity-picker';
describe('Searchable entity selection', () => {
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
  });
  function setup() {
    TestBed.configureTestingModule({
      imports: [EntityPicker],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    return TestBed.createComponent(EntityPicker);
  }
  it('distinguishes names by email while emitting only the internal identifier', async () => {
    const f = setup();
    f.componentRef.setInput('options', [
      { value: 3, label: 'Ana Paz', detail: 'ana@a.test' },
      { value: 5, label: 'Ana Paz', detail: 'ana@b.test' },
    ]);
    f.detectChanges();
    const change = vi.fn();
    f.componentInstance.registerOnChange(change);
    f.componentInstance.search.setValue('b.test');
    await f.componentInstance.load();
    expect(f.componentInstance.rows().map((x) => x.value)).toEqual([5]);
    f.componentInstance.choose(f.componentInstance.rows()[0]!);
    expect(change).toHaveBeenCalledWith('5');
    f.detectChanges();
    expect(f.nativeElement.textContent).not.toContain('#5');
  });
  it('clears the relationship without inventing a new entity', () => {
    const f = setup();
    f.componentRef.setInput('options', [{ value: 3, label: 'Ana' }]);
    f.detectChanges();
    const change = vi.fn();
    f.componentInstance.registerOnChange(change);
    f.componentInstance.writeValue(3);
    f.componentInstance.clear();
    expect(change).toHaveBeenCalledWith('');
    expect(f.componentInstance.selected()).toBeNull();
  });
  it('does not submit the parent form on Enter and closes its search on Escape', () => {
    const f = setup();
    f.componentRef.setInput('options', [{ value: 1, label: 'Ana' }]);
    f.detectChanges();
    f.componentInstance.toggle();
    f.detectChanges();
    const search = f.nativeElement.querySelector('input');
    const enter = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
    search.dispatchEvent(enter);
    expect(enter.defaultPrevented).toBe(true);
    search.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }),
    );
    expect(f.componentInstance.opened()).toBe(false);
  });
});
