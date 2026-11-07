import { Resource } from './admin-api';
export interface Filter { key: string; label: string; type?: string; options?: readonly string[]; }
export interface Catalog { label: string; singular: string; columns: [string,string][]; filters: Filter[]; orders: string[]; }
const q:Filter={key:'q',label:'Buscar nombre o email'};
const active:Filter={key:'activo',label:'Cuenta activa',options:['true','false']};
const current:Filter={key:'vigente',label:'Perfil vigente',options:['true','false']};
const noAccount:Filter={key:'sinCuenta',label:'Sin cuenta',options:['true','false']};
const studentId:Filter={key:'alumnoId',label:'ID del alumno',type:'number'};
const professorId:Filter={key:'profesorId',label:'ID del profesor',type:'number'};
const routineId:Filter={key:'rutinaId',label:'ID de familia de rutina',type:'number'};
export const ROLES=['ADMIN','PROFESOR','ALUMNO'] as const;
export const LEVELS=['PRINCIPIANTE','INTERMEDIO','AVANZADO'] as const;
export const OBJECTIVES=['PERDER_PESO','GANAR_MASA_MUSCULAR','MEJORAR_RESISTENCIA'] as const;
export const CATALOG:Record<Resource,Catalog>={
 usuarios:{label:'Usuarios',singular:'Usuario',columns:[['id','ID'],['email','Email de acceso'],['rol','Rol'],['activo','Activo']],filters:[q,{key:'rol',label:'Rol',options:ROLES},active],orders:['id','email','rol','activo']},
 alumnos:{label:'Alumnos',singular:'Alumno',columns:[['perfil.id','ID'],['perfil.nombre','Nombre'],['perfil.apellido','Apellido'],['perfil.email','Email'],['perfil.nivel','Nivel'],['perfilVigente','Vigente'],['cuenta.activo','Cuenta activa']],filters:[q,active,current,noAccount,{key:'nivel',label:'Nivel',options:LEVELS},{key:'objetivo',label:'Objetivo',options:OBJECTIVES},professorId],orders:['id','nombre','apellido','email','nivel','objetivo']},
 profesores:{label:'Profesores',singular:'Profesor',columns:[['perfil.id','ID'],['perfil.nombre','Nombre'],['perfil.apellido','Apellido'],['perfil.email','Email'],['perfil.especialidad','Especialidad'],['perfilVigente','Vigente'],['cuenta.activo','Cuenta activa']],filters:[q,active,current,noAccount,{key:'especialidad',label:'Especialidad'},studentId],orders:['id','nombre','apellido','email','especialidad']},
 rutinas:{label:'Rutinas',singular:'Rutina',columns:[['id','ID'],['nombre','Nombre'],['numeroVersion','Versión'],['profesor','Autor'],['archivada','Archivada']],filters:[{key:'q',label:'Buscar nombre o descripción'},professorId,{key:'archivada',label:'Archivada',options:['true','false']}],orders:['id','nombre','capturadaEn']},
 asignaciones:{label:'Asignaciones',singular:'Asignación',columns:[['id','ID'],['alumnoNombre','Alumno'],['rutinaNombre','Rutina'],['numeroVersion','Versión'],['activa','Activa'],['fechaInicio','Desde'],['fechaFin','Hasta']],filters:[studentId,professorId,routineId,active],orders:['id','fechaInicio','fechaFin']},
 sesiones:{label:'Sesiones',singular:'Sesión',columns:[['id','ID'],['alumnoId','Alumno'],['rutinaNombre','Rutina'],['fechaEfectiva','Fecha efectiva'],['finalizada','Finalizada'],['diaCumplido','Día cumplido']],filters:[studentId,professorId,routineId,{key:'finalizada',label:'Finalizada',options:['true','false']},{key:'desde',label:'Desde',type:'date'},{key:'hasta',label:'Hasta',type:'date'}],orders:['id','fecha']},
 auditoria:{label:'Auditoría administrativa',singular:'Evento',columns:[['id','ID'],['actorId','Actor'],['usuarioId','Cuenta afectada'],['accion','Acción'],['motivo','Motivo'],['creadaEn','Fecha']],filters:[{key:'usuarioId',label:'ID de cuenta afectada',type:'number'}],orders:['id','creadaEn']},
};
export function label(key:string):string {return ({id:'ID',cuenta:'Cuenta de acceso',perfil:'Perfil',perfilVigente:'Perfil vigente',revision:'Revisión de seguridad',antes:'Estado anterior',despues:'Estado posterior',activo:'Cuenta activa',activa:'Activa',diaCumplido:'Día cumplido',finalizada:'Finalizada',motivo:'Motivo',creadaEn:'Fecha del evento',fechaEfectiva:'Fecha efectiva',rutinaVersionId:'ID exacto de versión',numeroVersion:'Número de versión',actorId:'ID del actor',usuarioId:'ID de cuenta afectada',accion:'Acción',emailContacto:'Email de contacto'} as Record<string,string>)[key] ?? key.replace(/([a-z])([A-Z])/g,'$1 $2').replace(/^./,c=>c.toUpperCase());}
