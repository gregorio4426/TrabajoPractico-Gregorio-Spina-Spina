import { HttpErrorResponse } from '@angular/common/http';
import { describe, it, expect } from 'vitest';
import { adminError } from '../admin/admin-utils';
import { profesorError } from '../profesor/utils';
import { alumnoError } from '../alumno/utils';
describe('Errors across all three roles', () => {
  for (const [role, format] of [
    ['ADMIN', adminError],
    ['PROFESOR', profesorError],
    ['ALUMNO', alumnoError],
  ] as const) {
    it(`${role}: preserves 400 validation and gives actionable 401/403/404/409 messages`, () => {
      expect(
        format(
          new HttpErrorResponse({
            status: 400,
            error: { mensaje: 'El período debe contener entre 1 y 366 días inclusivos' },
          }),
        ),
      ).toContain('366');
      expect(format(new HttpErrorResponse({ status: 401 }))).toContain('sesión');
      expect(format(new HttpErrorResponse({ status: 403 }))).toMatch(/permiso|acceso/);
      expect(format(new HttpErrorResponse({ status: 404 }))).toContain('disponible');
      expect(format(new HttpErrorResponse({ status: 409 }))).toMatch(/Recargá|Recarg|recarg/);
    });
    it(`${role}: never exposes internal diagnostics from 500 or malformed validation bodies`, () => {
      for (const status of [400, 500])
        expect(
          format(
            new HttpErrorResponse({
              status,
              error: { trace: 'SQL_SECRET', message: 'SQL_SECRET' },
            }),
          ),
        ).not.toContain('SQL_SECRET');
      expect(
        format(new HttpErrorResponse({ status: 400, error: { mensaje: 'x'.repeat(601) } })),
      ).not.toContain('x'.repeat(601));
      expect(format(new HttpErrorResponse({ status: 0 }))).toContain('conectar');
    });
  }
});
