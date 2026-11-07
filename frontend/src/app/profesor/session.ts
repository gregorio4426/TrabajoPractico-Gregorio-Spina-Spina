import { Component,inject,signal } from '@angular/core';
import { ActivatedRoute,RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProfesorApi } from './profesor-api';
import { RecordView } from '../admin/record-view';
import * as D from '../admin/contracts';
import { profesorError } from './utils';
@Component({selector:'app-teacher-session',imports:[RouterLink,RecordView],template:`<section class="page-intro"><h1>Detalle del entrenamiento</h1><p>Consulta de resultados, correcciones y anulaciones. No se registran resultados en nombre del alumno.</p></section><button class="secondary" (click)="load()" [disabled]="loading()">Recargar entrenamiento</button>@if(loading()){<p class="notice" role="status">Consultando autorización y entrenamiento…</p>}@if(error()){<p class="error-message" role="alert">{{error()}}</p>}@if(data();as s){<p><a class="admin-link" [routerLink]="['/profesor/alumnos',s.alumnoId]">Volver al alumno #{{s.alumnoId}}</a></p><section class="admin-panel"><h2>Resultados registrados</h2><app-record-view [value]="s"/></section><section class="admin-panel"><h2>Auditoría del entrenamiento</h2><app-record-view [value]="{eventos:audit()}"/></section>}`})
export class TeacherSession {
 private readonly api=inject(ProfesorApi);private readonly route=inject(ActivatedRoute);private id=0;private seq=0;readonly data=signal<D.SesionResponse|null>(null);readonly audit=signal<D.AuditoriaEntrenamientoResponse[]>([]);readonly error=signal('');readonly loading=signal(false);
 constructor(){this.route.paramMap.pipe(takeUntilDestroyed()).subscribe(p=>{this.id=Number(p.get('id'));void this.load();});}
 async load(){const seq=++this.seq;this.data.set(null);this.audit.set([]);this.error.set('');this.loading.set(true);try{if(!Number.isSafeInteger(this.id)||this.id<1)throw new Error('ID inválido');const[data,audit]=await Promise.all([firstValueFrom(this.api.session(this.id)),firstValueFrom(this.api.audit(this.id))]);if(seq===this.seq){this.data.set(data);this.audit.set(audit);}}catch(e){if(seq===this.seq)this.error.set(profesorError(e));}finally{if(seq===this.seq)this.loading.set(false);}}
}
