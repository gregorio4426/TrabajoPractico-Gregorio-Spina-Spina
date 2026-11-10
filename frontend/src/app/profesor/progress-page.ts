import { Component } from '@angular/core';
import { Progress } from './progress';
/** Route inputs must not overwrite the reusable chart's defaults or its authorization scope. */
@Component({
  selector: 'app-teacher-progress-page',
  imports: [Progress],
  template:
    '<section class="page-intro"><h1>Progreso de mis alumnos.</h1><p>Consultá el entrenamiento registrado de tus alumnos vinculados.</p></section><app-teacher-progress />',
})
export class TeacherProgressPage {}
