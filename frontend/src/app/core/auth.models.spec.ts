import { describe, it, expect } from 'vitest';
import { parseSession } from './auth.models';
import { testSession } from './auth.fixtures';
describe('JWT session validation', () => {
  it.each(['ADMIN', 'PROFESOR', 'ALUMNO'] as const)('accepts the %s response contract', role => {
    expect(parseSession(testSession(role))?.rol).toBe(role);
  });
  it.each([null, {}, { token: 'broken' }, { ...testSession(), rol: 'ROOT' }, { ...testSession(), perfilId: -1 }, { ...testSession(), email: 'other@example.test' }, testSession('ALUMNO', -1)])('rejects malformed, inconsistent and expired sessions %#', value => {
    expect(parseSession(value)).toBeNull();
  });
});
