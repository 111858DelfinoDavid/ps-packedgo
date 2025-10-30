import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { OrderService } from '../../../core/services/order.service';
import { PaymentService } from '../../../core/services/payment.service';
import { CartService } from '../../../core/services/cart.service';
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
  private orderService = inject(OrderService);
  private paymentService = inject(PaymentService);
  private cartService = inject(CartService);

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
  
  // Subscriptions
  private pollingSubscription: Subscription | null = null;
  private timerSubscription: Subscription | null = null;

  ngOnInit(): void {
    this.initiateCheckout();
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
        this.errorMessage = error.message || 'Error al procesar el checkout';
        this.isLoading = false;
      }
    });
  }

  /**
   * Genera preferencias de pago para todos los grupos
   */
  private async generatePaymentPreferences(): Promise<void> {
    this.isGeneratingPayments = true;

    for (const group of this.paymentGroups) {
      try {
        const payment = await this.paymentService.createPaymentPreference(
          group.adminId,
          group.orderNumber,
          group.amount
        ).toPromise();

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
   * Abre el checkout de Mercado Pago en una nueva ventana
   */
  openPaymentCheckout(group: PaymentGroup): void {
    if (group.initPoint) {
      window.open(group.initPoint, '_blank');
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
