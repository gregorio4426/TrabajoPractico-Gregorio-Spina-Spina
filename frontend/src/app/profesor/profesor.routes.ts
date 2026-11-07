import { Routes } from '@angular/router';
export const profesorRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./dashboard').then((m) => m.TeacherDashboard),
    title: 'Profesor · Impulso',
  },
  {
    path: 'alumnos',
    loadComponent: () => import('./students').then((m) => m.Students),
    title: 'Mis alumnos · Impulso',
  },
  {
    path: 'alumnos/:id',
    loadComponent: () => import('./student').then((m) => m.Student),
    title: 'Alumno vinculado · Impulso',
  },
  {
    path: 'rutinas',
    loadComponent: () => import('./routines').then((m) => m.Routines),
    title: 'Rutinas · Profesor',
  },
  {
    path: 'rutinas/nueva',
    loadComponent: () => import('./routine-editor').then((m) => m.RoutineEditor),
    title: 'Crear rutina · Profesor',
  },
  {
    path: 'rutinas/:id/editar',
    loadComponent: () => import('./routine-editor').then((m) => m.RoutineEditor),
    title: 'Nueva versión · Profesor',
  },
  {
    path: 'rutinas/:id/versiones/:version',
    loadComponent: () => import('./routine').then((m) => m.Routine),
    title: 'Versión histórica · Profesor',
  },
  {
    path: 'rutinas/:id',
    loadComponent: () => import('./routine').then((m) => m.Routine),
    title: 'Rutina · Profesor',
  },
  {
    path: 'sesiones/:id',
    loadComponent: () => import('./session').then((m) => m.TeacherSession),
    title: 'Entrenamiento · Profesor',
  },
  {
    path: 'ejercicios',
    loadComponent: () => import('./exercises').then((m) => m.Exercises),
    title: 'Ejercicios · Profesor',
  },
  {
    path: 'perfil',
    loadComponent: () => import('./profile').then((m) => m.TeacherProfile),
    title: 'Mi perfil · Profesor',
  },
  {
    path: 'progreso',
    loadComponent: () => import('./progress').then((m) => m.Progress),
    title: 'Progreso · Profesor',
  },
];
