import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  // Clonar request y agregar token si existe
  const clonedReq = token 
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      })
    : req;

  // Manejar la respuesta y errores
  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Si es error 401 (No autorizado) o 403 (Forbidden), redirigir al login
      if (error.status === 401 || error.status === 403) {
        console.warn('Token expirado o inválido. Redirigiendo al login...');
        authService.logout();
        
        // Guardar la URL actual para redirigir después del login
        const currentUrl = router.url;
        if (!currentUrl.includes('/login') && !currentUrl.includes('/register')) {
          router.navigate(['/customer/login'], {
            queryParams: { returnUrl: currentUrl }
          });
        } else {
          router.navigate(['/customer/login']);
        }
      }
      
      return throwError(() => error);
    })
  );
};
