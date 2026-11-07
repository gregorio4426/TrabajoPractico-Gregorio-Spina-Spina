import { AuthResponse, Role } from './auth.models';
// Synthetic sessions used ONLY by tests. Not accepted by the real backend.
export function testSession(role: Role = 'ALUMNO', expiresIn = 3600): AuthResponse {
  const encode = (value: unknown) => btoa(JSON.stringify(value)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  return { token: `${encode({ alg: 'HS256' })}.${encode({ exp: Math.floor(Date.now()/1000) + expiresIn, sub: 'test@example.test', uid: 1, ver: 0 })}.test-signature`, email: 'test@example.test', rol: role, perfilId: role === 'ADMIN' ? null : 1 };
}
