import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-customer-login',
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './customer-login.component.html',
  styleUrl: './customer-login.component.css'
})
export class CustomerLoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loginForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  showPassword = false;
  showResendEmailButton = false;
  resendingEmail = false;
  resendSuccess = false;

  constructor() {
    this.loginForm = this.fb.group({
      document: ['', [Validators.required, Validators.pattern(/^\d+$/)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.showResendEmailButton = false;
    this.resendSuccess = false;

    // Convert document string to number
    const credentials = {
      document: Number(this.loginForm.value.document),
      password: this.loginForm.value.password
    };

    this.authService.customerLogin(credentials).subscribe({
      next: (response) => {
        console.log('Login exitoso:', response);
        
        // Verificar si el email está verificado
        if (response.user.isEmailVerified === false) {
          this.errorMessage = 'Tu correo electrónico aún no ha sido verificado. Por favor, revisa tu bandeja de entrada.';
          this.showResendEmailButton = true;
          this.isLoading = false;
          return;
        }
        
        this.router.navigate(['/customer/dashboard']);
      },
      error: (error) => {
        console.error('Error en login:', error);
        this.errorMessage = error.error?.message || 'Credenciales inválidas. Por favor, intenta nuevamente.';
        this.isLoading = false;
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  resendVerificationEmail(): void {
    this.resendingEmail = true;
    this.resendSuccess = false;
    this.errorMessage = '';

    const email = this.authService.getCurrentUser()?.email;
    if (!email) {
      this.errorMessage = 'No se pudo obtener el email del usuario.';
      this.resendingEmail = false;
      return;
    }

    this.authService.resendVerificationEmail(email).subscribe({
      next: () => {
        this.resendSuccess = true;
        this.resendingEmail = false;
        this.errorMessage = '✓ Correo de verificación enviado exitosamente. Revisa tu bandeja de entrada.';
        this.showResendEmailButton = false;
        
        // Ocultar mensaje de éxito después de 5 segundos
        setTimeout(() => {
          this.errorMessage = '';
        }, 5000);
      },
      error: (error) => {
        console.error('Error al reenviar correo:', error);
        this.errorMessage = error.error?.message || 'Error al enviar el correo. Por favor, intenta nuevamente.';
        this.resendingEmail = false;
      }
    });
  }

  // Getters para validación en template
  get document() {
    return this.loginForm.get('document');
  }

  get password() {
    return this.loginForm.get('password');
  }
}
