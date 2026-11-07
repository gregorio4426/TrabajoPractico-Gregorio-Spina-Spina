import { HttpErrorResponse } from '@angular/common/http';
export function adminError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse))
    return 'No pudimos completar la operación. Intentá nuevamente.';
  if (error.status === 0)
    return 'No pudimos conectar con el servidor. Revisá la conexión y reintentá.';
  if (error.status === 409)
    return 'Hay un conflicto de revisión o estado. Recargá los datos antes de volver a intentar. El último administrador activo y los emails duplicados están protegidos.';
  if (error.status === 401) return 'La sesión dejó de ser válida. Volvé a ingresar.';
  if (error.status === 403) return 'Tu cuenta no tiene permiso para esta operación.';
  if (error.status === 404) return 'El registro ya no está disponible. Actualizá el listado.';
  if (error.status === 400) {
    const body: unknown = error.error;
    const message = body && typeof body === 'object' && 'mensaje' in body ? body.mensaje : null;
    return typeof message === 'string' && message.length <= 600
      ? message
      : 'Revisá los datos y las reglas del formulario.';
  }
  return 'El servidor no pudo completar la operación. Intentá nuevamente.';
}
export function record(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};
}
export function at(value: unknown, path: string): unknown {
  return path.split('.').reduce<unknown>((current, key) => record(current)[key], value);
}
export function text(value: unknown): string {
  if (value === null || value === undefined || value === '') return 'Sin dato';
  if (typeof value === 'boolean') return value ? 'Sí' : 'No';
  if (typeof value === 'object')
    return Array.isArray(value) ? `${value.length} registros` : 'Ver detalle';
  return String(value).replaceAll('_', ' ');
}
export function idOf(value: unknown): number {
  return Number(at(value, 'id') ?? at(value, 'perfil.id'));
}
