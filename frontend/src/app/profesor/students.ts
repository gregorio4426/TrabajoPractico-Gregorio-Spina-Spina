import { Component,computed,inject,signal } from '@angular/core';
import { FormBuilder,ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProfesorApi } from './profesor-api';
import { AlumnoResponse } from '../admin/contracts';
import { LEVELS,OBJECTIVES } from '../admin/catalog';
import { profesorError,text } from './utils';
@Component({selector:'app-teacher-students',imports:[ReactiveFormsModule,RouterLink],template:`<section class="page-intro"><span class="eyebrow muted">TU EQUIPO</span><h1>Alumnos vinculados<span class="accent">.</span></h1><p>Solo personas con una vinculación vigente con tu perfil.</p></section><form class="admin-filters" [formGroup]="form" (ngSubmit)="load()"><label>Buscar alumno<input type="search" formControlName="q"></label><label>Nivel<select formControlName="nivel"><option value="">Todos</option>@for(level of levels;track level){<option [value]="level">{{text(level)}}</option>}</select></label><label>Objetivo<select formControlName="objetivo"><option value="">Todos</option>@for(goal of objectives;track goal){<option [value]="goal">{{text(goal)}}</option>}</select></label><button class="secondary" [disabled]="loading()">Recargar alumnos</button></form>
@if(error()){<p class="error-message" role="alert">{{error()}}</p>}@if(loading()){<p class="notice" role="status">Consultando alumnos vinculados…</p>}
@if(!loading()&&!error()){<p class="admin-help">{{filtered().length}} alumnos encontrados. Búsqueda y paginación local sobre la lista recibida del servidor.</p>@if(filtered().length){<div class="admin-table-wrap"><table class="admin-table"><thead><tr><th>Alumno</th><th>Email</th><th>Nivel</th><th>Objetivo</th><th>Acciones</th></tr></thead><tbody>@for(student of visible();track student.id){<tr><td>{{student.nombre}} {{student.apellido}}</td><td>{{student.email}}</td><td>{{text(student.nivel)}}</td><td>{{text(student.objetivo)}}</td><td><a [routerLink]="['/profesor/alumnos',student.id]">Ver alumno #{{student.id}}</a></td></tr>}</tbody></table></div><nav class="admin-pagination" aria-label="Páginas de alumnos"><span>Página {{page()+1}} de {{pages()}}</span><div><button class="secondary" (click)="page.set(page()-1)" [disabled]="page()===0">Anterior</button><button class="secondary" (click)="page.set(page()+1)" [disabled]="page()+1>=pages()">Siguiente</button></div></nav>}@else{<p class="admin-empty">No hay alumnos vinculados con estos filtros.</p>}}
`})
export class Students {
 private readonly api=inject(ProfesorApi);readonly data=signal<AlumnoResponse[]>([]);readonly error=signal('');readonly loading=signal(false);readonly levels=LEVELS;readonly objectives=OBJECTIVES;readonly text=text;readonly page=signal(0);
 readonly form=inject(FormBuilder).nonNullable.group({q:'',nivel:'',objetivo:''});private readonly criteria=signal(this.form.getRawValue());
 readonly filtered=computed(()=>{const c=this.criteria(),q=c.q.trim().toLocaleLowerCase('es');return this.data().filter(s=>(!q||`${s.nombre} ${s.apellido} ${s.email}`.toLocaleLowerCase('es').includes(q))&&(!c.nivel||s.nivel===c.nivel)&&(!c.objetivo||s.objetivo===c.objetivo));});readonly pages=computed(()=>Math.ceil(this.filtered().length/20));readonly visible=computed(()=>this.filtered().slice(this.page()*20,this.page()*20+20));private seq=0;
 constructor(){this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(()=>{this.criteria.set(this.form.getRawValue());this.page.set(0);});void this.load();}
 async load(){const seq=++this.seq;this.error.set('');this.loading.set(true);this.data.set([]);this.page.set(0);try{const result=await firstValueFrom(this.api.students());if(seq===this.seq)this.data.set(result);}catch(e){if(seq===this.seq)this.error.set(profesorError(e));}finally{if(seq===this.seq)this.loading.set(false);}}
}
