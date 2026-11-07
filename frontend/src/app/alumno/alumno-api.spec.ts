import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController,provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach,beforeEach,describe,it,expect } from 'vitest';
import { AlumnoApi } from './alumno-api';
import { nextSeriesNumber,validSeries } from './utils';
describe('ALUMNO contracts and training semantics',()=>{
 let api:AlumnoApi;let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});api=TestBed.inject(AlumnoApi);http=TestBed.inject(HttpTestingController);});
 afterEach(()=>{http.verify();TestBed.resetTestingModule();});
 it('requests personal dashboard and history through me endpoints',()=>{api.dashboard().subscribe();http.expectOne('/api/alumnos/me/dashboard').flush({});api.sessions().subscribe();http.expectOne('/api/sesiones/me/historial').flush([]);});
 it('never sends a foreign student ID to personal statistics',()=>{api.evolution(999,3,{pagina:1}).subscribe();const r=http.expectOne(q=>q.url==='/api/alumnos/me/estadisticas/ejercicios/3/evolucion');expect(r.request.params.get('pagina')).toBe('1');r.flush({contenido:[]});});
 it('sends logical annulment with reason and the series revision in DELETE body',()=>{api.annul(2,7,{motivo:'Error de registro',revision:4}).subscribe();const r=http.expectOne('/api/sesiones/2/series/7');expect(r.request.method).toBe('DELETE');expect(r.request.body).toEqual({motivo:'Error de registro',revision:4});r.flush({anulada:true});});
 it('does not invent a completed field when registering a series',()=>{api.addSeries(2,{diaEjercicioId:5,numeroSerie:1,repsHechas:8,pesoUsado:null}).subscribe();const r=http.expectOne('/api/sesiones/2/series');expect(r.request.body).toEqual({diaEjercicioId:5,numeroSerie:1,repsHechas:8,pesoUsado:null});r.flush({});});
 it('preserves known zero and excludes annulled/incomplete/legacy data',()=>{const s={completada:true,numeroSerie:1,repsHechas:8,pesoUsado:0};expect(validSeries(s)).toBe(true);expect(validSeries({...s,pesoUsado:null})).toBe(true);expect(validSeries({...s,anulada:true})).toBe(false);expect(validSeries({...s,legadoExento:true})).toBe(false);});
 it('numbers series globally by exercise, including repeated prescription blocks',()=>{expect(nextSeriesNumber([{ejercicioId:3,diaEjercicioId:8,numeroSerie:1},{ejercicioId:3,diaEjercicioId:9,numeroSerie:2},{ejercicioId:4,numeroSerie:3}],3)).toBe(3);expect(nextSeriesNumber([{ejercicioId:3,numeroSerie:1,anulada:true}],3)).toBe(1);});
});
