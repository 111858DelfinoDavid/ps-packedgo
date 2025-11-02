import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { interval, Subscription, firstValueFrom } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { OrderService } from '../../../core/services/order.service';
import { PaymentService } from '../../../core/services/payment.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { 
  MultiOrderCheckoutResponse, 
  PaymentGroup 
} from '../../../shared/models/order.model';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.css']
})
export class CheckoutComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private orderService = inject(OrderService);
  private paymentService = inject(PaymentService);
  private cartService = inject(CartService);
  private authService = inject(AuthService);

  // Estado del checkout
  checkoutResponse: MultiOrderCheckoutResponse | null = null;
  paymentGroups: PaymentGroup[] = [];
  sessionId: string = '';
  expiresAt: Date | null = null;
  timeRemaining: number = 0; // en segundos
  
  // Estado de UI
  isLoading = true;
  errorMessage = '';
  isGeneratingPayments = false;
  paymentReturnMessage: string = ''; // Mensaje de retorno de MercadoPago
  paymentReturnType: 'success' | 'error' | 'pending' | '' = ''; // Tipo de mensaje
  
  // Subscriptions
  private pollingSubscription: Subscription | null = null;
  private timerSubscription: Subscription | null = null;

  ngOnInit(): void {
    // Detectar si venimos de un retorno de MercadoPago
    this.route.queryParams.subscribe(params => {
      if (params['status']) {
        this.handleMercadoPagoReturn(params);
      }
      
      // Si viene con sessionId en la URL, cargar ese checkout
      if (params['sessionId']) {
        this.sessionId = params['sessionId'];
        this.loadExistingCheckout(this.sessionId);
      } else {
        // Si no hay sessionId, iniciar nuevo checkout
        this.initiateCheckout();
      }
    });
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.stopTimer();
  }

  /**
   * Inicia el proceso de checkout multi-admin
   */
  private initiateCheckout(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.orderService.checkoutMulti().subscribe({
      next: (response) => {
        this.checkoutResponse = response;
        this.paymentGroups = response.paymentGroups;
        this.sessionId = response.sessionId;
        this.expiresAt = new Date(response.expiresAt);
        
        console.log('Checkout response:', response);
        
        // Actualizar URL con sessionId SIN navegación (solo modifica la URL en el historial)
        const url = this.router.createUrlTree([], {
          relativeTo: this.route,
          queryParams: { sessionId: this.sessionId },
          queryParamsHandling: 'merge'
        }).toString();
        
        // Usar replaceState para cambiar la URL sin recargar el componente
        window.history.replaceState(null, '', url);
        
        // Iniciar timer de expiración
        this.startExpirationTimer();
        
        // Generar preferencias de pago para cada grupo
        this.generatePaymentPreferences();
        
        // Iniciar polling de estado
        this.startSessionPolling();
        
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error en checkout:', error);
        
        // Si no hay carrito activo, redirigir al dashboard
        if (error.message?.includes('No active cart found')) {
          alert('No tienes items en el carrito. Por favor agrega eventos antes de hacer checkout.');
          this.router.navigate(['/customer/dashboard']);
        } else {
          this.errorMessage = error.message || 'Error al procesar el checkout';
        }
        
        this.isLoading = false;
      }
    });
  }

  /**
   * Carga un checkout existente por sessionId
   */
  private loadExistingCheckout(sessionId: string): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.orderService.getSessionStatus(sessionId).subscribe({
      next: (response) => {
        this.checkoutResponse = response;
        this.paymentGroups = response.paymentGroups;
        this.sessionId = response.sessionId;
        this.expiresAt = new Date(response.expiresAt);
        
        console.log('Loaded existing checkout:', response);
        
        // Iniciar timer de expiración
        this.startExpirationTimer();
        
        // Generar preferencias de pago para grupos sin payment preference
        this.generateMissingPaymentPreferences();
        
        // Iniciar polling de estado
        this.startSessionPolling();
        
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error cargando checkout:', error);
        this.errorMessage = error.message || 'Error al cargar el checkout';
        this.isLoading = false;
        
        // Si el error es de sesión inválida/expirada y el usuario no está autenticado, redirigir al login
        if (!this.authService.isAuthenticated()) {
          console.warn('Session invalid and user not authenticated, redirecting to login');
          this.router.navigate(['/customer/login'], { 
            queryParams: { returnUrl: '/customer/checkout', sessionId: sessionId } 
          });
        }
      }
    });
  }

  /**
   * Maneja el retorno desde MercadoPago
   */
  private handleMercadoPagoReturn(params: any): void {
    const status = params['status'];
    const paymentId = params['payment_id'];
    const merchantOrderId = params['merchant_order_id'];

    console.log('Retorno de MercadoPago:', { status, paymentId, merchantOrderId });

    switch (status) {
      case 'approved':
        this.paymentReturnType = 'success';
        this.paymentReturnMessage = '✅ ¡Pago aprobado! Tu orden ha sido confirmada.';
        break;
      case 'pending':
        this.paymentReturnType = 'pending';
        this.paymentReturnMessage = '⏳ Pago pendiente. Te notificaremos cuando se confirme.';
        break;
      case 'rejected':
      case 'failure':
        this.paymentReturnType = 'error';
        this.paymentReturnMessage = '❌ El pago fue rechazado. Puedes intentar nuevamente.';
        break;
      default:
        this.paymentReturnType = 'pending';
        this.paymentReturnMessage = 'Verificando el estado de tu pago...';
    }

    // Limpiar los query params después de 5 segundos
    setTimeout(() => {
      this.paymentReturnMessage = '';
      this.paymentReturnType = '';
    }, 8000);
  }

  /**
   * Genera preferencias de pago para todos los grupos
   */
  private async generatePaymentPreferences(): Promise<void> {
    this.isGeneratingPayments = true;

    for (const group of this.paymentGroups) {
      try {
        const payment = await firstValueFrom(
          this.paymentService.createPaymentPreference(
            group.adminId,
            group.orderNumber,
            group.amount,
            this.sessionId // Pasar sessionId para URLs de retorno
          )
        );

        if (payment) {
          group.paymentPreferenceId = payment.preferenceId;
          group.qrUrl = payment.qrUrl;
          group.initPoint = payment.initPoint;
        }
      } catch (error) {
        console.error(`Error generando pago para orden ${group.orderNumber}:`, error);
        // Continuar con los demás pagos aunque uno falle
      }
    }

    this.isGeneratingPayments = false;
  }

  /**
   * Genera preferencias de pago solo para grupos que no tienen
   */
  private async generateMissingPaymentPreferences(): Promise<void> {
    this.isGeneratingPayments = true;

    for (const group of this.paymentGroups) {
      // Solo generar si no tiene preferencia de pago Y está pendiente
      if (!group.paymentPreferenceId && group.status === 'PENDING_PAYMENT') {
        try {
          const payment = await firstValueFrom(
            this.paymentService.createPaymentPreference(
              group.adminId,
              group.orderNumber,
              group.amount,
              this.sessionId // Pasar sessionId para URLs de retorno
            )
          );

          if (payment) {
            group.paymentPreferenceId = payment.preferenceId;
            group.qrUrl = payment.qrUrl;
            group.initPoint = payment.initPoint;
          }
        } catch (error: any) {
          console.error(`Error generando pago para orden ${group.orderNumber}:`, error);
          
          // Si es error 401 (Unauthorized), el token expiró - redirigir al login
          if (error.status === 401 || error.status === 0) {
            console.warn('Token expired or unauthorized, redirecting to login');
            this.router.navigate(['/customer/login'], { 
              queryParams: { 
                returnUrl: '/customer/checkout',
                sessionId: this.sessionId,
                message: 'Tu sesión expiró. Por favor inicia sesión nuevamente.'
              } 
            });
            this.isGeneratingPayments = false;
            return; // Detener el proceso
          }
        }
      }
    }

    this.isGeneratingPayments = false;
  }

  /**
   * Inicia el polling del estado de la sesión cada 10 segundos
   */
  private startSessionPolling(): void {
    this.pollingSubscription = interval(10000)
      .pipe(
        switchMap(() => this.orderService.getSessionStatus(this.sessionId))
      )
      .subscribe({
        next: (status) => {
          console.log('Session status update:', status);
          this.checkoutResponse = status;
          // Preservar initPoint y otros datos de pago al actualizar
          status.paymentGroups.forEach(updatedGroup => {
            const existingGroup = this.paymentGroups.find(g => g.orderId === updatedGroup.orderId);
            if (existingGroup) {
              // Actualizar solo el status, preservando initPoint, qrUrl, preferenceId
              updatedGroup.initPoint = existingGroup.initPoint;
              updatedGroup.qrUrl = existingGroup.qrUrl;
              updatedGroup.paymentPreferenceId = existingGroup.paymentPreferenceId;
            }
          });
          this.paymentGroups = status.paymentGroups;

          // Si la sesión está completa, redirigir a página de éxito
          if (status.sessionStatus === 'COMPLETED') {
            this.stopPolling();
            this.stopTimer();
            // Limpiar el carrito ya que la compra está completa
            this.cartService.loadCart(); // Refrescar el carrito
            this.router.navigate(['/customer/orders/success'], {
              queryParams: { sessionId: this.sessionId }
            });
          }

          // Actualizar grupos de pago con el estado actualizado
          this.updatePaymentGroupsStatus(status.paymentGroups);
        },
        error: (error) => {
          console.error('Error polling session status:', error);
        }
      });
  }

  /**
   * Actualiza el estado de los grupos de pago
   */
  private updatePaymentGroupsStatus(updatedGroups: PaymentGroup[]): void {
    updatedGroups.forEach(updatedGroup => {
      const existingGroup = this.paymentGroups.find(g => g.orderId === updatedGroup.orderId);
      if (existingGroup) {
        existingGroup.status = updatedGroup.status;
      }
    });
  }

  /**
   * Inicia el timer de expiración de la sesión
   */
  private startExpirationTimer(): void {
    if (!this.expiresAt) return;

    this.timerSubscription = interval(1000).subscribe(() => {
      const now = new Date();
      const remaining = this.expiresAt!.getTime() - now.getTime();
      
      if (remaining <= 0) {
        this.timeRemaining = 0;
        this.stopTimer();
        this.errorMessage = 'La sesión de pago ha expirado. Por favor, intenta nuevamente.';
        this.stopPolling();
      } else {
        this.timeRemaining = Math.floor(remaining / 1000);
      }
    });
  }

  /**
   * Detiene el polling de estado
   */
  private stopPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = null;
    }
  }

  /**
   * Detiene el timer de expiración
   */
  private stopTimer(): void {
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
      this.timerSubscription = null;
    }
  }

  /**
   * Formatea el tiempo restante a MM:SS
   */
  formatTimeRemaining(): string {
    const minutes = Math.floor(this.timeRemaining / 60);
    const seconds = this.timeRemaining % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  /**
   * Abre el checkout de Mercado Pago en la misma pestaña (redirección directa)
   */
  openPaymentCheckout(group: PaymentGroup): void {
    if (group.initPoint) {
      // Redirigir en la misma pestaña en lugar de abrir nueva ventana
      window.location.href = group.initPoint;
    }
  }

  /**
   * Muestra el QR de un grupo de pago en tamaño grande
   */
  showQRCode(group: PaymentGroup): void {
    if (group.qrUrl) {
      // Implementar modal con QR en grande (por ahora abre en nueva ventana)
      window.open(group.qrUrl, '_blank', 'width=400,height=500');
    }
  }

  /**
   * Verifica manualmente el estado de un pago
   */
  refreshPaymentStatus(group: PaymentGroup): void {
    if (!group.paymentPreferenceId) return;

    this.paymentService.getPaymentStatus(group.paymentPreferenceId).subscribe({
      next: (status) => {
        console.log('Payment status:', status);
        // El backend actualiza automáticamente via webhook, esto es solo visual
        alert('Verificando estado del pago...');
      },
      error: (error) => {
        console.error('Error checking payment status:', error);
      }
    });
  }

  /**
   * Cancela el checkout y vuelve al carrito
   */
  cancelCheckout(): void {
    if (confirm('¿Estás seguro de que quieres cancelar? Los pagos ya realizados seguirán activos.')) {
      this.stopPolling();
      this.stopTimer();
      this.router.navigate(['/customer/dashboard']);
    }
  }

  /**
   * Verifica si se puede abandonar la sesión (no hay pagos completados)
   */
  get canAbandonSession(): boolean {
    return !this.paymentGroups.some(group => group.status === 'PAID');
  }

  /**
   * Obtiene el texto del botón de cancelar según el estado
   */
  get cancelButtonText(): string {
    return this.canAbandonSession ? 'Abandonar Checkout' : 'Volver al Dashboard';
  }

  /**
   * Maneja el click en el botón de cancelar/abandonar
   */
  handleCancelClick(): void {
    if (this.canAbandonSession) {
      // Si no hay pagos, ofrecer recuperar items al carrito
      if (confirm('¿Deseas cancelar el checkout? Los items volverán a tu carrito.')) {
        this.abandonCheckout();
      }
    } else {
      // Si hay pagos, solo volver al dashboard
      if (confirm('¿Deseas salir del checkout? Los pagos completados se mantendrán.')) {
        this.stopPolling();
        this.stopTimer();
        this.router.navigate(['/customer/dashboard']);
      }
    }
  }

  /**
   * Abandona la sesión de checkout y recupera items al carrito
   */
  private abandonCheckout(): void {
    this.orderService.abandonSession(this.sessionId).subscribe({
      next: (response) => {
        console.log('Sesión abandonada:', response);
        this.stopPolling();
        this.stopTimer();
        // Redirigir al dashboard (no existe ruta /customer/cart)
        alert('Checkout cancelado. Los items han sido devueltos al carrito.');
        this.router.navigate(['/customer/dashboard']);
      },
      error: (error) => {
        console.error('Error al abandonar sesión:', error);
        alert('Error al cancelar el checkout: ' + error.message);
      }
    });
  }

  /**
   * Obtiene el color del badge según el estado
   */
  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PAID':
        return 'badge-success';
      case 'PENDING':
        return 'badge-warning';
      case 'EXPIRED':
        return 'badge-danger';
      default:
        return 'badge-secondary';
    }
  }

  /**
   * Obtiene el texto del estado en español
   */
  getStatusText(status: string): string {
    switch (status) {
      case 'PAID':
        return 'Pagado ✓';
      case 'PENDING':
        return 'Pendiente';
      case 'EXPIRED':
        return 'Expirado';
      default:
        return status;
    }
  }

  /**
   * Obtiene el color del badge de la sesión
   */
  getSessionStatusBadgeClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'badge-success';
      case 'PARTIAL':
        return 'badge-info';
      case 'PENDING':
        return 'badge-warning';
      default:
        return 'badge-secondary';
    }
  }

  /**
   * Obtiene el texto del estado de la sesión en español
   */
  getSessionStatusText(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'Completada ✓';
      case 'PARTIAL':
        return 'Parcialmente pagada';
      case 'PENDING':
        return 'Pendiente';
      default:
        return status;
    }
  }
}
