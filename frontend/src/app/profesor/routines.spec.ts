import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting,HttpTestingController } from '@angular/common/http/testing';
import { Router,provideRouter } from '@angular/router';
import { afterEach,beforeEach,describe,it,expect,vi } from 'vitest';
import { Routines } from './routines';
describe('Archived routine lookup',()=>{
 beforeEach(()=>TestBed.configureTestingModule({imports:[Routines],providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting()]}));
 afterEach(()=>{TestBed.inject(HttpTestingController).verify();TestBed.resetTestingModule();vi.restoreAllMocks();});
 it('uses native submit for its standalone reactive ID control',()=>{const navigate=vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);const fixture=TestBed.createComponent(Routines);TestBed.inject(HttpTestingController).expectOne('/api/rutinas').flush([]);fixture.detectChanges();fixture.componentInstance.filters.controls.id.setValue('17');const event=new Event('submit',{bubbles:true,cancelable:true});fixture.nativeElement.querySelectorAll('form')[1].dispatchEvent(event);expect(event.defaultPrevented).toBe(true);expect(navigate).toHaveBeenCalledWith(['/profesor/rutinas',17]);});
});
