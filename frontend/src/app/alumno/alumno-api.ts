import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_BASE } from '../core/api';
import { Query } from '../admin/admin-api';
import { StatPage } from '../profesor/profesor-api';
import * as D from '../admin/contracts';
@Injectable({ providedIn: 'root' })
export class AlumnoApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE);
  get<T>(path: string, query: Query = {}) {
    let params = new HttpParams();
    for (const [k, v] of Object.entries(query))
      if (v !== null && v !== undefined && v !== '') params = params.set(k, String(v));
    return this.http.get<T>(this.base + path, { params });
  }
  dashboard(query: Query = {}) {
    return this.get<D.DashboardAlumno>('/alumnos/me/dashboard', query);
  }
  profile() {
    return this.get<D.AlumnoResponse>('/alumnos/me');
  }
  updateProfile(body: D.AlumnoRequest) {
    return this.http.put<D.AlumnoResponse>(this.base + '/alumnos/me', body);
  }
  email(body: D.CambioEmailRequest) {
    return this.http.put<unknown>(this.base + '/auth/me/email', body);
  }
  password(body: D.CambioPasswordRequest) {
    return this.http.put<unknown>(this.base + '/auth/me/password', body);
  }
  active() {
    return this.get<D.RutinaResponse>('/rutinas/me/activa');
  }
  history() {
    return this.get<D.RutinaResponse[]>('/rutinas/me/historial');
  }
  sessions() {
    return this.get<D.SesionResponse[]>('/sesiones/me/historial');
  }
  session(id: number) {
    return this.get<D.SesionResponse>(`/sesiones/${id}`);
  }
  version(root: number, number: number) {
    return this.get<D.RutinaResponse>(`/rutinas/${root}/versiones/${number}`);
  }
  start(body: D.SesionRequest) {
    return this.http.post<D.SesionResponse>(this.base + '/sesiones', body);
  }
  addSeries(id: number, body: D.SerieRequest) {
    return this.http.post<D.SerieResponse>(`${this.base}/sesiones/${id}/series`, body);
  }
  finish(id: number) {
    return this.http.put<D.SesionResponse>(`${this.base}/sesiones/${id}/completar`, null);
  }
  correctSeries(id: number, seriesId: number, body: D.CorreccionSerieRequest) {
    return this.http.patch<D.SerieResponse>(`${this.base}/sesiones/${id}/series/${seriesId}`, body);
  }
  annul(id: number, seriesId: number, body: D.AnulacionSerieRequest) {
    return this.http.delete<D.SerieResponse>(`${this.base}/sesiones/${id}/series/${seriesId}`, {
      body,
    });
  }
  restore(id: number, seriesId: number, body: D.CorreccionSerieRequest) {
    return this.http.put<D.SerieResponse>(
      `${this.base}/sesiones/${id}/series/${seriesId}/restaurar`,
      body,
    );
  }
  correctDate(id: number, body: D.CorreccionSesionRequest) {
    return this.http.patch<D.SesionResponse>(`${this.base}/sesiones/${id}/correccion`, body);
  }
  audit(id: number) {
    return this.get<D.AuditoriaEntrenamientoResponse[]>(`/sesiones/${id}/auditoria`);
  }
  last(exerciseId: number) {
    return this.get<D.UltimoEjercicioResponse>(`/sesiones/me/ejercicios/${exerciseId}/ultima`);
  }
  professors() {
    return this.get<D.ProfesorResponse[]>('/profesores');
  }
  link(professorId: number) {
    return this.http.post<D.AlumnoResponse>(
      `${this.base}/alumnos/me/profesores/${professorId}`,
      null,
    );
  }
  unlink(professorId: number) {
    return this.http.delete<D.AlumnoResponse>(`${this.base}/alumnos/me/profesores/${professorId}`);
  }
  replace(studentId: number, oldId: number, newId: number) {
    return this.http.put<D.AlumnoResponse>(
      `${this.base}/alumnos/${studentId}/profesores/${oldId}/reemplazo/${newId}`,
      null,
    );
  }
  summary(_studentId: number | null, query: Query = {}) {
    return this.get<D.ResumenEstadistico>('/alumnos/me/estadisticas', query);
  }
  activity(_studentId: number | null, query: Query = {}) {
    return this.get<StatPage<D.ActividadDia>>('/alumnos/me/estadisticas/actividad', query);
  }
  exerciseStats(_studentId: number | null, query: Query = {}) {
    return this.get<StatPage<D.EjercicioEstadistica>>('/alumnos/me/estadisticas/ejercicios', query);
  }
  evolution(_studentId: number | null, exerciseId: number, query: Query = {}) {
    return this.get<StatPage<D.EvolucionEjercicio>>(
      `/alumnos/me/estadisticas/ejercicios/${exerciseId}/evolucion`,
      query,
    );
  }
}
