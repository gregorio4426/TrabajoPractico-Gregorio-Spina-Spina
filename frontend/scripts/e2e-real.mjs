import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

// A real validation run must fail, not silently skip, when its private inputs are missing.
try {
  const path = process.env.GYM_E2E_CREDENTIALS_FILE;
  if (!path) throw new Error();
  const accounts = JSON.parse(readFileSync(path, 'utf8'));
  for (const role of ['ADMIN', 'PROFESOR']) {
    if (
      typeof accounts[role]?.email !== 'string' ||
      !accounts[role].email.trim() ||
      typeof accounts[role]?.password !== 'string' ||
      !accounts[role].password
    )
      throw new Error();
  }
} catch {
  console.error(
    'Configurá GYM_E2E_CREDENTIALS_FILE con un JSON privado externo que contenga cuentas ADMIN y PROFESOR de la base de pruebas.',
  );
  process.exit(1);
}
const cli = fileURLToPath(import.meta.resolve('@playwright/test/cli'));
const result = spawnSync(process.execPath, [cli, 'test', ...process.argv.slice(2)], {
  stdio: 'inherit',
  env: { ...process.env, GYM_E2E_REAL_API: '1' },
});
if (result.error) console.error('No se pudo iniciar Playwright. Revisá Node y npm ci.');
process.exit(result.status ?? 1);
