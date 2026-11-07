import { test, expect, Page, APIRequestContext } from '@playwright/test';
import { randomBytes } from 'node:crypto';
import { readFileSync } from 'node:fs';
const unique = () => randomBytes(5).toString('hex');
type Account = { email: string; password: string };
type Identity = { token: string; perfilId: number };
type Routine = {
  id: number;
  rutinaVersionId: number;
  numeroVersion: number;
  dias: { id: number; ejercicios: { id: number }[] }[];
};
test.describe('PROFESOR · backend real', () => {
  test.skip(
    !process.env['GYM_E2E_REAL_API'] || !process.env['GYM_E2E_CREDENTIALS_FILE'],
    'Requiere backend local aislado y credenciales privadas de prueba.',
  );
  let adminToken: string,
    teacher: Account,
    identity: Identity,
    student: Identity & { email: string },
    stranger: Identity,
    exercise: { id: number; nombre: string },
    foreign: Routine;
  const studentName = 'AlumnoDocente';
  async function newTeacher(request: APIRequestContext): Promise<Account> {
    const account = {
      email: `teacher-${unique()}@example.test`,
      password: randomBytes(20).toString('base64url'),
    };
    const response = await request.post('/api/admin/profesores', {
      headers: { Authorization: 'Bearer ' + adminToken },
      data: {
        ...account,
        perfil: { nombre: 'Docente', apellido: 'Prueba', especialidad: 'Fuerza' },
      },
    });
    expect(response.status()).toBe(201);
    return account;
  }
  async function register(request: APIRequestContext, name = studentName) {
    const account = {
      email: `student-teacher-${unique()}@example.test`,
      password: randomBytes(20).toString('base64url'),
    };
    const response = await request.post('/api/auth/register/alumno', {
      data: {
        ...account,
        nombre: name,
        apellido: 'Prueba',
        edad: 25,
        nivel: 'PRINCIPIANTE',
        objetivo: 'MEJORAR_RESISTENCIA',
      },
    });
    expect(response.status()).toBe(201);
    return { ...((await response.json()) as Identity), email: account.email };
  }
  async function apiLogin(request: APIRequestContext, account: Account) {
    const r = await request.post('/api/auth/login', { data: account });
    expect(r.status()).toBe(200);
    return (await r.json()) as Identity;
  }
  async function login(page: Page, account = teacher) {
    await page.goto('/login');
    await page.getByLabel('Email', { exact: true }).fill(account.email);
    await page.getByLabel('Contraseña', { exact: true }).fill(account.password);
    await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
    await expect(page).toHaveURL(/\/profesor$/);
    await expect(page.getByRole('heading', { name: 'Acompañá cada avance.' })).toBeVisible();
  }
  async function confirm(page: Page) {
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.getByRole('button', { name: 'Confirmar cambio', exact: true }).click();
  }
  test.beforeAll(async ({ request }) => {
    const accounts = JSON.parse(readFileSync(process.env['GYM_E2E_CREDENTIALS_FILE']!, 'utf8')) as {
      ADMIN: Account;
      PROFESOR: Account;
    };
    adminToken = (await apiLogin(request, accounts.ADMIN)).token;
    teacher = await newTeacher(request);
    identity = await apiLogin(request, teacher);
    student = await register(request);
    stranger = await register(request, 'SinVinculo');
    const linked = await request.put(
      `/api/admin/alumnos/${student.perfilId}/profesores/${identity.perfilId}`,
      {
        headers: { Authorization: 'Bearer ' + adminToken },
        data: { motivo: 'Preparación de prueba docente' },
      },
    );
    expect(linked.status()).toBe(200);
    const createdExercise = await request.post('/api/ejercicios', {
      headers: { Authorization: 'Bearer ' + identity.token },
      data: {
        nombre: 'Press docente ' + unique(),
        descripcion: 'Ejercicio de prueba real',
        grupoMuscular: 'PECHO',
      },
    });
    expect(createdExercise.status()).toBe(201);
    exercise = (await createdExercise.json()) as typeof exercise;
    const body = {
      nombre: 'Historia docente ' + unique(),
      descripcion: 'Plan de validación histórica',
      dias: [
        {
          nombre: 'Día 1',
          numeroOrden: 1,
          ejercicios: [
            { ejercicioId: exercise.id, seriesSugeridas: 2, repsSugeridas: 8, pesoSugerido: 20 },
          ],
        },
      ],
    };
    const r = await request.post('/api/rutinas', {
      headers: { Authorization: 'Bearer ' + identity.token },
      data: body,
    });
    expect(r.status()).toBe(201);
    const own = (await r.json()) as Routine;
    expect(
      (
        await request.post(`/api/rutinas/asignar/alumno/${student.perfilId}/rutina/${own.id}`, {
          headers: { Authorization: 'Bearer ' + identity.token },
        })
      ).status(),
    ).toBe(200);
    const session = await request.post('/api/sesiones', {
      headers: { Authorization: 'Bearer ' + student.token },
      data: { diaRutinaId: own.dias[0]!.id, fecha: new Date().toISOString().slice(0, 10) },
    });
    expect(session.status()).toBe(201);
    const sessionId = ((await session.json()) as { id: number }).id;
    expect(
      (
        await request.post(`/api/sesiones/${sessionId}/series`, {
          headers: { Authorization: 'Bearer ' + student.token },
          data: {
            diaEjercicioId: own.dias[0]!.ejercicios[0]!.id,
            numeroSerie: 1,
            repsHechas: 8,
            pesoUsado: 20,
          },
        })
      ).status(),
    ).toBe(201);
    expect(
      (
        await request.put(`/api/sesiones/${sessionId}/completar`, {
          headers: { Authorization: 'Bearer ' + student.token },
        })
      ).status(),
    ).toBe(200);
    const other = await apiLogin(request, accounts.PROFESOR);
    expect(
      (
        await request.put(`/api/admin/alumnos/${student.perfilId}/profesores/${other.perfilId}`, {
          headers: { Authorization: 'Bearer ' + adminToken },
          data: { motivo: 'Historia de otro autor' },
        })
      ).status(),
    ).toBe(200);
    const f = await request.post('/api/rutinas', {
      headers: { Authorization: 'Bearer ' + other.token },
      data: { ...body, nombre: 'Otro autor ' + unique() },
    });
    expect(f.status()).toBe(201);
    foreign = (await f.json()) as Routine;
    expect(
      (
        await request.post(`/api/rutinas/asignar/alumno/${student.perfilId}/rutina/${foreign.id}`, {
          headers: { Authorization: 'Bearer ' + other.token },
        })
      ).status(),
    ).toBe(200);
  });
  test('dashboard and real progress graphs preserve exact recorded values', async ({ page }) => {
    await login(page);
    await expect(page.getByRole('link', { name: /Alumnos vinculados/ })).toBeVisible();
    await page
      .getByRole('button', { name: `${exercise.nombre} · #${exercise.id}`, exact: true })
      .click();
    await expect(
      page.getByRole('heading', { name: 'Peso máximo registrado por sesión' }),
    ).toBeVisible();
    await expect(page.getByRole('img', { name: /Actividad por fecha efectiva/ })).toBeVisible();
    await page
      .getByText('Ver valores exactos de peso máximo registrado por sesión', { exact: true })
      .click();
    await expect(page.getByRole('cell', { name: /^20(?:\.0+)?$/ })).toBeVisible();
    await page.getByLabel('Desde', { exact: true }).fill('2025-01-01');
    await page.getByLabel('Hasta', { exact: true }).fill('2025-01-02');
    await page.getByRole('button', { name: 'Actualizar dashboard', exact: true }).click();
    await expect(
      page.getByText('No hay ejercicios con resultados válidos en este período.', { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole('link', { name: /Entrenamientos con datos válidos/ }).locator('strong'),
    ).toHaveText('0');
  });
  test('linked student search, follow-up, full histories and read-only training', async ({
    page,
  }) => {
    await login(page);
    await page.getByRole('link', { name: 'Mis alumnos', exact: true }).click();
    await page.getByRole('searchbox', { name: 'Buscar alumno' }).fill(student.email);
    await page.getByRole('link', { name: `Ver alumno #${student.perfilId}`, exact: true }).click();
    await expect(page.getByRole('heading', { name: `${studentName} Prueba` })).toBeVisible();
    await page.getByRole('combobox', { name: 'Nivel', exact: true }).selectOption('AVANZADO');
    await page
      .getByRole('combobox', { name: 'Objetivo', exact: true })
      .selectOption('GANAR_MASA_MUSCULAR');
    await page.getByRole('button', { name: 'Actualizar seguimiento', exact: true }).click();
    await confirm(page);
    await expect(page.getByText('Seguimiento actualizado correctamente.')).toBeVisible();
    await page.getByRole('button', { name: 'Rutinas e historial', exact: true }).click();
    await expect(
      page.getByRole('link', { name: 'Consultar versión asignada', exact: true }),
    ).toBeVisible();
    await expect(page.getByRole('link', { name: /Ver versión histórica/ })).toHaveCount(2);
    await page.getByRole('button', { name: 'Entrenamientos', exact: true }).click();
    await page.getByRole('link', { name: /Ver entrenamiento #/ }).click();
    await expect(page.getByRole('heading', { name: 'Resultados registrados' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Auditoría del entrenamiento' })).toBeVisible();
    await expect(page.getByRole('button', { name: /Registrar serie/ })).toHaveCount(0);
  });
  test('create, version, assign and archive a multi-day own routine', async ({ page }) => {
    test.setTimeout(90000);
    await login(page);
    await page.goto('/profesor/rutinas/nueva');
    const name = 'Plan UI ' + unique();
    await page.getByLabel('Nombre de rutina *', { exact: true }).fill(name);
    await page
      .getByLabel('Descripción de rutina *', { exact: true })
      .fill('Dos días con prescripción real');
    await page.getByLabel('Nombre del día 1 *', { exact: true }).fill('Pecho');
    await page
      .getByRole('combobox', { name: 'Ejercicio día 1, fila 1 *', exact: true })
      .selectOption({ label: `#${exercise.id} · ${exercise.nombre} · PECHO` });
    await page.getByLabel('Series día 1, fila 1 *', { exact: true }).fill('2');
    await page.getByLabel('Repeticiones día 1, fila 1 *', { exact: true }).fill('8');
    await page.getByLabel('Peso sugerido día 1, fila 1', { exact: true }).fill('0');
    await page.getByRole('button', { name: 'Agregar día', exact: true }).click();
    await page.getByLabel('Nombre del día 2 *', { exact: true }).fill('Técnica');
    await page
      .getByRole('combobox', { name: 'Ejercicio día 2, fila 1 *', exact: true })
      .selectOption({ label: `#${exercise.id} · ${exercise.nombre} · PECHO` });
    await page.getByRole('button', { name: 'Crear rutina', exact: true }).click();
    await confirm(page);
    await expect(page).toHaveURL(/\/profesor\/rutinas\/\d+\?guardada=1$/);
    const id = Number(new URL(page.url()).pathname.split('/').pop());
    await expect(page.getByText('Rutina guardada correctamente. Versión 1.')).toBeVisible();
    await page
      .getByRole('combobox', { name: 'Alumno vinculado', exact: true })
      .selectOption(String(student.perfilId));
    await page.getByRole('button', { name: 'Asignar última versión', exact: true }).click();
    await confirm(page);
    await expect(page.getByText(/Asignación #.*guardada: versión 1/)).toBeVisible();
    await page.getByRole('link', { name: 'Editar última versión', exact: true }).click();
    await page.getByLabel('Nombre de rutina *', { exact: true }).fill(name + ' v2');
    await page.getByRole('button', { name: 'Guardar nueva versión', exact: true }).click();
    await confirm(page);
    await expect(page).toHaveURL(/guardada=2$/);
    await page.getByRole('link', { name: /Versión 1 ·/ }).click();
    await expect(page.getByRole('heading', { name: name, exact: true })).toBeVisible();
    await page.goto(`/profesor/rutinas/${id}`);
    await page.getByRole('button', { name: 'Archivar rutina', exact: true }).click();
    await confirm(page);
    await expect(
      page.getByText('Rutina archivada. Las versiones y los entrenamientos se conservan.'),
    ).toBeVisible();
    await expect(
      page.getByRole('link', { name: 'Editar última versión', exact: true }),
    ).toHaveCount(0);
    await page.goto('/profesor/rutinas');
    await page.getByLabel('Consultar ID de rutina', { exact: true }).fill(String(id));
    await page.getByRole('button', { name: 'Consultar por ID', exact: true }).click();
    await expect(page.getByText(/Archivada\s*\. ID de versión exacta/)).toBeVisible();
  });
  test('shared exercises CRUD and protected deletion of referenced exercises', async ({ page }) => {
    await login(page);
    await page.goto('/profesor/ejercicios');
    const name = 'Catálogo UI ' + unique();
    await page.getByRole('button', { name: 'Crear ejercicio', exact: true }).click();
    await page.getByLabel('Nombre del catálogo *', { exact: true }).fill(name);
    await page.getByLabel('Descripción del catálogo *', { exact: true }).fill('Catálogo real');
    await page
      .getByRole('combobox', { name: 'Grupo muscular del catálogo *', exact: true })
      .selectOption('ESPALDA');
    await page.getByRole('button', { name: 'Guardar ejercicio', exact: true }).click();
    await confirm(page);
    await expect(page.getByText('Ejercicio guardado correctamente.')).toBeVisible();
    await page.getByRole('searchbox', { name: 'Nombre del ejercicio', exact: true }).fill(name);
    await page.getByRole('button', { name: 'Buscar ejercicios', exact: true }).click();
    await page
      .getByRole('row')
      .filter({ has: page.getByRole('cell', { name: new RegExp(name) }) })
      .getByRole('button', { name: /Editar ejercicio #/ })
      .click();
    await page
      .getByLabel('Descripción del catálogo *', { exact: true })
      .fill('Descripción actualizada');
    await page.getByRole('button', { name: 'Guardar ejercicio', exact: true }).click();
    await confirm(page);
    await expect(
      page.getByRole('cell', { name: 'Descripción actualizada', exact: true }),
    ).toBeVisible();
    await page
      .getByRole('row')
      .filter({ has: page.getByRole('cell', { name: new RegExp(name) }) })
      .getByRole('button', { name: /Eliminar ejercicio #/ })
      .click();
    await confirm(page);
    await expect(page.getByText('Ejercicio eliminado del catálogo.')).toBeVisible();
    await page
      .getByRole('searchbox', { name: 'Nombre del ejercicio', exact: true })
      .fill(exercise.nombre);
    await page.getByRole('button', { name: 'Buscar ejercicios', exact: true }).click();
    await page
      .getByRole('button', { name: `Eliminar ejercicio #${exercise.id}`, exact: true })
      .click();
    await confirm(page);
    await expect(page.getByRole('alert')).toContainText('referenciado');
  });
  test('does not expose foreign editors or unlinked student profiles', async ({
    page,
    request,
  }) => {
    await login(page);
    await page.goto(`/profesor/rutinas/${foreign.id}/editar`);
    await expect(page.getByRole('alert')).toContainText('propias');
    await expect(page.getByLabel('Nombre de rutina *', { exact: true })).toHaveCount(0);
    expect(
      (
        await request.put(`/api/rutinas/${foreign.id}`, {
          headers: { Authorization: 'Bearer ' + identity.token },
          data: { nombre: 'No autorizado', descripcion: 'No autorizado', dias: [] },
        })
      ).status(),
    ).toBe(403);
    await page.goto(`/profesor/alumnos/${stranger.perfilId}`);
    await expect(page.getByRole('alert')).toContainText('acceso vigente');
    await expect(page.getByRole('heading', { name: 'SinVinculo Prueba' })).toHaveCount(0);
    await page.goto('/admin/usuarios');
    await expect(page).toHaveURL(/\/profesor$/);
  });
  test('revoked linkage clears already loaded profile and progress on the next request', async ({
    page,
    request,
  }) => {
    const linked = await register(request, 'VinculoRevocado');
    expect(
      (
        await request.put(`/api/admin/alumnos/${linked.perfilId}/profesores/${identity.perfilId}`, {
          headers: { Authorization: 'Bearer ' + adminToken },
          data: { motivo: 'Prueba de revocación docente' },
        })
      ).status(),
    ).toBe(200);
    await login(page);
    await page.goto(`/profesor/alumnos/${linked.perfilId}`);
    await expect(page.getByRole('heading', { name: 'VinculoRevocado Prueba' })).toBeVisible();
    await page.getByRole('button', { name: 'Progreso del alumno', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Progreso registrado' })).toBeVisible();
    expect(
      (
        await request.delete(
          `/api/admin/alumnos/${linked.perfilId}/profesores/${identity.perfilId}`,
          {
            headers: { Authorization: 'Bearer ' + adminToken },
            data: { motivo: 'Revocación para comprobar acceso' },
          },
        )
      ).status(),
    ).toBe(200);
    await page.getByRole('button', { name: 'Consultar progreso', exact: true }).click();
    await expect(page.getByRole('alert')).toContainText('Ya no tenés');
    await expect(page.getByRole('heading', { name: 'VinculoRevocado Prueba' })).toHaveCount(0);
  });
  test('personal profile and mobile navigation preserve ADMIN separation', async ({ page }) => {
    await login(page);
    await page.goto('/profesor/perfil');
    await page
      .getByLabel('Especialidad del profesor *', { exact: true })
      .fill('Movilidad y fuerza');
    await page
      .getByLabel('Email de contacto (opcional)', { exact: true })
      .fill('contacto@example.test');
    await page.getByRole('button', { name: 'Guardar perfil', exact: true }).click();
    await confirm(page);
    await expect(page.getByText('Perfil actualizado correctamente.')).toBeVisible();
    await page.reload();
    await expect(page.getByLabel('Especialidad del profesor *', { exact: true })).toHaveValue(
      'Movilidad y fuerza',
    );
    await page.setViewportSize({ width: 390, height: 844 });
    await page.getByRole('button', { name: 'Abrir menú' }).click();
    await page.getByRole('link', { name: 'Mis alumnos', exact: true }).click();
    await expect(page).toHaveURL(/\/profesor\/alumnos$/);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
  });
  test('credential changes replace JWT, revoke the old token and allow login with new credentials', async ({
    page,
    request,
  }) => {
    const account = await newTeacher(request);
    await login(page, account);
    await page.goto('/profesor/perfil');
    await page.getByRole('button', { name: 'Email de acceso', exact: true }).click();
    const email = 'changed-teacher-' + unique() + '@example.test';
    const old = await page.evaluate(
      () => (JSON.parse(localStorage.getItem('gym.session.v1')!) as { token: string }).token,
    );
    await page.getByLabel('Nuevo email de acceso *', { exact: true }).fill(email);
    await page.getByLabel('Contraseña actual *', { exact: true }).fill(account.password);
    await page.getByRole('button', { name: 'Actualizar email', exact: true }).click();
    await confirm(page);
    await expect(
      page.getByText('Credenciales actualizadas. La sesión nueva reemplazó a la anterior.'),
    ).toBeVisible();
    const replaced = await page.evaluate(
      () => (JSON.parse(localStorage.getItem('gym.session.v1')!) as { token: string }).token,
    );
    expect(old === replaced).toBe(false);
    expect(
      (
        await request.get('/api/profesores/me', { headers: { Authorization: 'Bearer ' + old } })
      ).status(),
    ).toBe(401);
    await page.getByRole('button', { name: 'Contraseña', exact: true }).click();
    await expect(
      page.getByRole('heading', { name: 'Cambiar contraseña', exact: true }),
    ).toBeVisible();
    const password = randomBytes(20).toString('base64url');
    await page.getByLabel('Contraseña actual *', { exact: true }).fill(account.password);
    await page.getByLabel('Nueva contraseña *', { exact: true }).fill(password);
    await page.getByLabel('Confirmar nueva contraseña *', { exact: true }).fill(password);
    await page.getByRole('button', { name: 'Actualizar contraseña', exact: true }).click();
    await confirm(page);
    await expect(
      page.getByText('Credenciales actualizadas. La sesión nueva reemplazó a la anterior.'),
    ).toBeVisible();
    expect(
      (
        await request.get('/api/profesores/me', {
          headers: { Authorization: 'Bearer ' + replaced },
        })
      ).status(),
    ).toBe(401);
    await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Cerrar sesión', exact: true })
      .click();
    await login(page, { email, password });
  });

  test('own unlink uses confirmation and terminates access without deleting the student account', async ({
    page,
    request,
  }) => {
    const linked = await register(request, 'DesvinculoPropio');
    expect(
      (
        await request.put(`/api/admin/alumnos/${linked.perfilId}/profesores/${identity.perfilId}`, {
          headers: { Authorization: 'Bearer ' + adminToken },
          data: { motivo: 'Comprobar desvínculo propio' },
        })
      ).status(),
    ).toBe(200);
    await login(page);
    await page.goto(`/profesor/alumnos/${linked.perfilId}`);
    await page.getByRole('button', { name: 'Desvincular alumno', exact: true }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Cancelar', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'DesvinculoPropio Prueba' })).toBeVisible();
    await page.getByRole('button', { name: 'Desvincular alumno', exact: true }).click();
    await confirm(page);
    await expect(page).toHaveURL(/\/profesor\/alumnos$/);
    expect(
      (
        await request.get(`/api/alumnos/${linked.perfilId}`, {
          headers: { Authorization: 'Bearer ' + identity.token },
        })
      ).status(),
    ).toBe(403);
    expect(
      (
        await request.get('/api/alumnos/me', {
          headers: { Authorization: 'Bearer ' + linked.token },
        })
      ).status(),
    ).toBe(200);
  });
});
