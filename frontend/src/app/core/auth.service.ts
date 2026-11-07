import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { map, tap } from 'rxjs';
import { API_BASE } from './api';
import { AuthResponse, LoginRequest, ROLE_PATH, Session, parseSession } from './auth.models';

export const SESSION_KEY = 'gym.session.v1';
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly base = inject(API_BASE);
  private readonly state = signal<Session | null>(null);
  readonly session = this.state.asReadonly();
  readonly authenticated = computed(() => this.state() !== null);
  readonly notice = signal('');
  private timer: ReturnType<typeof setTimeout> | undefined;
  private readonly onStorage = (event: StorageEvent) => {
    if (event.key === SESSION_KEY || event.key === null) {
      this.restore();
      void this.router.navigateByUrl(this.state() ? this.home() : '/login');
    }
  };
  private readonly onFocus = () => { this.validSession(); };

  constructor() {
    this.restore();
    window.addEventListener('storage', this.onStorage);
    window.addEventListener('focus', this.onFocus);
    inject(DestroyRef).onDestroy(() => {
      clearTimeout(this.timer);
      window.removeEventListener('storage', this.onStorage);
      window.removeEventListener('focus', this.onFocus);
    });
  }
  login(credentials: LoginRequest) {
    return this.http.post<unknown>(`${this.base}/auth/login`, credentials).pipe(
      map(response => {
        const session = parseSession(response);
        if (!session) throw new Error('La API devolvió una sesión inválida. Volvé a intentarlo.');
        return session;
      }),
      tap(session => this.accept(session)),
    );
  }
  replaceIdentity(response: unknown): Session {
    const session = parseSession(response);
    if (!session) {
      this.logout('La identidad cambió, pero no se pudo validar la nueva sesión. Iniciá sesión nuevamente.');
      throw new Error('Respuesta de identidad inválida');
    }
    this.accept(session);
    return session;
  }
  home(): string { return this.state() ? ROLE_PATH[this.state()!.rol] : '/login'; }
  validSession(): Session | null {
    const session = this.state();
    if (session && session.expiresAt <= Date.now()) { this.logout('Tu sesión venció. Iniciá sesión nuevamente.'); return null; }
    return session;
  }
  logout(message = ''): void {
    clearTimeout(this.timer);
    this.state.set(null);
    try { localStorage.removeItem(SESSION_KEY); } catch { /* Private mode: memory session still clears. */ }
    this.notice.set(message);
    void this.router.navigateByUrl('/login');
  }
  private accept(session: Session): void {
    this.notice.set('');
    try { localStorage.setItem(SESSION_KEY, JSON.stringify(session satisfies AuthResponse)); }
    catch { this.notice.set('El navegador no permite guardar la sesión. Se cerrará al recargar.'); }
    this.state.set(session);
    this.schedule(session);
  }
  private restore(): void {
    clearTimeout(this.timer);
    let session: Session | null = null;
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      session = raw ? parseSession(JSON.parse(raw) as unknown) : null;
      if (raw && !session) { localStorage.removeItem(SESSION_KEY); this.notice.set('Tu sesión ya no es válida. Iniciá sesión nuevamente.'); }
    } catch {
      // A damaged saved value must not be retried on every reload.
      try { localStorage.removeItem(SESSION_KEY); } catch { /* Storage may be disabled. */ }
    }
    this.state.set(session);
    if (session) this.schedule(session);
  }
  private schedule(session: Session): void {
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.validSession(), Math.max(0, session.expiresAt - Date.now() + 1));
  }
}
