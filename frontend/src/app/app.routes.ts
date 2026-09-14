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
];
