import { Routes } from '@angular/router';
import { Resource } from './admin-api';
const browser = () => import('./browser').then((m) => m.Browser);
const detail = () => import('./detail').then((m) => m.Detail);
export const adminRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./dashboard').then((m) => m.Dashboard),
    title: 'Dashboard ADMIN · Impulso',
  },
  ...(['usuarios', 'alumnos', 'profesores'] as const).map((resource) => ({
    path: `${resource}/nuevo`,
    loadComponent: detail,
    data: { resource, creating: true },
    title: 'Crear cuenta · Impulso',
  })),
  ...[
    { parent: 'alumnos', sub: 'profesores', resource: 'profesores' },
    { parent: 'alumnos', sub: 'historial', resource: 'asignaciones' },
    { parent: 'alumnos', sub: 'sesiones', resource: 'sesiones' },
    { parent: 'profesores', sub: 'alumnos', resource: 'alumnos' },
    { parent: 'rutinas', sub: 'versiones', resource: 'rutinas' },
  ].map((data) => ({
    path: `${data.parent}/:id/${data.sub}`,
    loadComponent: browser,
    data,
    title: 'Consulta histórica · Impulso',
  })),
  ...(
    [
      'usuarios',
      'alumnos',
      'profesores',
      'rutinas',
      'asignaciones',
      'sesiones',
      'auditoria',
    ] as Resource[]
  ).map((resource) => ({
    path: resource,
    loadComponent: browser,
    data: { resource },
    title: `${resource} · ADMIN`,
  })),
  ...(
    ['usuarios', 'alumnos', 'profesores', 'rutinas', 'asignaciones', 'sesiones'] as Resource[]
  ).map((resource) => ({
    path: `${resource}/:id`,
    loadComponent: detail,
    data: { resource },
    title: 'Detalle · ADMIN',
  })),
];
