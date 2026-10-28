import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';

/**
 * Rutas de la aplicacion. Las vistas de cada dominio se cargan de forma
 * diferida; los layouts agrupan las pantallas publicas y las privadas.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'app/dashboard',
  },
  {
    path: 'auth',
    loadComponent: () =>
      import('./layouts/auth-layout/pz-auth-layout').then((m) => m.PzAuthLayout),
    children: [
      {
        path: 'login',
        title: 'Iniciar sesion | PowerZone',
        canActivate: [guestGuard],
        loadComponent: () => import('./pages/auth/login/pz-login').then((m) => m.PzLogin),
      },
      {
        path: 'register',
        title: 'Crear cuenta | PowerZone',
        canActivate: [guestGuard],
        loadComponent: () =>
          import('./pages/auth/register/pz-register').then((m) => m.PzRegister),
      },
      {
        path: 'forgot-password',
        title: 'Recuperar contrasena | PowerZone',
        loadComponent: () =>
          import('./pages/auth/forgot-password/pz-forgot-password').then(
            (m) => m.PzForgotPassword,
          ),
      },
      {
        path: 'reset-password',
        title: 'Nueva contrasena | PowerZone',
        loadComponent: () =>
          import('./pages/auth/reset-password/pz-reset-password').then(
            (m) => m.PzResetPassword,
          ),
      },
      { path: '', pathMatch: 'full', redirectTo: 'login' },
    ],
  },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () => import('./layouts/shell/pz-shell').then((m) => m.PzShell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Panel | PowerZone',
        loadComponent: () =>
          import('./pages/dashboard/pz-dashboard').then((m) => m.PzDashboard),
      },
      {
        path: 'routines',
        title: 'Rutinas | PowerZone',
        loadComponent: () =>
          import('./pages/routines/pz-routines').then((m) => m.PzRoutines),
      },
      {
        path: 'members',
        title: 'Miembros | PowerZone',
        loadComponent: () => import('./pages/members/pz-members').then((m) => m.PzMembers),
      },
      {
        path: 'settings',
        title: 'Ajustes | PowerZone',
        loadComponent: () =>
          import('./pages/settings/pz-settings').then((m) => m.PzSettings),
      },
    ],
  },
  {
    path: '**',
    title: 'Pagina no encontrada | PowerZone',
    loadComponent: () => import('./pages/not-found/pz-not-found').then((m) => m.PzNotFound),
  },
];
