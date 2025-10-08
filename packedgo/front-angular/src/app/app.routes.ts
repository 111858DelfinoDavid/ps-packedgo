import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  // Redirect root to admin login by default
  { path: '', redirectTo: '/admin/login', pathMatch: 'full' },
  
  // Auth routes - Admin
  {
    path: 'admin/login',
    loadComponent: () => import('./features/auth/admin-login/admin-login.component').then(m => m.AdminLoginComponent)
  },
  {
    path: 'admin/register',
    loadComponent: () => import('./features/auth/admin-register/admin-register.component').then(m => m.AdminRegisterComponent)
  },
  
  // Auth routes - Customer
  {
    path: 'customer/login',
    loadComponent: () => import('./features/auth/customer-login/customer-login.component').then(m => m.CustomerLoginComponent)
  },
  {
    path: 'customer/register',
    loadComponent: () => import('./features/auth/customer-register/customer-register.component').then(m => m.CustomerRegisterComponent)
  },
  
  // Admin routes (protected)
  {
    path: 'admin/dashboard',
    loadComponent: () => import('./features/admin/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'admin/events',
    loadComponent: () => import('./features/admin/events-management/events-management.component').then(m => m.EventsManagementComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'admin/categories',
    loadComponent: () => import('./features/admin/categories-management/categories-management.component').then(m => m.CategoriesManagementComponent),
    canActivate: [adminGuard]
  },
  
  // Customer routes (protected)
  {
    path: 'customer/dashboard',
    loadComponent: () => import('./features/customer/customer-dashboard/customer-dashboard.component').then(m => m.CustomerDashboardComponent),
    canActivate: [authGuard]
  },
  
  // Wildcard route
  { path: '**', redirectTo: '/admin/login' }
];

