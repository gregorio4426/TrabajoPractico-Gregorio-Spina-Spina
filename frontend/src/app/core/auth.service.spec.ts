import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService, SESSION_KEY } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { testSession } from './auth.fixtures';

describe('Session and HTTP integration', () => {
  let auth: AuthService;
  let http: HttpClient;
  let controller: HttpTestingController;
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()] });
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { controller.verify(); TestBed.resetTestingModule(); localStorage.clear(); vi.restoreAllMocks(); vi.useRealTimers(); });
  function login() {
    auth = TestBed.inject(AuthService);
    auth.login({ email: 'test@example.test', password: 'test-only' }).subscribe();
    const request = controller.expectOne('/api/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'test@example.test', password: 'test-only' });
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(testSession());
  }
  it('persists the AuthResponse, never the password, and attaches Bearer only to API calls', () => {
    login();
    expect(auth.authenticated()).toBe(true);
    expect(localStorage.getItem(SESSION_KEY)).not.toContain('test-only');
    http.get('/api/alumnos/me').subscribe();
    const request = controller.expectOne('/api/alumnos/me');
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${auth.session()?.token}`);
    request.flush({});
    for (const url of ['https://external.example/api/private', '/api-other/private', '/assets/logo.svg']) {
      http.get(url).subscribe();
      const external = controller.expectOne(url);
      expect(external.request.headers.has('Authorization')).toBe(false);
      external.flush({});
    }
  });
  it('restores a persisted session on reload', () => {
    localStorage.setItem(SESSION_KEY, JSON.stringify(testSession('PROFESOR')));
    auth = TestBed.inject(AuthService);
    expect(auth.home()).toBe('/profesor');
  });
  it('rejects corrupted storage', () => {
    localStorage.setItem(SESSION_KEY, '{broken');
    auth = TestBed.inject(AuthService);
    expect(auth.session()).toBeNull();
  });
  it('expires automatically without a request', () => {
    vi.useFakeTimers();
    localStorage.setItem(SESSION_KEY, JSON.stringify(testSession('ADMIN', 2)));
    auth = TestBed.inject(AuthService);
    vi.advanceTimersByTime(3000);
    expect(auth.session()).toBeNull();
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
    expect(auth.notice()).toContain('venció');
  });
  it('clears the current session on 401 and propagates the error', () => {
    login(); let status = 0;
    http.get('/api/alumnos/me').subscribe({ error: e => { status = e.status; } });
    controller.expectOne('/api/alumnos/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(status).toBe(401); expect(auth.session()).toBeNull();
  });
  it('preserves the session on 403', () => {
    login();
    http.get('/api/admin/usuarios').subscribe({ error: () => undefined });
    controller.expectOne('/api/admin/usuarios').flush({}, { status: 403, statusText: 'Forbidden' });
    expect(auth.authenticated()).toBe(true);
  });
  it('does not send stale Authorization to login or registration', () => {
    login();
    for (const url of ['/api/auth/login', '/api/auth/register/alumno']) {
      http.post(url, {}, { headers: { Authorization: 'Bearer obsolete' } }).subscribe({error: () => undefined});
      const request = controller.expectOne(url);
      expect(request.request.headers.has('Authorization')).toBe(false);
      request.flush({}, {status:401,statusText:'Unauthorized'});
    }
    expect(auth.authenticated()).toBe(true);
  });
  it('rejects a malformed server response without creating a session', () => {
    auth = TestBed.inject(AuthService); let failed = false;
    auth.login({email:'test@example.test',password:'test-only'}).subscribe({error: () => {failed=true;}});
    controller.expectOne('/api/auth/login').flush({token:'invalid',rol:'ADMIN'});
    expect(failed).toBe(true); expect(auth.session()).toBeNull();
  });
  it('handles unavailable browser storage using an in-memory session', () => {
    vi.spyOn(Storage.prototype,'setItem').mockImplementation(() => {throw new Error('blocked');});
    login(); expect(auth.authenticated()).toBe(true); expect(auth.notice()).toContain('no permite');
  });
  it('synchronizes logout from another tab', () => {
    login(); localStorage.removeItem(SESSION_KEY);
    window.dispatchEvent(new StorageEvent('storage', {key:SESSION_KEY}));
    expect(auth.session()).toBeNull();
  });
  it('logout clears storage and navigates to login', () => {
    login(); auth.logout();
    expect(auth.session()).toBeNull(); expect(localStorage.getItem(SESSION_KEY)).toBeNull();
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
