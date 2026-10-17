import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/home/home').then((m) => m.Home),
  },
  {
    path: 'puntos-limpios',
    loadComponent: () =>
      import('./pages/puntos-limpios/puntos-limpios').then((m) => m.PuntosLimpios),
    canActivate: [authGuard],
  },
  {
    path: 'estadisticas',
    loadComponent: () => import('./pages/estadisticas/estadisticas').then((m) => m.Estadisticas),
    canActivate: [authGuard],
  },
  {
    path: 'tickets',
    loadComponent: () => import('./pages/tickets/tickets').then((m) => m.Tickets),
    canActivate: [authGuard],
  },
  {
    path: 'admin-rabbitmq',
    loadComponent: () => import('./pages/admin-rabbitmq/admin-rabbitmq').then((m) => m.AdminRabbitmq),
    canActivate: [authGuard],
  },
];
