import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './auth.models';

export const authGuard: CanActivateFn = () => {
  return inject(AuthService).validSession() ? true : inject(Router).createUrlTree(['/login']);
};
export const roleGuard: CanActivateFn = route => {
  const auth = inject(AuthService);
  const session = auth.validSession();
  const router = inject(Router);
  if (!session) return router.createUrlTree(['/login']);
  return session.rol === (route.data['role'] as Role) ? true : router.parseUrl(auth.home());
};
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.validSession() ? inject(Router).parseUrl(auth.home()) : true;
};
export const entryGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  auth.validSession();
  return inject(Router).parseUrl(auth.home());
};
