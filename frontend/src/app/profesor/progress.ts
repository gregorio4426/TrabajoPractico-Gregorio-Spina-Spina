import { Component, effect, inject, input, output, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ProfesorApi, StatPage } from './profesor-api';
import * as D from '../admin/contracts';
import { Chart, ChartPoint } from './chart';
import { isDenied, profesorError, text, validDates } from './utils';
@Component({selector:'app-teacher-progress',imports:[ReactiveFormsModule,Chart],templateUrl:'./progress.html'})
export class Progress {
 private readonly api=inject(ProfesorApi);readonly studentId=input<number|null>(null);readonly denied=output<void>();
 readonly form=inject(FormBuilder).nonNullable.group({desde:'',hasta:''});readonly text=text;
 readonly summary=signal<D.ResumenEstadistico|null>(null);readonly activity=signal<StatPage<D.ActividadDia>|null>(null);readonly exercises=signal<StatPage<D.EjercicioEstadistica>|null>(null);readonly evolution=signal<StatPage<D.EvolucionEjercicio>|null>(null);
 readonly selected=signal<number|null>(null);readonly loading=signal(false);readonly error=signal('');private seq=0;private evoSeq=0;private applied={desde:'',hasta:''};
 constructor(){effect(()=>{const id=this.studentId();untracked(()=>void this.load(id));});}
 async load(id=this.studentId()){
  const query=this.form.getRawValue();if(!validDates(query.desde,query.hasta)){this.error.set('La fecha desde debe ser anterior o igual a hasta.');return;}
  const seq=++this.seq;++this.evoSeq;this.loading.set(true);this.error.set('');this.summary.set(null);this.activity.set(null);this.exercises.set(null);this.evolution.set(null);this.selected.set(null);
  try{const[summary,activity,exercises]=await Promise.all([firstValueFrom(this.api.summary(id,query)),firstValueFrom(this.api.activity(id,{...query,pagina:0,tamanio:30})),firstValueFrom(this.api.exerciseStats(id,{...query,pagina:0,tamanio:20}))]);if(seq!==this.seq)return;this.applied={desde:summary.periodo?.desde??query.desde,hasta:summary.periodo?.hasta??query.hasta};this.summary.set(summary);this.activity.set(activity);this.exercises.set(exercises);}
  catch(e){if(seq===this.seq){this.error.set(profesorError(e));if(isDenied(e))this.denied.emit();}}
  finally{if(seq===this.seq)this.loading.set(false);}
 }
 async activityPage(page:number){this.loading.set(true);this.error.set('');const seq=this.seq;try{const value=await firstValueFrom(this.api.activity(this.studentId(),{...this.applied,pagina:page,tamanio:30}));if(seq===this.seq)this.activity.set(value);}catch(e){this.handle(e);}finally{this.loading.set(false);}}
 async exercisePage(page:number){this.loading.set(true);this.error.set('');const seq=this.seq;try{const value=await firstValueFrom(this.api.exerciseStats(this.studentId(),{...this.applied,pagina:page,tamanio:20}));if(seq===this.seq){this.exercises.set(value);this.selected.set(null);this.evolution.set(null);++this.evoSeq;}}catch(e){this.handle(e);}finally{this.loading.set(false);}}
 async chooseExercise(id:number,page=0){this.selected.set(id);this.evolution.set(null);this.loading.set(true);this.error.set('');const seq=++this.evoSeq;try{const value=await firstValueFrom(this.api.evolution(this.studentId(),id,{...this.applied,pagina:page,tamanio:30}));if(seq===this.evoSeq)this.evolution.set(value);}catch(e){if(seq===this.evoSeq)this.handle(e);}finally{if(seq===this.evoSeq)this.loading.set(false);}}
 private handle(e:unknown){this.error.set(profesorError(e));if(isDenied(e)){++this.seq;++this.evoSeq;this.summary.set(null);this.activity.set(null);this.exercises.set(null);this.evolution.set(null);this.denied.emit();}}
 activityPoints():ChartPoint[]{return(this.activity()?.contenido??[]).map(p=>({label:p.fecha??'Sin fecha',value:p.datos?.entrenamientosConDatosValidos}));}
 frequencyPoints():ChartPoint[]{const f=this.summary()?.frecuencia;return f?[{label:'Entrenamientos por semana',value:f.entrenamientosPorSemana},{label:'Días entrenados por semana',value:f.diasEntrenadosPorSemana}]:[];}
 evolutionPoints(metric:'pesoMaximo'|'repeticionesTotales'|'volumenRegistrado'):ChartPoint[]{return(this.evolution()?.contenido??[]).map(p=>({label:`${p.fechaEfectiva} · sesión #${p.sesionId} · ${p.nombreHistorico}`,value:p.datos?.[metric]}));}
}
