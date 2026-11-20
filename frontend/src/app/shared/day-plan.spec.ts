import { TestBed } from '@angular/core/testing';
import { afterEach, describe, it, expect } from 'vitest';
import { DayPlan } from './day-plan';
describe('complete block rounds', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('renders one round count for three exercises instead of prescribing independent series', () => {
    const f = TestBed.createComponent(DayPlan);
    f.componentRef.setInput(
      'exercises',
      [12, 10, 15].map((reps, i) => ({
        id: i + 1,
        bloqueOrden: 1,
        bloqueNombre: 'Bloque A',
        vueltas: 3,
        seriesSugeridas: 3,
        ordenEnBloque: i + 1,
        repsSugeridas: reps,
        ejercicioNombre: ['Press', 'Remo', 'Flexiones'][i],
      })),
    );
    f.detectChanges();
    expect(f.componentInstance.blocks()).toHaveLength(1);
    expect(f.nativeElement.querySelectorAll('.student-prescription')).toHaveLength(3);
    expect(f.nativeElement.textContent).toContain('3 vueltas');
    expect(f.nativeElement.textContent).toContain('Una vuelta incluye todos');
    expect(f.nativeElement.textContent).not.toContain('3 series');
    expect(f.nativeElement.textContent).not.toContain('Peso sugerido');
  });
});
