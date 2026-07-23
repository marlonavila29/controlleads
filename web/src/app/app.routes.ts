import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./pages/forgot-password/forgot-password').then((m) => m.ForgotPassword)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./pages/reset-password/reset-password').then((m) => m.ResetPassword)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard)
  },
  {
    path: 'leads',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/leads/leads').then((m) => m.Leads)
  },
  {
    path: 'leads/new',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/lead-form/lead-form').then((m) => m.LeadForm)
  },
  {
    path: 'leads/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/lead-detail/lead-detail').then((m) => m.LeadDetail)
  },
  {
    path: 'board',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/board/board').then((m) => m.Board)
  },
  {
    path: 'broadcasts',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/broadcasts/broadcasts').then((m) => m.Broadcasts)
  },
  {
    path: 'users',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./pages/users/users').then((m) => m.Users)
  },
  {
    path: 'catalogs',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./pages/catalogs/catalogs').then((m) => m.Catalogs)
  },
  { path: '**', redirectTo: '' }
];
