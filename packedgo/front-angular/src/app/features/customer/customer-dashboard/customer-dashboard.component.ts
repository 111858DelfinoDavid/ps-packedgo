import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { EventService } from '../../../core/services/event.service';
import { UserService } from '../../../core/services/user.service';
import { Event } from '../../../shared/models/event.model';

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
  cartItems: any[] = [];
  cartTimer: any = null;
  timeRemaining = 600; // 10 minutos en segundos

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
    this.loadCartFromStorage();
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
  loadCartFromStorage(): void {
    const savedCart = localStorage.getItem('shopping_cart');
    if (savedCart) {
      this.cartItems = JSON.parse(savedCart);
      if (this.cartItems.length > 0) {
        this.startCartTimer();
      }
    }
  }

  startCartTimer(): void {
    if (this.cartTimer) clearInterval(this.cartTimer);
    
    this.cartTimer = setInterval(() => {
      this.timeRemaining--;
      
      if (this.timeRemaining <= 0) {
        this.clearCart();
        alert('El tiempo de reserva ha expirado. Tu carrito ha sido vaciado.');
      }
    }, 1000);
  }

  get cartCount(): number {
    return this.cartItems.length;
  }

  get cartTotal(): number {
    return this.cartItems.reduce((sum, item) => sum + item.total, 0);
  }

  getTimerDisplay(): string {
    const minutes = Math.floor(this.timeRemaining / 60);
    const seconds = this.timeRemaining % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  removeFromCart(index: number): void {
    if (confirm('¿Estás seguro de que quieres eliminar este item del carrito?')) {
      this.cartItems.splice(index, 1);
      this.saveCartToStorage();
      
      if (this.cartItems.length === 0) {
        this.clearCart();
      }
    }
  }

  clearCart(): void {
    this.cartItems = [];
    this.timeRemaining = 600;
    if (this.cartTimer) {
      clearInterval(this.cartTimer);
      this.cartTimer = null;
    }
    localStorage.removeItem('shopping_cart');
  }

  proceedToCheckout(): void {
    if (this.cartItems.length === 0) {
      alert('Tu carrito está vacío');
      return;
    }
    
    // TODO: Implementar proceso de pago con MercadoPago
    alert('Redirigiendo a pasarela de pago... (Por implementar)');
  }

  private saveCartToStorage(): void {
    localStorage.setItem('shopping_cart', JSON.stringify(this.cartItems));
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
    if (this.cartTimer) {
      clearInterval(this.cartTimer);
    }
    this.authService.logout();
  }

  ngOnDestroy(): void {
    if (this.cartTimer) {
      clearInterval(this.cartTimer);
    }
  }
}
