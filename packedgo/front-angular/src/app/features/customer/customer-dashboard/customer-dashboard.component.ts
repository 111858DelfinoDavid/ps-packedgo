import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import Swal from 'sweetalert2';
import { AuthService } from '../../../core/services/auth.service';
import { EventService } from '../../../core/services/event.service';
import { UserService } from '../../../core/services/user.service';
import { CartService } from '../../../core/services/cart.service';
import { TicketService, Ticket } from '../../../core/services/ticket.service';
import { OrderService } from '../../../core/services/order.service';
import { Event } from '../../../shared/models/event.model';
import { Cart } from '../../../shared/models/cart.model';

type TabType = 'events' | 'cart' | 'tickets' | 'profile';

@Component({
  selector: 'app-customer-dashboard',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './customer-dashboard.component.html',
  styleUrl: './customer-dashboard.component.css'
})
export class CustomerDashboardComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private eventService = inject(EventService);
  private userService = inject(UserService);
  private cartService = inject(CartService);
  private ticketService = inject(TicketService);
  private orderService = inject(OrderService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  // User & Navigation
  currentUser = this.authService.getCurrentUser();
  activeTab: TabType = 'events';
  
  // Events Section
  isLoadingEvents = true;
  events: Event[] = [];
  allEvents: Event[] = [];
  searchTerm = '';

  // Cart Section
  cart: Cart | null = null;
  groupedItems: { adminId: number, items: any[], total: number }[] = [];
  private cartSubscription?: Subscription;

  // Tickets Section
  isLoadingTickets = false;
  myTickets: Ticket[] = [];
  showQrModal = false;
  selectedTicket: Ticket | null = null;

  // Profile Section
  isLoadingProfile = false;
  isEditingProfile = false;
  profileForm: FormGroup;
  changePasswordForm: FormGroup;
  originalProfileData: any = null;
  userAuthData: any = null; // Para username, email, document (readonly)

  // Modal para agregar consumiciones
  showModal = false;
  selectedItemId: number | null = null;
  availableConsumptions: any[] = [];
  isLoadingConsumptions = false;

  constructor() {
    // Formulario de perfil - DESHABILITADO por defecto
    this.profileForm = this.fb.group({
      name: [{value: '', disabled: true}, [Validators.required, Validators.minLength(2)]],
      lastName: [{value: '', disabled: true}, [Validators.required, Validators.minLength(2)]],
      telephone: [{value: '', disabled: true}, [Validators.required, Validators.pattern(/^\d{10}$/)]],
      gender: [{value: '', disabled: true}, Validators.required],
      bornDate: [{value: '', disabled: true}, Validators.required],
      profileImageUrl: [{value: '', disabled: true}]
    });

    // Formulario de cambio de contraseña
    this.changePasswordForm = this.fb.group({
      currentPassword: ['', [Validators.required, Validators.minLength(6)]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(6)]]
    }, { validators: this.passwordMatchValidator });
  }

  // Validador personalizado para confirmar contraseñas
  private passwordMatchValidator(g: FormGroup) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value
      ? null : { 'mismatch': true };
  }

  ngOnInit(): void {
    this.loadEvents();
    
    // Suscribirse a cambios del carrito
    this.cartSubscription = this.cartService.cart$.subscribe(cart => {
      this.cart = cart;
      if (cart) {
        this.groupItemsByAdmin(cart);
      } else {
        this.groupedItems = [];
      }
    });
    
    // SIEMPRE cargar el carrito del backend al iniciar el dashboard
    // Esto asegura que tengamos el estado más actualizado, especialmente después de recargar la página
    this.loadCart();
  }

  // ==================== TAB NAVIGATION ====================
  switchTab(tab: TabType): void {
    this.activeTab = tab;
    
    if (tab === 'tickets' && this.myTickets.length === 0) {
      this.loadMyTickets();
    }
    
    if (tab === 'profile' && !this.originalProfileData) {
      this.loadProfile();
    }
  }

  // ==================== EVENTS SECTION ====================
  loadEvents(): void {
    this.isLoadingEvents = true;
    this.eventService.getEvents().subscribe({
      next: (events: any) => {
        this.allEvents = events.filter((e: any) => e.active);
        this.events = [...this.allEvents];
        this.isLoadingEvents = false;
      },
      error: (error: any) => {
        console.error('Error al cargar eventos:', error);
        this.isLoadingEvents = false;
      }
    });
  }

  searchEvents(): void {
    if (!this.searchTerm.trim()) {
      this.events = [...this.allEvents];
      return;
    }
    
    const term = this.searchTerm.toLowerCase();
    this.events = this.allEvents.filter(event => 
      event.name.toLowerCase().includes(term) ||
      event.description.toLowerCase().includes(term)
    );
  }

  viewEventDetails(eventId: number): void {
    this.router.navigate(['/customer/events', eventId]);
  }

  // ==================== CART SECTION ====================
  loadCart(): void {
    this.cartService.loadCart();
  }

  private groupItemsByAdmin(cart: Cart): void {
    const groups = new Map<number, { adminId: number, items: any[], total: number }>();

    cart.items.forEach(item => {
      // Si por alguna razón adminId es null/undefined, usar 0 o un placeholder
      const adminId = item.adminId || 0;
      
      if (!groups.has(adminId)) {
        groups.set(adminId, { adminId: adminId, items: [], total: 0 });
      }
      const group = groups.get(adminId)!;
      group.items.push(item);
      group.total += item.subtotal;
    });

    this.groupedItems = Array.from(groups.values());
  }

  payAdminGroup(adminId: number): void {
    Swal.fire({
      title: '¿Proceder al pago?',
      text: '¿Deseas pagar los items de este administrador?',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, pagar',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.orderService.checkoutSingleAdmin(adminId).subscribe({
          next: (response) => {
            if (response.paymentUrl) {
              window.location.href = response.paymentUrl;
            } else {
              Swal.fire('Error', 'No se recibió URL de pago', 'error');
            }
          },
          error: (error) => {
            console.error('Error initiating checkout:', error);
            Swal.fire('Error', 'Error al iniciar el pago: ' + (error.error?.message || error.message), 'error');
          }
        });
      }
    });
  }

  get cartCount(): number {
    return this.cart ? this.cart.itemCount : 0;
  }

  get cartTotal(): number {
    return this.cart ? this.cart.totalAmount : 0;
  }

  removeFromCart(itemId: number): void {
    Swal.fire({
      title: '¿Eliminar item?',
      text: '¿Estás seguro de que quieres eliminar este item del carrito?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        this.cartService.removeCartItem(itemId).subscribe({
          next: (cart) => {
            console.log('Item removed from cart');
            if (!cart) {
              console.log('Cart is now empty');
            }
            Swal.fire({
              title: 'Eliminado',
              text: 'El item ha sido eliminado del carrito',
              icon: 'success',
              timer: 1500,
              showConfirmButton: false
            });
          },
          error: (error) => {
            console.error('Error removing item:', error);
            Swal.fire('Error', 'Error al eliminar el item del carrito: ' + error.message, 'error');
          }
        });
      }
    });
  }

  /**
   * Duplica un item del carrito (crea una copia idéntica)
   * Con Opción A: cada item = 1 entrada = 1 QR
   */
  duplicateItem(item: any): void {
    Swal.fire({
      title: '¿Duplicar entrada?',
      text: 'Se creará una copia idéntica con las mismas consumiciones.',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, duplicar',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        // Construir las consumiciones para el nuevo item
        const consumptions = item.consumptions?.map((c: any) => ({
          consumptionId: c.consumptionId,
          quantity: c.quantity
        })) || [];

        // Agregar al carrito (esto creará un nuevo item con quantity=1)
        this.cartService.addToCart({
          eventId: item.eventId,
          quantity: 1,
          consumptions: consumptions
        }).subscribe({
          next: () => {
            console.log('Item duplicado exitosamente');
            Swal.fire({
              title: 'Duplicado',
              text: 'Entrada duplicada exitosamente',
              icon: 'success',
              timer: 1500,
              showConfirmButton: false
            });
          },
          error: (error) => {
            console.error('Error al duplicar item:', error);
            Swal.fire('Error', error.error?.message || 'Error al duplicar la entrada', 'error');
          }
        });
      }
    });
  }

  /**
   * Incrementa la cantidad de una consumición específica
   */
  incrementConsumption(itemId: number, consumption: any): void {
    const newQuantity = consumption.quantity + 1;
    
    this.cartService.updateConsumptionQuantity(itemId, consumption.consumptionId, newQuantity).subscribe({
      next: () => {
        console.log('Consumption quantity incremented');
      },
      error: (error) => {
        console.error('Error incrementing consumption:', error);
        Swal.fire('Error', error.error?.message || 'Error al actualizar la cantidad de consumición', 'error');
      }
    });
  }

  /**
   * Decrementa la cantidad de una consumición específica
   * Si llega a 0, elimina la consumición automáticamente
   */
  decrementConsumption(itemId: number, consumption: any): void {
    const newQuantity = consumption.quantity - 1;
    
    // Si la nueva cantidad es 0 o menos, eliminar la consumición
    if (newQuantity <= 0) {
      this.removeConsumption(itemId, consumption);
      return;
    }
    
    this.cartService.updateConsumptionQuantity(itemId, consumption.consumptionId, newQuantity).subscribe({
      next: () => {
        console.log('Consumption quantity decremented');
      },
      error: (error) => {
        console.error('Error decrementing consumption:', error);
        Swal.fire('Error', error.error?.message || 'Error al actualizar la cantidad de consumición', 'error');
      }
    });
  }

  /**
   * Elimina una consumición específica del item del carrito
   */
  removeConsumption(itemId: number, consumption: any): void {
    Swal.fire({
      title: '¿Eliminar consumición?',
      text: `¿Eliminar "${consumption.consumptionName}" del carrito?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        this.cartService.removeConsumptionFromItem(itemId, consumption.consumptionId).subscribe({
          next: () => {
            console.log('Consumption removed successfully');
            Swal.fire({
              title: 'Eliminado',
              text: 'Consumición eliminada',
              icon: 'success',
              timer: 1500,
              showConfirmButton: false
            });
          },
          error: (error) => {
            console.error('Error removing consumption:', error);
            Swal.fire('Error', error.error?.message || 'Error al eliminar la consumición', 'error');
          }
        });
      }
    });
  }

  /**
   * Muestra el modal para agregar consumiciones a un item específico
   */
  showAddConsumptionModal(item: any): void {
    this.selectedItemId = item.id;
    this.isLoadingConsumptions = true;
    this.showModal = true;
    
    // Obtener consumiciones disponibles del evento
    this.eventService.getEventById(item.eventId).subscribe({
      next: (event: any) => {
        // El backend devuelve availableConsumptions (no consumptions)
        this.availableConsumptions = event.availableConsumptions || [];
        this.isLoadingConsumptions = false;
        console.log('Available consumptions:', this.availableConsumptions);
      },
      error: (err: any) => {
        console.error('Error loading consumptions:', err);
        Swal.fire('Error', 'No se pudieron cargar las consumiciones del evento', 'error');
        this.closeModal();
      }
    });
  }

  /**
   * Agrega una consumición al item seleccionado
   */
  addConsumption(consumptionId: number): void {
    if (!this.selectedItemId) return;
    
    this.cartService.addConsumptionToItem(this.selectedItemId, consumptionId, 1).subscribe({
      next: () => {
        console.log('Consumption added successfully');
        this.closeModal();
        Swal.fire({
          title: 'Agregado',
          text: 'Consumición agregada exitosamente',
          icon: 'success',
          timer: 1500,
          showConfirmButton: false
        });
      },
      error: (err) => {
        console.error('Error adding consumption:', err);
        Swal.fire('Error', err.error?.message || 'Error al agregar consumición', 'error');
      }
    });
  }

  /**
   * Cierra el modal de agregar consumiciones
   */
  closeModal(): void {
    this.showModal = false;
    this.selectedItemId = null;
    this.availableConsumptions = [];
    this.isLoadingConsumptions = false;
  }

  clearCart(): void {
    Swal.fire({
      title: '¿Vaciar carrito?',
      text: '¿Estás seguro de que quieres vaciar todo el carrito?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, vaciar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        this.cartService.clearCart().subscribe({
          next: () => {
            console.log('Cart cleared successfully');
            Swal.fire({
              title: 'Vaciado',
              text: 'El carrito ha sido vaciado',
              icon: 'success',
              timer: 1500,
              showConfirmButton: false
            });
          },
          error: (error) => {
            console.error('Error clearing cart:', error);
            Swal.fire('Error', 'Error al vaciar el carrito: ' + error.message, 'error');
          }
        });
      }
    });
  }

  getConsumptionsTotal(consumptions: any[]): number {
    if (!consumptions || consumptions.length === 0) return 0;
    return consumptions.reduce((total, consumption) => total + consumption.subtotal, 0);
  }

  proceedToCheckout(): void {
    if (!this.cart || this.cart.itemCount === 0) {
      Swal.fire('Carrito vacío', 'Tu carrito está vacío', 'info');
      return;
    }
    
    // Navegar al checkout multi-admin
    this.router.navigate(['/customer/checkout']);
  }

  // ==================== TICKETS SECTION ====================
  loadMyTickets(): void {
    this.isLoadingTickets = true;
    const userId = this.currentUser?.id;
    
    if (!userId) {
      console.error('No se pudo obtener el ID del usuario');
      this.isLoadingTickets = false;
      return;
    }
    
    console.log('🎟️ Cargando tickets para usuario:', userId);
    
    this.ticketService.getUserTickets(userId).subscribe({
      next: (tickets) => {
        console.log('✅ Tickets cargados:', tickets);
        this.myTickets = tickets;
        this.isLoadingTickets = false;
      },
      error: (error) => {
        console.error('❌ Error cargando tickets:', error);
        this.myTickets = [];
        this.isLoadingTickets = false;
        Swal.fire('Error', 'Error al cargar tus entradas: ' + (error.error?.message || error.message), 'error');
      }
    });
  }

  showTicketQR(ticket: Ticket): void {
    console.log('🎟️ Mostrando QR para ticket:', ticket);
    this.selectedTicket = ticket;
    this.showQrModal = true;
    console.log('showQrModal:', this.showQrModal);
  }

  closeQrModal(): void {
    console.log('❌ Cerrando modal QR');
    this.showQrModal = false;
    this.selectedTicket = null;
  }

  // ==================== PROFILE SECTION ====================
  loadProfile(): void {
    this.isLoadingProfile = true;
    const userId = this.currentUser?.id;
    
    if (!userId) {
      console.error('No se encontró ID de usuario');
      this.isLoadingProfile = false;
      return;
    }

    this.userService.getUserProfile(userId).subscribe({
      next: (profile: any) => {
        this.originalProfileData = profile;
        
        // Guardar datos de autenticación (readonly)
        this.userAuthData = {
          username: this.currentUser?.username || '',
          email: this.currentUser?.email || '',
          document: profile.document
        };
        
        // Llenar formulario con datos del perfil
        this.profileForm.patchValue({
          name: profile.name,
          lastName: profile.lastName,
          telephone: profile.telephone,
          gender: profile.gender,
          bornDate: profile.bornDate,
          profileImageUrl: profile.profileImageUrl || ''
        });
        this.isLoadingProfile = false;
      },
      error: (error: any) => {
        console.error('Error al cargar perfil:', error);
        
        if (error.status === 500 || error.status === 404) {
          // Perfil no existe - mostrar mensaje informativo
          Swal.fire('Perfil no disponible', 'Tu perfil no está disponible. Por favor, contacta con el administrador o intenta registrarte nuevamente.', 'warning');
        } else {
          Swal.fire('Error', 'Error al cargar el perfil. Por favor, intenta nuevamente.', 'error');
        }
        
        this.isLoadingProfile = false;
        // Volver a la pestaña de eventos
        this.activeTab = 'events';
      }
    });
  }

  toggleEditProfile(): void {
    this.isEditingProfile = !this.isEditingProfile;
    
    if (this.isEditingProfile) {
      this.profileForm.enable();
    } else {
      this.profileForm.disable();
      // Restaurar valores originales si cancela
      if (this.originalProfileData) {
        this.profileForm.patchValue(this.originalProfileData);
      }
    }
  }

  onSubmitProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const userId = this.currentUser?.id;
    if (!userId) return;

    this.isLoadingProfile = true;
    
    // ⚠️ IMPORTANTE: Incluir TODOS los campos requeridos por el backend
    // El backend requiere: name, lastName, document, gender, bornDate, telephone, profileImageUrl
    const profileData = {
      name: this.profileForm.get('name')?.value,
      lastName: this.profileForm.get('lastName')?.value,
      document: this.userAuthData?.document || 0, // Del auth-service (readonly)
      gender: this.profileForm.get('gender')?.value,
      bornDate: this.profileForm.get('bornDate')?.value,
      telephone: this.profileForm.get('telephone')?.value,
      profileImageUrl: this.profileForm.get('profileImageUrl')?.value || ''
    };

    console.log('📤 Enviando datos de perfil:', profileData);

    this.userService.updateUserProfile(userId, profileData).subscribe({
      next: (response: any) => {
        console.log('Perfil actualizado:', response);
        this.originalProfileData = response;
        this.isEditingProfile = false;
        this.profileForm.disable();
        this.isLoadingProfile = false;
        Swal.fire('Éxito', 'Perfil actualizado exitosamente', 'success');
      },
      error: (error: any) => {
        console.error('Error al actualizar perfil:', error);
        this.isLoadingProfile = false;
        Swal.fire('Error', 'Error al actualizar el perfil. Intenta nuevamente.', 'error');
      }
    });
  }

  onChangePassword(): void {
    if (this.changePasswordForm.invalid) {
      this.changePasswordForm.markAllAsTouched();
      return;
    }

    const user = this.authService.getCurrentUser();
    if (!user) {
      Swal.fire('Error', 'Usuario no encontrado', 'error');
      return;
    }

    const currentPassword = this.changePasswordForm.get('currentPassword')?.value;
    const newPassword = this.changePasswordForm.get('newPassword')?.value;

    this.authService.changePassword(user.id, currentPassword, newPassword).subscribe({
      next: () => {
        Swal.fire('Éxito', 'Contraseña actualizada exitosamente', 'success');
        this.changePasswordForm.reset();
      },
      error: (error) => {
        const errorMessage = error.error?.message || 'Error al cambiar la contraseña. Verifica que la contraseña actual sea correcta.';
        Swal.fire('Error', errorMessage, 'error');
        console.error('Error al cambiar contraseña:', error);
      }
    });
  }

  // ==================== GENERAL ====================
  logout(): void {
    this.authService.logout();
  }

  ngOnDestroy(): void {
    // Limpiar suscripciones para evitar memory leaks
    if (this.cartSubscription) {
      this.cartSubscription.unsubscribe();
    }
  }
}
