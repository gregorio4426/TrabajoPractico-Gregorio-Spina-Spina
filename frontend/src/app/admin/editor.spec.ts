import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { afterEach,beforeEach,describe,it,expect } from 'vitest';
import { Editor, Operation } from './editor';
import { People } from './admin-api';
describe('ADMIN reactive forms',()=>{
 beforeEach(()=>TestBed.configureTestingModule({imports:[Editor],providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting()]}));
 afterEach(()=>{TestBed.inject(HttpTestingController).verify();TestBed.resetTestingModule();});
 function create(op:Operation,type:People='usuarios',initial:unknown=null){const fixture=TestBed.createComponent(Editor);fixture.componentRef.setInput('operation',op);fixture.componentRef.setInput('type',type);fixture.componentRef.setInput('initial',initial);fixture.componentRef.setInput('id',7);fixture.detectChanges();return fixture;}
 it('requires at least 12 characters for a new ADMIN and removes irrelevant profile requirements',()=>{const f=create('create');const form=f.componentInstance.form;form.patchValue({email:'admin@example.test',password:'short'});expect(form.invalid).toBe(true);form.controls['password']!.setValue('long-test-password');expect(form.valid).toBe(true);expect(f.componentInstance.fields().some(x=>x.key==='nombre')).toBe(false);});
 it('requires only the destination profile on role selection',()=>{const f=create('create');const form=f.componentInstance.form;form.controls['rol']!.setValue('ALUMNO');expect(f.componentInstance.fields().some(x=>x.key==='edad')).toBe(true);form.controls['rol']!.setValue('PROFESOR');expect(f.componentInstance.fields().some(x=>x.key==='especialidad')).toBe(true);expect(form.controls['edad']!.disabled).toBe(true);});
 it('reuses a conserved profile when changing roles',()=>{const f=create('role','usuarios',{id:7,rol:'ADMIN',revision:3,alumnoId:11});f.componentInstance.form.controls['rol']!.setValue('ALUMNO');expect(f.componentInstance.fields().some(x=>x.key==='nombre')).toBe(false);});
 it('requires a reason and rejects blank values',()=>{const f=create('state','usuarios',{id:7,activo:true,revision:3});f.componentInstance.form.controls['motivo']!.setValue('   ');expect(f.componentInstance.form.invalid).toBe(true);});
 it('validates password bytes, not just character count',()=>{const f=create('create');f.componentInstance.form.patchValue({email:'a@example.test',password:'é'.repeat(40)});expect(f.componentInstance.form.controls['password']!.hasError('bytes')).toBe(true);});
 it('does not permit fractional IDs for linking',()=>{const f=create('link','alumnos');f.componentInstance.form.patchValue({profesorId:'1.5',motivo:'Equipo'});expect(f.componentInstance.form.invalid).toBe(true);});
 it('prefills profile fields and preserves null optional dates as empty inputs',()=>{const f=create('profile','alumnos',{perfil:{nombre:'Ana',apellido:'Paz',edad:25,nivel:'PRINCIPIANTE',objetivo:'MEJORAR_RESISTENCIA',fechaNacimiento:null}});expect(f.componentInstance.form.controls['nombre']!.value).toBe('Ana');expect(f.componentInstance.form.controls['fechaNacimiento']!.value).toBe('');});
});
