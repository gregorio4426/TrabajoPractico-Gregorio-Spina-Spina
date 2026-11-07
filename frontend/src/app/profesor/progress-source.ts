import { InjectionToken,inject } from '@angular/core';
import { ProfesorApi } from './profesor-api';
import { profesorError } from './utils';
export type ProgressSource=Pick<ProfesorApi,'summary'|'activity'|'exerciseStats'|'evolution'>;
export const PROGRESS_SOURCE=new InjectionToken<ProgressSource>('PROGRESS_SOURCE',{factory:()=>inject(ProfesorApi)});
export const PROGRESS_ERROR=new InjectionToken<(error:unknown)=>string>('PROGRESS_ERROR',{factory:()=>profesorError});
