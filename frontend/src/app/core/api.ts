import { InjectionToken } from '@angular/core';
// Same-origin API: ng serve proxies /api; production reverse proxy must do the same.
export const API_BASE = new InjectionToken<string>('API_BASE', { factory: () => '/api' });
export function isApiUrl(url: string, base: string): boolean {
  const target = new URL(url, window.location.origin);
  const api = new URL(base, window.location.origin);
  return target.origin === api.origin && (target.pathname === api.pathname || target.pathname.startsWith(api.pathname + '/'));
}
