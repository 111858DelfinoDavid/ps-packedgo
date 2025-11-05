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
      const comesFromMercadoPago = params['status'] || params['paymentStatus'] || params['payment_id'];
      
      // Puede venir con 'status' o 'paymentStatus'
      if (comesFromMercadoPago) {
        this.handleMercadoPagoReturn(params);
      }
      
      // Si viene con sessionId en la URL, cargar ese checkout
      if (params['sessionId']) {
        this.sessionId = params['sessionId'];
        this.loadExistingCheckout(this.sessionId, !!comesFromMercadoPago);
      } else {
        // Si no hay sessionId en URL, intentar recuperar desde localStorage
        const savedToken = localStorage.getItem('checkout_session_token');
        if (savedToken) {
          console.log('Found saved session token, attempting recovery');
          this.recoverCheckoutFromToken(savedToken);
        } else {
          // Si no hay token guardado, iniciar nuevo checkout
          this.initiateCheckout();
        }
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
        
        // Guardar session token en localStorage para recuperación
        if (response.sessionToken) {
          localStorage.setItem('checkout_session_token', response.sessionToken);
          console.log('Session token saved to localStorage');
        }
        
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
  private loadExistingCheckout(sessionId: string, comesFromMercadoPago: boolean = false): void {
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
        // PERO NO VIENE DE MERCADOPAGO, redirigir al login
        if (!comesFromMercadoPago && !this.authService.isAuthenticated()) {
          console.warn('Session invalid and user not authenticated, redirecting to login');
          this.router.navigate(['/customer/login'], { 
            queryParams: { returnUrl: '/customer/checkout', sessionId: sessionId } 
          });
        } else if (comesFromMercadoPago) {
          // Si viene de MercadoPago pero hay error, mostrar mensaje pero NO redirigir
          console.warn('Error loading session after MercadoPago return, but not redirecting to login');
          this.errorMessage = 'Error al cargar el estado de la sesión. Por favor, verifica tu pago en "Mis Órdenes".';
        }
      }
    });
  }

  /**
   * Recupera un checkout usando el token guardado en localStorage
   */
  private recoverCheckoutFromToken(sessionToken: string): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.orderService.recoverSession(sessionToken).subscribe({
      next: (response) => {
        this.checkoutResponse = response;
        this.paymentGroups = response.paymentGroups;
        this.sessionId = response.sessionId;
        this.expiresAt = new Date(response.expiresAt);
        
        console.log('Session recovered from token:', response);
        
        // Actualizar URL con sessionId
        const url = this.router.createUrlTree([], {
          relativeTo: this.route,
          queryParams: { sessionId: this.sessionId },
          queryParamsHandling: 'merge'
        }).toString();
        window.history.replaceState(null, '', url);
        
        // Iniciar timer de expiración
        this.startExpirationTimer();
        
        // Generar preferencias de pago para grupos sin payment preference
        this.generateMissingPaymentPreferences();
        
        // Iniciar polling de estado
        this.startSessionPolling();
        
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error recovering session:', error);
        
        // Si el token es inválido o la sesión expiró, limpiar y crear nuevo checkout
        if (error.message?.includes('expired') || error.message?.includes('not found')) {
          console.warn('Session expired or invalid, clearing token and starting new checkout');
          localStorage.removeItem('checkout_session_token');
          this.initiateCheckout();
        } else {
          this.errorMessage = error.message || 'Error al recuperar la sesión';
          this.isLoading = false;
        }
      }
    });
  }

  /**
   * Maneja el retorno desde MercadoPago
   */
  private handleMercadoPagoReturn(params: any): void {
    const status = params['status'] || params['paymentStatus']; // Puede venir de dos formas
    const orderId = params['orderId'];
    const paymentId = params['payment_id'];
    const merchantOrderId = params['merchant_order_id'];

    console.log('Retorno de MercadoPago:', { status, orderId, paymentId, merchantOrderId });

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
          this.orderService.getSessionStatus(this.sessionId).subscribe({
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
                  // Limpiar token de sesión ya que se completó exitosamente
                  localStorage.removeItem('checkout_session_token');
                  console.log('Session token cleared (checkout completed)');
                  
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
            // Usar sandboxInitPoint para testing, initPoint para producción
            group.initPoint = payment.sandboxInitPoint || payment.initPoint;
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
            
            // Limpiar token de sesión ya que se completó exitosamente
            localStorage.removeItem('checkout_session_token');
            console.log('Session token cleared (polling detected completion)');
            
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
   * Abre el checkout de Mercado Pago en una nueva pestaña para mantener el polling activo
   */
  openPaymentCheckout(group: PaymentGroup): void {
    if (group.initPoint) {
      // Abrir en nueva pestaña para que el checkout actual siga con polling
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

    console.log('Simulando aprobación de pago para:', group);

    // Primero, simular la aprobación del pago (esto dispara el webhook simulado)
    this.paymentService.simulatePaymentApproval(group.paymentPreferenceId).subscribe({
      next: (approvalResponse) => {
        console.log('Payment approval simulation response:', approvalResponse);
        
        // Después de simular la aprobación, actualizar el estado de la sesión
        this.orderService.getSessionStatus(this.sessionId).subscribe({
          next: (sessionStatus) => {
            console.log('Session status after approval:', sessionStatus);
            
            // Actualizar el estado local
            this.checkoutResponse = sessionStatus;
            this.paymentGroups = sessionStatus.paymentGroups;
            
            // Verificar si el pago fue aprobado
            const updatedGroup = this.paymentGroups.find(g => g.orderId === group.orderId);
            if (updatedGroup?.status === 'PAID') {
              alert('✅ ¡Pago aprobado exitosamente! Las entradas han sido generadas.');
              
              // Si todos los pagos están completos, redirigir
              if (sessionStatus.sessionStatus === 'COMPLETED') {
                this.stopPolling();
                this.stopTimer();
                this.cartService.loadCart();
                
                setTimeout(() => {
                  // Limpiar token de sesión ya que se completó exitosamente
                  localStorage.removeItem('checkout_session_token');
                  console.log('Session token cleared (simulate payment completed)');
                  
                  this.router.navigate(['/customer/orders/success'], {
                    queryParams: { sessionId: this.sessionId }
                  });
                }, 2000);
              }
            } else if (updatedGroup?.status === 'PENDING') {
              alert('⏳ El pago aún está pendiente de confirmación');
            } else {
              alert('Estado del pago actualizado: ' + (updatedGroup?.status || 'desconocido'));
            }
          },
          error: (error) => {
            console.error('Error updating session status:', error);
            alert('Error al actualizar el estado de la sesión');
          }
        });
      },
      error: (error) => {
        console.error('Error simulating payment approval:', error);
        
        // Si ya estaba aprobado, solo actualizar la sesión
        if (error.error?.status === 'already_approved') {
          alert('✅ El pago ya estaba aprobado');
          
          // Actualizar el estado de la sesión
          this.orderService.getSessionStatus(this.sessionId).subscribe({
            next: (sessionStatus) => {
              this.checkoutResponse = sessionStatus;
              this.paymentGroups = sessionStatus.paymentGroups;
            }
          });
        } else {
          alert('Error al verificar el estado del pago: ' + (error.error?.error || error.message));
        }
      }
    });
  }

  /**
   * Simula la aprobación de TODOS los pagos pendientes en el checkout multi-admin
   * Útil para testing sin MercadoPago real
   */
  simulateAllPayments(): void {
    const pendingGroups = this.paymentGroups.filter(g => g.status === 'PENDING_PAYMENT');
    
    if (pendingGroups.length === 0) {
      alert('No hay pagos pendientes para simular');
      return;
    }

    console.log(`Simulando ${pendingGroups.length} pagos pendientes...`);
    
    // Simular todos los pagos en paralelo
    const simulationObservables = pendingGroups.map(group => 
      this.paymentService.simulatePaymentApproval(group.paymentPreferenceId!)
    );

    // Ejecutar todas las simulaciones en paralelo usando forkJoin
    const forkJoinObservable = pendingGroups.length === 1 
      ? simulationObservables[0].pipe(map(result => [result]))
      : forkJoin(simulationObservables);

    forkJoinObservable.subscribe({
      next: (results) => {
        console.log('All payments simulated:', results);
        
        // Actualizar el estado de la sesión después de simular todos
        this.orderService.getSessionStatus(this.sessionId).subscribe({
          next: (sessionStatus) => {
            console.log('Session status after simulating all payments:', sessionStatus);
            
            // Actualizar el estado local
            this.checkoutResponse = sessionStatus;
            this.paymentGroups = sessionStatus.paymentGroups;
            
            // Verificar cuántos pagos fueron aprobados
            const approvedCount = this.paymentGroups.filter(g => g.status === 'PAID').length;
            alert(`✅ ${approvedCount} de ${this.paymentGroups.length} pagos aprobados exitosamente!`);
            
            // Si todos los pagos están completos, redirigir
            if (sessionStatus.sessionStatus === 'COMPLETED') {
              this.stopPolling();
              this.stopTimer();
              this.cartService.loadCart();
              
              setTimeout(() => {
                // Limpiar token de sesión ya que se completó exitosamente
                localStorage.removeItem('checkout_session_token');
                console.log('Session token cleared (simulate all payments completed)');
                
                this.router.navigate(['/customer/orders/success'], {
                  queryParams: { sessionId: this.sessionId }
                });
              }, 2000);
            }
          },
          error: (error) => {
            console.error('Error updating session status:', error);
            alert('Error al actualizar el estado de la sesión');
          }
        });
      },
      error: (error) => {
        console.error('Error simulating payments:', error);
        alert('Error al simular algunos pagos: ' + (error.error?.message || error.message));
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
