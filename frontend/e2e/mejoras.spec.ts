import { test, expect, Page, APIRequestContext } from '@playwright/test';
import { randomBytes } from 'node:crypto';
import { readFileSync } from 'node:fs';
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
async function apiLogin(request: APIRequestContext, account: Account) {
  const r = await request.post('/api/auth/login', { data: account });
  expect(r.status()).toBe(200);
  return (await r.json()) as { token: string; perfilId: number };
}
test.describe('Mejoras funcionales · persistencia real', () => {
  test.skip(
    !process.env['GYM_E2E_REAL_API'] || !process.env['GYM_E2E_CREDENTIALS_FILE'],
    'Requiere backend MySQL de pruebas y cuentas privadas.',
  );
  test('custom groups, versioned blocks, free goals and student comments reach the linked teacher', async ({
    page,
    request,
    browser,
  }) => {
    test.setTimeout(120000);
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    const accounts = JSON.parse(readFileSync(process.env['GYM_E2E_CREDENTIALS_FILE']!, 'utf8')) as {
      ADMIN: Account;
    };
    const admin = await apiLogin(request, accounts.ADMIN),
      teacher = {
        email: 'blocks-teacher-' + unique() + '@example.test',
        password: randomBytes(20).toString('base64url'),
      };
    const created = await request.post('/api/admin/profesores', {
      headers: { Authorization: 'Bearer ' + admin.token },
      data: {
        ...teacher,
        perfil: { nombre: 'Bloques', apellido: 'Docente', especialidad: 'Fuerza' },
      },
    });
    expect(created.status()).toBe(201);
    const identity = await apiLogin(request, teacher),
      headers = { Authorization: 'Bearer ' + identity.token };
    const student = {
      email: 'blocks-student-' + unique() + '@example.test',
      password: randomBytes(20).toString('base64url'),
    };
    const registered = await request.post('/api/auth/register/alumno', {
      data: {
        ...student,
        nombre: 'AlumnoBloques',
        apellido: 'Prueba',
        edad: 25,
        nivel: 'PRINCIPIANTE',
        objetivo: 'MEJORAR_RESISTENCIA',
      },
    });
    expect(registered.status()).toBe(201);
    const who = (await registered.json()) as { token: string; perfilId: number };
    expect(
      (
        await request.put(`/api/admin/alumnos/${who.perfilId}/profesores/${identity.perfilId}`, {
          headers: { Authorization: 'Bearer ' + admin.token },
          data: { motivo: 'Prueba de bloques y comentarios' },
        })
      ).status(),
    ).toBe(200);
    await login(page, teacher);
    await page.goto('/profesor/ejercicios');
    const muscle = 'Movilidad ' + unique();
    await page.getByLabel('Nombre del grupo muscular', { exact: true }).fill(muscle);
    await page.getByRole('button', { name: 'Crear grupo muscular', exact: true }).click();
    const [groupResponse] = await Promise.all([
      page.waitForResponse(
        (r) => r.url().endsWith('/api/grupos-musculares') && r.request().method() === 'POST',
      ),
      confirm(page),
    ]);
    expect(groupResponse.status()).toBe(201);
    await expect(page.getByText('Grupo muscular guardado.', { exact: true })).toBeVisible();
    const catalogue = await request.get('/api/grupos-musculares', { headers });
    expect(catalogue.status()).toBe(200);
    const group = ((await catalogue.json()) as Array<{ id: number; nombre: string }>).find(
      (g) => g.nombre === muscle,
    )!;
    expect(group).toBeDefined();
    await page.getByRole('searchbox', { name: 'Buscar grupo muscular', exact: true }).fill(muscle);
    await expect(page.getByRole('button', { name: 'Editar ' + muscle, exact: true })).toBeVisible();
    const exercise = 'Ejercicio bloques ' + unique();
    await page.getByRole('button', { name: 'Crear ejercicio', exact: true }).click();
    await page.getByLabel('Nombre del ejercicio *', { exact: true }).fill(exercise);
    await page
      .getByLabel('Descripción del ejercicio *', { exact: true })
      .fill('Personalizado para biserie');
    await page
      .getByRole('combobox', { name: 'Grupo muscular del ejercicio *', exact: true })
      .selectOption({ label: muscle });
    await page.getByRole('button', { name: 'Guardar ejercicio', exact: true }).click();
    await confirm(page);
    await expect(
      page.getByText('Ejercicio guardado correctamente.', { exact: true }),
    ).toBeVisible();
    await page.goto('/profesor/rutinas/nueva');
    await page.getByLabel('Nombre de rutina *', { exact: true }).fill('Biserie real ' + unique());
    await page
      .getByLabel('Descripción de rutina *', { exact: true })
      .fill('Dos ejercicios con pesos individuales');
    await page.getByLabel('Nombre del día 1 *', { exact: true }).fill('Día de biseries');
    await page.getByRole('button', { name: 'Quitar ejercicio 1 del día 1', exact: true }).click();
    await page.getByRole('button', { name: 'Agregar biserie', exact: true }).click();
    await page.getByLabel('Nombre del bloque (opcional)', { exact: true }).fill('Bloque principal');
    await page.getByLabel('Descanso entre vueltas (segundos)', { exact: true }).fill('60');
    for (let i = 1; i <= 2; i++)
      await selectEntity(page, `Ejercicio del día 1, fila ${i}`, exercise);
    await page.getByLabel('Vueltas día 1, fila 1 *', { exact: true }).fill('2');
    await expect(page.getByLabel('Vueltas día 1, fila 2 *', { exact: true })).toHaveValue('2');
    await page.getByLabel('Repeticiones día 1, fila 1 *', { exact: true }).fill('8');
    await page.getByLabel('Peso sugerido día 1, fila 1', { exact: true }).fill('0');
    await page.getByLabel('Repeticiones día 1, fila 2 *', { exact: true }).fill('12');
    await page
      .getByLabel('Descanso después del ejercicio (segundos)', { exact: true })
      .nth(0)
      .fill('0');
    await page
      .getByLabel('Descanso después del ejercicio (segundos)', { exact: true })
      .nth(1)
      .fill('20');
    await page.setViewportSize({ width: 390, height: 844 });
    await expect(page.locator('body')).toHaveJSProperty('scrollWidth', 390);
    await page.screenshot({
      path: '/tmp/gym-mejoras-editor-mobile.png',
      fullPage: true,
      animations: 'disabled',
    });
    await page.getByRole('button', { name: 'Crear rutina', exact: true }).click();
    const [saved] = await Promise.all([
      page.waitForResponse(
        (r) => r.url().endsWith('/api/rutinas') && r.request().method() === 'POST',
      ),
      confirm(page),
    ]);
    expect(saved.status()).toBe(201);
    await expect(page).toHaveURL(/\/profesor\/rutinas\/\d+\?guardada=/);
    const routineId = Number(new URL(page.url()).pathname.split('/').at(-1));
    const persisted = await request.get(`/api/rutinas/${routineId}`, { headers });
    expect(persisted.status()).toBe(200);
    const routine = await persisted.json();
    expect(
      routine.dias[0].ejercicios.map(
        (e: { vueltas: number; repsSugeridas: number; pesoSugerido: number | null }) => [
          e.vueltas,
          e.repsSugeridas,
          e.pesoSugerido,
        ],
      ),
    ).toEqual([
      [2, 8, 0],
      [2, 12, null],
    ]);
    expect(
      (
        await request.post(`/api/rutinas/asignar/alumno/${who.perfilId}/rutina/${routine.id}`, {
          headers,
        })
      ).status(),
    ).toBe(200);
    const mobile = await browser.newContext({
        viewport: { width: 390, height: 844 },
        baseURL: process.env['GYM_E2E_BASE_URL'] || 'http://127.0.0.1:4200',
      }),
      studentPage = await mobile.newPage();
    studentPage.on('pageerror', (e) => errors.push(e.message));
    try {
      await login(studentPage, student);
      await studentPage.goto('/alumno/perfil');
      await studentPage.getByLabel('Objetivo del alumno').fill('Correr 5 km y mejorar movilidad');
      await studentPage
        .getByRole('combobox', { name: 'Categoría de objetivo (opcional)', exact: true })
        .selectOption('');
      await studentPage.getByRole('button', { name: 'Guardar perfil', exact: true }).click();
      await confirm(studentPage);
      await studentPage.reload();
      await expect(studentPage.getByLabel('Objetivo del alumno')).toHaveValue(
        'Correr 5 km y mejorar movilidad',
      );
      await studentPage.goto('/alumno/rutina');
      await expect(
        studentPage.getByRole('heading', { name: 'Bloque principal · Biserie', exact: true }),
      ).toBeVisible();
      await studentPage
        .getByRole('button', { name: 'Iniciar Día de biseries', exact: true })
        .click();
      await confirm(studentPage);
      await expect(studentPage).toHaveURL(/\/alumno\/sesiones\/\d+/);
      const sessionId = Number(studentPage.url().split('/').at(-1));
      await studentPage.getByRole('button', { name: /Registrar .*posición 1$/ }).click();
      await studentPage.getByLabel('Repeticiones realizadas *', { exact: true }).fill('8');
      await studentPage.getByLabel('Peso realizado (opcional)', { exact: true }).fill('0');
      await studentPage.getByRole('button', { name: 'Guardar serie', exact: true }).click();
      await confirm(studentPage);
      await expect(studentPage.locator('.student-series-card')).toHaveCount(1);
      await studentPage.reload();
      await expect(studentPage.locator('.student-series-card')).toHaveCount(1);
      await studentPage
        .getByLabel('Comentario para tus profesores (opcional)', { exact: true })
        .fill('Me costó la segunda vuelta, sin dolor.');
      await studentPage
        .getByRole('button', { name: 'Finalizar entrenamiento', exact: true })
        .click();
      const [closed] = await Promise.all([
        studentPage.waitForResponse(
          (r) => r.url().endsWith('/completar') && r.request().method() === 'PUT',
        ),
        confirm(studentPage),
      ]);
      expect(closed.status()).toBe(200);
      const persistedSession = await request.get(`/api/sesiones/${sessionId}`, {
        headers: { Authorization: 'Bearer ' + who.token },
      });
      expect((await persistedSession.json()).comentario).toBe(
        'Me costó la segunda vuelta, sin dolor.',
      );
      await studentPage.reload();
      await expect(
        studentPage.getByText('Me costó la segunda vuelta, sin dolor.', { exact: true }),
      ).toBeVisible();
      await expect(studentPage.locator('body')).toHaveJSProperty('scrollWidth', 390);
      await studentPage.screenshot({ path: '/tmp/gym-mejoras-alumno-mobile.png', fullPage: true });
      await page.goto(`/profesor/alumnos/${who.perfilId}`);
      await expect(page.getByLabel('Objetivo del alumno')).toHaveValue(
        'Correr 5 km y mejorar movilidad',
      );
      await page.goto(`/profesor/sesiones/${sessionId}`);
      await expect(page.locator('.session-comment')).toHaveText(
        'Me costó la segunda vuelta, sin dolor.',
      );
      // Renaming a shared group keeps references and immutable routine snapshots.
      expect(
        (
          await request.put(`/api/grupos-musculares/${group.id}`, {
            headers,
            data: { nombre: muscle + ' nuevo' },
          })
        ).status(),
      ).toBe(200);
      const old = await request.get(`/api/rutinas/${routine.id}`, { headers });
      expect((await old.json()).dias[0].ejercicios[0].grupoMuscular).toBe(muscle);
      expect(
        (await request.delete(`/api/grupos-musculares/${group.id}`, { headers })).status(),
      ).toBe(409);
      expect(errors).toEqual([]);
    } finally {
      await mobile.close();
    }
  });
});
