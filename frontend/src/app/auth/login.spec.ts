import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { afterEach,beforeEach,describe,it,expect } from 'vitest';
import { Login } from './login';

describe('Reactive login form', () => {
  beforeEach(() => { localStorage.clear(); TestBed.configureTestingModule({imports:[Login],providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting()]}); });
  afterEach(() => {TestBed.inject(HttpTestingController).verify();TestBed.resetTestingModule();localStorage.clear();});
  it('validates email and required password before requesting login', () => {
    const fixture=TestBed.createComponent(Login);fixture.detectChanges();
    fixture.componentInstance.submit();fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Ingresá un email válido');
    expect(fixture.componentInstance.form.invalid).toBe(true);
    TestBed.inject(HttpTestingController).expectNone('/api/auth/login');
  });
  it('normalizes email, permits legacy passwords and prevents duplicate submissions', () => {
    const fixture=TestBed.createComponent(Login);const login=fixture.componentInstance;
    login.form.setValue({email:'TEST@EXAMPLE.TEST',password:'old'});
    login.submit();login.submit();
    const request=TestBed.inject(HttpTestingController).expectOne('/api/auth/login');
    expect(request.request.body).toEqual({email:'test@example.test',password:'old'});
    expect(login.pending()).toBe(true);
    request.flush({mensaje:'private diagnostic'}, {status:401,statusText:'Unauthorized'});
    fixture.detectChanges();
    expect(login.pending()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Email o contraseña incorrectos');
    expect(fixture.nativeElement.textContent).not.toContain('private diagnostic');
  });
  it('shows a retryable connection error', () => {
    const fixture=TestBed.createComponent(Login);const login=fixture.componentInstance;
    login.form.setValue({email:'test@example.test',password:'test'});login.submit();
    TestBed.inject(HttpTestingController).expectOne('/api/auth/login').error(new ProgressEvent('error'));
    expect(login.pending()).toBe(false);expect(login.error()).toContain('conexión');
  });
});
