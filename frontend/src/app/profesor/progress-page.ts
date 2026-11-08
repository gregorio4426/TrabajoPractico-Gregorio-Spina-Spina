import { Component } from '@angular/core';
import { Progress } from './progress';
/** Route inputs must not overwrite the reusable chart's defaults or its authorization scope. */
@Component({
  selector: 'app-teacher-progress-page',
  imports: [Progress],
  template: '<app-teacher-progress />',
})
export class TeacherProgressPage {}
