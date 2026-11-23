import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProfesorApi } from './profesor-api';
import { AlumnoResponse, DashboardProfesor, RutinaResponse } from '../admin/contracts';
import { profesorError, text, isDenied } from './utils';
import { dateAR } from '../shared/measurement';
@Component({selector:'app-teacher-dashboard',imports:[RouterLink,ReactiveFormsModule],template:`
<section class="page-intro"><span class="eyebrow muted">TU EQUIPO EN MOVIMIENTO</span><h1>Acompañá cada avance<span class="accent">.</span></h1><p>Organizá el entrenamiento de hoy.</p></section>
@if(loading()){<p class="notice" role="status">Cargando tu equipo…</p>}
@if(error()){<p class="error-message" role="alert">{{ error() }} <button class="secondary" (click)="load()">Reintentar</button></p>}
@if(data();as d){
<section class="admin-metrics daily-metrics" aria-label="Tu trabajo diario">
 <a routerLink="/profesor/alumnos"><span>Mis alumnos</span><strong>{{ text(d.alumnosVinculados) }}</strong><small>Ver mi equipo →</small></a>
 <a routerLink="/profesor/rutinas"><span>Mis rutinas</span><strong>{{ d.rutinasPropias == null || d.rutinasPropiasArchivadas == null ? '—' : d.rutinasPropias - d.rutinasPropiasArchivadas }}</strong><small>Consultar y editar →</small></a>
 <a routerLink="/profesor/alumnos"><span>Rutinas asignadas</span><strong>{{ text(d.asignacionesActivasDeSusAlumnos) }}</strong><small>Organizar entrenamientos →</small></a>
 <a routerLink="/profesor/progreso"><span>Entrenamientos recientes</span><strong>{{ text(d.entrenamientos?.entrenamientosConDatosValidos) }}</strong><small>{{ dateAR(d.periodo?.desde) }} — {{ dateAR(d.periodo?.hasta) }} →</small></a>
</section>
<nav class="quick-actions" aria-label="Acciones rápidas"><a class="primary" routerLink="/profesor/rutinas/nueva">+ Crear rutina</a><a class="secondary" routerLink="/profesor/rutinas">Asignar rutina</a><a class="secondary" routerLink="/profesor/alumnos">Ver mis alumnos</a></nav>
<section class="admin-panel daily-students"><header class="admin-kicker"><h2>Mis alumnos</h2><a class="admin-link" routerLink="/profesor/alumnos">Ver todos mis alumnos →</a></header>
<label>Buscar por nombre<input type="search" [formControl]="search" placeholder="Nombre o apellido" /></label>
@if(detailsLoading()){<p class="admin-help" role="status">Consultando rutinas y últimos entrenamientos…</p>}
@for(student of preview();track student.id){ @let detail=details().get(student.id!);
<article class="daily-student"><div><h3>{{ student.nombre }} {{ student.apellido }}</h3>
 @if(detail){<p><span class="student-badge">{{ detail.unavailable ? 'Consultar perfil' : detail.routine ? 'Con rutina' : 'Sin rutina' }}</span> {{ detail.routine?.nombre }}</p>@if(detail.last){<small>Último entrenamiento: {{ dateAR(detail.last) }}</small>}}
</div><div class="admin-actions"><a class="secondary" [routerLink]="['/profesor/alumnos',student.id]">Ver alumno</a>@if(detail && !detail.routine && !detail.unavailable){<a class="primary" routerLink="/profesor/rutinas" [queryParams]="{alumno:student.id}">Asignar rutina</a>}</div></article>
} @empty {<p class="admin-empty">{{ search.value ? 'No encontramos alumnos con ese nombre.' : 'Todavía no tenés alumnos en tu equipo.' }}</p>}
</section>}
`})
export class TeacherDashboard {
 private readonly api=inject(ProfesorApi);
 readonly data=signal<DashboardProfesor|null>(null);readonly students=signal<AlumnoResponse[]>([]);readonly error=signal('');readonly loading=signal(false);readonly detailsLoading=signal(false);readonly text=text;readonly dateAR=dateAR;
 readonly search=new FormControl('',{nonNullable:true}); private readonly term=signal('');
 readonly preview=computed(()=>this.students().filter(s=>`${s.nombre} ${s.apellido}`.toLocaleLowerCase('es').includes(this.term())).slice(0,8));
 readonly details=signal(new Map<number,{routine:RutinaResponse|null;last:string|null;unavailable?:boolean}>());private seq=0;private detailsSeq=0;
 constructor(){this.search.valueChanges.pipe(takeUntilDestroyed()).subscribe(q=>this.term.set(q.trim().toLocaleLowerCase('es')));effect(()=>{const students=this.preview();untracked(()=>void this.loadDetails(students));});void this.load();}
 async load(){const seq=++this.seq;this.loading.set(true);this.error.set('');this.data.set(null);this.students.set([]);try{const [dashboard,students]=await Promise.all([firstValueFrom(this.api.dashboard()),firstValueFrom(this.api.students())]);if(seq===this.seq){this.data.set(dashboard);this.students.set(students);}}catch(e){if(seq===this.seq)this.error.set(profesorError(e));}finally{if(seq===this.seq)this.loading.set(false);}}
 private async loadDetails(students:AlumnoResponse[]){const seq=++this.detailsSeq;this.details.set(new Map());if(!students.length){this.detailsLoading.set(false);return;}this.detailsLoading.set(true);try{const details=await Promise.all(students.map(async student=>{try{const [routine,sessions]=await Promise.all([firstValueFrom(this.api.active(student.id!)).catch(e=>{if(e.status===404)return null;throw e;}),firstValueFrom(this.api.sessions(student.id!))]);const last=sessions.filter(s=>s.finalizada && Number(s.seriesRealizadas)>0).map(s=>s.fechaEfectiva??s.fecha).filter((d):d is string=>!!d).sort().at(-1)??null;return [student.id!,{routine,last}] as const;}catch(e){if(isDenied(e))throw e;return [student.id!,{routine:null,last:null,unavailable:true}] as const;}}));if(seq===this.detailsSeq)this.details.set(new Map(details));}catch(e){if(seq===this.detailsSeq){this.students.set([]);this.details.set(new Map());this.error.set(profesorError(e));}}finally{if(seq===this.detailsSeq)this.detailsLoading.set(false);}}
}
