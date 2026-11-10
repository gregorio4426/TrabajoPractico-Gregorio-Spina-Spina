import { test, expect, APIRequestContext, Page } from '@playwright/test';
import { randomBytes } from 'node:crypto';
import { readFileSync } from 'node:fs';
type Identity = { token: string; email: string; rol: string; perfilId: number | null };
type Account = { email: string; password: string };
const unique = () => randomBytes(5).toString('hex');
test.describe('FINAL · integración, responsive y accesibilidad básica reales', () => {
  test.skip(
    !process.env['GYM_E2E_REAL_API'] || !process.env['GYM_E2E_CREDENTIALS_FILE'],
    'Requiere backend aislado y cuentas privadas.',
  );
  let admin: Identity,
    teacher: Identity,
    student: Identity,
    studentAccount: Account,
    teacherAccount: Account,
    studentUser: number,
    teacherUser: number,
    routine: any,
    assignment: any,
    closed: any,
    open: any,
    exercise: any;
  const headers = (who: Identity) => ({ Authorization: 'Bearer ' + who.token });
  async function api(
    request: APIRequestContext,
    method: 'get' | 'post' | 'put' | 'patch',
    path: string,
    who: Identity | null,
    data?: unknown,
    status = 200,
  ) {
    const r = await request[method](path, {
      headers: who ? headers(who) : {},
      ...(data === undefined ? {} : { data }),
    });
    expect(r.status(), method + ' ' + path).toBe(status);
    return r.json();
  }
  async function enter(page: Page, who: Identity, path: string) {
    await page.goto('/login');
    await page.evaluate((x) => localStorage.setItem('gym.session.v1', JSON.stringify(x)), who);
    await page.goto(path);
  }
  test.beforeAll(async ({ request }) => {
    const a = JSON.parse(readFileSync(process.env['GYM_E2E_CREDENTIALS_FILE']!, 'utf8'));
    admin = await api(request, 'post', '/api/auth/login', null, a.ADMIN);
    teacherAccount = {
      email: 'final-prof-' + unique() + '@example.test',
      password: randomBytes(20).toString('base64url'),
    };
    studentAccount = {
      email: 'final-alumno-' + unique() + '@example.test',
      password: randomBytes(20).toString('base64url'),
    };
    const t = await api(
      request,
      'post',
      '/api/admin/profesores',
      admin,
      {
        ...teacherAccount,
        perfil: { nombre: 'FinalDocente', apellido: 'Integracion', especialidad: 'Fuerza' },
      },
      201,
    );
    teacherUser = t.cuenta.id;
    teacher = await api(request, 'post', '/api/auth/login', null, teacherAccount);
    const s = await api(
      request,
      'post',
      '/api/admin/alumnos',
      admin,
      {
        ...studentAccount,
        perfil: {
          nombre: 'FinalAlumno',
          apellido: 'Integracion',
          edad: 26,
          fechaNacimiento: '2000-01-01',
          nivel: 'PRINCIPIANTE',
          objetivo: 'MEJORAR_RESISTENCIA',
        },
      },
      201,
    );
    studentUser = s.cuenta.id;
    student = await api(request, 'post', '/api/auth/login', null, studentAccount);
    await api(
      request,
      'put',
      `/api/admin/alumnos/${student.perfilId}/profesores/${teacher.perfilId}`,
      admin,
      { motivo: 'Vinculación de integración final' },
    );
    exercise = await api(
      request,
      'post',
      '/api/ejercicios',
      teacher,
      {
        nombre: 'Prensa final ' + unique(),
        descripcion: 'Ejercicio de prueba integral',
        grupoMuscular: 'PIERNAS',
      },
      201,
    );
    routine = await api(
      request,
      'post',
      '/api/rutinas',
      teacher,
      {
        nombre: 'Plan integral ' + unique(),
        descripcion: 'Rutina existente para revisión responsive y de integración',
        dias: [
          {
            nombre: 'Día de fuerza',
            numeroOrden: 1,
            ejercicios: [
              { ejercicioId: exercise.id, seriesSugeridas: 2, repsSugeridas: 10, pesoSugerido: 25 },
            ],
          },
        ],
      },
      201,
    );
    assignment = await api(
      request,
      'post',
      `/api/rutinas/asignar/alumno/${student.perfilId}/rutina/${routine.id}`,
      teacher,
    );
    const date = new Date().toISOString().slice(0, 10);
    closed = await api(
      request,
      'post',
      '/api/sesiones',
      student,
      { diaRutinaId: routine.dias[0].id, fecha: date },
      201,
    );
    await api(
      request,
      'post',
      `/api/sesiones/${closed.id}/series`,
      student,
      {
        diaEjercicioId: routine.dias[0].ejercicios[0].id,
        numeroSerie: 1,
        repsHechas: 8,
        pesoUsado: 0,
      },
      201,
    );
    closed = await api(request, 'put', `/api/sesiones/${closed.id}/completar`, student);
    open = await api(
      request,
      'post',
      '/api/sesiones',
      student,
      { diaRutinaId: routine.dias[0].id, fecha: date },
      201,
    );
  });
  function watch(page: Page) {
    const problems: string[] = [];
    page.on('pageerror', (e) => problems.push('JavaScript: ' + e.message));
    page.on('console', (m) => {
      if (m.type() === 'error') problems.push('Consola: ' + m.text().slice(0, 200));
    });
    page.on('response', (r) => {
      if (new URL(r.url()).pathname.startsWith('/api/') && r.status() >= 400)
        problems.push(`${r.status()} ${new URL(r.url()).pathname}`);
    });
    page.on('requestfailed', (r) => {
      if (!r.failure()?.errorText.includes('ERR_ABORTED'))
        problems.push(`Red: ${new URL(r.url()).pathname}`);
    });
    return problems;
  }
  for (const role of ['ADMIN', 'PROFESOR', 'ALUMNO'] as const)
    test(`${role}: pantallas en 1440, 820 y 360 px sin errores inesperados`, async ({ page }) => {
      test.setTimeout(180000);
      const problems = watch(page);
      const who = role === 'ADMIN' ? admin : role === 'PROFESOR' ? teacher : student;
      const paths =
        role === 'ADMIN'
          ? [
              '/admin',
              '/admin/estadisticas',
              '/admin/usuarios',
              '/admin/usuarios/nuevo',
              `/admin/usuarios/${studentUser}`,
              '/admin/alumnos',
              '/admin/alumnos/nuevo',
              `/admin/alumnos/${student.perfilId}`,
              `/admin/alumnos/${student.perfilId}/historial`,
              `/admin/alumnos/${student.perfilId}/sesiones`,
              `/admin/alumnos/${student.perfilId}/estadisticas`,
              '/admin/profesores',
              `/admin/alumnos/${student.perfilId}/profesores`,
              `/admin/profesores/${teacher.perfilId}/alumnos`,
              '/admin/profesores/nuevo',
              `/admin/profesores/${teacher.perfilId}`,
              '/admin/rutinas',
              `/admin/rutinas/${routine.id}`,
              `/admin/rutinas/${routine.id}/versiones`,
              '/admin/asignaciones',
              `/admin/asignaciones/${assignment.id}`,
              '/admin/sesiones',
              `/admin/sesiones/${closed.id}`,
              '/admin/auditoria',
            ]
          : role === 'PROFESOR'
            ? [
                '/profesor',
                '/profesor/alumnos',
                `/profesor/alumnos/${student.perfilId}`,
                '/profesor/rutinas',
                '/profesor/rutinas/nueva',
                `/profesor/rutinas/${routine.id}`,
                `/profesor/rutinas/${routine.id}/editar`,
                `/profesor/rutinas/${routine.id}/versiones/${routine.numeroVersion}`,
                '/profesor/ejercicios',
                '/profesor/perfil',
                '/profesor/progreso',
                `/profesor/sesiones/${closed.id}`,
              ]
            : [
                '/alumno',
                '/alumno/rutina',
                '/alumno/rutinas',
                '/alumno/sesiones',
                `/alumno/sesiones/${open.id}`,
                `/alumno/sesiones/${closed.id}`,
                '/alumno/progreso',
                '/alumno/perfil',
                '/alumno/profesores',
              ];
      await enter(page, who, paths[0]!);
      for (const width of [1440, 820, 360]) {
        await page.setViewportSize({ width, height: 900 });
        for (const path of paths) {
          await page.goto(path);
          await expect(page.locator('main')).toBeVisible();
          await expect(
            page
              .locator('main .notice[role="status"]')
              .filter({ hasText: /Cargando|Consultando|Recuperando/ }),
          ).toHaveCount(0);
          await expect(page.locator('main [role="alert"]')).toHaveCount(0);
          expect(
            await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
            `${path} @${width}`,
          ).toBe(true);
          const unlabelled = await page
            .locator('main input, main select, main textarea')
            .evaluateAll((els) =>
              els
                .filter((el) => {
                  const f = el as HTMLInputElement;
                  if (f.type === 'hidden' || !f.getClientRects().length) return false;
                  return (
                    !f.labels?.length &&
                    !f.getAttribute('aria-label') &&
                    !f.getAttribute('aria-labelledby')
                  );
                })
                .map((el) => el.tagName + ':' + el.getAttribute('formControlName')),
            );
          expect(unlabelled, `${path}: etiquetas`).toEqual([]);
          if (path === `/alumno/sesiones/${open.id}`) {
            await page
              .getByRole('button', {
                name: new RegExp('bloque #' + routine.dias[0].ejercicios[0].id + '$'),
              })
              .click();
            await expect(
              page.getByLabel('Repeticiones realizadas *', { exact: true }),
            ).toBeFocused();
            expect(
              await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
              'Formulario móvil de series',
            ).toBe(true);
            await expect(page.getByLabel('Peso realizado (opcional)')).toBeEmpty();
            if (width === 360)
              await page
                .locator('.student-block-form')
                .screenshot({ path: '/tmp/gym-final-mobile-entry.png' });
          }
        }
      }
      expect(problems).toEqual([]);
    });
  test('PROFESOR asigna una nueva versión: ALUMNO y ADMIN ven el cambio sin alterar la sesión anterior', async ({
    browser,
    request,
  }) => {
    const tc = await browser.newContext(),
      sc = await browser.newContext(),
      ac = await browser.newContext();
    const tp = await tc.newPage(),
      sp = await sc.newPage(),
      ap = await ac.newPage();
    const issues = [watch(tp), watch(sp), watch(ap)];
    const name = routine.nombre + ' v2';
    try {
      await enter(tp, teacher, `/profesor/rutinas/${routine.id}/editar`);
      await tp.getByLabel('Nombre de rutina *', { exact: true }).fill(name);
      await tp.getByLabel('Repeticiones día 1, fila 1 *', { exact: true }).fill('12');
      await tp.getByRole('button', { name: 'Guardar nueva versión', exact: true }).click();
      await tp.getByRole('button', { name: 'Confirmar cambio', exact: true }).click();
      await expect(tp).toHaveURL(/guardada=2$/);
      await tp
        .getByRole('combobox', { name: 'Alumno vinculado', exact: true })
        .selectOption(String(student.perfilId));
      await tp.getByRole('button', { name: 'Asignar última versión', exact: true }).click();
      await tp.getByRole('button', { name: 'Confirmar cambio', exact: true }).click();
      await expect(tp.getByText(/Asignación #.*guardada: versión 2/)).toBeVisible();
      await enter(sp, student, '/alumno/rutina');
      await expect(sp.getByRole('heading', { name, exact: true })).toBeVisible();
      await expect(sp.locator('main')).toContainText('Versión 2');
      await sp.goto(`/alumno/sesiones/${open.id}`);
      await expect(sp.locator('.student-session-status')).toContainText('Versión 1');
      await expect(sp.locator('.student-prescription')).toContainText('10 repeticiones');
      await enter(ap, admin, `/admin/alumnos/${student.perfilId}/historial`);
      await expect(ap.locator('main')).toContainText(name);
      const active = await api(request, 'get', '/api/rutinas/me/activa', student);
      expect(active.numeroVersion).toBe(2);
      expect(active.nombre).toBe(name);
      const preserved = await api(request, 'get', `/api/sesiones/${open.id}`, student);
      expect(preserved.numeroVersion).toBe(1);
      expect(issues.flat()).toEqual([]);
    } finally {
      await Promise.all([tc.close(), sc.close(), ac.close()]);
    }
  });
  test('ADMIN → PROFESOR → ALUMNO: perfil, asignación y entrenamiento visibles entre interfaces', async ({
    browser,
    request,
  }) => {
    const contexts = await Promise.all([
      browser.newContext(),
      browser.newContext(),
      browser.newContext({ viewport: { width: 390, height: 844 } }),
    ]);
    const [ap, tp, sp] = await Promise.all(contexts.map((c) => c.newPage()));
    const problems = [...contexts.map(() => [] as string[])];
    [ap!, tp!, sp!].forEach((p, i) => {
      problems[i] = watch(p);
    });
    try {
      await enter(ap!, admin, `/admin/alumnos/${student.perfilId}`);
      await ap!.getByRole('button', { name: 'Editar perfil', exact: true }).click();
      await ap!.getByLabel('Nombre *', { exact: true }).fill('AlumnoActualizado');
      // The existing ADMIN editor requires a reason and an explicit confirmation.
      const reason = ap!.getByLabel(/Motivo/);
      if (await reason.count()) await reason.fill('Actualización visible entre roles');
      await ap!.getByRole('button', { name: 'Revisar y guardar', exact: true }).click();
      await ap!.getByRole('button', { name: 'Confirmar cambio', exact: true }).click();
      await expect(ap!.getByText('Cambio guardado correctamente.')).toBeVisible();
      await enter(tp!, teacher, `/profesor/alumnos/${student.perfilId}`);
      await expect(
        tp!.getByRole('heading', { name: 'AlumnoActualizado Integracion', exact: true }),
      ).toBeVisible();
      await enter(sp!, student, '/alumno/perfil');
      await expect(sp!.getByLabel('Nombre del alumno *', { exact: true })).toHaveValue(
        'AlumnoActualizado',
      );
      await sp!.goto(`/alumno/sesiones/${open.id}`);
      await sp!
        .getByRole('button', {
          name: new RegExp('bloque #' + routine.dias[0].ejercicios[0].id + '$'),
        })
        .click();
      await sp!.getByLabel('Repeticiones realizadas *', { exact: true }).fill('10');
      await sp!.getByLabel('Peso realizado (opcional)').fill('12.5');
      await sp!.getByRole('button', { name: 'Guardar serie', exact: true }).click();
      await sp!.getByRole('button', { name: 'Confirmar cambio', exact: true }).click();
      await expect(sp!.getByText('Serie registrada correctamente.')).toBeVisible();
      await sp!.reload();
      await expect(sp!.locator('.student-series-card')).toHaveCount(1);
      await sp!.getByRole('button', { name: 'Finalizar entrenamiento', exact: true }).click();
      await sp!.getByRole('button', { name: 'Confirmar cambio', exact: true }).click();
      await expect(sp!.locator('.student-session-status')).toContainText('Finalizada');
      await tp!.goto(`/profesor/sesiones/${open.id}`);
      await expect(tp!.locator('main')).toContainText('12.5');
      await ap!.goto(`/admin/sesiones/${open.id}`);
      await expect(ap!.locator('main')).toContainText('12.5');
      const own = await api(request, 'get', '/api/alumnos/me/estadisticas', student);
      const authorized = await api(
        request,
        'get',
        `/api/admin/alumnos/${student.perfilId}/estadisticas`,
        admin,
      );
      const linked = await api(
        request,
        'get',
        `/api/profesores/me/alumnos/${student.perfilId}/estadisticas`,
        teacher,
      );
      expect(own.datos).toEqual(authorized.datos);
      expect(linked.datos).toEqual(own.datos);
      expect(problems.flat()).toEqual([]);
    } finally {
      await Promise.all(contexts.map((c) => c.close()));
    }
  });
  test('ALUMNO móvil: foco, teclado, diálogo y logout con sesión real', async ({ page }) => {
    await page.setViewportSize({ width: 360, height: 800 });
    await enter(page, student, '/alumno/perfil');
    await expect(page.locator('main')).toBeFocused();
    const name = page.getByLabel('Nombre del alumno *', { exact: true });
    await expect(name).toBeVisible();
    expect(
      await name.evaluate((el) => parseFloat(getComputedStyle(el).fontSize)),
    ).toBeGreaterThanOrEqual(16);
    expect(await name.evaluate((el) => el.getBoundingClientRect().height)).toBeGreaterThanOrEqual(
      44,
    );
    await page.getByRole('button', { name: 'Abrir menú', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Cerrar menú', exact: true })).toBeFocused();
    await page.keyboard.press('Shift+Tab');
    await page.keyboard.press('Shift+Tab');
    await expect(page.getByRole('link', { name: 'Mis profesores', exact: true })).toBeFocused();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('button', { name: 'Abrir menú', exact: true })).toBeFocused();
    await page.getByRole('button', { name: 'Abrir menú', exact: true }).click();
    await page.getByRole('link', { name: 'Mis sesiones', exact: true }).click();
    await expect(page.locator('main')).toBeFocused();
    await expect(page.locator('aside')).toHaveAttribute('inert', '');
    await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).not.toBeVisible();
    await expect(page.getByRole('button', { name: 'Cerrar sesión', exact: true })).toBeFocused();
    await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Cerrar sesión', exact: true })
      .click();
    await expect(page).toHaveURL(/\/login$/);
    expect(await page.evaluate(() => localStorage.getItem('gym.session.v1'))).toBeNull();
    await page.goto('/alumno/sesiones');
    await expect(page).toHaveURL(/\/login$/);
  });
  test('400 real: un filtro inválido muestra la validación y permite volver a consultar', async ({
    page,
  }) => {
    const failed = page.waitForResponse(
      (r) => new URL(r.url()).pathname === '/api/admin/sesiones' && r.status() === 400,
    );
    await enter(page, admin, '/admin/sesiones?orden=campo-inexistente');
    expect((await failed).status()).toBe(400);
    await expect(page.getByRole('alert')).toContainText('Orden no permitido');
    await page.getByRole('combobox', { name: 'Ordenar por', exact: true }).selectOption('id');
    await page.getByRole('button', { name: 'Aplicar filtros', exact: true }).click();
    await expect(page.getByRole('alert')).toHaveCount(0);
    await expect(page.locator('.admin-table')).toBeVisible();
    expect(await page.evaluate(() => localStorage.getItem('gym.session.v1') !== null)).toBe(true);
  });
  test('409 real: profesor inactivo se rechaza sin perder la sesión del alumno', async ({
    page,
    request,
  }) => {
    const account = {
      email: 'final-inactivo-' + unique() + '@example.test',
      password: randomBytes(20).toString('base64url'),
    };
    const created = await api(
      request,
      'post',
      '/api/admin/profesores',
      admin,
      {
        ...account,
        perfil: { nombre: 'TutorInactivo', apellido: 'Prueba', especialidad: 'Movilidad' },
      },
      201,
    );
    await api(request, 'put', `/api/admin/usuarios/${created.cuenta.id}/estado`, admin, {
      activo: false,
      revision: created.cuenta.revision,
      motivo: 'Prueba de validación de vínculos',
    });
    await enter(page, student, '/alumno/profesores');
    await page.getByLabel('Buscar profesor por nombre, email o especialidad').fill(account.email);
    await page
      .getByRole('button', { name: 'Vincular profesor #' + created.perfil.id, exact: true })
      .click();
    const [response] = await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().endsWith(`/api/alumnos/me/profesores/${created.perfil.id}`) &&
          r.request().method() === 'POST',
      ),
      page.getByRole('button', { name: 'Confirmar cambio', exact: true }).click(),
    ]);
    expect(response.status()).toBe(409);
    await expect(page.getByRole('alert')).toContainText('vínculo');
    await expect(page.getByRole('alert')).not.toContainText('número de serie');
    await expect(page).toHaveURL(/\/alumno\/profesores$/);
    expect(await page.evaluate(() => localStorage.getItem('gym.session.v1') !== null)).toBe(true);
  });
  test('404 y revocación 401 reales permiten recuperar navegación sin datos protegidos', async ({
    page,
    request,
  }) => {
    await enter(page, admin, '/admin/usuarios/900000000');
    await expect(page.getByRole('alert')).toContainText('no está disponible');
    await page.getByRole('link', { name: /Volver a usuarios/i }).click();
    await expect(page).toHaveURL(/\/admin\/usuarios$/);
    await enter(page, student, '/alumno/perfil');
    await expect(page.getByLabel('Nombre del alumno *', { exact: true })).toBeVisible();
    const current = await api(request, 'get', `/api/admin/usuarios/${studentUser}`, admin);
    await api(request, 'put', `/api/admin/usuarios/${studentUser}/estado`, admin, {
      activo: false,
      revision: current.revision,
      motivo: 'Revocación real de prueba final',
    });
    await page.goto('/alumno/sesiones');
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByText(/sesión venció o fue revocada/)).toBeVisible();
    expect(await page.evaluate(() => localStorage.getItem('gym.session.v1'))).toBeNull();
    const inactive = await api(request, 'get', `/api/admin/usuarios/${studentUser}`, admin);
    await api(request, 'put', `/api/admin/usuarios/${studentUser}/estado`, admin, {
      activo: true,
      revision: inactive.revision,
      motivo: 'Restauración de cuenta de prueba final',
    });
  });
});
