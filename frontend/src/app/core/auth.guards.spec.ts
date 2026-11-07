import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { SESSION_KEY } from './auth.service';
import { authGuard, guestGuard, roleGuard } from './auth.guards';
import { testSession } from './auth.fixtures';

describe('Route protection', () => {
  beforeEach(() => { localStorage.clear(); TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient()]}); });
  afterEach(() => {TestBed.resetTestingModule();localStorage.clear();});
  const state = {} as RouterStateSnapshot;
  it('redirects anonymous requests to login', () => {
    const result = TestBed.runInInjectionContext(() => authGuard({} as ActivatedRouteSnapshot,state));
    expect(TestBed.inject(Router).serializeUrl(result as ReturnType<Router['parseUrl']>)).toBe('/login');
  });
  it.each(['ADMIN','PROFESOR','ALUMNO'] as const)('permits only the %s workspace', role => {
    localStorage.setItem(SESSION_KEY,JSON.stringify(testSession(role)));
    const allowed = TestBed.runInInjectionContext(() => roleGuard({data:{role}} as unknown as ActivatedRouteSnapshot,state));
    expect(allowed).toBe(true);
    const denied = TestBed.runInInjectionContext(() => roleGuard({data:{role:'OTHER'}} as unknown as ActivatedRouteSnapshot,state));
    expect(TestBed.inject(Router).serializeUrl(denied as ReturnType<Router['parseUrl']>)).toBe('/'+role.toLowerCase());
    const guest = TestBed.runInInjectionContext(() => guestGuard({} as ActivatedRouteSnapshot,state));
    expect(TestBed.inject(Router).serializeUrl(guest as ReturnType<Router['parseUrl']>)).toBe('/'+role.toLowerCase());
  });
});
