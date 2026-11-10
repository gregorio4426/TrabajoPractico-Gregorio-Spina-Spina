import { test, expect, APIRequestContext, Page } from '@playwright/test';
import { randomBytes } from 'node:crypto';
import { readFileSync } from 'node:fs';
type Identity = { token: string; email: string; rol: string; perfilId: number | null };
type Metrics = {
  sesionesRegistradas: number;
  sesionesAbiertas: number;
  entrenamientosConDatosValidos: number;
  seriesExcluidas: number;
  seriesSinPeso: number;
  volumenRegistrado: string | null;
  volumenCompleto: boolean;
  pesoMaximo: string | null;
  repeticionesTotales: number;
};
type Point = { sesionId: number; fechaOriginal: string; fechaEfectiva: string; datos: Metrics };
const uid = () => randomBytes(6).toString('hex');
const today = new Date().toISOString().slice(0, 10);
const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10);
test.describe('ESTADÍSTICAS · coherencia real entre roles', () => {
  test.use({
    viewport: { width: 390, height: 844 },
    locale: 'es-AR',
    timezoneId: 'America/Buenos_Aires',
  });
  test.skip(
    !process.env['GYM_E2E_REAL_API'] || !process.env['GYM_E2E_CREDENTIALS_FILE'],
    'Requiere backend y cuentas privadas de prueba.',
  );
  let admin: Identity,
    teacher: Identity,
    student: Identity,
    exercise: { id: number; nombre: string };
  const headers = (who: Identity) => ({ Authorization: 'Bearer ' + who.token });
  async function json(
    request: APIRequestContext,
    method: 'post' | 'put' | 'patch' | 'delete',
    path: string,
    who: Identity | null,
    data?: unknown,
    status = 200,
  ) {
    const response = await request[method](path, {
      headers: who ? headers(who) : {},
      ...(data === undefined ? {} : { data }),
    });
    expect(response.status(), `${method} ${path}`).toBe(status);
    return response.status() === 204 ? null : response.json();
  }
  test.beforeAll(async ({ request }) => {
    const accounts = JSON.parse(readFileSync(process.env['GYM_E2E_CREDENTIALS_FILE']!, 'utf8'));
    admin = await json(request, 'post', '/api/auth/login', null, accounts.ADMIN);
    const teacherAccount = {
      email: 'stats-prof-' + uid() + '@example.test',
      password: randomBytes(20).toString('base64url'),
    };
    await json(
      request,
      'post',
      '/api/admin/profesores',
      admin,
      {
        ...teacherAccount,
        perfil: { nombre: 'Estadisticas', apellido: 'Prueba', especialidad: 'Fuerza' },
      },
      201,
    );
    teacher = await json(request, 'post', '/api/auth/login', null, teacherAccount);
    student = await json(
      request,
      'post',
      '/api/auth/register/alumno',
      null,
      {
        email: 'stats-alumno-' + uid() + '@example.test',
        password: randomBytes(20).toString('base64url'),
        nombre: 'Estadisticas',
        apellido: 'Alumno',
        edad: 25,
        nivel: 'PRINCIPIANTE',
        objetivo: 'MEJORAR_RESISTENCIA',
      },
      201,
    );
    await json(
      request,
      'put',
      `/api/admin/alumnos/${student.perfilId}/profesores/${teacher.perfilId}`,
      admin,
      { motivo: 'Prueba aislada de estadísticas' },
    );
    exercise = await json(
      request,
      'post',
      '/api/ejercicios',
      teacher,
      {
        nombre: 'Carga estadística ' + uid(),
        descripcion: 'Resultados reales de prueba',
        grupoMuscular: 'PECHO',
      },
      201,
    );
    const other = await json(
      request,
      'post',
      '/api/ejercicios',
      teacher,
      {
        nombre: 'Peso cero ' + uid(),
        descripcion: 'Resultados reales de prueba',
        grupoMuscular: 'PECHO',
      },
      201,
    );
    const routine = await json(
      request,
      'post',
      '/api/rutinas',
      teacher,
      {
        nombre: 'Plan estadístico ' + uid(),
        descripcion: 'Ventana de resultados reales de prueba',
        dias: [
          {
            nombre: 'Día estadístico',
            numeroOrden: 1,
            ejercicios: [
              { ejercicioId: exercise.id, seriesSugeridas: 4, repsSugeridas: 10, pesoSugerido: 50 },
              { ejercicioId: other.id, seriesSugeridas: 1, repsSugeridas: 10, pesoSugerido: 25 },
            ],
          },
        ],
      },
      201,
    );
    await json(
      request,
      'post',
      `/api/rutinas/asignar/alumno/${student.perfilId}/rutina/${routine.id}`,
      teacher,
      undefined,
      200,
    );
    const start = async (date: string) =>
      json(
        request,
        'post',
        '/api/sesiones',
        student,
        { diaRutinaId: routine.dias[0].id, fecha: date },
        201,
      );
    const add = async (
      s: number,
      n: number,
      reps: number,
      weight: number | null,
      block = routine.dias[0].ejercicios[0].id,
    ) =>
      json(
        request,
        'post',
        `/api/sesiones/${s}/series`,
        student,
        { diaEjercicioId: block, numeroSerie: n, repsHechas: reps, pesoUsado: weight },
        201,
      );
    const finish = async (id: number) =>
      json(request, 'put', `/api/sesiones/${id}/completar`, student);
    const s0 = await start(yesterday);
    await add(s0.id, 1, 8, null);
    await add(s0.id, 1, 5, 0, routine.dias[0].ejercicios[1].id);
    await finish(s0.id);
    const s1 = await start(today);
    await add(s1.id, 1, 10, 0);
    await add(s1.id, 2, 7, null);
    const annul = await add(s1.id, 3, 4, 25);
    await finish(s1.id);
    await json(request, 'delete', `/api/sesiones/${s1.id}/series/${annul.id}`, student, {
      revision: annul.revision,
      motivo: 'Serie anulada excluida de métricas',
    });
    const s2 = await start(today);
    await add(s2.id, 1, 8, 12.5);
    await add(s2.id, 2, 6, 10);
    const closed = await finish(s2.id);
    await json(request, 'patch', `/api/sesiones/${s2.id}/correccion`, student, {
      revision: closed.revision,
      fecha: yesterday,
      motivo: 'Fecha efectiva para verificar filtros históricos',
    });
    const open = await start(today);
    await add(open.id, 1, 99, 100);
    const summaryResponse = await request.get(
      `/api/alumnos/me/estadisticas?desde=${yesterday}&hasta=${today}`,
      { headers: headers(student) },
    );
    expect(summaryResponse.status()).toBe(200);
    const summary = await summaryResponse.json();
    expect(summary.datos).toMatchObject({
      sesionesRegistradas: 4,
      sesionesAbiertas: 1,
      entrenamientosConDatosValidos: 3,
      seriesValidas: 6,
      seriesExcluidas: 1,
      seriesSinPeso: 2,
      repeticionesTotales: 44,
      volumenCompleto: false,
    });
    expect(Number(summary.datos.volumenRegistrado)).toBe(160);
    expect(Number(summary.datos.pesoMaximo)).toBe(12.5);
    expect(Number(summary.frecuencia.entrenamientosPorSemana)).toBe(10.5);
  });
  for (const role of ['ADMIN', 'PROFESOR', 'ALUMNO'] as const)
    test(`${role}: dashboard, resumen y período mantienen los mismos valores`, async ({
      page,
      request,
    }) => {
      const who = role === 'ADMIN' ? admin : role === 'PROFESOR' ? teacher : student;
      const root = '/' + role.toLowerCase(),
        api =
          role === 'ADMIN'
            ? '/api/admin'
            : role === 'PROFESOR'
              ? '/api/profesores/me'
              : '/api/alumnos/me';
      await page.goto('/login');
      await page.evaluate(
        (identity) => localStorage.setItem('gym.session.v1', JSON.stringify(identity)),
        who,
      );
      await page.goto(root);
      const form = page.locator('main form').first();
      await form.getByLabel('Desde', { exact: true }).fill(yesterday);
      await form.getByLabel('Hasta', { exact: true }).fill(today);
      await form.getByRole('button').click();
      const d = await (
        await request.get(`${api}/dashboard?desde=${yesterday}&hasta=${today}`, {
          headers: headers(who),
        })
      ).json();
      const summary = await (
        await request.get(`${api}/estadisticas?desde=${yesterday}&hasta=${today}`, {
          headers: headers(who),
        })
      ).json();
      expect(d.entrenamientos).toEqual(summary.datos);
      expect(d.frecuencia).toEqual(summary.frecuencia);
      expect(d.periodo).toEqual(summary.periodo);
      if (role === 'ADMIN') {
        await expect(
          page
            .locator('dl > div')
            .filter({
              has: page.locator('dt').filter({ hasText: /^Entrenamientos con datos válidos$/ }),
            })
            .locator('dd'),
        ).toHaveText(String(summary.datos.entrenamientosConDatosValidos));
        await page
          .getByRole('link', { name: 'Ver gráficos e historial del período', exact: true })
          .click();
        await expect(page).toHaveURL(
          (url) =>
            url.pathname === '/admin/estadisticas' &&
            url.searchParams.get('desde') === yesterday &&
            url.searchParams.get('hasta') === today,
        );
      } else {
        await expect(
          page
            .locator('.admin-metrics a')
            .filter({
              has: page.locator('span').filter({ hasText: /^Entrenamientos con datos válidos$/ }),
            })
            .locator('strong'),
        ).toHaveText(String(summary.datos.entrenamientosConDatosValidos));
        await expect(
          page
            .locator('app-teacher-progress')
            .getByText(`${yesterday} → ${today}`, { exact: false }),
        ).toBeVisible();
      }
    });
  const scopes = ['ADMIN', 'ADMIN alumno', 'PROFESOR', 'PROFESOR alumno', 'ALUMNO'] as const;
  for (const scope of scopes)
    test(`${scope}: filtros, valores exactos, cobertura, móvil y navegación`, async ({
      page,
      request,
    }) => {
      const who = scope.startsWith('ADMIN')
        ? admin
        : scope.startsWith('PROFESOR')
          ? teacher
          : student;
      const root = scope.startsWith('ADMIN')
        ? '/admin'
        : scope.startsWith('PROFESOR')
          ? '/profesor'
          : '/alumno';
      const stats =
        scope === 'ADMIN'
          ? '/api/admin/estadisticas'
          : scope === 'ADMIN alumno'
            ? `/api/admin/alumnos/${student.perfilId}/estadisticas`
            : scope === 'PROFESOR'
              ? '/api/profesores/me/estadisticas'
              : scope === 'PROFESOR alumno'
                ? `/api/profesores/me/alumnos/${student.perfilId}/estadisticas`
                : '/api/alumnos/me/estadisticas';
      await page.goto('/login');
      await page.evaluate(
        (identity) => localStorage.setItem('gym.session.v1', JSON.stringify(identity)),
        who,
      );
      const url =
        scope === 'ADMIN alumno'
          ? `/admin/alumnos/${student.perfilId}/estadisticas`
          : scope === 'ADMIN'
            ? '/admin/estadisticas'
            : scope === 'PROFESOR alumno'
              ? `/profesor/alumnos/${student.perfilId}`
              : root + '/progreso';
      await page.goto(url);
      if (scope === 'PROFESOR alumno')
        await page.getByRole('button', { name: 'Progreso del alumno', exact: true }).click();
      const progress = page.locator('app-teacher-progress');
      await progress.getByLabel('Desde', { exact: true }).fill(yesterday);
      await progress.getByLabel('Hasta', { exact: true }).fill(today);
      await progress.getByRole('button', { name: 'Consultar progreso', exact: true }).click();
      await expect(progress.getByRole('status')).toHaveCount(0);
      const summary = await (
        await request.get(`${stats}?desde=${yesterday}&hasta=${today}`, { headers: headers(who) })
      ).json();
      for (const [label, value] of [
        ['Entrenamientos con datos válidos', summary.datos.entrenamientosConDatosValidos],
        ['Series excluidas', summary.datos.seriesExcluidas],
        ['Series sin peso registrado', summary.datos.seriesSinPeso],
        ['Frecuencia observada semanal', summary.frecuencia.entrenamientosPorSemana],
      ]) {
        await expect(
          progress
            .locator('dl > div')
            .filter({ has: page.locator('dt').filter({ hasText: new RegExp('^' + label + '$') }) })
            .locator('dd'),
        ).toHaveText(String(value));
      }
      // New exercise is last in the stable ID catalogue; wait for each rendered page.
      const choose = async () => {
        const pick = progress.getByRole('button', {
          name: exercise.nombre + ' · #' + exercise.id,
          exact: true,
        });
        while (!(await pick.count())) {
          const next = progress.getByRole('button', { name: 'Ejercicios siguientes', exact: true });
          await expect(next).toBeEnabled();
          const pagination = progress
            .getByRole('navigation', { name: 'Páginas de ejercicios estadísticos' })
            .locator('span');
          const previousPage = await pagination.innerText();
          await next.click();
          await expect(pagination).not.toHaveText(previousPage);
          await expect(progress.getByRole('status')).toHaveCount(0);
        }
        await pick.click();
      };
      await choose();
      const response = await request.get(
        `${stats}/ejercicios/${exercise.id}/evolucion?desde=${yesterday}&hasta=${today}&pagina=0&tamanio=30`,
        { headers: headers(who) },
      );
      expect(response.status()).toBe(200);
      const evolution = (await response.json()).contenido as Point[];
      expect(evolution).toHaveLength(3);
      for (const [title, metric] of [
        ['Peso máximo registrado por sesión', 'pesoMaximo'],
        ['Repeticiones realizadas por sesión', 'repeticionesTotales'],
        ['Volumen registrado por sesión', 'volumenRegistrado'],
      ] as const) {
        const chart = progress
          .locator('.teacher-chart')
          .filter({ has: page.getByRole('heading', { name: title, exact: true }) });
        await chart.locator('summary').click();
        const rows = chart.locator('tbody tr');
        await expect(rows).toHaveCount(3);
        for (let i = 0; i < evolution.length; i++) {
          await expect(rows.nth(i).locator('td').nth(1)).toHaveText(
            evolution[i]!.datos[metric] === null ? 'Sin dato' : String(evolution[i]!.datos[metric]),
          );
          await expect(rows.nth(i).getByRole('link')).toHaveAttribute(
            'href',
            `${root}/sesiones/${evolution[i]!.sesionId}`,
          );
        }
        if (metric === 'volumenRegistrado') {
          await expect(chart).toContainText('parcial · 1 series sin peso');
          await expect(chart).toContainText('Fecha original: ' + today);
        }
        const scroll = chart.locator('.teacher-chart-scroll');
        expect(await scroll.evaluate((el) => el.scrollWidth > el.clientWidth)).toBe(true);
      }
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
        true,
      );
      if (scope === 'ALUMNO')
        await page.screenshot({ path: '/tmp/gym-stage5-alumno-mobile.png', fullPage: true });
      const chart = progress.locator('.teacher-chart').filter({
        has: page.getByRole('heading', { name: 'Volumen registrado por sesión', exact: true }),
      });
      await chart.getByRole('link').first().click();
      await expect(page).toHaveURL(new RegExp(`${root}/sesiones/${evolution[0]!.sesionId}$`));
      await expect(page.getByRole('alert')).toHaveCount(0);
      await page.goto(url);
      if (scope === 'PROFESOR alumno')
        await page.getByRole('button', { name: 'Progreso del alumno', exact: true }).click();
      await progress.getByLabel('Desde', { exact: true }).fill(today);
      await progress.getByLabel('Hasta', { exact: true }).fill(today);
      await progress.getByRole('button', { name: 'Consultar progreso', exact: true }).click();
      await expect(progress.getByText(`${today} → ${today}`, { exact: false })).toBeVisible();
      await choose();
      const singleChart = progress
        .locator('.teacher-chart')
        .filter({
          has: page.getByRole('heading', { name: 'Volumen registrado por sesión', exact: true }),
        });
      await singleChart.locator('summary').click();
      await expect(singleChart.locator('tbody tr')).toHaveCount(1);
      await expect(singleChart.locator('tbody td').nth(1)).toHaveText('0.0');
      await expect(singleChart).not.toContainText('Fecha original:');
      await progress.getByLabel('Desde', { exact: true }).fill('2000-01-01');
      await progress.getByLabel('Hasta', { exact: true }).fill('2000-01-02');
      await progress.getByRole('button', { name: 'Consultar progreso', exact: true }).click();
      await expect(
        progress.getByText('No hay ejercicios con resultados válidos en este período.'),
      ).toBeVisible();
      await expect(progress.getByRole('link', { name: /Ver entrenamiento:/ })).toHaveCount(0);
      await progress.getByLabel('Hasta', { exact: true }).fill('2002-01-01');
      await progress.getByRole('button', { name: 'Consultar progreso', exact: true }).click();
      await expect(progress.getByRole('alert')).toContainText('366');
    });
});
