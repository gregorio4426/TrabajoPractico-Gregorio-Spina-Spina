import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_BASE } from '../core/api';
import * as D from '../admin/contracts';
import { Page, Query } from '../admin/admin-api';
export type StatPage<T> = Page<T> & { periodo?: D.Periodo };
@Injectable({ providedIn: 'root' })
export class ProfesorApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE);
  get<T>(path: string, query: Query = {}) {
    let params = new HttpParams();
    for (const [k, v] of Object.entries(query))
      if (v !== null && v !== undefined && v !== '') params = params.set(k, String(v));
    return this.http.get<T>(this.base + path, { params });
  }
  dashboard(query: Query = {}) {
    return this.get<D.DashboardProfesor>('/profesores/me/dashboard', query);
  }
  students() {
    return this.get<D.AlumnoResponse[]>('/profesores/me/alumnos');
  }
  student(id: number) {
    return this.get<D.AlumnoResponse>(`/alumnos/${id}`);
  }
  profile() {
    return this.get<D.ProfesorResponse>('/profesores/me');
  }
  updateProfile(body: D.ProfesorRequest) {
    return this.http.put<D.ProfesorResponse>(this.base + '/profesores/me', body);
  }
  email(body: D.CambioEmailRequest) {
    return this.http.put<unknown>(this.base + '/auth/me/email', body);
  }
  password(body: D.CambioPasswordRequest) {
    return this.http.put<unknown>(this.base + '/auth/me/password', body);
  }
  follow(id: number, body: D.SeguimientoAlumnoRequest) {
    return this.http.patch<D.AlumnoResponse>(`${this.base}/alumnos/${id}/seguimiento`, body);
  }
  unlink(id: number) {
    return this.http.delete<void>(`${this.base}/profesores/me/alumnos/${id}`);
  }
  active(id: number) {
    return this.get<D.RutinaResponse>(`/rutinas/activa/alumno/${id}`);
  }
  history(id: number) {
    return this.get<D.RutinaResponse[]>(`/rutinas/historial/alumno/${id}`);
  }
  sessions(id: number) {
    return this.get<D.SesionResponse[]>(`/sesiones/alumno/${id}/historial`);
  }
  session(id: number) {
    return this.get<D.SesionResponse>(`/sesiones/${id}`);
  }
  audit(id: number) {
    return this.get<D.AuditoriaEntrenamientoResponse[]>(`/sesiones/${id}/auditoria`);
  }
  routines() {
    return this.get<D.RutinaResponse[]>('/rutinas');
  }
  routine(id: number) {
    return this.get<D.RutinaResponse>(`/rutinas/${id}`);
  }
  versions(id: number) {
    return this.get<D.RutinaResponse[]>(`/rutinas/${id}/versiones`);
  }
  version(id: number, number: number) {
    return this.get<D.RutinaResponse>(`/rutinas/${id}/versiones/${number}`);
  }
  createRoutine(body: D.RutinaRequest) {
    return this.http.post<D.RutinaResponse>(this.base + '/rutinas', body);
  }
  updateRoutine(id: number, body: D.RutinaRequest) {
    return this.http.put<D.RutinaResponse>(`${this.base}/rutinas/${id}`, body);
  }
  archive(id: number) {
    return this.http.delete<void>(`${this.base}/rutinas/${id}`);
  }
  assign(studentId: number, routineId: number) {
    return this.http.post<D.AsignacionResponse>(
      `${this.base}/rutinas/asignar/alumno/${studentId}/rutina/${routineId}`,
      null,
    );
  }
  exercises(query: Query = {}) {
    return this.get<D.EjercicioResponse[]>('/ejercicios', query);
  }
  createExercise(body: D.EjercicioRequest) {
    return this.http.post<D.EjercicioResponse>(this.base + '/ejercicios', body);
  }
  updateExercise(id: number, body: D.EjercicioRequest) {
    return this.http.put<D.EjercicioResponse>(`${this.base}/ejercicios/${id}`, body);
  }
  deleteExercise(id: number) {
    return this.http.delete<void>(`${this.base}/ejercicios/${id}`);
  }
  statsPath(studentId: number | null) {
    return studentId === null
      ? '/profesores/me/estadisticas'
      : `/profesores/me/alumnos/${studentId}/estadisticas`;
  }
  summary(studentId: number | null, query: Query = {}) {
    return this.get<D.ResumenEstadistico>(this.statsPath(studentId), query);
  }
  activity(studentId: number | null, query: Query = {}) {
    return this.get<StatPage<D.ActividadDia>>(this.statsPath(studentId) + '/actividad', query);
  }
  exerciseStats(studentId: number | null, query: Query = {}) {
    return this.get<StatPage<D.EjercicioEstadistica>>(
      this.statsPath(studentId) + '/ejercicios',
      query,
    );
  }
  evolution(studentId: number | null, exerciseId: number, query: Query = {}) {
    return this.get<StatPage<D.EvolucionEjercicio>>(
      `${this.statsPath(studentId)}/ejercicios/${exerciseId}/evolucion`,
      query,
    );
  }
}
