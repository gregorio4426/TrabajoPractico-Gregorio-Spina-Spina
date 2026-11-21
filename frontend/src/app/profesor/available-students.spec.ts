import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { AvailableStudents } from './available-students';
describe('available students directory', () => {
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
  });
  it('searches the minimal paginated endpoint without navigating on form submit and refreshes after linking', async () => {
    TestBed.configureTestingModule({
      imports: [AvailableStudents],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const f = TestBed.createComponent(AvailableStudents),
      c = f.componentInstance,
      http = TestBed.inject(HttpTestingController);
    f.detectChanges();
    http.expectNone(() => true);
    c.q.setValue('Juan');
    c.show();
    http
      .expectOne('/api/profesores/me/alumnos-disponibles?q=Juan&pagina=0&tamanio=20')
      .flush({
        contenido: [{ id: 7, nombre: 'Juan', apellido: 'Demo', email: 'juan@example.test' }],
        pagina: 0,
        totalElementos: 1,
        totalPaginas: 1,
      });
    await Promise.resolve();
    f.detectChanges();
    const event = new Event('submit', { cancelable: true, bubbles: true });
    f.nativeElement.querySelector('form').dispatchEvent(event);
    expect(event.defaultPrevented).toBe(true);
    http
      .expectOne('/api/profesores/me/alumnos-disponibles?q=Juan&pagina=0&tamanio=20')
      .flush({ contenido: [], pagina: 0, totalElementos: 0, totalPaginas: 0 });
    await Promise.resolve();
    let linked = false;
    c.linked.subscribe(() => (linked = true));
    const result = c.link({ id: 7, nombre: 'Juan', apellido: 'Demo', email: 'juan@example.test' });
    const write = http.expectOne('/api/profesores/me/alumnos-disponibles/7/vincular');
    expect(write.request.method).toBe('POST');
    write.flush(null, { status: 204, statusText: 'No Content' });
    await Promise.resolve();
    http
      .expectOne('/api/profesores/me/alumnos-disponibles?q=Juan&pagina=0&tamanio=20')
      .flush({ contenido: [], pagina: 0, totalElementos: 0, totalPaginas: 0 });
    await result;
    expect(linked).toBe(true);
    expect(c.success()).toContain('Mis alumnos');
  });
});
