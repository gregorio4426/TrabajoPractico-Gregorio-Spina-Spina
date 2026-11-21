import { test, expect, Page } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { randomBytes } from 'node:crypto';
const uid = () => randomBytes(5).toString('hex');
async function login(page: Page, account: { email: string; password: string }) {
  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/login');
  await page.getByLabel('Email', { exact: true }).fill(account.email);
  await page.getByLabel('Contraseña', { exact: true }).fill(account.password);
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await expect(page).not.toHaveURL(/\/login/);
}
test('ADMIN sin asignar → vinculación segura → tres días y bloques por arrastre → resultados individuales persistentes', async ({
  page,
  request,
  browser,
}) => {
  test.skip(!process.env['GYM_E2E_REAL_API'], 'Requiere API real de pruebas');
  test.setTimeout(120000);
  page.setDefaultTimeout(10000);
  const accounts = JSON.parse(readFileSync(process.env['GYM_E2E_CREDENTIALS_FILE']!, 'utf8'));
  const admin = await (await request.post('/api/auth/login', { data: accounts.ADMIN })).json();
  const ah = { Authorization: 'Bearer ' + admin.token };
  const teacher = {
    email: 'simple-prof-' + uid() + '@example.test',
    password: randomBytes(20).toString('base64url'),
  };
  expect(
    (
      await request.post('/api/admin/profesores', {
        headers: ah,
        data: {
          ...teacher,
          perfil: { nombre: 'Simple', apellido: 'Docente', especialidad: 'Fuerza' },
        },
      })
    ).status(),
  ).toBe(201);
  const who = await (await request.post('/api/auth/login', { data: teacher })).json(),
    th = { Authorization: 'Bearer ' + who.token };
  const student = {
    email: 'simple-alumno-' + uid() + '@example.test',
    password: randomBytes(20).toString('base64url'),
  };
  const created = await request.post('/api/admin/alumnos', {
    headers: ah,
    data: {
      ...student,
      perfil: {
        nombre: 'Disponible',
        apellido: 'Prueba',
        edad: 25,
        nivel: 'PRINCIPIANTE',
        objetivo: 'MEJORAR_RESISTENCIA',
      },
    },
  });
  expect(created.status()).toBe(201);
  const studentId = (await created.json()).perfil.id;
  await login(page, accounts.ADMIN);
  await page.goto('/admin/alumnos');
  await page.getByLabel('Buscar nombre o email', { exact: true }).fill(student.email);
  await page.getByRole('button', { name: 'Aplicar filtros', exact: true }).click();
  await expect(page.getByRole('cell', { name: 'Sin asignar', exact: true })).toBeVisible();
  expect((await request.get('/api/alumnos/' + studentId, { headers: th })).status()).toBe(403);
  await login(page, teacher);
  await page.goto('/profesor/alumnos');
  await page.getByRole('button', { name: 'Buscar alumnos disponibles', exact: true }).click();
  await page.getByLabel('Buscar nombre o email', { exact: true }).fill(student.email);
  await page.getByRole('button', { name: 'Buscar', exact: true }).click();
  await page
    .locator('app-available-students article')
    .filter({ hasText: student.email })
    .getByRole('button', { name: 'Vincular a mis alumnos', exact: true })
    .click();
  await expect(
    page.getByRole('link', { name: 'Ver Disponible Prueba', exact: true }),
  ).toBeVisible();
  const exercise = await (
    await request.post('/api/ejercicios', {
      headers: th,
      data: { nombre: 'Sentadilla ' + uid(), grupoMuscular: 'PIERNAS' },
    })
  ).json();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/profesor/rutinas/nueva');
  await page.getByLabel('Nombre de rutina', { exact: true }).fill('Plan simple ' + uid());
  await page
    .getByLabel('Descripción de rutina', { exact: true })
    .fill('Tres días, bloques reutilizables');
  for (let di = 1; di <= 3; di++) {
    const day = page.getByRole('group', { name: 'Día ' + di, exact: true });
    await day.getByLabel('Nombre del día', { exact: true }).fill('Entrenamiento ' + di);
    await day.getByLabel('Vueltas', { exact: true }).fill('3');
    for (let ei = 0; ei < (di === 1 ? 2 : 1); ei++) {
      await day.getByRole('button', { name: '+ Agregar ejercicio', exact: true }).click();
      const row = day.locator('.compact-exercise').nth(ei);
      await row.getByRole('combobox', { name: 'Ejercicio', exact: true }).fill(exercise.nombre);
      await row.getByRole('option', { name: new RegExp(exercise.nombre) }).click();
      await row.getByLabel('Repeticiones', { exact: true }).fill(String(ei === 0 ? 12 : 8));
    }
    if (di < 3) await day.getByRole('button', { name: '+ Agregar día', exact: true }).click();
  }
  await expect(page.getByLabel('Orden del día', { exact: true })).toHaveCount(0);
  await expect(page.getByLabel('Peso sugerido', { exact: true })).toHaveCount(0);
  await expect(
    page.getByRole('button', { name: /Subir bloque|Bajar bloque|Mover al bloque/ }),
  ).toHaveCount(0);
  const day1 = page.getByRole('group', { name: 'Día 1', exact: true });
  await day1.getByRole('button', { name: '+ Agregar bloque', exact: true }).click();
  const blocks = day1.locator('.routine-block');
  await blocks.nth(0).getByLabel('Nombre del bloque (opcional)', { exact: true }).fill('Principal');
  await blocks.nth(1).getByLabel('Nombre del bloque (opcional)', { exact: true }).fill('Final');
  await blocks.nth(1).getByRole('button', { name: '+ Agregar ejercicio', exact: true }).click();
  const extra = blocks.nth(1).getByRole('combobox', { name: 'Ejercicio', exact: true });
  await extra.fill(exercise.nombre);
  await blocks
    .nth(1)
    .getByRole('option', { name: new RegExp(exercise.nombre) })
    .click();
  const handle = blocks.nth(1).getByRole('button', { name: 'Reordenar bloque 2', exact: true });
  await handle.focus();
  await handle.press('ArrowUp');
  await expect(
    blocks.nth(0).getByLabel('Nombre del bloque (opcional)', { exact: true }),
  ).toHaveValue('Final');
  await blocks
    .nth(0)
    .getByRole('button', { name: 'Reordenar bloque 1', exact: true })
    .press('ArrowDown');
  await page.screenshot({ path: '/tmp/gym-simple-editor-mobile.png', fullPage: true });
  expect(await page.locator('body').evaluate((el) => el.scrollWidth <= window.innerWidth)).toBe(
    true,
  );
  await page.screenshot({ path: '/tmp/gym-simple-editor-mobile.png', fullPage: true });
  await page.setViewportSize({ width: 1280, height: 1000 });
  const firstBlock = day1.locator('.routine-block').first();
  const exerciseHandles = firstBlock.getByRole('button', {
    name: 'Reordenar ejercicio',
    exact: true,
  });
  await exerciseHandles.nth(0).scrollIntoViewIfNeeded();
  const from = await exerciseHandles.nth(1).boundingBox(),
    to = await exerciseHandles.nth(0).boundingBox();
  expect(from).not.toBeNull();
  expect(to).not.toBeNull();
  await page.mouse.move(from!.x + from!.width / 2, from!.y + from!.height / 2);
  await page.mouse.down();
  await page.mouse.move(from!.x + from!.width / 2, from!.y - 10, { steps: 5 });
  await page.mouse.move(to!.x + to!.width / 2, to!.y + 2, { steps: 20 });
  await page.mouse.up();
  await expect(firstBlock.getByLabel('Repeticiones', { exact: true }).first()).toHaveValue('8');
  await exerciseHandles.nth(0).press('ArrowDown');
  await expect(firstBlock.getByLabel('Repeticiones', { exact: true }).first()).toHaveValue('12');
  await page.getByRole('button', { name: 'Crear rutina', exact: true }).click();
  await expect(page).toHaveURL(/guardada=1/);
  const id = Number(new URL(page.url()).pathname.split('/').pop());
  const routine = await (await request.get('/api/rutinas/' + id, { headers: th })).json();
  expect(routine.dias.map((d: any) => d.numeroOrden)).toEqual([1, 2, 3]);
  expect(routine.dias[0].ejercicios.map((e: any) => e.repsSugeridas)).toEqual([12, 8, 1]);
  await page.goto('/profesor/rutinas/' + id + '/editar');
  await expect(page.getByLabel('Nombre del día', { exact: true })).toHaveCount(3);
  await page
    .getByLabel('Descripción de rutina', { exact: true })
    .fill('Tres días, bloques reutilizables. Versión revisada.');
  await page.getByRole('button', { name: 'Guardar nueva versión', exact: true }).click();
  await expect(page).toHaveURL(/guardada=2/);
  const current = await (await request.get('/api/rutinas/' + id, { headers: th })).json();
  expect((await request.get('/api/rutinas/' + id + '/versiones/1', { headers: th })).status()).toBe(
    200,
  );
  expect(
    (
      await request.post('/api/rutinas/asignar/alumno/' + studentId + '/rutina/' + id, {
        headers: th,
        data: {
          pesos: [{ diaEjercicioId: current.dias[0].ejercicios[0].id, pesoSugerido: 60 }],
        },
      })
    ).status(),
  ).toBe(200);
  const sa = await (await request.post('/api/auth/login', { data: student })).json(),
    sh = { Authorization: 'Bearer ' + sa.token };
  const session = await (
    await request.post('/api/sesiones', {
      headers: sh,
      data: { diaRutinaId: current.dias[0].id, fecha: new Date().toISOString().slice(0, 10) },
    })
  ).json();
  const context = await browser.newContext({ viewport: { width: 390, height: 844 } });
  const sp = await context.newPage();
  sp.setDefaultTimeout(10000);
  await login(sp, student);
  await sp.goto('/alumno/sesiones/' + session.id);
  const grid = sp.locator('app-workout-grid'),
    rows = grid.locator('.workout-series-row');
  for (let i = 0; i < 3; i++) {
    const row = rows.nth(i);
    await row.getByRole('spinbutton', { name: /repeticiones$/ }).fill(String([12, 10, 8][i]));
    await row.getByRole('spinbutton', { name: /peso$/ }).fill(String([60, 60, 65][i]));
    await row.getByRole('button', { name: 'Guardar serie', exact: true }).click();
    await expect(row.getByRole('button', { name: 'Guardar cambio', exact: true })).toBeVisible();
    await expect(sp.getByRole('dialog')).not.toBeVisible();
    await expect(sp.getByRole('button', { name: 'Recargar sesión', exact: true })).toBeEnabled();
  }
  await sp.reload();
  for (let i = 0; i < 3; i++) {
    await expect(rows.nth(i).getByRole('spinbutton', { name: /repeticiones$/ })).toHaveValue(
      String([12, 10, 8][i]),
    );
    await expect(rows.nth(i).getByRole('spinbutton', { name: /peso$/ })).toHaveValue(
      String([60, 60, 65][i]),
    );
  }
  expect(await sp.locator('body').evaluate((el) => el.scrollWidth <= window.innerWidth)).toBe(true);
  await context.close();
});
