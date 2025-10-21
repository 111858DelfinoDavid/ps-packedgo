import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { EventService } from '../../../core/services/event.service';
import { UserService } from '../../../core/services/user.service';
import { CartService } from '../../../core/services/cart.service';
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
  timeRemaining = 0;
  private previousTimeRemaining = 0;
  private cartSubscription?: Subscription;
  private timerSubscription?: Subscription;

  // Tickets Section
  isLoadingTickets = false;
  myTickets: any[] = [];

  // Profile Section
  isLoadingProfile = false;
  isEditingProfile = false;
  profileForm: FormGroup;
  originalProfileData: any = null;

  constructor() {
    this.profileForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      telephone: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
      gender: ['', Validators.required],
      bornDate: ['', Validators.required],
      profileImageUrl: ['']
    });
  }

  ngOnInit(): void {
    this.loadEvents();
    
    // Suscribirse a cambios del carrito PRIMERO
    this.cartSubscription = this.cartService.cart$.subscribe(cart => {
      this.cart = cart;
    });
    
    // Suscribirse a cambios del timer
    this.timerSubscription = this.cartService.timeRemaining$.subscribe(seconds => {
      // Detectar transición de tiempo > 0 a 0 (verdadera expiración)
      if (this.previousTimeRemaining > 0 && seconds === 0 && this.cart && this.cart.items && this.cart.items.length > 0) {
        alert('El tiempo de reserva ha expirado. Tu carrito ha sido vaciado.');
        this.cart = null;
      }
      
      this.previousTimeRemaining = this.timeRemaining;
      this.timeRemaining = seconds;
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

  getTimerDisplay(): string {
    return this.cartService.formatTimeRemaining(this.timeRemaining);
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
   * Incrementa la cantidad de un item en el carrito
   */
  incrementQuantity(itemId: number, currentQuantity: number): void {
    const maxTickets = this.cartService.MAX_TICKETS_PER_PERSON;
    
    if (currentQuantity >= maxTickets) {
      alert(`No puedes agregar más de ${maxTickets} entradas por evento`);
      return;
    }
    
    this.cartService.updateCartItemQuantity(itemId, currentQuantity + 1).subscribe({
      next: () => {
        console.log('Quantity incremented');
      },
      error: (error) => {
        console.error('Error incrementing quantity:', error);
        alert('Error al actualizar la cantidad');
      }
    });
  }

  /**
   * Decrementa la cantidad de un item en el carrito
   */
  decrementQuantity(itemId: number, currentQuantity: number): void {
    if (currentQuantity <= 1) {
      // Si es la última entrada, mejor eliminar el item
      this.removeFromCart(itemId);
      return;
    }
    
    this.cartService.updateCartItemQuantity(itemId, currentQuantity - 1).subscribe({
      next: () => {
        console.log('Quantity decremented');
      },
      error: (error) => {
        console.error('Error decrementing quantity:', error);
        alert('Error al actualizar la cantidad');
      }
    });
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

  proceedToCheckout(): void {
    if (!this.cart || this.cart.itemCount === 0) {
      alert('Tu carrito está vacío');
      return;
    }
    
    // TODO: Implementar proceso de pago con MercadoPago
    alert('Redirigiendo a pasarela de pago... (Por implementar)');
  }

  // ==================== TICKETS SECTION ====================
  loadMyTickets(): void {
    this.isLoadingTickets = true;
    // TODO: Llamar al servicio de orders para obtener las entradas del usuario
    setTimeout(() => {
      this.myTickets = []; // Mock data
      this.isLoadingTickets = false;
    }, 1000);
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
    const profileData = this.profileForm.value;

    this.userService.updateUserProfile(userId, profileData).subscribe({
      next: (response: any) => {
        console.log('Perfil actualizado:', response);
        this.originalProfileData = this.profileForm.value;
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

  // ==================== GENERAL ====================
  logout(): void {
    this.authService.logout();
  }

  ngOnDestroy(): void {
    // Limpiar suscripciones para evitar memory leaks
    if (this.cartSubscription) {
      this.cartSubscription.unsubscribe();
    }
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
    }
  }
}
