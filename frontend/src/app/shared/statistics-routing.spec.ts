import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, describe, it, expect, vi } from 'vitest';
import { TeacherProgressPage } from '../profesor/progress-page';
import { StudentProgressPage } from '../alumno/progress';
describe('Statistics pages with real router input binding', () => {
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
  });
  for (const role of ['profesor', 'alumno'] as const)
    it(`${role}: keeps date filters and own scope when opening a standalone page`, async () => {
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          provideRouter(
            [
              {
                path: 'progreso',
                component: role === 'profesor' ? TeacherProgressPage : StudentProgressPage,
              },
            ],
            withComponentInputBinding(),
          ),
        ],
      });
      const harness = await RouterTestingHarness.create();
      await harness.navigateByUrl(
        '/progreso?studentId=999&showDates=false&sessionBase=/admin/sesiones',
      );
      const h = TestBed.inject(HttpTestingController),
        base =
          role === 'profesor' ? '/api/profesores/me/estadisticas' : '/api/alumnos/me/estadisticas';
      h.expectOne(base).flush({ periodo: { desde: '2026-11-01', hasta: '2026-11-07' } });
      h.expectOne((r) => r.url === base + '/actividad').flush({ contenido: [], pagina: 0 });
      h.expectOne((r) => r.url === base + '/ejercicios').flush({
        contenido: [{ ejercicioId: 7, nombreActual: 'Ejercicio' }],
        pagina: 0,
      });
      await harness.fixture.whenStable();
      harness.detectChanges();
      expect(harness.routeNativeElement?.querySelectorAll('input[type="date"]')).toHaveLength(2);
      await vi.waitFor(() => {
        harness.detectChanges();
        expect(harness.routeNativeElement?.textContent).toContain('Ejercicio');
      });
      const pick = [...harness.routeNativeElement!.querySelectorAll('button')].find((b) =>
        /Ejercicio\s*·\s*#7/.test(b.textContent ?? ''),
      )!;
      pick.click();
      harness.detectChanges();
      h.expectOne((r) => r.url === base + '/ejercicios/7/evolucion').flush({
        contenido: [{ sesionId: 17, datos: { pesoMaximo: '0' } }],
        pagina: 0,
      });
      await harness.fixture.whenStable();
      harness.detectChanges();
      await vi.waitFor(() => {
        harness.detectChanges();
        expect(harness.routeNativeElement?.querySelector('a')?.getAttribute('href')).toBe(
          `/${role}/sesiones/17`,
        );
      });
    });
});
