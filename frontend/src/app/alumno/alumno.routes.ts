import { Routes } from '@angular/router';
export const alumnoRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./dashboard').then((m) => m.StudentDashboard),
    title: 'Mi entrenamiento · Impulso',
  },
  {
    path: 'rutina',
    loadComponent: () => import('./routines').then((m) => m.StudentRoutines),
    title: 'Mi rutina activa · Impulso',
  },
  {
    path: 'rutinas',
    loadComponent: () => import('./routines').then((m) => m.StudentRoutines),
    data: { history: true },
    title: 'Historial de rutinas · Impulso',
  },
  {
    path: 'sesiones',
    loadComponent: () => import('./sessions').then((m) => m.Sessions),
    title: 'Mis sesiones · Impulso',
  },
  {
    path: 'sesiones/:id',
    loadComponent: () => import('./session').then((m) => m.StudentSession),
    title: 'Mi entrenamiento · Impulso',
  },
  {
    path: 'perfil',
    loadComponent: () => import('./profile').then((m) => m.StudentProfile),
    title: 'Mi perfil · Alumno',
  },
  {
    path: 'profesores',
    loadComponent: () => import('./professors').then((m) => m.Professors),
    title: 'Mis profesores · Alumno',
  },
  {
    path: 'progreso',
    loadComponent: () => import('./progress').then((m) => m.StudentProgress),
    title: 'Mi progreso · Impulso',
  },
];
