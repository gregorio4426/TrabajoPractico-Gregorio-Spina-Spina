import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, it, expect, vi } from 'vitest';
import { AdminApi } from './admin-api';
import { authInterceptor } from '../core/auth.interceptor';
import { SESSION_KEY } from '../core/auth.service';
import { testSession } from '../core/auth.fixtures';
import { adminError } from './admin-utils';
import { HttpErrorResponse } from '@angular/common/http';
describe('ADMIN HTTP contracts',()=>{
 let api:AdminApi;let http:HttpTestingController;
 beforeEach(()=>{localStorage.setItem(SESSION_KEY,JSON.stringify(testSession('ADMIN')));TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(withInterceptors([authInterceptor])),provideHttpClientTesting()]});api=TestBed.inject(AdminApi);http=TestBed.inject(HttpTestingController);});
 afterEach(()=>{http.verify();TestBed.resetTestingModule();localStorage.clear();vi.restoreAllMocks();});
 it('sends exact backend pagination and filters, preserving false and literal search',()=>{
  api.list('usuarios',{pagina:2,tamanio:10,q:'%_Ana',activo:false,rol:'ALUMNO',empty:'',absent:null}).subscribe();
  const req=http.expectOne(r=>r.url==='/api/admin/usuarios');
  expect(req.request.params.get('pagina')).toBe('2');expect(req.request.params.get('activo')).toBe('false');expect(req.request.params.get('q')).toBe('%_Ana');expect(req.request.params.has('empty')).toBe(false);expect(req.request.headers.get('Authorization')).toContain('Bearer ');req.flush({contenido:[],pagina:2,tamanio:10,totalElementos:0,totalPaginas:0});
 });
 it('sends revision and reason for account state',()=>{api.state(9,{activo:false,revision:4,motivo:'Baja solicitada'}).subscribe();const req=http.expectOne('/api/admin/usuarios/9/estado');expect(req.request.method).toBe('PUT');expect(req.request.body).toEqual({activo:false,revision:4,motivo:'Baja solicitada'});req.flush({id:9});});
 it('uses DELETE with reason for unlink, never physical deletion',()=>{api.unlink(2,3,{motivo:'Cambio de equipo'}).subscribe();const req=http.expectOne('/api/admin/alumnos/2/profesores/3');expect(req.request.method).toBe('DELETE');expect(req.request.body).toEqual({motivo:'Cambio de equipo'});req.flush({});});
 it('replaces a single link with the two actual DTO IDs',()=>{api.replace(2,{profesorAnteriorId:3,profesorNuevoId:5,motivo:'Reemplazo'}).subscribe();const req=http.expectOne('/api/admin/alumnos/2/profesores/reemplazo');expect(req.request.body.profesorNuevoId).toBe(5);req.flush({});});
 it('creates a profile using the nested perfil contract',()=>{const perfil={nombre:'Ana',apellido:'Paz',especialidad:'Fuerza'};api.createProfesor({email:'ana@example.test',password:'test-only',perfil}).subscribe();const req=http.expectOne('/api/admin/profesores');expect(req.request.method).toBe('POST');expect(req.request.body.perfil).toEqual(perfil);req.flush({});});
 it('uses the dashboard endpoint and requested period',()=>{api.dashboard({desde:'2026-10-01',hasta:'2026-10-30'}).subscribe();const req=http.expectOne(r=>r.url==='/api/admin/dashboard');expect(req.request.params.get('hasta')).toBe('2026-10-30');req.flush({});});
 it('preserves 409 conflicts for explicit reload and never retries a mutation',()=>{let error:unknown;api.role(1,{rol:'ADMIN',revision:0,motivo:'Cambio'}).subscribe({error:e=>error=e});http.expectOne('/api/admin/usuarios/1/rol').flush({mensaje:'conflict'},{status:409,statusText:'Conflict'});expect(adminError(error)).toContain('Recargá');http.expectNone('/api/admin/usuarios/1/rol');});
 it('never displays internal server diagnostics',()=>{expect(adminError(new HttpErrorResponse({status:500,error:{mensaje:'SQL secret diagnostic'}}))).not.toContain('SQL');});
});
