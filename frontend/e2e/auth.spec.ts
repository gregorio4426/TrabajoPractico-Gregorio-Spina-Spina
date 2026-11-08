import { test, expect } from '@playwright/test';

// UI isolation fixtures; production login always calls the real backend.
function session(rol: string, seconds = 3600) {
  const encode = (value: unknown) => Buffer.from(JSON.stringify(value)).toString('base64url');
  return {
    token: `${encode({ alg: 'HS256' })}.${encode({ exp: Math.floor(Date.now() / 1000) + seconds, sub: 'ui@example.test', uid: 1, ver: 0 })}.ui-only`,
    email: 'ui@example.test',
    rol,
    perfilId: rol === 'ADMIN' ? null : 1,
  };
}
test.beforeEach(async ({ page }) => {
  await page.route('**/api/alumnos/me/dashboard*',route=>route.fulfill({json:{periodo:{},entrenamientos:{},frecuencia:{},rutinaActiva:null}}));
  await page.route(/\/api\/alumnos\/me\/estadisticas(?:\/|\?|$)/,route=>route.fulfill({json:route.request().url().includes('/actividad')||route.request().url().includes('/ejercicios')?{contenido:[],pagina:0,totalPaginas:0,totalElementos:0,tamanio:20}:{periodo:{},datos:{},frecuencia:{}}}));
  await page.route('**/api/sesiones/me/historial',route=>route.fulfill({json:[]}));

  await page.route('**/api/profesores/me/dashboard*', (route) =>
    route.fulfill({ json: { periodo: {}, entrenamientos: {}, frecuencia: {} } }),
  );
  await page.route(/\/api\/profesores\/me\/estadisticas(?:\/|\?|$)/, (route) =>
    route.fulfill({
      json:
        route.request().url().includes('/actividad') ||
        route.request().url().includes('/ejercicios')
          ? { contenido: [], pagina: 0, totalPaginas: 0, totalElementos: 0, tamanio: 20 }
          : { periodo: {}, datos: {}, frecuencia: {} },
    }),
  );
  // Isolated shell tests; real ADMIN requests are covered by admin.spec.ts.
  await page.route('**/api/admin/dashboard*', (route) =>
    route.fulfill({ json: { estadoActual: {}, entrenamientos: {}, frecuencia: {}, periodo: {} } }),
  );
});
test('anonymous deep links redirect to the single login and form validates', async ({ page }) => {
  await page.goto('/admin');
  await expect(page).toHaveURL(/\/login$/);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await expect(page.getByText('Ingresá un email válido.')).toBeVisible();
  await expect(page.getByText('Ingresá tu contraseña.')).toBeVisible();
  await page.getByLabel('Contraseña', { exact: true }).fill('test');
  await page.getByRole('button', { name: 'Mostrar contraseña' }).click();
  await expect(page.getByLabel('Contraseña', { exact: true })).toHaveAttribute('type', 'text');
});
for (const role of ['ADMIN', 'PROFESOR', 'ALUMNO']) {
  test(`${role}: role redirect, persistent session, mobile shell and logout`, async ({ page }) => {
    await page.goto('/login');
    await page.evaluate(
      (data) => localStorage.setItem('gym.session.v1', JSON.stringify(data)),
      session(role),
    );
    await page.reload();
    await expect(page).toHaveURL(new RegExp('/' + role.toLowerCase() + '$'));
    await expect(
      page.getByRole('heading', {
        name:
          role === 'ADMIN'
            ? 'El pulso de tu equipo.'
            : role === 'PROFESOR'
              ? 'Acompañá cada avance.'
              : 'Tu próximo paso empieza acá.',
      }),
    ).toBeVisible();
    await page.goto(role === 'ADMIN' ? '/profesor' : '/admin');
    await expect(page).toHaveURL(new RegExp('/' + role.toLowerCase() + '$'));
    await page.reload();
    await expect(page.getByText('ui@example.test')).toBeVisible();
    await page.setViewportSize({ width: 390, height: 844 });
    await page.getByRole('button', { name: 'Abrir menú' }).click();
    await expect(
      page.getByRole('navigation', { name: 'Menú del espacio personal', exact: true }),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Cerrar menú', exact: true }).click();
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth > window.innerWidth,
    );
    expect(overflow).toBe(false);
    await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).not.toBeVisible();
    await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Cerrar sesión', exact: true })
      .click();
    await expect(page).toHaveURL(/\/login$/);
    expect(await page.evaluate(() => localStorage.getItem('gym.session.v1'))).toBeNull();
  });
}
test('expired persisted token cannot open a protected page', async ({ page }) => {
  await page.goto('/login');
  await page.evaluate(
    (data) => localStorage.setItem('gym.session.v1', JSON.stringify(data)),
    session('ADMIN', -5),
  );
  await page.goto('/admin');
  await expect(page).toHaveURL(/\/login$/);
});
test('session expires while the page is idle', async ({ page }) => {
  await page.goto('/login');
  await page.evaluate(
    (data) => localStorage.setItem('gym.session.v1', JSON.stringify(data)),
    session('ALUMNO', 4),
  );
  await page.goto('/alumno');
  await expect(page).toHaveURL(/\/alumno$/);
  await expect(page).toHaveURL(/\/login$/, { timeout: 8000 });
  await expect(page.getByRole('status')).toContainText('venció');
});
test('login errors allow retry and never expose server diagnostics', async ({ page }) => {
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({ status: 401, json: { mensaje: 'internal-sensitive-diagnostic' } }),
  );
  await page.goto('/login');
  await page.getByLabel('Email', { exact: true }).fill('ui@example.test');
  await page.getByLabel('Contraseña', { exact: true }).fill('test');
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await expect(page.getByRole('alert')).toContainText('Email o contraseña incorrectos');
  await expect(page.getByRole('button', { name: 'Iniciar sesión' })).toBeEnabled();
  await expect(page.getByText('internal-sensitive-diagnostic')).not.toBeVisible();
});
test('login and all workspaces have no horizontal overflow at 360px', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto('/login');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  for (const role of ['ADMIN', 'PROFESOR', 'ALUMNO']) {
    await page.evaluate(
      (data) => localStorage.setItem('gym.session.v1', JSON.stringify(data)),
      session(role),
    );
    await page.goto('/' + role.toLowerCase());
    await expect(
      page.getByRole('heading', {
        name:
          role === 'ADMIN'
            ? 'El pulso de tu equipo.'
            : role === 'PROFESOR'
              ? 'Acompañá cada avance.'
              : 'Tu próximo paso empieza acá.',
      }),
    ).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
  }
});
