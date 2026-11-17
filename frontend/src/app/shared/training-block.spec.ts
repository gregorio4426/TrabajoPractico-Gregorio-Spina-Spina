import { TestBed } from '@angular/core/testing';
import { describe, it, expect, afterEach } from 'vitest';
import { TrainingBlock } from './training-block';
describe('immutable training block presentation', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('shows round count and known zero rest separately from unknown', () => {
    const f = TestBed.createComponent(TrainingBlock),
      row = { bloqueOrden: 1, vueltas: 3, descansoVueltaSegundos: 0 };
    f.componentRef.setInput('row', row);
    f.componentRef.setInput('exercises', [row, { ...row }]);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Biserie');
    expect(f.nativeElement.textContent).toContain('3 vueltas');
    expect(f.nativeElement.textContent).toContain('0 s');
    f.componentRef.setInput('row', { ...row, descansoVueltaSegundos: null });
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Sin indicar');
  });
  it('does not invent grouping metadata for legacy prescriptions', () => {
    const f = TestBed.createComponent(TrainingBlock);
    f.componentRef.setInput('row', { seriesSugeridas: 3 });
    f.componentRef.setInput('exercises', []);
    f.detectChanges();
    expect(f.nativeElement.textContent.trim()).toBe('');
  });
});
