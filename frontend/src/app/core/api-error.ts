import { HttpErrorResponse } from '@angular/common/http';
export function loginError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) return 'La respuesta del servidor no es válida. Intentá nuevamente.';
  if (error.status === 0) return 'No pudimos conectar con el gimnasio. Revisá tu conexión e intentá nuevamente.';
  if (error.status === 401) return 'Email o contraseña incorrectos, o cuenta desactivada.';
  if (error.status === 403) return 'El servidor no permite este acceso. Contactá a la administración.';
  if (error.status === 429) return 'Demasiados intentos. Esperá un momento antes de volver a intentar.';
  return 'No pudimos iniciar sesión. Intentá nuevamente en unos minutos.';
}
