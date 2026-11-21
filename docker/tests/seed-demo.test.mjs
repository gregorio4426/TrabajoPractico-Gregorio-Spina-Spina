import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, readFile, stat, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { seedDemo, readCredentials } from '../seed-demo.mjs';
import { Readable } from 'node:stream';

test('reads piped credentials without a TTY, preserving unicode and special characters', async () => {
 const credentials = {email:'admin@example.test', password:'Prueba-ñ-🔒-$"\\'};
 const json = '\uFEFF' + JSON.stringify(credentials) + '\r\n';
 assert.deepEqual(await readCredentials(Readable.from([json.slice(0, 12), json.slice(12)])), credentials);
});
test('rejects invalid, incomplete and excessive stdin without revealing its contents', async () => {
 for (const text of ['private-secret', '{}', 'null', '{"email":"admin@test","password":""}', 'x'.repeat(16385)]) {
   await assert.rejects(readCredentials(Readable.from([text])), error => !error.message.includes('private-secret') && /credenciales|email|contraseña/.test(error.message));
 }
});
function fixture() {
  const accounts = new Map(), exercises = [{id: 41,nombre:'PRESS DE BANCA',grupoMuscular:'PECHO',descripcion:'Conservar descripción existente'}], links = new Set();
  const calls = []; let next = 100;
  const response = (status, body) => new Response(JSON.stringify(body), {status,headers:{'Content-Type':'application/json'}});
  const fetchImpl = async (url, options) => {
    const path = new URL(url).pathname.replace('/api',''), body = options.body ? JSON.parse(options.body) : null;
    calls.push({path,method:options.method,body});
    if(path==='/auth/login') {
      if(body.email==='admin@example.test' && body.password==='PrivateAdminTestValue')return response(200,{rol:'ADMIN',token:'private-admin-token'});
      const a=accounts.get(body.email);return a && a.password===body.password ? response(200,{rol:a.rol,token:a.email,perfilId:a.id}) : response(401,{});
    }
    if(options.method==='POST' && ['/admin/alumnos','/admin/profesores'].includes(path)) {
      assert.equal(options.headers.Authorization,'Bearer private-admin-token');
      if(accounts.has(body.email))return response(409,{});
      accounts.set(body.email,{...body,id:++next,rol:path.endsWith('profesores')?'PROFESOR':'ALUMNO'});return response(201,{});
    }
    const linked=/^\/admin\/alumnos\/(\d+)\/profesores(?:\/(\d+))?$/.exec(path);
    if(linked){assert.equal(options.headers.Authorization,'Bearer private-admin-token');if(options.method==='PUT'){links.add(linked[1]+':'+linked[2]);return response(200,{});}return response(200,{contenido:[...links].filter(s=>s.startsWith(linked[1]+':')).map(s=>({perfil:{id:Number(s.split(':')[1])}}))});}
    if(path==='/ejercicios') {
      const email=options.headers.Authorization?.replace('Bearer ','');assert.equal(accounts.get(email)?.rol,'PROFESOR');
      if(options.method==='GET')return response(200,exercises);
      if(exercises.some(e=>e.nombre.toLowerCase()===body.nombre.toLowerCase()))return response(409,{});
      const e={...body,id:++next};exercises.push(e);return response(201,e);
    }
    throw new Error('Unexpected route '+options.method+' '+path);
  };
  return {accounts,exercises,links,calls,fetchImpl};
}
const settings = async (f) => ({apiUrl:'http://localhost:8088/api',stateFile:join(await mkdtemp(join(tmpdir(),'gym-demo-')),'accounts.json'),adminEmail:'admin@example.test',adminPassword:'PrivateAdminTestValue',fetchImpl:f.fetchImpl});
test('creates real-endpoint payloads, preserves existing exercises and repeats without duplicates',async()=>{
 const f=fixture(),options=await settings(f),logs=[];
 await seedDemo({...options,log:s=>logs.push(s)});
 assert.equal(f.accounts.size,6);assert.equal(f.exercises.length,20);assert.equal(f.links.size,5);assert.equal(f.exercises[0].descripcion,'Conservar descripción existente');
 const created=f.calls.filter(c=>c.method==='POST'&&c.path!=='/auth/login').length;
 await seedDemo({...options,log:s=>logs.push(s)});
 assert.equal(f.calls.filter(c=>c.method==='POST'&&c.path!=='/auth/login').length,created);assert.equal(f.links.size,5);
 const state=JSON.parse(await readFile(options.stateFile,'utf8'));
 assert.equal(new Set(state.accounts.map(a=>a.password)).size,6);assert.ok(state.accounts.every(a=>a.password.length>=12));assert.equal((await stat(options.stateFile)).mode&0o777,0o600);
 assert.ok(!JSON.stringify(state).includes('PrivateAdminTestValue'));assert.ok(!JSON.stringify(state).includes('private-admin-token'));
 for(const account of state.accounts)assert.ok(!logs.join('\n').includes(account.password));
});
test('stops for an existing demo account whose credentials changed, without resetting it',async()=>{
 const f=fixture(),options=await settings(f);await seedDemo({...options,log:()=>{}});const a=[...f.accounts.values()][0];a.password='ChangedByUser';
 await assert.rejects(seedDemo({...options,log:()=>{}}),/No se pudo verificar/);assert.equal(a.password,'ChangedByUser');assert.ok(!f.calls.some(c=>c.method==='DELETE'));
});
test('rejects a manifest for another API without sending its demo credentials',async()=>{
 const f=fixture(),options=await settings(f);await seedDemo({...options,log:()=>{}});const state=JSON.parse(await readFile(options.stateFile,'utf8'));state.apiUrl='http://another/api';await writeFile(options.stateFile,JSON.stringify(state));const before=f.calls.length;
 await assert.rejects(seedDemo({...options,log:()=>{}}),/no corresponde/);assert.equal(f.calls.length,before+1);
});
test('resumes an interrupted creation from credentials saved before the write',async()=>{
 const f=fixture(),options=await settings(f);let fail=true;
 const flaky=async(url,o)=>{const r=await f.fetchImpl(url,o);if(fail && o.method==='POST'&&url.endsWith('/admin/profesores')){fail=false;throw new Error('Lost response');}return r;};
 await assert.rejects(seedDemo({...options,fetchImpl:flaky,log:()=>{}}),/conectar/);
 await seedDemo({...options,log:()=>{}});assert.equal(f.accounts.size,6);assert.equal(f.exercises.length,20);
});
