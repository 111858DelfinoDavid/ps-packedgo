import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './verify-email.component.html',
  styleUrls: ['./verify-email.component.css']
})
export class VerifyEmailComponent implements OnInit {
  isVerifying = true;
  isSuccess = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Obtener el token de la URL
    const token = this.route.snapshot.queryParamMap.get('token');
    
    if (!token) {
      this.isVerifying = false;
      this.isSuccess = false;
      this.errorMessage = 'Token de verificación no proporcionado';
      return;
    }

    // Verificar el email
    this.authService.verifyEmail(token).subscribe({
      next: (response) => {
        console.log('✅ Email verificado exitosamente:', response);
        this.isVerifying = false;
        this.isSuccess = true;
        this.successMessage = 'Tu email ha sido verificado exitosamente. Ahora puedes iniciar sesión.';
        
        // Redirigir al login después de 3 segundos
        setTimeout(() => {
          this.router.navigate(['/admin/login'], {
            queryParams: { emailVerified: 'true' }
          });
        }, 3000);
      },
      error: (error) => {
        console.error('❌ Error al verificar email:', error);
        this.isVerifying = false;
        this.isSuccess = false;
        
        // Determinar el mensaje de error específico
        const errorMsg = error.error?.message || error.message || '';
        if (errorMsg.includes('expired') || errorMsg.includes('expirado')) {
          this.errorMessage = 'El link de verificación ha expirado. Por favor, solicita un nuevo email de verificación.';
        } else if (errorMsg.includes('Invalid') || errorMsg.includes('inválido') || error.status === 400) {
          this.errorMessage = 'Este link de verificación ya fue usado o es inválido. Si ya verificaste tu email, puedes iniciar sesión directamente.';
        } else {
          this.errorMessage = 'Error al verificar el email. Por favor, intenta nuevamente o solicita un nuevo email de verificación.';
        }
        
        // Redirigir al login después de 5 segundos
        setTimeout(() => {
          this.router.navigate(['/admin/login']);
        }, 5000);
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/admin/login']);
  }
}
