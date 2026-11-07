import { Routes } from '@angular/router';
import { authGuard, entryGuard, guestGuard, roleGuard } from './core/auth.guards';
export const routes: Routes = [
  { path: 'login', canActivate: [guestGuard], loadComponent: () => import('./auth/login').then(m => m.Login), title: 'Ingresar · Impulso' },
  ...(['ADMIN', 'PROFESOR', 'ALUMNO'] as const).map(role => ({
    path: role.toLowerCase(), canActivate: [authGuard, roleGuard], data: { role },
    loadComponent: () => import('./layout/shell').then(m => m.Shell),
    children: role === 'ADMIN' ? [{path:'',loadComponent:()=>import('./admin/dashboard').then(m=>m.Dashboard),title:'Administración · Impulso'}] : [{ path: '', loadComponent: () => import('./home/home').then(m => m.Home), title: `${role.charAt(0) + role.slice(1).toLowerCase()} · Impulso` }],
  })),
  { path: '', pathMatch: 'full', canActivate: [entryGuard], loadComponent: () => import('./auth/login').then(m => m.Login) },
  { path: '**', canActivate: [entryGuard], loadComponent: () => import('./auth/login').then(m => m.Login) },
];
