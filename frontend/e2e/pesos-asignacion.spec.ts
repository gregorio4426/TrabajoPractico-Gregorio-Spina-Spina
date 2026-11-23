import { test, expect, Page } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { randomBytes } from 'node:crypto';
import { selectEntity } from './entity-picker';
const unique = () => randomBytes(5).toString('hex');
type Account = { email: string; password: string };
async function login(page: Page, account: Account) {
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
test('Pesos por asignación: dos alumnos comparten plantilla con pesos independientes e historial estable', async ({
  page,
  request,
  browser,
}) => {
  test.skip(
    !process.env['GYM_E2E_REAL_API'] || !process.env['GYM_E2E_CREDENTIALS_FILE'],
    'Requiere backend de pruebas real y credenciales privadas.',
  );
  test.setTimeout(120000);
  page.setDefaultTimeout(10000);
  const accounts = JSON.parse(readFileSync(process.env['GYM_E2E_CREDENTIALS_FILE']!, 'utf8')) as {
    ADMIN: Account;
  };
  const adminResponse = await request.post('/api/auth/login', { data: accounts.ADMIN });
  expect(adminResponse.status()).toBe(200);
  const admin = await adminResponse.json();
  const teacher = {
    email: 'pesos-prof-' + unique() + '@example.test',
    password: randomBytes(20).toString('base64url'),
  };
  expect(
    (
      await request.post('/api/admin/profesores', {
        headers: { Authorization: 'Bearer ' + admin.token },
        data: {
          ...teacher,
          perfil: { nombre: 'Pesos', apellido: 'Profesor', especialidad: 'Fuerza' },
        },
      })
    ).status(),
  ).toBe(201);
  const logged = await request.post('/api/auth/login', { data: teacher }),
    identity = await logged.json(),
    headers = { Authorization: 'Bearer ' + identity.token };
  const students = [];
  for (const name of ['Juan', 'Pedro']) {
    const account = {
      email: 'pesos-' + unique() + '@example.test',
      password: randomBytes(20).toString('base64url'),
    };
    const r = await request.post('/api/auth/register/alumno', {
      data: {
        ...account,
        nombre: name,
        apellido: 'Pesos',
        edad: 25,
        nivel: 'PRINCIPIANTE',
        objetivo: 'MEJORAR_RESISTENCIA',
      },
    });
    expect(r.status()).toBe(201);
    const who = await r.json();
    students.push({ ...account, ...who });
    expect(
      (
        await request.put(`/api/admin/alumnos/${who.perfilId}/profesores/${identity.perfilId}`, {
          headers: { Authorization: 'Bearer ' + admin.token },
          data: { motivo: 'Prueba de pesos independientes' },
        })
      ).status(),
    ).toBe(200);
  }
  const [juan, pedro] = students;
  const exercise = 'Press pesos ' + unique();
  expect(
    (
      await request.post('/api/ejercicios', {
        headers,
        data: { nombre: exercise, descripcion: 'Prueba de asignación', grupoMuscular: 'PECHO' },
      })
    ).status(),
  ).toBe(201);
  await login(page, teacher);
  await page.goto('/profesor/rutinas/nueva');
  await page.getByLabel('Nombre de rutina', { exact: true }).fill('Hipertrofia ' + unique());
  await page.getByLabel('Descripción de rutina', { exact: true }).fill('Plantilla compartida');
  await page.getByLabel('Nombre del día (opcional)', { exact: true }).fill('Día 1');
  await selectEntity(page, 'Ejercicio del día 1, fila 1', exercise);
  await expect(page.getByLabel('Vueltas', { exact: true })).toBeVisible();
  await expect(page.getByLabel('Repeticiones', { exact: true })).toBeVisible();
  await expect(page.locator('input[formcontrolname=pesoSugerido]')).toHaveCount(0);
  expect(await page.locator('label').allTextContents()).not.toEqual(
    expect.arrayContaining([expect.stringContaining('*')]),
  );
  await expect(page.getByText(/fila/i)).toHaveCount(0);
  await page.getByRole('button', { name: 'Crear rutina', exact: true }).click();
  await expect(page).toHaveURL(/\/profesor\/rutinas\/\d+\?guardada=/);
  const routineId = Number(new URL(page.url()).pathname.split('/').at(-1));
  async function active(student: typeof juan) {
    const r = await request.get('/api/rutinas/me/activa', {
      headers: { Authorization: 'Bearer ' + student.token },
    });
    expect(r.status()).toBe(200);
    return r.json();
  }
  await selectEntity(page, 'Alumno', juan.email);
  await expect(page.getByLabel('Peso sugerido', { exact: true })).toHaveValue('');
  await page.getByLabel('Peso sugerido', { exact: true }).fill('60');
  await page.getByRole('button', { name: 'Asignar rutina', exact: true }).click();
  await confirm(page);
  await expect(page.getByText(/Rutina asignada correctamente/)).toBeVisible();
  const first = await active(juan);
  expect(first.dias[0].ejercicios[0].pesoSugerido).toBe(60);
  const opened = await request.post('/api/sesiones', {
    headers: { Authorization: 'Bearer ' + juan.token },
    data: { diaRutinaId: first.dias[0].id, fecha: new Date().toISOString().slice(0, 10) },
  });
  expect(opened.status()).toBe(201);
  const session = await opened.json();
  await page.goto('/profesor/asignaciones/' + first.asignacionId);
  await expect(page.getByRole('heading', { name: /Juan Pesos/ })).toBeVisible();
  await expect(page.getByLabel('Peso sugerido', { exact: true })).toHaveValue('60');
  await page.getByLabel('Peso sugerido', { exact: true }).fill('80');
  await page.getByRole('button', { name: 'Guardar sugerencias', exact: true }).click();
  await expect(page.getByText('Sugerencias guardadas.', { exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByLabel('Peso sugerido', { exact: true })).toHaveValue('80');
  await page.goto('/profesor/rutinas/' + routineId);
  await selectEntity(page, 'Alumno', pedro.email);
  await expect(page.getByLabel('Peso sugerido', { exact: true })).toHaveValue('');
  await page.getByLabel('Peso sugerido', { exact: true }).fill('40');
  await page.getByRole('button', { name: 'Asignar rutina', exact: true }).click();
  await confirm(page);
  await expect(page.getByText(/Rutina asignada correctamente/)).toBeVisible();
  expect((await active(pedro)).dias[0].ejercicios[0].pesoSugerido).toBe(40);
  expect((await active(juan)).dias[0].ejercicios[0].pesoSugerido).toBe(80);
  const template = await request.get('/api/rutinas/' + routineId, { headers });
  const definition = await template.json();
  expect(definition.numeroVersion).toBe(1);
  expect(definition.dias[0].ejercicios[0].pesoSugerido).toBeNull();
  const mobile = await browser.newContext({
      viewport: { width: 390, height: 844 },
      baseURL: process.env['GYM_E2E_BASE_URL'] || 'http://127.0.0.1:4200',
    }),
    studentPage = await mobile.newPage();
  try {
    await login(studentPage, juan);
    await studentPage.goto('/alumno/rutina');
    await expect(studentPage.locator('.student-prescription').first()).toContainText('80');
    await studentPage.goto('/alumno/sesiones/' + session.id);
    await expect(
      studentPage.locator('app-workout-grid .student-instruction').first(),
    ).toContainText('60');
    await studentPage.reload();
    await expect(
      studentPage.locator('app-workout-grid .student-instruction').first(),
    ).toContainText('60');
  } finally {
    await mobile.close();
  }
  await page.goto('/profesor/asignaciones/' + first.asignacionId);
  await page.getByLabel('Peso sugerido', { exact: true }).fill('0');
  await page.getByRole('button', { name: 'Guardar sugerencias', exact: true }).click();
  await expect(page.getByText('Sugerencias guardadas.', { exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByLabel('Peso sugerido', { exact: true })).toHaveValue('0');
  await page.getByLabel('Peso sugerido', { exact: true }).fill('');
  await page.getByRole('button', { name: 'Guardar sugerencias', exact: true }).click();
  await expect(page.getByText('Sugerencias guardadas.', { exact: true })).toBeVisible();
  expect((await active(juan)).dias[0].ejercicios[0].pesoSugerido).toBeNull();
  expect((await active(pedro)).dias[0].ejercicios[0].pesoSugerido).toBe(40);
  expect(
    (
      await request.get('/api/asignaciones/' + first.asignacionId, {
        headers: { Authorization: 'Bearer ' + pedro.token },
      })
    ).status(),
  ).toBe(403);
});
