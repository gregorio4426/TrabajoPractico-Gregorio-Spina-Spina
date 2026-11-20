import { test, expect, Page, Locator } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { randomBytes } from 'node:crypto';
import { selectEntity } from './entity-picker';
const unique = () => randomBytes(5).toString('hex');
type Account = { email: string; password: string };
async function login(page: Page, a: Account) {
  await page.goto('/login');
  await page.getByLabel('Email', { exact: true }).fill(a.email);
  await page.getByLabel('Contraseña', { exact: true }).fill(a.password);
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await expect(page).not.toHaveURL(/\/login/);
}
async function confirm(page: Page) {
  await page
    .getByRole('dialog')
    .getByRole('button', { name: 'Confirmar cambio', exact: true })
    .click();
}
async function createInline(
  page: Page,
  row: Locator,
  name: string,
  muscle: string,
  newMuscle = false,
) {
  await row.getByRole('combobox', { name: 'Ejercicio', exact: true }).fill(name);
  await row.getByRole('option', { name: 'Crear ejercicio: ' + name, exact: true }).click();
  const dialog = page.getByRole('dialog', { name: 'Crear ejercicio', exact: true });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByLabel('Nombre del ejercicio', { exact: true })).toHaveValue(name);
  const group = dialog.getByRole('combobox', { name: 'Grupo muscular', exact: true });
  await group.fill(muscle);
  await dialog
    .getByRole('option', {
      name: newMuscle ? 'Crear grupo muscular: ' + muscle : muscle,
      exact: true,
    })
    .click();
  if (newMuscle) await expect(group).toHaveValue(muscle);
  await dialog.getByRole('button', { name: 'Guardar ejercicio', exact: true }).click();
  await expect(dialog).not.toBeVisible();
  await expect(row.getByRole('combobox', { name: 'Ejercicio', exact: true })).toHaveValue(name);
}
test('Circuito de tres ejercicios y tres vueltas: crear dentro del editor, guardar, cerrar y recuperar sin pérdida', async ({
  page,
  request,
  browser,
}) => {
  test.skip(
    !process.env['GYM_E2E_REAL_API'] || !process.env['GYM_E2E_CREDENTIALS_FILE'],
    'Requiere backend MySQL de pruebas y credenciales privadas.',
  );
  test.setTimeout(120000);
  page.setDefaultTimeout(10000);
  const errors: string[] = [];
  page.on('pageerror', (e) => errors.push(e.message));
  const accounts = JSON.parse(readFileSync(process.env['GYM_E2E_CREDENTIALS_FILE']!, 'utf8')) as {
    ADMIN: Account;
  };
  const adminR = await request.post('/api/auth/login', { data: accounts.ADMIN });
  expect(adminR.status()).toBe(200);
  const admin = await adminR.json();
  const teacher = {
    email: 'circuito-prof-' + unique() + '@example.test',
    password: randomBytes(20).toString('base64url'),
  };
  expect(
    (
      await request.post('/api/admin/profesores', {
        headers: { Authorization: 'Bearer ' + admin.token },
        data: {
          ...teacher,
          perfil: { nombre: 'Circuito', apellido: 'Profesor', especialidad: 'Fuerza' },
        },
      })
    ).status(),
  ).toBe(201);
  const teacherR = await request.post('/api/auth/login', { data: teacher }),
    who = await teacherR.json(),
    headers = { Authorization: 'Bearer ' + who.token };
  const students = [];
  for (const nombre of ['Juan', 'Pedro']) {
    const account = {
      email: 'circuito-alumno-' + unique() + '@example.test',
      password: randomBytes(20).toString('base64url'),
    };
    const registered = await request.post('/api/auth/register/alumno', {
      data: {
        ...account,
        nombre,
        apellido: 'Circuito',
        edad: 25,
        nivel: 'PRINCIPIANTE',
        objetivo: 'MEJORAR_RESISTENCIA',
      },
    });
    expect(registered.status()).toBe(201);
    const student = await registered.json();
    students.push({ ...student, ...account });
    expect(
      (
        await request.put(`/api/admin/alumnos/${student.perfilId}/profesores/${who.perfilId}`, {
          headers: { Authorization: 'Bearer ' + admin.token },
          data: { motivo: 'Prueba de circuitos y buscadores' },
        })
      ).status(),
    ).toBe(200);
  }
  const [juan, pedro] = students;
  const name = 'Tren superior ' + unique(),
    muscle = 'Movilidad ' + unique(),
    exerciseNames = ['Press', 'Remo', 'Flexiones'].map((x) => x + ' ' + unique());
  await login(page, teacher);
  await page.goto('/profesor/rutinas/nueva');
  await page.getByLabel('Nombre de rutina', { exact: true }).fill(name);
  await page
    .getByLabel('Descripción de rutina', { exact: true })
    .fill('Tres vueltas al bloque completo');
  await page.getByLabel('Nombre del día', { exact: true }).fill('Día 1');
  await page.getByRole('button', { name: 'Agregar circuito', exact: true }).click();
  const day = page.getByRole('group', { name: 'Día 1', exact: true }),
    block = day.getByRole('group', { name: 'Bloque 1', exact: true });
  await block.getByLabel('Nombre del bloque (opcional)', { exact: true }).fill('Bloque A');
  await block.getByLabel('Vueltas', { exact: true }).fill('3');
  await block.getByLabel('Descanso entre vueltas (segundos)', { exact: true }).fill('60');
  for (let i = 0; i < 3; i++) {
    const row = day.getByRole('group', { name: 'Ejercicio ' + (i + 1), exact: true });
    await createInline(page, row, exerciseNames[i]!, muscle, i === 0);
    await row.getByLabel('Repeticiones', { exact: true }).fill(String([12, 10, 15][i]));
    await row
      .getByLabel('Descanso después del ejercicio (segundos)', { exact: true })
      .fill(String(i === 0 ? 0 : 20));
  }
  await expect(page.getByLabel('Nombre de rutina', { exact: true })).toHaveValue(name);
  await expect(page.getByLabel('Peso sugerido', { exact: true })).toHaveCount(0);
  await expect(block.getByLabel('Vueltas', { exact: true })).toHaveCount(1);
  await expect(block.getByLabel('Series', { exact: true })).toHaveCount(0);
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.locator('body')).toHaveJSProperty('scrollWidth', 390);
  await page.screenshot({
    path: '/tmp/gym-circuito-editor.png',
    fullPage: true,
    animations: 'disabled',
  });
  await page.getByRole('button', { name: 'Crear rutina', exact: true }).click();
  await confirm(page);
  await expect(page).toHaveURL(/\/profesor\/rutinas\/\d+\?guardada=1/);
  const id = Number(new URL(page.url()).pathname.split('/').at(-1));
  const savedR = await request.get('/api/rutinas/' + id, { headers });
  expect(savedR.status()).toBe(200);
  const saved = await savedR.json();
  expect(
    saved.dias[0].ejercicios.map((e: any) => [
      e.bloqueNombre,
      e.vueltas,
      e.ordenEnBloque,
      e.repsSugeridas,
      e.descansoVueltaSegundos,
    ]),
  ).toEqual([
    ['Bloque A', 3, 1, 12, 60],
    ['Bloque A', 3, 2, 10, 60],
    ['Bloque A', 3, 3, 15, 60],
  ]);
  await page.goto('/profesor');
  await page.goto(`/profesor/rutinas/${id}/editar`);
  await expect(page.getByLabel('Nombre del bloque (opcional)', { exact: true })).toHaveValue(
    'Bloque A',
  );
  await expect(page.getByLabel('Vueltas', { exact: true })).toHaveValue('3');
  for (let i = 0; i < 3; i++)
    await expect(
      page
        .getByRole('group', { name: 'Ejercicio ' + (i + 1), exact: true })
        .getByRole('combobox', { name: 'Ejercicio', exact: true }),
    ).toHaveValue(exerciseNames[i]!);
  // Existing suggestions support keyboard selection; circuits can grow beyond three exercises.
  await page.getByRole('button', { name: 'Agregar ejercicio al bloque', exact: true }).click();
  const fourth = page.getByRole('group', { name: 'Ejercicio 4', exact: true });
  const search = fourth.getByRole('combobox', { name: 'Ejercicio', exact: true });
  await search.fill(exerciseNames[0]!.toUpperCase());
  await search.press('ArrowDown');
  await search.press('Enter');
  await expect(search).toHaveValue(exerciseNames[0]!);
  await fourth.getByLabel('Repeticiones', { exact: true }).fill('8');
  await page.getByRole('button', { name: 'Agregar bloque de series', exact: true }).click();
  const fifth = page.getByRole('group', { name: 'Ejercicio 5', exact: true });
  await fifth.getByRole('combobox', { name: 'Ejercicio', exact: true }).fill(exerciseNames[1]!);
  await fifth.getByRole('option', { name: new RegExp(exerciseNames[1]!) }).click();
  await fourth.getByRole('button', { name: 'Mover al bloque siguiente', exact: true }).click();
  await page.getByRole('button', { name: 'Guardar nueva versión', exact: true }).click();
  await confirm(page);
  await expect(page).toHaveURL(/guardada=2/);
  const updated = await request.get('/api/rutinas/' + id, { headers }),
    v2 = await updated.json();
  expect(v2.dias[0].ejercicios.slice(0, 3).map((e: any) => e.vueltas)).toEqual([3, 3, 3]);
  expect(v2.dias[0].ejercicios.map((e: any) => e.ordenEnBloque)).toEqual([1, 2, 3, 1, 2]);
  const old = await request.get(`/api/rutinas/${id}/versiones/1`, { headers });
  expect((await old.json()).dias[0].ejercicios.length).toBe(3);
  for (const [student, weight] of [
    [juan, 60],
    [pedro, 40],
  ] as const) {
    const response = await request.post(
      `/api/rutinas/asignar/alumno/${student.perfilId}/rutina/${id}`,
      {
        headers,
        data: {
          pesos: v2.dias[0].ejercicios.map((e: any) => ({
            diaEjercicioId: e.id,
            pesoSugerido: weight,
          })),
        },
      },
    );
    expect(response.status()).toBe(200);
  }
  const mobile = await browser.newContext({
      viewport: { width: 390, height: 844 },
      baseURL: process.env['GYM_E2E_BASE_URL'] || 'http://127.0.0.1:4200',
    }),
    studentPage = await mobile.newPage();
  try {
    await login(studentPage, juan);
    await studentPage.goto('/alumno/rutina');
    const assigned = studentPage.getByRole('region', { name: 'Bloque A', exact: true });
    await expect(assigned).toContainText('3 vueltas');
    await expect(assigned).toContainText('Una vuelta incluye todos');
    await expect(assigned.locator('.student-prescription')).toHaveCount(3);
    await expect(assigned).toContainText('60');
    await expect(studentPage.locator('body')).toHaveJSProperty('scrollWidth', 390);
  } finally {
    await mobile.close();
  }
  const other = await request.get('/api/rutinas/me/activa', {
    headers: { Authorization: 'Bearer ' + pedro.token },
  });
  expect((await other.json()).dias[0].ejercicios[0].pesoSugerido).toBe(40);
  const template = await request.get('/api/rutinas/' + id, { headers });
  expect(
    (await template.json()).dias[0].ejercicios.every((e: any) => e.pesoSugerido === null),
  ).toBe(true);
  expect(errors).toEqual([]);
});
