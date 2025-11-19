import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { interval, Subscription, firstValueFrom, forkJoin } from 'rxjs';
import { switchMap, map } from 'rxjs/operators';
import { OrderService } from '../../../core/services/order.service';
import { PaymentService } from '../../../core/services/payment.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { 
  MultiOrderCheckoutResponse, 
  PaymentGroup,
  SessionStateResponse,
  SessionPaymentGroup
} from '../../../shared/models/order.model';
import Swal from 'sweetalert2';

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
  checkoutResponse: SessionStateResponse | null = null;
  paymentGroups: SessionPaymentGroup[] = [];
  sessionId: string = '';
  expiresAt: Date | null = null;
  timeRemaining: number = 0; // en segundos
  
  // Estado de UI
  isLoading = true;
  errorMessage = '';
  isGeneratingPayments = false;
  paymentReturnMessage: string = ''; // Mensaje de retorno de Stripe
  paymentReturnType: 'success' | 'error' | 'pending' | '' = ''; // Tipo de mensaje
  
  // Subscriptions
  private pollingSubscription: Subscription | null = null;
  private timerSubscription: Subscription | null = null;

  /**
   * Backend State Authority: ngOnInit ultra-simple
   * El backend maneja TODO, frontend solo renderiza
   */
  ngOnInit(): void {
    // Detectar si venimos de un retorno de Stripe
    this.route.queryParams.subscribe(params => {
      const comesFromStripe = params['status'] || params['paymentStatus'] || params['session_id'];
      
      if (comesFromStripe) {
        this.handleStripeReturn(params);
      }
    });

    // Cargar estado actual (backend decide si recuperar o crear nueva)
    this.loadCurrentCheckoutState();
    
    // Polling cada 5 segundos para auto-actualizar
    this.startPollingCheckoutState();
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.stopTimer();
  }

  /**
   * Backend State Authority: Carga el estado actual de checkout
   * El backend NUNCA falla, siempre retorna algo válido
   */
  private loadCurrentCheckoutState(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.orderService.getCurrentCheckoutState().subscribe({
      next: (state) => {
        console.log('✅ Estado de checkout cargado:', state);
        
        // Caso 1: No hay carrito
        if (state.sessionStatus === 'NO_CART') {
          this.errorMessage = 'Tu carrito está vacío. Agrega eventos para continuar.';
          this.isLoading = false;
          return;
        }
        
        // Caso 2: Sesión expirada (backend ya creó nueva desde cart)
        if (state.isExpired) {
          console.warn('⚠️ Sesión anterior expiró, backend creó nueva automáticamente');
        }
        
        // Caso 3: Sesión completada (todos los pagos hechos)
        if (state.isCompleted) {
          console.log('✅ Checkout completado, redirigiendo a success...');
          this.router.navigate(['/customer/order-success'], {
            queryParams: { sessionId: state.sessionId }
          });
          return;
        }
        
        // Mapear estado a la UI existente
        this.mapStateToUI(state);
        
        // Generar preferencias de pago para grupos sin payment preference
        this.generateMissingPaymentPreferences();
        
        // Iniciar polling de estado
        this.startPollingCheckoutState();
        
        // Timer countdown
        this.startCountdown();
        
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Error cargando checkout:', error);
        this.errorMessage = 'Error al cargar el checkout. Por favor recarga la página.';
        this.isLoading = false;
      }
    });
  }

  /**
   * Mapea SessionStateResponse del backend a las variables de UI
   */
  private mapStateToUI(state: SessionStateResponse): void {
    this.sessionId = state.sessionId;
    this.expiresAt = new Date(state.expiresAt);
    
    this.paymentGroups = state.paymentGroups;
    this.checkoutResponse = state;

    console.log(`💰 ${state.paidGroups}/${state.totalGroups} grupos pagados`);
    console.log(`⏱️  ${state.secondsUntilExpiration}s restantes hasta expiración`);
  }

  /**
   * Polling: Actualiza el estado silenciosamente cada 5 segundos
   */
  private startPollingCheckoutState(): void {
    this.pollingSubscription = interval(5000).subscribe(() => {
      this.updateCheckoutStateSilently();
    });
  }

  /**
   * Actualización silenciosa (sin spinner) para polling
   */
  private updateCheckoutStateSilently(): void {
    this.orderService.getCurrentCheckoutState().subscribe({
      next: (state) => {
        if (state.isCompleted) {
          this.stopPolling();
          this.router.navigate(['/customer/order-success'], {
            queryParams: { sessionId: state.sessionId }
          });
          return;
        }
        
        this.mapStateToUI(state);
      },
      error: (error) => {
        console.warn('⚠️ Error en polling (se reintentará):', error);
      }
    });
  }

  /**
   * Carga un checkout existente por sessionId
   */
  private loadExistingCheckout(sessionId: string, comesFromStripe: boolean = false): void {
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
        
        // Si el error es de sesión inválida/expirada y el usuario no está autenticado
        // PERO NO VIENE DE STRIPE, redirigir al login
        if (!comesFromStripe && !this.authService.isAuthenticated()) {
          console.warn('Session invalid and user not authenticated, redirecting to login');
          this.router.navigate(['/customer/login'], { 
            queryParams: { returnUrl: '/customer/checkout', sessionId: sessionId } 
          });
        } else if (comesFromStripe) {
          // Si viene de Stripe pero hay error, mostrar mensaje pero NO redirigir
          console.warn('Error loading session after Stripe return, but not redirecting to login');
          this.errorMessage = 'Error al cargar el estado de la sesión. Por favor, verifica tu pago en "Mis Órdenes".';
        }
      }
    });
  }

  /**
   * Maneja el retorno desde Stripe
   */
  private handleStripeReturn(params: any): void {
    const status = params['status'] || params['paymentStatus']; // Puede venir de dos formas
    const orderId = params['orderId'];
    const sessionId = params['session_id'];
    const paymentIntentId = params['payment_intent'];

    console.log('Retorno de Stripe:', { status, orderId, sessionId, paymentIntentId });

    // Scroll al inicio para que el usuario vea el mensaje
    window.scrollTo({ top: 0, behavior: 'smooth' });

    switch (status) {
      case 'approved':
      case 'success':
        this.paymentReturnType = 'success';
        this.paymentReturnMessage = `✅ ¡Pago aprobado exitosamente!
        
Orden ${orderId} confirmada. Actualizando el estado...

${this.paymentGroups.length > 1 ? 'Nota: Si tienes otros pagos pendientes, aparecerán abajo.' : ''}`;
        
        // Forzar actualización del estado de la sesión inmediatamente
        if (this.sessionId) {
          // Primero, intentar actualizar el estado de la sesión
          this.orderService.getSessionStatus(this.sessionId!).subscribe({
            next: (sessionStatus) => {
              console.log('Session status after payment:', sessionStatus);
              this.checkoutResponse = sessionStatus;
              this.paymentGroups = sessionStatus.paymentGroups;
              
              // Verificar si se completó todo
              if (sessionStatus.sessionStatus === 'COMPLETED') {
                this.stopPolling();
                this.stopTimer();
                this.cartService.loadCart();
                
                // Mostrar mensaje de éxito y redirigir
                this.paymentReturnMessage = '✅ ¡Todos los pagos completados! Redirigiendo...';
                
                setTimeout(() => {
                  this.router.navigate(['/customer/orders/success'], {
                    queryParams: { sessionId: this.sessionId }
                  });
                }, 2000);
              } else {
                // Si no está completo, mostrar mensaje y actualizar UI
                setTimeout(() => {
                  this.paymentReturnMessage = '';
                  this.paymentReturnType = '';
                }, 5000);
              }
            },
            error: (error) => {
              console.error('Error updating session after payment:', error);
              // Si hay error, mostrar mensaje y limpiar después
              setTimeout(() => {
                this.paymentReturnMessage = '';
                this.paymentReturnType = '';
              }, 5000);
            }
          });
        }
        break;
      case 'pending':
        this.paymentReturnType = 'pending';
        this.paymentReturnMessage = `⏳ Pago en proceso de aprobación
        
Orden ${orderId} registrada. Te notificaremos por email cuando se confirme el pago.

Por favor revisa tu bandeja de entrada.`;
        // Limpiar mensaje después de 10 segundos para pending
        setTimeout(() => {
          this.paymentReturnMessage = '';
          this.paymentReturnType = '';
        }, 10000);
        break;
      case 'rejected':
      case 'failure':
        this.paymentReturnType = 'error';
        this.paymentReturnMessage = `❌ El pago fue rechazado
        
Orden ${orderId} no pudo procesarse. Por favor verifica tus datos e intenta nuevamente.

El botón de pago aparece más abajo.`;
        // Limpiar mensaje después de 12 segundos para errores
        setTimeout(() => {
          this.paymentReturnMessage = '';
          this.paymentReturnType = '';
        }, 12000);
        break;
      default:
        this.paymentReturnType = 'pending';
        this.paymentReturnMessage = '⏳ Verificando el estado de tu pago...';
        setTimeout(() => {
          this.paymentReturnMessage = '';
          this.paymentReturnType = '';
        }, 8000);
    }

    // Limpiar los query params después de mostrar el mensaje
    setTimeout(() => {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { sessionId: this.sessionId }, // Mantener solo el sessionId
        replaceUrl: true
      });
    }, 100);
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
          // Usar sandboxInitPoint para testing, initPoint para producción
          group.initPoint = payment.sandboxInitPoint || payment.initPoint;
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
      // Solo generar si no tiene initPoint Y está pendiente (PENDING o PENDING_PAYMENT)
      const isPending = group.paymentStatus === 'PENDING_PAYMENT' || group.paymentStatus === 'PENDING';
      if (!group.initPoint && isPending) {
        try {
          console.log(`🔄 Generando pago para orden ${group.orderNumber}...`);
          
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
            // Para Stripe, usar initPoint (Stripe Checkout URL)
            group.initPoint = payment.initPoint;
            
            console.log(`✅ Link de pago generado para orden ${group.orderNumber}:`, group.initPoint);
          }
        } catch (error: any) {
          console.error(`❌ Error generando pago para orden ${group.orderNumber}:`, error);
          
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
      } else if (group.initPoint) {
        console.log(`✓ Orden ${group.orderNumber} ya tiene link de pago:`, group.initPoint);
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
        switchMap(() => this.orderService.getSessionStatus(this.sessionId!))
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
  private updatePaymentGroupsStatus(updatedGroups: SessionPaymentGroup[]): void {
    updatedGroups.forEach(updatedGroup => {
      const existingGroup = this.paymentGroups.find(g => g.orderId === updatedGroup.orderId);
      if (existingGroup) {
        existingGroup.paymentStatus = updatedGroup.paymentStatus;
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
   * Alias para mantener compatibilidad con código existente
   */
  private startCountdown(): void {
    this.startExpirationTimer();
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
   * Abre el checkout de Stripe (redirige en la misma ventana)
   */
  openPaymentCheckout(group: SessionPaymentGroup): void {
    if (group.initPoint) {
      // Stripe redirige en la misma ventana y luego regresa
      window.location.href = group.initPoint;
    }
  }

  /**
   * Verifica manualmente el estado de un pago
   */
  refreshPaymentStatus(group: SessionPaymentGroup): void {
    console.log('Actualizando estado de pago para:', group);

    // Actualizar el estado de la sesión directamente
    this.orderService.getCurrentCheckoutState().subscribe({
      next: (state) => {
        console.log('Session status refreshed:', state);
        
        // Mapear el estado actualizado
        this.mapStateToUI(state);
        
        // Verificar si el pago fue aprobado
        const updatedGroup = this.paymentGroups.find(g => g.orderId === group.orderId);
        if (updatedGroup?.paymentStatus === 'PAID' || updatedGroup?.paymentStatus === 'COMPLETED') {
          Swal.fire({
            title: '¡Pago aprobado!',
            text: 'Las entradas han sido generadas exitosamente.',
            icon: 'success',
            confirmButtonText: 'Genial'
          });
          
          // Si todos los pagos están completos, redirigir
          if (state.isCompleted) {
            this.stopPolling();
            this.stopTimer();
            this.cartService.loadCart();
            
            setTimeout(() => {
              this.router.navigate(['/customer/orders/success'], {
                queryParams: { sessionId: this.sessionId }
              });
            }, 2000);
          }
        } else if (updatedGroup?.paymentStatus === 'PENDING_PAYMENT') {
          Swal.fire({
            title: 'Pago pendiente',
            text: 'El pago aún está pendiente de confirmación.',
            icon: 'info',
            confirmButtonText: 'Entendido'
          });
        } else {
          Swal.fire({
            title: 'Estado actualizado',
            text: 'Estado del pago: ' + (updatedGroup?.paymentStatus || 'desconocido'),
            icon: 'info',
            confirmButtonText: 'OK'
          });
        }
      },
      error: (error) => {
        console.error('Error refreshing payment status:', error);
        Swal.fire({
          title: 'Error',
          text: 'Error al actualizar el estado del pago: ' + (error.message || 'Error desconocido'),
          icon: 'error',
          confirmButtonText: 'Cerrar'
        });
      }
    });
  }

  /**
   * Actualiza el estado de TODOS los pagos desde el backend
   * NOTA: simulateAllPayments removido con MercadoPago, ahora solo actualiza estado
   */
  simulateAllPayments(): void {
    console.log('Actualizando estado de todos los pagos...');
    
    // Actualizar el estado de la sesión
    this.orderService.getCurrentCheckoutState().subscribe({
      next: (state) => {
        console.log('Session state refreshed:', state);
        
        // Mapear el estado actualizado
        this.mapStateToUI(state);
        
        // Verificar cuántos pagos fueron aprobados
        const approvedCount = this.paymentGroups.filter(g => g.paymentStatus === 'PAID' || g.paymentStatus === 'COMPLETED').length;
        Swal.fire({
          title: 'Estado actualizado',
          text: `${approvedCount} de ${this.paymentGroups.length} pagos completados`,
          icon: 'info',
          confirmButtonText: 'OK'
        });
        
        // Si todos los pagos están completos, redirigir
        if (state.isCompleted) {
          this.stopPolling();
          this.stopTimer();
          this.cartService.loadCart();
          
          setTimeout(() => {
            this.router.navigate(['/customer/orders/success'], {
              queryParams: { sessionId: this.sessionId }
            });
          }, 2000);
        }
      },
      error: (error) => {
        console.error('Error refreshing session state:', error);
        Swal.fire({
          title: 'Error',
          text: 'Error al actualizar el estado: ' + (error.message || 'Error desconocido'),
          icon: 'error',
          confirmButtonText: 'Cerrar'
        });
      }
    });
  }

  /**
   * Cancela el checkout y vuelve al carrito
   */
  cancelCheckout(): void {
    Swal.fire({
      title: '¿Estás seguro?',
      text: 'Los pagos ya realizados seguirán activos.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, cancelar',
      cancelButtonText: 'No, continuar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.stopPolling();
        this.stopTimer();
        this.router.navigate(['/customer/dashboard']);
      }
    });
  }

  /**
   * Verifica si se puede abandonar la sesión (no hay pagos completados)
   */
  get canAbandonSession(): boolean {
    return !this.paymentGroups.some(group => group.paymentStatus === 'PAID');
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
      Swal.fire({
        title: '¿Cancelar checkout?',
        text: 'Los items volverán a tu carrito.',
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: 'Sí, cancelar',
        cancelButtonText: 'No, seguir comprando'
      }).then((result) => {
        if (result.isConfirmed) {
          this.abandonCheckout();
        }
      });
    } else {
      // Si hay pagos, solo volver al dashboard
      Swal.fire({
        title: '¿Salir del checkout?',
        text: 'Los pagos completados se mantendrán.',
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: 'Sí, salir',
        cancelButtonText: 'No, quedarme'
      }).then((result) => {
        if (result.isConfirmed) {
          this.stopPolling();
          this.stopTimer();
          this.router.navigate(['/customer/dashboard']);
        }
      });
    }
  }

  /**
   * Abandona la sesión de checkout y recupera items al carrito
   */
  private abandonCheckout(): void {
    this.orderService.abandonSession(this.sessionId!).subscribe({
      next: (response) => {
        console.log('Sesión abandonada:', response);
        this.stopPolling();
        this.stopTimer();
        // Redirigir al dashboard (no existe ruta /customer/cart)
        Swal.fire({
          title: 'Checkout cancelado',
          text: 'Los items han sido devueltos al carrito.',
          icon: 'success',
          timer: 2000,
          showConfirmButton: false
        }).then(() => {
          this.router.navigate(['/customer/dashboard']);
        });
      },
      error: (error) => {
        console.error('Error al abandonar sesión:', error);
        Swal.fire({
          title: 'Error',
          text: 'Error al cancelar el checkout: ' + error.message,
          icon: 'error',
          confirmButtonText: 'Cerrar'
        });
      }
    });
  }

  /**
   * Obtiene el color del badge según el estado
   */
  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PAID':
      case 'COMPLETED':
        return 'badge-success';
      case 'PENDING':
      case 'PENDING_PAYMENT':
        return 'badge-warning';
      case 'EXPIRED':
      case 'FAILED':
      case 'CANCELLED':
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
      case 'COMPLETED':
        return 'Pagado ✓';
      case 'PENDING':
      case 'PENDING_PAYMENT':
        return 'Pendiente';
      case 'EXPIRED':
        return 'Expirado';
      case 'FAILED':
        return 'Fallido';
      case 'CANCELLED':
        return 'Cancelado';
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
   * Obtiene el texto de la sesión en español
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

  /**
   * Calcula el monto pendiente de pago
   */
  get pendingAmount(): number {
    return this.paymentGroups
      .filter(g => g.paymentStatus !== 'PAID' && g.paymentStatus !== 'COMPLETED')
      .reduce((sum, g) => sum + g.amount, 0);
  }
}
