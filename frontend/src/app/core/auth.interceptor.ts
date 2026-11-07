import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { API_BASE, isApiUrl } from './api';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const base = inject(API_BASE);
  if (!isApiUrl(request.url, base)) return next(request);
  const auth = inject(AuthService);
  const path = new URL(request.url, window.location.origin).pathname;
  const apiPath = new URL(base, window.location.origin).pathname;
  const isPublic = path === `${apiPath}/auth/login` || path.startsWith(`${apiPath}/auth/register/`);
  const session = isPublic ? null : auth.validSession();
  const headers = request.headers.delete('Authorization');
  const outgoing = request.clone({ headers: session ? headers.set('Authorization', `Bearer ${session.token}`) : headers });
  return next(outgoing).pipe(catchError((error: unknown) => {
    if (!isPublic && error instanceof HttpErrorResponse && error.status === 401
      && session && auth.session()?.token === session.token) {
      auth.logout('Tu sesión venció o fue revocada. Iniciá sesión nuevamente.');
    }
    return throwError(() => error);
  }));
};
