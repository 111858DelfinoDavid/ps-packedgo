import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { EventService } from '../../../core/services/event.service';
import { UserService } from '../../../core/services/user.service';
import { CartService } from '../../../core/services/cart.service';
import { TicketService, Ticket } from '../../../core/services/ticket.service';
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

  get cartCount(): number {
    return this.cart ? this.cart.itemCount : 0;
  }

  get cartTotal(): number {
    return this.cart ? this.cart.totalAmount : 0;
  }

  removeFromCart(itemId: number): void {
    if (confirm('¿Estás seguro de que quieres eliminar este item del carrito?')) {
      this.cartService.removeCartItem(itemId).subscribe({
        next: (cart) => {
          console.log('Item removed from cart');
          if (!cart) {
            console.log('Cart is now empty');
          }
        },
        error: (error) => {
          console.error('Error removing item:', error);
          alert('Error al eliminar el item del carrito: ' + error.message);
        }
      });
    }
  }

  /**
   * Duplica un item del carrito (crea una copia idéntica)
   * Con Opción A: cada item = 1 entrada = 1 QR
   */
  duplicateItem(item: any): void {
    if (!confirm('¿Deseas duplicar esta entrada? Se creará una copia idéntica con las mismas consumiciones.')) {
      return;
    }

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
      },
      error: (error) => {
        console.error('Error al duplicar item:', error);
        alert(error.error?.message || 'Error al duplicar la entrada');
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
        alert(error.error?.message || 'Error al actualizar la cantidad de consumición');
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
        alert(error.error?.message || 'Error al actualizar la cantidad de consumición');
      }
    });
  }

  /**
   * Elimina una consumición específica del item del carrito
   */
  removeConsumption(itemId: number, consumption: any): void {
    if (!confirm(`¿Eliminar "${consumption.consumptionName}" del carrito?`)) {
      return;
    }

    this.cartService.removeConsumptionFromItem(itemId, consumption.consumptionId).subscribe({
      next: () => {
        console.log('Consumption removed successfully');
      },
      error: (error) => {
        console.error('Error removing consumption:', error);
        alert(error.error?.message || 'Error al eliminar la consumición');
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
        alert('No se pudieron cargar las consumiciones del evento');
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
      },
      error: (err) => {
        console.error('Error adding consumption:', err);
        alert(err.error?.message || 'Error al agregar consumición');
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
    if (confirm('¿Estás seguro de que quieres vaciar todo el carrito?')) {
      this.cartService.clearCart().subscribe({
        next: () => {
          console.log('Cart cleared successfully');
        },
        error: (error) => {
          console.error('Error clearing cart:', error);
          alert('Error al vaciar el carrito: ' + error.message);
        }
      });
    }
  }

  getConsumptionsTotal(consumptions: any[]): number {
    if (!consumptions || consumptions.length === 0) return 0;
    return consumptions.reduce((total, consumption) => total + consumption.subtotal, 0);
  }

  proceedToCheckout(): void {
    if (!this.cart || this.cart.itemCount === 0) {
      alert('Tu carrito está vacío');
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
        alert('Error al cargar tus entradas: ' + (error.error?.message || error.message));
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
        alert('Error al cargar el perfil. Por favor, intenta nuevamente.');
        this.isLoadingProfile = false;
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
        alert('Perfil actualizado exitosamente');
      },
      error: (error: any) => {
        console.error('Error al actualizar perfil:', error);
        this.isLoadingProfile = false;
        alert('Error al actualizar el perfil. Intenta nuevamente.');
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
      alert('Error: Usuario no encontrado');
      return;
    }

    const currentPassword = this.changePasswordForm.get('currentPassword')?.value;
    const newPassword = this.changePasswordForm.get('newPassword')?.value;

    this.authService.changePassword(user.id, currentPassword, newPassword).subscribe({
      next: () => {
        alert('✓ Contraseña actualizada exitosamente');
        this.changePasswordForm.reset();
      },
      error: (error) => {
        const errorMessage = error.error?.message || 'Error al cambiar la contraseña. Verifica que la contraseña actual sea correcta.';
        alert('❌ ' + errorMessage);
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
