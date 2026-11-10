import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, ValidatorFn } from '@angular/forms';
import { adminError } from '../admin/admin-utils';
import * as D from '../admin/contracts';
export { text } from '../admin/admin-utils';
export function alumnoError(e: unknown) {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 403)
      return 'No tenés permiso para consultar o modificar este registro. Solo podés acceder a tus propios entrenamientos.';
    if (e.status === 409)
      return 'Los datos cambiaron o ese número de serie ya está ocupado. Recargá la sesión y revisá los registros antes de volver a intentar.';
  }
  return adminError(e);
}
export function isClosed(s: D.SesionResponse) {
  return s.finalizada === true || s.completada === true;
}
export function validSeries(s: D.SerieResponse) {
  return (
    !s.anulada &&
    !s.legadoExento &&
    s.completada === true &&
    Number.isSafeInteger(s.numeroSerie) &&
    Number(s.numeroSerie) > 0 &&
    Number.isSafeInteger(s.repsHechas) &&
    Number(s.repsHechas) > 0 &&
    (s.pesoUsado == null || (Number.isFinite(s.pesoUsado) && s.pesoUsado >= 0))
  );
}
export function nextSeriesNumber(rows: D.SerieResponse[], exerciseId: number) {
  const used = new Set(
    rows
      .filter((s) => s.ejercicioId === exerciseId && !s.anulada && !s.legadoExento)
      .map((s) => s.numeroSerie),
  );
  let next = 1;
  while (used.has(next)) next++;
  return next;
}
export function localDate() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
export function utcDate() {
  return new Date().toISOString().slice(0, 10);
}
export const pastOrToday: ValidatorFn = (c: AbstractControl) =>
  validCalendarDate(String(c.value)) && String(c.value) <= utcDate() ? null : { date: true };

export function validCalendarDate(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(value + 'T00:00:00Z');
  return Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === value;
}
export const optionalPast: ValidatorFn = (c: AbstractControl) =>
  !c.value || (validCalendarDate(String(c.value)) && String(c.value) < utcDate())
    ? null
    : { date: true };
