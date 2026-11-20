import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, it, expect } from 'vitest';
import { ExercisePicker } from './exercise-picker';
import { SESSION_KEY } from '../core/auth.service';
import { testSession } from '../core/auth.fixtures';
describe('inline exercise creation', () => {
  beforeEach(() => {
    localStorage.setItem(SESSION_KEY, JSON.stringify(testSession('PROFESOR')));
    TestBed.configureTestingModule({
      imports: [ExercisePicker],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
    localStorage.clear();
  });
  it('refreshes the real catalogue and reuses an existing normalized name without posting a duplicate', async () => {
    const f = TestBed.createComponent(ExercisePicker);
    f.componentRef.setInput('catalogue', []);
    f.detectChanges();
    f.nativeElement.querySelector('dialog').close = () => {};
    const c = f.componentInstance,
      http = TestBed.inject(HttpTestingController);
    c.form.setValue({ nombre: ' PRESS ', grupoId: '1', descripcion: '' });
    let selected = '';
    c.registerOnChange((v) => (selected = v));
    const save = c.save();
    http.expectOne('/api/ejercicios').flush([{ id: 8, nombre: 'Press', grupoId: 1 }]);
    await save;
    expect(selected).toBe('8');
    http.expectNone((r) => r.method === 'POST');
  });
  it('persists an optional description and selects the created exercise', async () => {
    const f = TestBed.createComponent(ExercisePicker);
    f.componentRef.setInput('catalogue', []);
    f.detectChanges();
    f.nativeElement.querySelector('dialog').close = () => {};
    const c = f.componentInstance,
      http = TestBed.inject(HttpTestingController);
    c.form.setValue({ nombre: 'Nuevo', grupoId: '1', descripcion: '' });
    let selected = '';
    c.registerOnChange((v) => (selected = v));
    const save = c.save();
    http.expectOne('/api/ejercicios').flush([]);
    await Promise.resolve();
    const request = http.expectOne('/api/ejercicios');
    expect(request.request.body).toEqual({ nombre: 'Nuevo', grupoId: 1, descripcion: '' });
    request.flush({ id: 9, nombre: 'Nuevo', grupoId: 1, descripcion: '' });
    await save;
    expect(selected).toBe('9');
  });
});
