import { Component,computed,inject,signal,viewChild } from '@angular/core';
import { ActivatedRoute,RouterLink } from '@angular/router';
import { FormBuilder,ReactiveFormsModule,Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { ProfesorApi } from './profesor-api';
import { AuthService } from '../core/auth.service';
import { Confirm } from './confirm';
import { RecordView } from '../admin/record-view';
import * as D from '../admin/contracts';
import { positiveInteger,profesorError } from './utils';
@Component({selector:'app-teacher-routine',imports:[RouterLink,ReactiveFormsModule,Confirm,RecordView],templateUrl:'./routine.html'})
export class Routine {
 private readonly api=inject(ProfesorApi);private readonly route=inject(ActivatedRoute);private readonly auth=inject(AuthService);private readonly confirmation=viewChild.required<Confirm>('confirmation');
 readonly data=signal<D.RutinaResponse|null>(null);readonly versions=signal<D.RutinaResponse[]>([]);readonly students=signal<D.AlumnoResponse[]>([]);readonly selectedVersion=signal<number|null>(null);readonly id=signal(0);readonly loading=signal(false);readonly pending=signal(false);readonly error=signal('');readonly success=signal('');readonly action=signal<'assign'|'archive'>('assign');private seq=0;
 readonly own=computed(()=>this.data()?.profesorId===this.auth.session()?.perfilId);readonly form=inject(FormBuilder).nonNullable.group({alumnoId:['',[Validators.required,positiveInteger]]});
 constructor(){const saved=this.route.snapshot.queryParamMap.get('guardada');if(saved)this.success.set('Rutina guardada correctamente. Versión '+saved+'.');this.route.paramMap.pipe(takeUntilDestroyed()).subscribe(p=>{this.id.set(Number(p.get('id')));this.selectedVersion.set(p.has('version')?Number(p.get('version')):null);void this.load();});}
 async load(){const seq=++this.seq;this.error.set('');this.loading.set(true);this.data.set(null);this.versions.set([]);this.students.set([]);try{if(!Number.isSafeInteger(this.id())||this.id()<1||this.selectedVersion()!==null&&(!Number.isSafeInteger(this.selectedVersion())||this.selectedVersion()!<1))throw new Error('ID no válido');const[data,versions,students]=await Promise.all([firstValueFrom(this.selectedVersion()===null?this.api.routine(this.id()):this.api.version(this.id(),this.selectedVersion()!)),firstValueFrom(this.api.versions(this.id())),firstValueFrom(this.api.students())]);if(seq===this.seq){this.data.set(data);this.versions.set(versions);this.students.set(students);}}catch(e){if(seq===this.seq)this.error.set(profesorError(e));}finally{if(seq===this.seq)this.loading.set(false);}}
 ask(action:'assign'|'archive'){this.form.markAllAsTouched();if(this.pending()||!this.own()||this.data()?.archivada||(action==='assign'&&this.form.invalid))return;this.action.set(action);this.confirmation().open();}
 async save(){if(this.pending())return;this.pending.set(true);this.error.set('');this.success.set('');try{if(this.action()==='archive'){await firstValueFrom(this.api.archive(this.id()));await this.load();if(!this.error())this.success.set('Rutina archivada. Las versiones y los entrenamientos se conservan.');}else{const result=await firstValueFrom(this.api.assign(Number(this.form.controls.alumnoId.value),this.id()));this.success.set(`Asignación #${result.id} guardada: versión ${result.numeroVersion}. La asignación anterior del alumno se desactiva.`);}}catch(e){this.error.set(profesorError(e));}finally{this.pending.set(false);}}
}
