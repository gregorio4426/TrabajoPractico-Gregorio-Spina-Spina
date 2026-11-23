import { test, expect, Page } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { randomBytes } from 'node:crypto';
import { selectEntity } from './entity-picker';
const unique = () => randomBytes(5).toString('hex');
async function login(page: Page, account: { email: string; password: string }) {
  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/login');
  await page.getByLabel('Email', { exact: true }).fill(account.email);
  await page.getByLabel('Contraseña', { exact: true }).fill(account.password);
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await expect(page).not.toHaveURL(/\/login/);
}
async function confirm(page: Page) {
  await page
    .getByRole('dialog')
    .getByRole('button', { name: 'Confirmar cambio', exact: true })
    .click();
}
test('IMPULSO: días automáticos, segundos y sugerencias personales, progreso y panel de trabajo reales', async ({
  page,
  request,
  browser,
}) => {
  test.skip(!process.env['GYM_E2E_REAL_API'], 'Requiere backend MySQL de pruebas');
  test.setTimeout(120000);
  page.setDefaultTimeout(10000);
  const errors: string[] = [];
  page.on('pageerror', (e) => errors.push(e.message));
  const accounts = JSON.parse(readFileSync(process.env['GYM_E2E_CREDENTIALS_FILE']!, 'utf8'));
  const admin = await (await request.post('/api/auth/login', { data: accounts.ADMIN })).json(),
    ah = { Authorization: 'Bearer ' + admin.token };
  const teacher = {
    email: 'ux-prof-' + unique() + '@example.test',
    password: randomBytes(20).toString('base64url'),
  };
  expect(
    (
      await request.post('/api/admin/profesores', {
        headers: ah,
        data: {
          ...teacher,
          perfil: { nombre: 'Impulso', apellido: 'Docente', especialidad: 'Fuerza' },
        },
      })
    ).status(),
  ).toBe(201);
  const who = await (await request.post('/api/auth/login', { data: teacher })).json(),
    th = { Authorization: 'Bearer ' + who.token };
  const students = [];
  for (const nombre of ['Juan', 'Pedro']) {
    const account = {
      email: 'ux-alumno-' + unique() + '@example.test',
      password: randomBytes(20).toString('base64url'),
    };
    const response = await request.post('/api/admin/alumnos', {
      headers: ah,
      data: {
        ...account,
        perfil: {
          nombre,
          apellido: 'Impulso',
          edad: 25,
          nivel: 'PRINCIPIANTE',
          objetivo: 'MEJORAR_RESISTENCIA',
        },
      },
    });
    expect(response.status()).toBe(201);
    const created = await response.json();
    expect(
      (
        await request.put(
          '/api/admin/alumnos/' + created.perfil.id + '/profesores/' + who.perfilId,
          { headers: ah, data: { motivo: 'Prueba UX' } },
        )
      ).status(),
    ).toBe(200);
    students.push({ ...account, id: created.perfil.id, nombre });
  }
  const exercises = [];
  for (const nombre of ['Press', 'Plancha']) {
    const response = await request.post('/api/ejercicios', {
      headers: th,
      data: { nombre: nombre + ' UX ' + unique(), grupoMuscular: 'PECHO' },
    });
    expect(response.status()).toBe(201);
    exercises.push(await response.json());
  }
  await login(page, teacher);
  await expect(page.getByRole('link', { name: /Mis alumnos/ }).first()).toBeVisible();
  await expect(page.locator('app-progress-chart')).toHaveCount(0);
  await expect(
    page.getByRole('link', { name: 'Ver todos mis alumnos →', exact: true }),
  ).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/profesor/rutinas/nueva');
  const name = 'Plan UX ' + unique();
  await page.getByLabel('Nombre de rutina', { exact: true }).fill(name);
  await page.getByLabel('Descripción de rutina', { exact: true }).fill('Fuerza y estabilidad');
  const day = page.getByRole('group', { name: 'Día 1', exact: true });
  await day.getByLabel('Vueltas', { exact: true }).fill('2');
  for (let i = 0; i < 2; i++) {
    await day.getByRole('button', { name: '+ Agregar ejercicio', exact: true }).click();
    const row = day.locator('.compact-exercise').nth(i);
    await row.getByRole('combobox', { name: 'Ejercicio', exact: true }).fill(exercises[i].nombre);
    await row.getByRole('option', { name: new RegExp(exercises[i].nombre) }).click();
    if (i === 0) await row.getByLabel('Repeticiones', { exact: true }).fill('12');
    else {
      await row.getByRole('combobox', { name: 'Medir por', exact: true }).selectOption('SEGUNDOS');
      await row.getByLabel('Segundos', { exact: true }).fill('45');
      await expect(row.getByLabel('Repeticiones', { exact: true })).toHaveCount(0);
    }
  }
  await expect(page.getByLabel('Peso sugerido', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Crear rutina', exact: true }).click();
  await expect(page).toHaveURL(/guardada=1/);
  const id = Number(new URL(page.url()).pathname.split('/').pop()),
    routine = await (await request.get('/api/rutinas/' + id, { headers: th })).json();
  expect(routine.dias[0].nombre).toBe('Día 1');
  expect(routine.dias[0].ejercicios[1]).toMatchObject({
    tipoMedicion: 'SEGUNDOS',
    segundosSugeridos: 45,
    repsSugeridas: null,
  });
  await expect(page.getByRole('link', { name: 'Editar rutina', exact: true })).toBeVisible();
  await expect(page.locator('app-assignment-weights')).toHaveCount(0);
  await selectEntity(page, 'Alumno', students[0].nombre + ' Impulso');
  const personalize = page.locator('app-assignment-weights');
  await expect(personalize).toBeVisible();
  await personalize.getByLabel('Peso sugerido', { exact: true }).first().fill('60');
  await personalize.getByLabel('Repeticiones sugeridas', { exact: true }).fill('10');
  await personalize.getByLabel('Tiempo sugerido (segundos)', { exact: true }).fill('60');
  await page.getByRole('button', { name: 'Asignar rutina', exact: true }).click();
  await confirm(page);
  await expect(
    page.getByText('Rutina asignada correctamente al alumno.', { exact: true }),
  ).toBeVisible();
  await selectEntity(page, 'Alumno', students[1].nombre + ' Impulso');
  await expect(personalize.getByLabel('Peso sugerido', { exact: true }).first()).toBeEmpty();
  await page.getByRole('button', { name: 'Asignar rutina', exact: true }).click();
  await confirm(page);
  const [juan, pedro] = await Promise.all(
    students.map(
      async (s) =>
        await (
          await request.post('/api/auth/login', { data: { email: s.email, password: s.password } })
        ).json(),
    ),
  );
  const jr = await (
      await request.get('/api/rutinas/me/activa', {
        headers: { Authorization: 'Bearer ' + juan.token },
      })
    ).json(),
    pr = await (
      await request.get('/api/rutinas/me/activa', {
        headers: { Authorization: 'Bearer ' + pedro.token },
      })
    ).json();
  expect(jr.dias[0].ejercicios[0].repsPersonalizadas).toBe(10);
  expect(pr.dias[0].ejercicios[0].repsPersonalizadas).toBeNull();
  expect(pr.dias[0].ejercicios[0].repsSugeridas).toBe(12);
  await page.goto('/profesor/rutinas');
  await expect(page.locator('.teacher-history-list>div').filter({ hasText: name })).toHaveCount(1);
  await page.getByRole('link', { name: 'Editar rutina', exact: true }).click();
  await page
    .getByLabel('Descripción de rutina', { exact: true })
    .fill('Fuerza, estabilidad y constancia');
  await page.getByRole('button', { name: 'Guardar rutina', exact: true }).click();
  await expect(page).toHaveURL(/guardada=2/);
  await page.goto('/profesor/rutinas');
  await expect(page.locator('.teacher-history-list>div').filter({ hasText: name })).toHaveCount(1);
  const context = await browser.newContext({ viewport: { width: 390, height: 844 } }),
    sp = await context.newPage();
  sp.setDefaultTimeout(10000);
  sp.on('pageerror', (e) => errors.push(e.message));
  await login(sp, students[0]);
  await sp.goto('/alumno/rutina');
  await expect(sp.locator('main')).toContainText('60 segundos');
  await expect(sp.locator('main')).toContainText('10 repeticiones');
  await expect(sp.locator('main')).not.toContainText('Sin peso sugerido');
  const mainOutline = await sp.locator('main').evaluate((el) => getComputedStyle(el).outlineStyle);
  expect(mainOutline).toBe('none');
  await sp.getByRole('button', { name: 'Iniciar Día 1', exact: true }).click();
  await confirm(sp);
  await expect(sp).toHaveURL(/\/alumno\/sesiones\/\d+/);
  const session = Number(new URL(sp.url()).pathname.split('/').pop());
  const rows = sp.locator('app-workout-grid .workout-series-row');
  const first = rows.nth(0),
    second = rows.nth(1);
  await second.getByRole('spinbutton', { name: /repeticiones$/ }).fill('8');
  await first.getByRole('spinbutton', { name: /repeticiones$/ }).fill('10');
  await first.getByRole('spinbutton', { name: /peso$/ }).fill('60');
  await first.getByRole('button', { name: 'Guardar serie', exact: true }).click();
  await expect(sp.getByRole('button', { name: 'Recargar sesión', exact: true })).toBeEnabled();
  await expect(second.getByRole('spinbutton', { name: /repeticiones$/ })).toHaveValue('8');
  await expect(sp.locator('.student-series-card')).toHaveCount(0);
  await expect(sp.locator('.student-block-form')).toHaveCount(0);
  await first.getByRole('spinbutton', { name: /repeticiones$/ }).fill('9');
  await first.getByRole('spinbutton', { name: /peso$/ }).fill('62');
  await first.getByRole('button', { name: 'Guardar cambio', exact: true }).click();
  await expect(sp.getByRole('button', { name: 'Recargar sesión', exact: true })).toBeEnabled();
  const timed = rows.nth(2);
  await timed.getByRole('spinbutton', { name: /segundos$/ }).fill('44');
  await timed.getByRole('button', { name: 'Guardar serie', exact: true }).click();
  await expect(sp.getByRole('button', { name: 'Recargar sesión', exact: true })).toBeEnabled();
  await sp.reload();
  await expect(first.getByRole('spinbutton', { name: /repeticiones$/ })).toHaveValue('9');
  await expect(first.getByRole('spinbutton', { name: /peso$/ })).toHaveValue('62');
  await expect(rows.nth(2).getByRole('spinbutton', { name: /segundos$/ })).toHaveValue('44');
  const saved = await (
    await request.get('/api/sesiones/' + session, {
      headers: { Authorization: 'Bearer ' + juan.token },
    })
  ).json();
  expect(saved.series.find((s: any) => s.tipoMedicion === 'SEGUNDOS')).toMatchObject({
    segundosHechos: 44,
    repsHechas: null,
  });
  await sp.getByRole('button', { name: 'Finalizar entrenamiento', exact: true }).click();
  await confirm(sp);
  await expect(sp.locator('.student-session-status')).toContainText('Finalizada');
  await expect(sp.locator('.student-series-card')).toHaveCount(2);
  for (const card of await sp.locator('.student-series-card').all()) {
    expect((await card.boundingBox())!.height).toBeLessThan(90);
  }
  expect(await sp.locator('body').evaluate((el) => el.scrollWidth <= innerWidth)).toBe(true);
  await sp.screenshot({ path: '/tmp/impulso-sesion-compacta.png', fullPage: true });
  await sp.goto('/alumno/progreso');
  await expect(sp.getByRole('heading', { name: 'Días que entrenaste', exact: true })).toBeVisible();
  await expect(sp.locator('.week-days .trained')).toHaveCount(1);
  await expect(sp.locator('main')).not.toContainText('Frecuencia observada');
  await expect(sp.locator('main')).not.toContainText('Fechas UTC');
  await expect(sp.locator('app-progress-chart')).toHaveCount(1);
  await sp.locator('.exercise-choice select').selectOption(String(exercises[0].id));
  await expect(sp.locator('app-progress-chart')).toHaveCount(2);
  expect(await sp.locator('body').evaluate((el) => el.scrollWidth <= innerWidth)).toBe(true);
  await sp.screenshot({ path: '/tmp/impulso-progreso-mobile.png', fullPage: true });
  const trainedDay = sp.locator('.week-days .trained');
  const selectedDate = new URL((await trainedDay.getAttribute('href'))!, sp.url()).searchParams.get(
    'desde',
  )!;
  await trainedDay.click();
  await expect(sp.getByRole('heading', { name: 'Mis entrenamientos.' })).toBeVisible();
  await expect(sp.getByLabel('Desde', { exact: true })).toHaveValue(selectedDate);
  await expect(sp.getByLabel('Hasta', { exact: true })).toHaveValue(selectedDate);
  await expect(sp.locator('.student-session-row')).toHaveCount(1);
  await page.goto('/profesor');
  await expect(page.locator('.daily-student').filter({ hasText: 'Juan Impulso' })).toContainText(
    'Con rutina',
  );
  await expect(page.locator('app-progress-chart')).toHaveCount(0);
  await page.screenshot({ path: '/tmp/impulso-profesor-mobile.png', fullPage: true });
  expect(errors).toEqual([]);
  await context.close();
});
