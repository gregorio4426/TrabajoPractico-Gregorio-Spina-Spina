import { test, expect, Page } from '@playwright/test';
import { randomBytes } from 'node:crypto';
import { readFileSync } from 'node:fs';
const unique=()=>randomBytes(5).toString('hex');

test.describe('ADMIN · API real',()=>{
 test.skip(!process.env['GYM_E2E_REAL_API']||!process.env['GYM_E2E_CREDENTIALS_FILE'],'Requiere backend local aislado y cuentas privadas de prueba.');
 async function login(page:Page){
  const accounts=JSON.parse(readFileSync(process.env['GYM_E2E_CREDENTIALS_FILE']!,'utf8')) as {ADMIN:{email:string;password:string}};
  await page.goto('/login');await page.getByLabel('Email',{exact:true}).fill(accounts.ADMIN.email);await page.getByLabel('Contraseña',{exact:true}).fill(accounts.ADMIN.password);await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();await expect(page).toHaveURL(/\/admin$/);await expect(page.getByRole('heading',{name:'El pulso de tu equipo.'})).toBeVisible();
 }
 async function confirm(page:Page){await page.getByRole('button',{name:'Revisar y guardar'}).click();await expect(page.getByRole('dialog')).toBeVisible();await page.getByRole('button',{name:'Confirmar cambio',exact:true}).click();}
 async function createPerson(page:Page,role:'ALUMNO'|'PROFESOR'){
  const email=`admin-ui-${unique()}@example.test`;const type=role==='ALUMNO'?'alumnos':'profesores';
  await page.goto(`/admin/${type}/nuevo`);await page.getByRole('textbox',{name:'Email de acceso *',exact:true}).fill(email);await page.getByLabel('Contraseña inicial').fill(randomBytes(20).toString('base64url'));
  await page.getByLabel('Nombre *',{exact:true}).fill('Prueba');await page.getByLabel('Apellido *',{exact:true}).fill('Admin');
  if(role==='ALUMNO'){await page.getByLabel('Edad *',{exact:true}).fill('25');await page.getByRole('combobox',{name:'Nivel *',exact:true}).selectOption('PRINCIPIANTE');await page.getByRole('combobox',{name:'Objetivo *',exact:true}).selectOption('MEJORAR_RESISTENCIA');}
  else await page.getByLabel('Especialidad').fill('Fuerza');
  await confirm(page);await expect(page).toHaveURL(new RegExp(`/admin/${type}\\?`));await expect(page.getByText('Registro creado correctamente.')).toBeVisible();
  await page.getByLabel('Buscar nombre o email').fill(email);await page.getByRole('button',{name:'Aplicar filtros'}).click();await expect(page.getByRole('cell',{name:email,exact:true})).toBeVisible();await page.getByRole('row').filter({has:page.getByRole('cell',{name:email,exact:true})}).getByRole('button',{name:/Ver detalle #/}).click();await expect(page).toHaveURL(new RegExp(`/admin/${type}/\\d+$`));
  const id=Number(new URL(page.url()).pathname.split('/').pop());await expect(page.getByRole('heading',{name:`${role==='ALUMNO'?'Alumno':'Profesor'} #${id}`})).toBeVisible();return {id,email};
 }
 async function reason(page:Page,value:string){await page.getByLabel('Motivo de la operación').fill(value);await confirm(page);await expect(page.getByText('Cambio guardado correctamente.')).toBeVisible();}
 test('dashboard, filters, server pagination and mobile navigation',async({page})=>{
  await login(page);await expect(page.getByRole('link',{name:/Cuentas activas/})).toBeVisible();await page.getByRole('link',{name:'Usuarios',exact:true}).click();await page.getByLabel('Por página').selectOption('10');await page.getByRole('combobox',{name:'Rol',exact:true}).selectOption('ALUMNO');await page.getByRole('button',{name:'Aplicar filtros'}).click();await expect(page).toHaveURL(/rol=ALUMNO/);await expect(page.getByText(/registros encontrados/)).toBeVisible();await page.reload();await expect(page.getByRole('combobox',{name:'Rol',exact:true})).toHaveValue('ALUMNO');
  await page.setViewportSize({width:390,height:844});await page.getByRole('button',{name:'Abrir menú'}).click();await page.getByRole('link',{name:'Auditoría',exact:true}).click();await expect(page).toHaveURL(/\/admin\/auditoria$/);expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 });
 test('create/edit profiles and link, replace and unlink professors',async({page})=>{
  test.setTimeout(90000);await login(page);const alumno=await createPerson(page,'ALUMNO');const profesor=await createPerson(page,'PROFESOR');const reemplazo=await createPerson(page,'PROFESOR');
  await page.goto(`/admin/alumnos/${alumno.id}`);await page.getByRole('button',{name:'Editar perfil',exact:true}).click();await page.getByLabel('Nombre *',{exact:true}).fill('Actualizado');await confirm(page);await expect(page.getByText('Cambio guardado correctamente.')).toBeVisible();
  await page.getByRole('button',{name:'Vincular profesor',exact:true}).click();await page.getByLabel('ID del profesor *',{exact:true}).fill(String(profesor.id));await reason(page,'Vínculo de prueba de interfaz');
  await page.getByRole('button',{name:'Reemplazar profesor',exact:true}).click();await page.getByLabel('ID del profesor anterior').fill(String(profesor.id));await page.getByLabel('ID del nuevo profesor').fill(String(reemplazo.id));await reason(page,'Reemplazo de prueba');
  await page.getByRole('link',{name:'Profesores vinculados',exact:true}).click();await expect(page.getByRole('cell',{name:reemplazo.email,exact:true})).toBeVisible();await expect(page.getByRole('cell',{name:profesor.email,exact:true})).not.toBeVisible();
  await page.goto(`/admin/alumnos/${alumno.id}`);await page.getByRole('button',{name:'Desvincular profesor',exact:true}).click();await page.getByLabel('ID del profesor *',{exact:true}).fill(String(reemplazo.id));await reason(page,'Desvínculo de prueba');await page.getByRole('link',{name:'Historial de asignaciones'}).click();await expect(page.getByText(/registros encontrados/)).toBeVisible();
 });
 test('state, email, role changes, preserved profiles and administrative audit',async({page})=>{
  test.setTimeout(90000);await login(page);const alumno=await createPerson(page,'ALUMNO');await page.getByRole('link',{name:'Gestionar cuenta',exact:true}).click();const userId=Number(new URL(page.url()).pathname.split('/').pop());
  await page.getByRole('button',{name:'Activar / desactivar',exact:true}).click();await reason(page,'Desactivar para prueba');await page.getByRole('button',{name:'Activar / desactivar',exact:true}).click();await reason(page,'Reactivar para prueba');
  await page.getByRole('button',{name:'Cambiar email',exact:true}).click();const newEmail=`changed-${unique()}@example.test`;await page.getByRole('textbox',{name:'Email de acceso *',exact:true}).fill(newEmail);await reason(page,'Identidad verificada en prueba');await expect(page.getByText(newEmail,{exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Cambiar rol',exact:true}).click();await page.getByRole('combobox',{name:'Rol *',exact:true}).selectOption('PROFESOR');await page.getByLabel('Nombre *',{exact:true}).fill('Nuevo');await page.getByLabel('Apellido *',{exact:true}).fill('Profesor');await page.getByLabel('Especialidad').fill('Movilidad');await reason(page,'Cambio de rol para prueba');
  await page.getByRole('link',{name:'Perfil alumno',exact:true}).click();await expect(page.getByText(alumno.email,{exact:true})).not.toBeVisible();await expect(page.getByText(newEmail,{exact:true}).first()).toBeVisible();
  await page.goto(`/admin/auditoria?usuarioId=${userId}`);await expect(page.getByText(/registros encontrados/)).toBeVisible();await page.getByRole('button',{name:/Ver detalle #/}).first().click();await expect(page.getByRole('heading',{name:'Detalle del evento'})).toBeVisible();
 });
 test('routines, assignments, sessions and audit are real paginated reads',async({page})=>{
  await login(page);for(const section of ['rutinas','asignaciones','sesiones','auditoria']){await page.goto('/admin/'+section);await expect(page.getByText(/registros encontrados/)).toBeVisible();await expect(page.getByRole('alert')).toHaveCount(0);}
 });

 test('historical routine versions, exact assignments, partial session and its audit',async({page,request})=>{
  test.setTimeout(90000);await login(page);
  const adminToken=await page.evaluate(()=>(JSON.parse(localStorage.getItem('gym.session.v1')!) as {token:string}).token);
  const accounts=JSON.parse(readFileSync(process.env['GYM_E2E_CREDENTIALS_FILE']!,'utf8')) as {PROFESOR:{email:string;password:string}};
  const professorLogin=await request.post('/api/auth/login',{data:accounts.PROFESOR});expect(professorLogin.status()).toBe(200);
  const professor=await professorLogin.json() as {token:string;perfilId:number};const professorHeaders={Authorization:'Bearer '+professor.token};
  const registration=await request.post('/api/auth/register/alumno',{data:{email:`history-${unique()}@example.test`,password:randomBytes(20).toString('base64url'),nombre:'Historia',apellido:'Prueba',edad:25,nivel:'PRINCIPIANTE',objetivo:'MEJORAR_RESISTENCIA'}});expect(registration.status()).toBe(201);
  const alumno=await registration.json() as {token:string;perfilId:number};const alumnoHeaders={Authorization:'Bearer '+alumno.token};
  const link=await request.put(`/api/admin/alumnos/${alumno.perfilId}/profesores/${professor.perfilId}`,{headers:{Authorization:'Bearer '+adminToken},data:{motivo:'Preparación de historia de prueba'}});expect(link.status()).toBe(200);
  const exercise=await request.post('/api/ejercicios',{headers:professorHeaders,data:{nombre:'Prueba '+unique(),descripcion:'Ejercicio de validación',grupoMuscular:'PECHO'}});expect(exercise.status()).toBe(201);const exerciseId=(await exercise.json() as {id:number}).id;
  const routineBody={nombre:'Plan histórico '+unique(),descripcion:'Versión original de prueba',dias:[{nombre:'Día de prueba',numeroOrden:1,ejercicios:[{ejercicioId:exerciseId,seriesSugeridas:2,repsSugeridas:8,pesoSugerido:20}]}]};
  type Routine={id:number;rutinaVersionId:number;dias:{id:number;ejercicios:{id:number}[]}[]};
  const creation=await request.post('/api/rutinas',{headers:professorHeaders,data:routineBody});expect(creation.status()).toBe(201);const v1=await creation.json() as Routine;
  const assignment=await request.post(`/api/rutinas/asignar/alumno/${alumno.perfilId}/rutina/${v1.id}`,{headers:professorHeaders});expect(assignment.status()).toBe(200);
  const start=await request.post('/api/sesiones',{headers:alumnoHeaders,data:{diaRutinaId:v1.dias[0]!.id,fecha:new Date().toISOString().slice(0,10)}});expect(start.status()).toBe(201);const sessionId=(await start.json() as {id:number}).id;
  const series=await request.post(`/api/sesiones/${sessionId}/series`,{headers:alumnoHeaders,data:{diaEjercicioId:v1.dias[0]!.ejercicios[0]!.id,numeroSerie:1,repsHechas:8,pesoUsado:20,completada:true}});expect(series.status()).toBe(201);
  const finish=await request.put(`/api/sesiones/${sessionId}/completar`,{headers:alumnoHeaders});expect(finish.status()).toBe(200);
  const v2=await request.put(`/api/rutinas/${v1.id}`,{headers:professorHeaders,data:{...routineBody,nombre:'Plan actualizado '+unique()}});expect(v2.status()).toBe(200);
  await page.goto(`/admin/rutinas/${v1.id}/versiones`);await expect(page.getByText(/registros encontrados/)).toBeVisible();await expect(page.getByRole('button',{name:/Ver detalle #/})).toHaveCount(2);
  await page.getByRole('button',{name:/Ver detalle #/}).first().click();await expect(page.getByRole('heading',{name:'Detalle de la versión histórica'})).toBeVisible();await expect(page.getByText('Versión original de prueba',{exact:true})).toBeVisible();
  await page.goto(`/admin/alumnos/${alumno.perfilId}/historial`);await expect(page.getByRole('cell',{name:routineBody.nombre,exact:true})).toBeVisible();
  await page.getByRole('button',{name:/Ver detalle #/}).click();await expect(page.getByRole('heading',{name:'Datos del registro'})).toBeVisible();
  await page.goto(`/admin/sesiones/${sessionId}`);await expect(page.getByRole('heading',{name:'Datos del registro'})).toBeVisible();await page.getByRole('button',{name:'Ver auditoría del entrenamiento'}).click();await expect(page.getByRole('heading',{name:'Auditoría del entrenamiento'})).toBeVisible();
 });
});
