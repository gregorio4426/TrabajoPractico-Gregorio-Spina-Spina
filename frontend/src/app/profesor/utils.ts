import { HttpErrorResponse } from '@angular/common/http';
import { adminError } from '../admin/admin-utils';
import { AbstractControl, ValidatorFn, Validators } from '@angular/forms';
export { text } from '../admin/admin-utils';
export const notBlank: ValidatorFn = (c) => (String(c.value ?? '').trim() ? null : { blank: true });
export const positiveInteger: ValidatorFn = (c) =>
  Number.isSafeInteger(Number(c.value)) && Number(c.value) > 0 ? null : { integer: true };
export const optionalWeight: ValidatorFn = (c) =>
  c.value === null || c.value === '' || (Number.isFinite(Number(c.value)) && Number(c.value) >= 0)
    ? null
    : { weight: true };
export const passwordValidators = [
  Validators.required,
  Validators.minLength(8),
  (c: AbstractControl) =>
    new TextEncoder().encode(String(c.value ?? '')).length <= 72 ? null : { bytes: true },
];
export function profesorError(error: unknown) {
  if (error instanceof HttpErrorResponse && error.status === 409)
    return 'Los datos cambiaron o existe un conflicto de identidad. Recargá los datos antes de volver a intentar.';
  return error instanceof HttpErrorResponse && error.status === 403
    ? 'No tenés acceso vigente a este recurso. Revisá la autoría de la rutina o las vinculaciones actuales y recargá los datos.'
    : adminError(error);
}
export function validDates(desde: string, hasta: string) {
  return !desde || !hasta || desde <= hasta;
}
export function isDenied(error: unknown) {
  return error instanceof HttpErrorResponse && error.status === 403;
}
