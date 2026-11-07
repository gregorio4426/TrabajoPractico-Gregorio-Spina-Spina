import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_BASE } from '../core/api';
import * as D from './contracts';
export type Query = Record<string, string | number | boolean | null | undefined>;
export interface Page<T> { contenido: T[]; pagina: number; tamanio: number; totalElementos: number; totalPaginas: number; }
export type People = 'usuarios' | 'alumnos' | 'profesores';
export interface Rows { usuarios: D.UsuarioVista; alumnos: D.AlumnoVista; profesores: D.ProfesorVista; rutinas: D.RutinaResponse; asignaciones: D.AsignacionResponse; sesiones: D.SesionResponse; auditoria: D.AuditoriaVista; }
export type Resource = keyof Rows;
@Injectable({providedIn:'root'})
export class AdminApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE) + '/admin';
  get<T>(path: string, query: Query = {}) {
    let params = new HttpParams();
    for (const [key,value] of Object.entries(query)) if (value !== null && value !== undefined && value !== '') params = params.set(key,String(value));
    return this.http.get<T>(this.base + '/' + path, {params});
  }
  list<K extends Resource>(type: K, query: Query = {}) { return this.get<Page<Rows[K]>>(type,query); }
  dashboard(query: Query = {}) { return this.get<D.DashboardAdmin>('dashboard',query); }
  createUser(body: D.CrearUsuario) { return this.http.post<D.UsuarioVista>(this.base+'/usuarios',body); }
  createAlumno(body: D.CrearAlumno) { return this.http.post<D.AlumnoVista>(this.base+'/alumnos',body); }
  createProfesor(body: D.CrearProfesor) { return this.http.post<D.ProfesorVista>(this.base+'/profesores',body); }
  editAlumno(id: number, body: D.AlumnoDatos) { return this.http.put<D.AlumnoVista>(`${this.base}/alumnos/${id}`,body); }
  editProfesor(id: number, body: D.ProfesorDatos) { return this.http.put<D.ProfesorVista>(`${this.base}/profesores/${id}`,body); }
  state(id: number, body: D.CambioEstado) { return this.http.put<D.UsuarioVista>(`${this.base}/usuarios/${id}/estado`,body); }
  role(id: number, body: D.CambioRol) { return this.http.put<D.UsuarioVista>(`${this.base}/usuarios/${id}/rol`,body); }
  email(id: number, body: D.CambioEmail) { return this.http.put<D.UsuarioVista>(`${this.base}/usuarios/${id}/email`,body); }
  account(type: 'alumnos'|'profesores', id: number, body: D.CrearCuentaPerfil) { return this.http.post<D.UsuarioVista>(`${this.base}/${type}/${id}/cuenta`,body); }
  link(alumnoId: number, profesorId: number, body: D.CambioVinculo) { return this.http.put<D.AlumnoVista>(`${this.base}/alumnos/${alumnoId}/profesores/${profesorId}`,body); }
  unlink(alumnoId: number, profesorId: number, body: D.CambioVinculo) { return this.http.delete<D.AlumnoVista>(`${this.base}/alumnos/${alumnoId}/profesores/${profesorId}`,{body}); }
  replace(alumnoId: number, body: D.ReemplazoVinculo) { return this.http.put<D.AlumnoVista>(`${this.base}/alumnos/${alumnoId}/profesores/reemplazo`,body); }
}
