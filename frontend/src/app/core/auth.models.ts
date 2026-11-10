export const ROLES = ['ADMIN', 'PROFESOR', 'ALUMNO'] as const;
export type Role = (typeof ROLES)[number];
export interface LoginRequest { email: string; password: string; }
export interface AuthResponse { token: string; email: string; rol: Role; perfilId: number | null; }
export interface Session extends AuthResponse { expiresAt: number; }
export const ROLE_PATH: Record<Role, string> = { ADMIN: '/admin', PROFESOR: '/profesor', ALUMNO: '/alumno' };

// Client-side decoding only controls UI/session timing. The backend verifies the JWT.
export function parseSession(value: unknown, now = Date.now()): Session | null {
  try {
    if (!value || typeof value !== 'object') return null;
    const v = value as Partial<AuthResponse>;
    if (typeof v.token !== 'string' || typeof v.email !== 'string' || !v.email.trim()
      || !ROLES.includes(v.rol as Role) || !(v.perfilId === null || (Number.isSafeInteger(v.perfilId) && Number(v.perfilId) > 0))) return null;
    const parts = v.token.split('.');
    if (parts.length !== 3 || !parts[0] || !parts[1] || !parts[2]) return null;
    const decode = (s: string): unknown => JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(s.replace(/-/g, '+').replace(/_/g, '/')), c => c.charCodeAt(0)))) as unknown;
    const header = decode(parts[0]) as { alg?: unknown };
    const claims = decode(parts[1]) as { exp?: unknown; sub?: unknown; uid?: unknown; ver?: unknown };
    if (header.alg !== 'HS256' || typeof claims.exp !== 'number' || !Number.isFinite(claims.exp)
      || claims.exp * 1000 <= now || claims.exp * 1000 > now + 86400000 + 60000
      || claims.sub !== v.email || !Number.isSafeInteger(claims.uid) || Number(claims.uid) < 1
      || !Number.isSafeInteger(claims.ver) || Number(claims.ver) < 0) return null;
    return { token: v.token, email: v.email, rol: v.rol as Role, perfilId: v.perfilId ?? null, expiresAt: claims.exp * 1000 };
  } catch { return null; }
}
