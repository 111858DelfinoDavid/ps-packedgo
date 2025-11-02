import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Si es la ruta de checkout con sessionId, permitir acceso temporalmente
  // El componente verificará la validez de la sesión
  if (state.url.includes('/customer/checkout') && route.queryParams['sessionId']) {
    return true;
  }

  if (authService.isAuthenticated()) {
    return true;
  }

  // Redirect to customer login page
  router.navigate(['/customer/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
