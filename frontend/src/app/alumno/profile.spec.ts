import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController,provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { afterEach,beforeEach,describe,it,expect } from 'vitest';
import { StudentProfile } from './profile';
import { validCalendarDate,pastOrToday } from './utils';
import { FormControl } from '@angular/forms';
describe('ALUMNO profile DTO and calendar dates',()=>{
 beforeEach(()=>TestBed.configureTestingModule({imports:[StudentProfile],providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting()]}));
 afterEach(()=>{TestBed.inject(HttpTestingController).verify();TestBed.resetTestingModule();});
 it('maps fechaNacimiento to the actual fechaDeNacimiento request property',async()=>{const fixture=TestBed.createComponent(StudentProfile);const http=TestBed.inject(HttpTestingController);const data={nombre:'Ana',apellido:'Paz',nivel:'PRINCIPIANTE',objetivo:'MEJORAR_RESISTENCIA',edad:25,fechaNacimiento:'2001-01-01'};http.expectOne('/api/alumnos/me').flush(data);await Promise.resolve();fixture.detectChanges();fixture.componentInstance.profile.controls.fechaDeNacimiento.setValue('2000-01-01');const pending=fixture.componentInstance.save();const req=http.expectOne('/api/alumnos/me');expect(req.request.method).toBe('PUT');expect(req.request.body.fechaDeNacimiento).toBe('2000-01-01');expect(req.request.body.fechaNacimiento).toBeUndefined();expect(req.request.body.edad).toBeUndefined();req.flush(data);await Promise.resolve();http.expectOne('/api/alumnos/me').flush(data);await pending;});
 it('rejects impossible dates rather than relying on lexical comparison alone',()=>{expect(validCalendarDate('2026-02-31')).toBe(false);expect(validCalendarDate('2024-02-29')).toBe(true);expect(pastOrToday(new FormControl('2099-01-01'))).not.toBeNull();});
});
