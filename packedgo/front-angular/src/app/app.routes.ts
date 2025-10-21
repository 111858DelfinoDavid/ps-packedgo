import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './core/services/auth.service';

export const routes: Routes = [
  // Landing page
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent)
  },
  {
    path: 'home',
    redirectTo: '',
    pathMatch: 'full'
  },
  
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
  {
    path: 'admin/consumptions',
    loadComponent: () => import('./features/admin/consumptions-management/consumptions-management.component').then(m => m.ConsumptionsManagementComponent),
    canActivate: [adminGuard]
  },
  
  // Customer routes (protected)
  {
    path: 'customer/dashboard',
    loadComponent: () => import('./features/customer/customer-dashboard/customer-dashboard.component').then(m => m.CustomerDashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'customer/events/:id',
    loadComponent: () => import('./features/customer/event-detail/event-detail.component').then(m => m.EventDetailComponent),
    canActivate: [authGuard]
  },
  
  // Wildcard route
  { path: '**', redirectTo: '/admin/login' }
];

