import { Component, input } from '@angular/core';
import { Progress } from '../profesor/progress';
import { PROGRESS_SOURCE, PROGRESS_ERROR } from '../profesor/progress-source';
import { AlumnoApi } from './alumno-api';
import { alumnoError } from './utils';
@Component({
  selector: 'app-student-progress',
  imports: [Progress],
  providers: [
    { provide: PROGRESS_SOURCE, useExisting: AlumnoApi },
    { provide: PROGRESS_ERROR, useValue: alumnoError },
  ],
  template: `<app-teacher-progress
    sessionBase="/alumno/sesiones"
    [period]="period()"
    [showDates]="showDates()"
  />`,
})
export class StudentProgress {
  readonly period = input<{ desde: string; hasta: string } | null>(null);
  readonly showDates = input(true);
}
