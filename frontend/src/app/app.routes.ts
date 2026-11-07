import { profesorRoutes } from './profesor/profesor.routes';
import { adminRoutes } from './admin/admin.routes';
import { Routes } from '@angular/router';
import { authGuard, entryGuard, guestGuard, roleGuard } from './core/auth.guards';
export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./auth/login').then((m) => m.Login),
    title: 'Ingresar · Impulso',
  },
  ...(['ADMIN', 'PROFESOR', 'ALUMNO'] as const).map((role) => ({
    path: role.toLowerCase(),
    canActivate: [authGuard, roleGuard],
    data: { role },
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children:
      role === 'ADMIN'
        ? adminRoutes
        : role === 'PROFESOR'
          ? profesorRoutes
          : [{path:'',loadComponent:()=>import('./alumno/dashboard').then(m=>m.StudentDashboard),title:'Alumno · Impulso'},{path:'progreso',loadComponent:()=>import('./alumno/progress').then(m=>m.StudentProgress),title:'Mi progreso · Impulso'}],
  })),
  {
    path: '',
    pathMatch: 'full',
    canActivate: [entryGuard],
    loadComponent: () => import('./auth/login').then((m) => m.Login),
  },
  {
    path: '**',
    canActivate: [entryGuard],
    loadComponent: () => import('./auth/login').then((m) => m.Login),
  },
];
