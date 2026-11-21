import { randomBytes } from 'node:crypto';
import { mkdir, readFile, writeFile, rename } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { createInterface, emitKeypressEvents } from 'node:readline';

export const EXERCISES = [
  ['Press de banca', 'PECHO', 'Empuje horizontal con barra.'],
  ['Press inclinado con mancuernas', 'PECHO', 'Empuje en banco inclinado.'],
  ['Aperturas con mancuernas', 'PECHO', 'Apertura de brazos en banco.'],
  ['Flexiones', 'PECHO', 'Empuje con el peso corporal.'],
  ['Remo con barra', 'ESPALDA', 'Tracción horizontal con barra.'],
  ['Remo con mancuerna', 'ESPALDA', 'Tracción horizontal unilateral.'],
  ['Jalón al pecho', 'ESPALDA', 'Tracción vertical en polea.'],
  ['Dominadas', 'ESPALDA', 'Tracción vertical con el peso corporal.'],
  ['Sentadilla', 'PIERNAS', 'Flexión y extensión de piernas.'],
  ['Prensa de piernas', 'PIERNAS', 'Empuje de piernas en máquina.'],
  ['Zancadas', 'PIERNAS', 'Trabajo alternado de piernas.'],
  ['Curl femoral', 'PIERNAS', 'Flexión de rodilla en máquina.'],
  ['Elevación de talones', 'PIERNAS', 'Trabajo de pantorrillas.'],
  ['Curl de bíceps', 'BRAZOS', 'Flexión de codo.'],
  ['Extensión de tríceps en polea', 'BRAZOS', 'Extensión de codo en polea.'],
  ['Press militar', 'HOMBROS', 'Empuje vertical.'],
  ['Elevaciones laterales', 'HOMBROS', 'Elevación lateral de brazos.'],
  ['Pájaros con mancuernas', 'HOMBROS', 'Trabajo posterior de hombros.'],
  ['Plancha abdominal', 'ABDOMINALES', 'Ejercicio isométrico de abdomen.'],
  ['Crunch abdominal', 'ABDOMINALES', 'Flexión de tronco.'],
];
export const normalizeName = (value) => value.normalize('NFD').replace(/\p{M}/gu, '').trim().replace(/\s+/g, ' ').toLowerCase();
class ApiError extends Error {
  constructor(status, method, path) { super(`La API respondió ${status} en ${method} ${path}.`); this.status = status; }
}
function apiClient(base, fetchImpl) {
  return async (method, path, token, body) => {
    let response;
    try {
      response = await fetchImpl(base + path, { method, redirect: 'error', signal: AbortSignal.timeout(30000),
        headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(body ? { 'Content-Type': 'application/json' } : {}) },
        ...(body ? { body: JSON.stringify(body) } : {}) });
    } catch { throw new Error('No se pudo conectar con la API. Comprobá que el backend esté funcionando.'); }
    if (!response.ok) throw new ApiError(response.status, method, path);
    return response.status === 204 ? null : response.json();
  };
}
async function persist(file, data, initial = false) {
  const content = JSON.stringify(data, null, 2) + '\n';
  if (initial) { await writeFile(file, content, { flag: 'wx', mode: 0o600 }); return; }
  const tmp = `${file}.${randomBytes(5).toString('hex')}.tmp`;
  await writeFile(tmp, content, { flag: 'wx', mode: 0o600 });
  await rename(tmp, file);
}
function newState(apiUrl) {
  const dataset = `demo-${randomBytes(5).toString('hex')}`;
  const account = (key, rol, perfil) => ({ key, rol, email: `${dataset}.${key}@example.test`, password: randomBytes(18).toString('base64url') + 'aA1!', perfil });
  return { schemaVersion: 1, apiUrl, dataset, accounts: [
    account('profesor', 'PROFESOR', { nombre: 'Profesor', apellido: 'Demo', especialidad: 'Entrenamiento' }),
    ...['Juan', 'Lucía', 'Pedro', 'Sofía', 'Mateo'].map((nombre, i) => account(`alumno${i + 1}`, 'ALUMNO', {
      nombre, apellido: 'Demo', edad: [25, 28, 32, 22, 35][i], nivel: ['PRINCIPIANTE', 'INTERMEDIO', 'PRINCIPIANTE', 'AVANZADO', 'INTERMEDIO'][i],
      objetivo: 'MEJORAR_RESISTENCIA', objetivoPersonalizado: 'Perfil ficticio para probar rutinas y entrenamientos.',
    })),
  ], exercises: [] };
}
function validateState(state, apiUrl) {
  if (state.schemaVersion !== 1 || state.apiUrl !== apiUrl || !Array.isArray(state.accounts) || state.accounts.length !== 6 ||
      state.accounts.some(a => !['PROFESOR', 'ALUMNO'].includes(a.rol) || !a.email || !a.password) ||
      state.accounts.filter(a => a.rol === 'PROFESOR').length !== 1 || new Set(state.accounts.map(a => a.email)).size !== 6) {
    throw new Error('El archivo de datos demo no corresponde a esta API o tiene formato inválido. No se modificó ni se reemplazó.');
  }
}
/** All writes use the real application endpoints and their existing role checks. */
export async function seedDemo({ apiUrl, stateFile, adminEmail, adminPassword, fetchImpl = fetch, log = console.log }) {
  const url = new URL(apiUrl);
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash) throw new Error('URL de API inválida.');
  const base = url.href.replace(/\/$/, ''), api = apiClient(base, fetchImpl);
  const admin = await api('POST', '/auth/login', null, { email: adminEmail.trim(), password: adminPassword });
  if (admin.rol !== 'ADMIN') throw new Error('Se necesita una cuenta ADMIN para cargar los datos de prueba.');
  const file = resolve(stateFile);
  await mkdir(dirname(file), { recursive: true, mode: 0o700 });
  let state;
  try { state = JSON.parse(await readFile(file, 'utf8')); }
  catch (error) {
    if (error.code !== 'ENOENT') throw new Error('No se pudo leer el archivo demo existente. No se sobrescribió.');
    state = newState(base);
    await persist(file, state, true); // Save credentials before HTTP writes, so an interrupted run can resume.
  }
  validateState(state, base);
  log(`Conjunto de prueba: ${state.dataset}`);
  const identities = new Map();
  for (const account of state.accounts) {
    let identity;
    try { identity = await api('POST', '/auth/login', null, { email: account.email, password: account.password }); }
    catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401) throw error;
      try {
        await api('POST', account.rol === 'PROFESOR' ? '/admin/profesores' : '/admin/alumnos', admin.token,
          { email: account.email, password: account.password, perfil: account.perfil });
      } catch (created) {
        if (!(created instanceof ApiError) || created.status !== 409) throw created;
        // Never reset or take over an existing account, even if its email matches.
      }
      try { identity = await api('POST', '/auth/login', null, { email: account.email, password: account.password }); }
      catch { throw new Error(`No se pudo verificar la cuenta demo ${account.key}; no se cambia su contraseña ni su estado.`); }
    }
    if (identity.rol !== account.rol || !Number.isSafeInteger(identity.perfilId) || identity.perfilId < 1) throw new Error(`El rol/perfil de ${account.key} cambió. No se modificó.`);
    account.perfilId = identity.perfilId;
    identities.set(account.key, identity);
    await persist(file, state);
    log(`Cuenta disponible: ${account.rol} · ${account.email}`);
  }
  const professor = state.accounts.find(a => a.rol === 'PROFESOR'), teacher = identities.get(professor.key);
  for (const student of state.accounts.filter(a => a.rol === 'ALUMNO')) {
    const linked = await api('GET', `/admin/alumnos/${student.perfilId}/profesores?q=${encodeURIComponent(professor.email)}&pagina=0&tamanio=20`, admin.token);
    if (!(linked.contenido ?? linked).some(p => (p.perfil ?? p).id === teacher.perfilId)) {
      await api('PUT', `/admin/alumnos/${student.perfilId}/profesores/${teacher.perfilId}`, admin.token, { motivo: `Vinculación de datos de prueba ${state.dataset}` });
    }
  }
  let exercises = await api('GET', '/ejercicios', teacher.token);
  for (const [nombre, grupoMuscular, descripcion] of EXERCISES) {
    let exercise = exercises.find(e => normalizeName(e.nombre) === normalizeName(nombre));
    if (!exercise) {
      try { exercise = await api('POST', '/ejercicios', teacher.token, { nombre, grupoMuscular, descripcion }); }
      catch (error) {
        if (!(error instanceof ApiError) || error.status !== 409) throw error;
        exercises = await api('GET', '/ejercicios', teacher.token);
        exercise = exercises.find(e => normalizeName(e.nombre) === normalizeName(nombre));
        if (!exercise) throw error;
      }
      exercises.push(exercise);
    }
    state.exercises = [...state.exercises.filter(e => e.name !== nombre), { id: exercise.id, name: nombre }];
    await persist(file, state);
  }
  log(`Listo: 5 alumnos, 1 profesor vinculado y ${EXERCISES.length} ejercicios disponibles.`);
  log(`Credenciales DEMO guardadas en ${file}. El archivo no contiene la contraseña ADMIN ni tokens JWT.`);
  return { dataset: state.dataset, accounts: 6, exercises: EXERCISES.length, stateFile: file };
}
async function passwordPrompt(message) {
  const input = process.stdin, output = process.stdout;
  if (!input.isTTY || !input.setRawMode) throw new Error('Ejecutá el comando desde una terminal interactiva.');
  output.write(message); emitKeypressEvents(input); input.setRawMode(true); input.resume();
  return new Promise((resolvePassword, reject) => {
    let value = '';
    const finish = () => { input.off('keypress', key); input.setRawMode(false); input.pause(); output.write('\n'); };
    const key = (text, info = {}) => {
      if (info.ctrl && info.name === 'c') { finish(); reject(new Error('Carga cancelada.')); }
      else if (info.name === 'return' || info.name === 'enter') { finish(); resolvePassword(value); }
      else if (info.name === 'backspace') value = Array.from(value).slice(0, -1).join('');
      else if (text && !info.ctrl && !info.meta && !text.startsWith('\u001b')) value += text;
    };
    input.on('keypress', key);
  });
}
export async function readCredentials(input) {
  let content = '';
  for await (const chunk of input) {
    content += chunk.toString('utf8');
    if (Buffer.byteLength(content, 'utf8') > 16384) throw new Error('La entrada de credenciales es demasiado grande.');
  }
  let credentials;
  try { credentials = JSON.parse(content.replace(/^\uFEFF/, '')); }
  catch { throw new Error('No se pudieron leer las credenciales por entrada estándar.'); }
  if (typeof credentials?.email !== 'string' || !credentials.email.trim() ||
      typeof credentials.password !== 'string' || !credentials.password) {
    throw new Error('Se necesitan email y contraseña ADMIN.');
  }
  return credentials;
}
async function main() {
  const apiUrl = process.env.GYM_DEMO_API_URL || 'http://localhost:8088/api';
  console.log('Carga opcional de datos ficticios. No elimina ni reemplaza registros existentes.');
  console.log(`API: ${apiUrl}`);
  let adminEmail, adminPassword;
  if (process.argv.includes('--credentials-stdin')) {
    process.stdin.setEncoding('utf8');
    const credentials = await readCredentials(process.stdin);
    adminEmail = credentials.email;
    adminPassword = credentials.password;
  } else {
    const rl = createInterface({ input: process.stdin, output: process.stdout });
    adminEmail = await new Promise(resolveEmail => rl.question('Email de tu ADMIN: ', resolveEmail));
    rl.close();
    adminPassword = await passwordPrompt('Contraseña ADMIN (no se muestra): ');
  }
  console.log('Verificando acceso ADMIN y cargando datos mediante la API…');
  await seedDemo({ apiUrl, stateFile: process.env.GYM_DEMO_FILE || './docker/demo-data/accounts.json', adminEmail, adminPassword });
}
if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  main().catch(error => { console.error(error.message); process.exitCode = 1; });
}
