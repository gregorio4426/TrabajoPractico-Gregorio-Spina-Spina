import { Component, ElementRef, OnInit, inject, input, output, signal, viewChild } from '@angular/core';
import { AbstractControl, FormControl, FormRecord, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { firstValueFrom, Observable } from 'rxjs';
import { AdminApi, Page, People } from './admin-api';
import { LEVELS, OBJECTIVES, ROLES } from './catalog';
import { adminError, at, record } from './admin-utils';
import { AuthService } from '../core/auth.service';
import * as D from './contracts';
export type Operation='create'|'profile'|'state'|'role'|'email'|'account'|'link'|'unlink'|'replace';
interface Field { key:string; label:string; type?:string; required?:boolean; max?:number; options?:readonly string[]; }
const notBlank:ValidatorFn=(control:AbstractControl)=>String(control.value??'').trim()?null:{blank:true};
const bytePassword:ValidatorFn=control=>new TextEncoder().encode(String(control.value??'')).length<=72?null:{bytes:true};
@Component({selector:'app-admin-editor',imports:[ReactiveFormsModule],templateUrl:'./editor.html'})
export class Editor implements OnInit {
 readonly operation=input.required<Operation>();readonly type=input.required<People>();readonly id=input(0);readonly initial=input<unknown>(null);
 readonly saved=output<void>();readonly cancelled=output<void>();
 private readonly api=inject(AdminApi);private readonly auth=inject(AuthService);
 readonly lookup = new FormControl('', {nonNullable:true});
 readonly lookupData=signal<Page<D.ProfesorVista>|null>(null);readonly lookupLoading=signal(false);
 readonly lookupScope=signal<'actuales'|'disponibles'>('disponibles');
 readonly form=new FormRecord<FormControl<string>>({});readonly fields=signal<Field[]>([]);readonly pending=signal(false);readonly error=signal('');
 private readonly dialog=viewChild.required<ElementRef<HTMLDialogElement>>('confirmationDialog');
 readonly titles:Record<Operation,string>={create:'Crear cuenta y perfil',profile:'Editar perfil',state:'Cambiar estado de cuenta',role:'Cambiar rol',email:'Cambiar email de acceso',account:'Dar acceso a perfil sin cuenta',link:'Vincular profesor',unlink:'Desvincular profesor',replace:'Reemplazar profesor'};
 ngOnInit(){this.configure(true);if(['link','unlink','replace'].includes(this.operation())){this.lookupScope.set(this.operation()==='link'?'disponibles':'actuales');void this.searchProfessors();}}
 async searchProfessors(page=0){
  this.lookupLoading.set(true);this.error.set('');
  try{const current=this.lookupScope()==='actuales';this.lookupData.set(await firstValueFrom(this.api.get<Page<D.ProfesorVista>>(current?`alumnos/${this.id()}/profesores`:'profesores',{q:this.lookup.value,pagina:page,tamanio:10,...(!current?{activo:true,vigente:true}:{})})));}
  catch(error){this.error.set(adminError(error));this.lookupData.set(null);}
  finally{this.lookupLoading.set(false);}
 }
 chooseProfessor(professor:D.ProfesorVista){
  const key=this.operation()==='replace'?(this.lookupScope()==='actuales'?'profesorAnteriorId':'profesorNuevoId'):'profesorId';
  this.form.controls[key]?.setValue(String(professor.perfil?.id??''));
 }
 switchScope(scope:'actuales'|'disponibles'){this.lookupScope.set(scope);void this.searchProfessors();}
 private field(key:string,label:string,type='text',required=true,max?:number,options?:readonly string[]):Field{return {key,label,type,required,max,options};}
 private configure(first=false){
  const mode=this.operation();const value=this.form.getRawValue();const user=record(this.type()==='usuarios'?this.initial():at(this.initial(),'cuenta'));
  const role=value['rol'] ?? String(user['rol']??(this.type()==='profesores'?'PROFESOR':this.type()==='alumnos'?'ALUMNO':'ADMIN'));
  const fields:Field[]=[];
  if(mode==='create'||mode==='email')fields.push(this.field('email','Email de acceso','email',true,150));
  if(mode==='create'||mode==='account')fields.push(this.field('password','Contraseña inicial','password'));
  if((mode==='create'&&this.type()==='usuarios')||mode==='role')fields.push(this.field('rol','Rol','select',true,undefined,ROLES));
  if(mode==='state')fields.push(this.field('activo','Cuenta activa','select',true,undefined,['true','false']));
  const profileRole=mode==='profile'?(this.type()==='alumnos'?'ALUMNO':'PROFESOR'):role;
  const existingTarget=mode==='role' && (profileRole==='ALUMNO'?user['alumnoId']:profileRole==='PROFESOR'?user['profesorId']:null);
  if(mode==='profile'||((mode==='create'||mode==='role')&&profileRole!=='ADMIN'&&!existingTarget)){
   fields.push(this.field('nombre','Nombre','text',true,20),this.field('apellido','Apellido','text',true,20));
   if(profileRole==='ALUMNO')fields.push(this.field('edad','Edad','number'),this.field('fechaNacimiento','Fecha de nacimiento','date',false),this.field('nivel','Nivel','select',true,undefined,LEVELS),this.field('objetivo','Objetivo','select',true,undefined,OBJECTIVES));
   else fields.push(this.field('especialidad','Especialidad','text',true,100));
   fields.push(this.field('emailContacto','Email de contacto','email',false,150));
  }
  if(mode==='link'||mode==='unlink')fields.push(this.field('profesorId','ID del profesor','number'));
  if(mode==='replace')fields.push(this.field('profesorAnteriorId','ID del profesor anterior','number'),this.field('profesorNuevoId','ID del nuevo profesor','number'));
  if(!['create','profile'].includes(mode))fields.push(this.field('motivo','Motivo de la operación','textarea',true,500));
  const profile=record(at(this.initial(),'perfil'));
  for(const control of Object.values(this.form.controls))control.disable({emitEvent:false});
  for(const field of fields){
   const validators:ValidatorFn[]=[];
   if(field.required)validators.push(Validators.required,notBlank);
   if(field.max)validators.push(Validators.maxLength(field.max));
   if(field.type==='email')validators.push(Validators.email);
   if(field.type==='number')validators.push(Validators.min(1),Validators.pattern(/^\d+$/),c=>Number.isSafeInteger(Number(c.value))?null:{integer:true});
   if(field.key==='edad')validators.push(Validators.max(150));
   if(field.key==='password')validators.push(Validators.minLength(role==='ADMIN'&&mode==='create'?12:8),bytePassword);
   let control=this.form.controls[field.key];
   if(!control){let initial=String(profile[field.key]??user[field.key]??'');if(field.key==='rol')initial=role;if(field.key==='activo')initial=String(!(user['activo']===true));control=new FormControl(initial,{nonNullable:true});this.form.addControl(field.key,control);}
   control.setValidators(validators);control.enable({emitEvent:false});control.updateValueAndValidity({emitEvent:false});
  }
  this.fields.set(fields);
  if(first&&this.form.controls['rol'])this.form.controls['rol'].valueChanges.subscribe(()=>this.configure());
 }
 confirm(){
  if(this.pending())return;this.form.markAllAsTouched();this.error.set('');
  if(this.form.invalid){this.error.set('Revisá los campos señalados. Contraseña: mínimo 8 caracteres (12 para ADMIN), máximo 72 bytes UTF-8.');return;}
  const v=this.form.getRawValue();
  if(this.fields().some(f=>f.key==='fechaNacimiento')&&v['fechaNacimiento']){
   const birth=v['fechaNacimiento'];const now=new Date();const today=now.toISOString().slice(0,10);
   const age=now.getUTCFullYear()-Number(birth.slice(0,4))-(today.slice(5)<birth.slice(5)?1:0);
   if(birth>=today||age!==Number(v['edad'])){this.error.set('La fecha de nacimiento debe ser pasada y concordar con la edad.');return;}
  }
  if(this.operation()==='replace'&&v['profesorAnteriorId']===v['profesorNuevoId']){this.error.set('Seleccioná un profesor diferente para el reemplazo.');return;}
  this.dialog().nativeElement.showModal();
 }
 cancelConfirm(){this.dialog().nativeElement.close();}
 private alumno(v:Record<string,string>):D.AlumnoDatos{return {nombre:v['nombre']!.trim(),apellido:v['apellido']!.trim(),edad:Number(v['edad']),fechaNacimiento:v['fechaNacimiento']||null,nivel:v['nivel'] as D.AlumnoDatos['nivel'],objetivo:v['objetivo'] as D.AlumnoDatos['objetivo'],emailContacto:v['emailContacto']?.trim()||null};}
 private profesor(v:Record<string,string>):D.ProfesorDatos{return {nombre:v['nombre']!.trim(),apellido:v['apellido']!.trim(),especialidad:v['especialidad']!.trim(),emailContacto:v['emailContacto']?.trim()||null};}
 async submit(){
  if(this.pending())return;this.cancelConfirm();this.pending.set(true);this.error.set('');
  const v=this.form.getRawValue();const op=this.operation();const type=this.type();const id=this.id();
  const user=record(type==='usuarios'?this.initial():at(this.initial(),'cuenta'));const revision=Number(user['revision']);const motivo=v['motivo']?.trim()??'';const rol=v['rol'] as D.CrearUsuario['rol'];
  let request:Observable<unknown>;
  try {
   if(op==='create'){
    const email=v['email']!.trim().toLowerCase(),password=v['password']!;
    if(type==='alumnos')request=this.api.createAlumno({email,password,perfil:this.alumno(v)});
    else if(type==='profesores')request=this.api.createProfesor({email,password,perfil:this.profesor(v)});
    else request=this.api.createUser({email,password,rol,...(rol==='ALUMNO'?{alumno:this.alumno(v)}:rol==='PROFESOR'?{profesor:this.profesor(v)}:{})});
   }else if(op==='profile')request=type==='alumnos'?this.api.editAlumno(id,this.alumno(v)):this.api.editProfesor(id,this.profesor(v));
   else if(op==='state')request=this.api.state(id,{activo:v['activo']==='true',revision,motivo});
   else if(op==='email')request=this.api.email(id,{email:v['email']!.trim().toLowerCase(),revision,motivo});
   else if(op==='role')request=this.api.role(id,{rol,revision,motivo,...(rol==='ALUMNO'&&!user['alumnoId']?{alumno:this.alumno(v)}:rol==='PROFESOR'&&!user['profesorId']?{profesor:this.profesor(v)}:{})});
   else if(op==='account')request=this.api.account(type as 'alumnos'|'profesores',id,{password:v['password']!,motivo});
   else if(op==='link')request=this.api.link(id,Number(v['profesorId']),{motivo});
   else if(op==='unlink')request=this.api.unlink(id,Number(v['profesorId']),{motivo});
   else request=this.api.replace(id,{profesorAnteriorId:Number(v['profesorAnteriorId']),profesorNuevoId:Number(v['profesorNuevoId']),motivo});
   await firstValueFrom(request);
   const self=user['email']===this.auth.session()?.email;
   const changed=(op==='state'&&(v['activo']==='true')!==user['activo'])||(op==='email'&&v['email']!.trim().toLowerCase()!==user['email'])||(op==='role'&&rol!==user['rol']);
   this.form.controls['password']?.reset('');
   if(self&&changed)this.auth.logout('Actualizaste tu propia cuenta. Iniciá sesión nuevamente con sus permisos actuales.');
   this.saved.emit();
  }catch(error){this.error.set(adminError(error));}
  finally{this.pending.set(false);}
 }
}
