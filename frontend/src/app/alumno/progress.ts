import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AlumnoApi } from './alumno-api';
import * as D from '../admin/contracts';
import { StatPage } from '../profesor/profesor-api';
import { Chart, ChartPoint } from '../profesor/chart';
import { alumnoError, localDate } from './utils';
import { dateAR } from '../shared/measurement';
import { progressValues } from './progress-values';
import { statisticsPeriodError } from '../shared/statistics-period';
@Component({selector:'app-student-progress',imports:[ReactiveFormsModule,RouterLink,Chart],template: `
@if (loading()) { <p class="notice" role="status">Buscando tus entrenamientos…</p> }
@if (error()) { <p class="error-message" role="alert">{{ error() }} <button class="secondary" (click)="load()">Reintentar</button></p> }
@if (sessions(); as sessions) {
<section class="progress-highlights" aria-label="Tu semana de entrenamiento">
 <article><span>Entrenamientos de esta semana</span><strong>{{ values().weekCount }}</strong><p>{{ values().weekCount ? 'Cada entrenamiento suma.' : 'Tu próximo entrenamiento empieza con un paso.' }}</p></article>
 <article><span>Tu constancia</span><strong>{{ values().streak }} <small>semanas</small></strong><p>Con entrenamientos en semanas consecutivas.</p></article>
 <article><span>Este mes</span><strong>{{ values().monthCount }}</strong><p>Entrenamientos completados.</p></article>
</section>
<section class="week-card"><h2>Días que entrenaste</h2><div class="week-days">
 @for(day of values().days;track day.date){ <a [routerLink]="'/alumno/sesiones'" [queryParams]="{desde:day.date,hasta:day.date}" [class.trained]="day.trained" [attr.aria-label]="day.label + ' ' + dateAR(day.date) + (day.trained ? ': entrenaste' : ': sin entrenamiento completado')"><span>{{ day.label }}</span><strong>{{ day.trained ? '✓' : '·' }}</strong></a> }
</div><p class="admin-help">Entrenamientos finalizados con resultados guardados.</p></section>
@if (!compact()) {
 @if(showDates()){<form class="admin-filters" [formGroup]="form" (ngSubmit)="loadCharts()"><label>Desde<input type="date" formControlName="desde" /></label><label>Hasta<input type="date" formControlName="hasta" /></label><button class="secondary" [disabled]="chartLoading()">Ver evolución</button></form>}
 <section class="student-evolution"><h2>Tu evolución</h2>
 <app-progress-chart title="Entrenamientos por semana" unit="Entrenamientos" [points]="weekly()" kind="bar" [friendly]="true" />
 <label class="exercise-choice">Elegí un ejercicio<select [value]="selected() ?? ''" (change)="choose(+$any($event.target).value)"><option value="">Seleccionar ejercicio</option>@for(exercise of exercises()?.contenido ?? [];track exercise.ejercicioId){<option [value]="exercise.ejercicioId">{{ exercise.nombreActual }}</option>}</select></label>
 @if(exercises();as page){@if(page.totalPaginas>1){<nav class="admin-pagination" aria-label="Ejercicios de tu progreso"><button class="secondary" [disabled]="chartLoading() || page.pagina===0" (click)="exercisePage(page.pagina-1)">Anteriores</button><button class="secondary" [disabled]="chartLoading() || page.pagina+1>=page.totalPaginas" (click)="exercisePage(page.pagina+1)">Siguientes</button></nav>}}
 @if(chartLoading()){<p role="status">Cargando tu evolución…</p>}
 @if(selected()) {<app-progress-chart title="Peso utilizado" unit="Peso registrado" [points]="weights()" [friendly]="true" />
 @if(evolution();as page){@if(page.totalPaginas>1){<nav class="admin-pagination" aria-label="Entrenamientos del ejercicio"><button class="secondary" [disabled]="chartLoading() || page.pagina===0" (click)="choose(selected()!,page.pagina-1)">Anteriores</button><button class="secondary" [disabled]="chartLoading() || page.pagina+1>=page.totalPaginas" (click)="choose(selected()!,page.pagina+1)">Siguientes</button></nav>}}}
 @else {<p class="admin-empty">Elegí un ejercicio para ver cómo cambió el peso que usaste.</p>}
 </section>
}
}`})
export class StudentProgress {
 private readonly api=inject(AlumnoApi);
 readonly period=input<{desde:string;hasta:string}|null>(null);readonly showDates=input(true);readonly compact=input(false);
 readonly today=localDate();readonly dateAR=dateAR;
 readonly form=inject(FormBuilder).nonNullable.group({desde:this.today.slice(0,7)+'-01',hasta:this.today});
 readonly sessions=signal<D.SesionResponse[]|null>(null);readonly exercises=signal<StatPage<D.EjercicioEstadistica>|null>(null);readonly evolution=signal<StatPage<D.EvolucionEjercicio>|null>(null);readonly selected=signal<number|null>(null);
 readonly loading=signal(false);readonly chartLoading=signal(false);readonly error=signal('');
 readonly values=computed(()=>progressValues(this.sessions()??[],this.today));
 readonly weekly=computed<ChartPoint[]>(()=>this.values().weeks.map(w=>({label:dateAR(w.date),value:w.count,link:'/alumno/sesiones'})));
 readonly weights=computed<ChartPoint[]>(()=> (this.evolution()?.contenido??[]).map(e=>({label:dateAR(e.fechaEfectiva),value:e.datos?.pesoMaximo,link:'/alumno/sesiones/'+e.sesionId})));
 private seq=0; private chartsSeq=0;
 constructor(){effect(()=>{const p=this.period(),compact=this.compact();untracked(()=>{if(p)this.form.setValue(p);void this.load(compact);});});}
 async load(compact=this.compact()){const seq=++this.seq;this.loading.set(true);this.error.set('');this.sessions.set(null);try{const sessions=await firstValueFrom(this.api.sessions());if(seq===this.seq)this.sessions.set(sessions);if(!compact&&seq===this.seq)await this.loadCharts();}catch(e){if(seq===this.seq)this.error.set(alumnoError(e));}finally{if(seq===this.seq)this.loading.set(false);}}
 async loadCharts(){const error=statisticsPeriodError(this.form.controls.desde.value,this.form.controls.hasta.value);if(error){this.error.set(error);return;}this.selected.set(null);this.evolution.set(null);await this.exercisePage(0);}
 async exercisePage(page:number){const seq=++this.chartsSeq;this.chartLoading.set(true);this.error.set('');try{const result=await firstValueFrom(this.api.exerciseStats(null,{...this.form.getRawValue(),pagina:page,tamanio:20}));if(seq===this.chartsSeq)this.exercises.set(result);}catch(e){if(seq===this.chartsSeq)this.error.set(alumnoError(e));}finally{if(seq===this.chartsSeq)this.chartLoading.set(false);}}
 async choose(id:number,page=0){const seq=++this.chartsSeq;this.selected.set(id||null);this.evolution.set(null);if(!id)return;this.chartLoading.set(true);this.error.set('');try{const result=await firstValueFrom(this.api.evolution(null,id,{...this.form.getRawValue(),pagina:page,tamanio:30}));if(seq===this.chartsSeq)this.evolution.set(result);}catch(e){if(seq===this.chartsSeq)this.error.set(alumnoError(e));}finally{if(seq===this.chartsSeq)this.chartLoading.set(false);}}
}
@Component({selector:'app-student-progress-page',imports:[StudentProgress],template:'<section class="page-intro"><h1>Mi progreso.</h1><p>Cada semana cuenta. Mirá lo que venís construyendo.</p></section><app-student-progress />'})
export class StudentProgressPage {}
