import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import Swal from 'sweetalert2';
import { EventService } from '../../../core/services/event.service';
import { CartService } from '../../../core/services/cart.service';
import { Event, Consumption } from '../../../shared/models/event.model';
import { AddToCartRequest, ConsumptionRequest } from '../../../shared/models/cart.model';
import { SafePipe } from '../../../shared/pipes/safe.pipe';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, SafePipe],
  templateUrl: './event-detail.component.html',
  styleUrls: ['./event-detail.component.css']
})
export class EventDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private eventService = inject(EventService);
  private cartService = inject(CartService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  event: Event | null = null;
  isLoading = true;
  errorMessage = '';
  quantity = 1;
  isAddingToCart = false;
  
  // Selected consumptions with quantities
  selectedConsumptions: Map<number, number> = new Map();
  
  // Geocoding & Map modal
  private addressCache = new Map<string, string>();
  showMapModal = false;

  ngOnInit(): void {
    // Cargar el carrito primero para tener el estado actual
    this.cartService.loadCart();
    
    const eventId = Number(this.route.snapshot.paramMap.get('id'));
    if (eventId) {
      this.loadEventDetails(eventId);
    } else {
      this.router.navigate(['/customer/dashboard']);
    }
  }

  loadEventDetails(eventId: number): void {
    this.isLoading = true;
    this.eventService.getEventById(eventId).subscribe({
      next: (event) => {
        // Construir imageUrl con prioridades: archivo local > URL externa > placeholder
        if (event.hasImageData && event.id) {
          event.imageUrl = this.eventService.getEventImageUrl(event.id);
        } else if (!event.imageUrl) {
          event.imageUrl = 'https://via.placeholder.com/800x400?text=Sin+Imagen';
        }
        
        this.event = event;
        
        // Pre-geocodificar la ubicación
        const key = `${event.lat},${event.lng}`;
        if (!this.addressCache.has(key)) {
          this.geocodeLocation(event.lat, event.lng, key);
        }
        
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error al cargar evento:', error);
        this.errorMessage = 'No se pudo cargar el evento';
        this.isLoading = false;
      }
    });
  }

  /**
   * Obtiene la URL de la imagen del evento con prioridades
   */
  getEventImageSrc(): string {
    if (!this.event) return 'https://via.placeholder.com/800x400?text=Sin+Imagen';
    
    // Prioridad 1: imagen subida al servidor (archivo local)
    if (this.event.hasImageData && this.event.id) {
      return this.eventService.getEventImageUrl(this.event.id);
    }
    // Prioridad 2: URL externa
    if (this.event.imageUrl) {
      return this.event.imageUrl;
    }
    // Prioridad 3: placeholder
    return 'https://via.placeholder.com/800x400?text=Sin+Imagen';
  }

  /**
   * Obtiene el nombre de la categoría del evento
   */
  getCategoryName(): string {
    if (!this.event?.category) return 'Sin categoría';
    return this.event.category.name || `Categoría ${this.event.categoryId}`;
  }

  incrementQuantity(): void {
    if (!this.event) return;
    
    // Calcular el máximo permitido considerando lo que ya está en el carrito
    const maxTickets = this.cartService.MAX_TICKETS_PER_PERSON;
    const currentInCart = this.cartService.getTicketCountForEvent(this.event.id!);
    const maxCanAdd = maxTickets - currentInCart;
    
    // También respetar el límite del evento
    const eventLimit = this.event.availablePasses || this.event.maxCapacity;
    const effectiveMax = Math.min(maxCanAdd, eventLimit);
    
    if (this.quantity < effectiveMax) {
      this.quantity++;
    } else if (this.quantity >= maxCanAdd) {
      Swal.fire('Límite alcanzado', `Ya tienes ${currentInCart} entrada(s) en tu carrito. Solo puedes agregar ${maxCanAdd} más (máximo ${maxTickets} por persona).`, 'warning');
    }
  }

  decrementQuantity(): void {
    if (this.quantity > 1) {
      this.quantity--;
    }
  }
  
  /**
   * Obtiene el máximo de entradas que se pueden agregar
   */
  getMaxQuantity(): number {
    if (!this.event) return 1;
    
    const maxTickets = this.cartService.MAX_TICKETS_PER_PERSON;
    const currentInCart = this.cartService.getTicketCountForEvent(this.event.id!);
    const maxCanAdd = maxTickets - currentInCart;
    const eventLimit = this.event.availablePasses || this.event.maxCapacity;
    
    return Math.min(maxCanAdd, eventLimit);
  }

  addToCart(): void {
    if (!this.event) return;

    // Validar que no exceda el máximo permitido
    const maxTickets = this.cartService.MAX_TICKETS_PER_PERSON;
    if (!this.cartService.canAddTickets(this.event.id!, this.quantity)) {
      const currentCount = this.cartService.getTicketCountForEvent(this.event.id!);
      const remaining = maxTickets - currentCount;
      Swal.fire('Límite alcanzado', `Ya tienes ${currentCount} entrada(s) de este evento en tu carrito. Solo puedes agregar ${remaining} más (máximo ${maxTickets} por persona).`, 'warning');
      return;
    }

    this.isAddingToCart = true;
    
    // Build consumptions array from selected items
    const consumptions: ConsumptionRequest[] = Array.from(this.selectedConsumptions.entries())
      .map(([consumptionId, quantity]) => ({
        consumptionId,
        quantity
      }));
    
    // Create request with eventId, quantity, and selected consumptions
    const request: AddToCartRequest = {
      eventId: this.event.id!,
      quantity: this.quantity, // Enviar la cantidad seleccionada
      consumptions: consumptions
    };
    
    this.cartService.addToCart(request).subscribe({
      next: (cart) => {
        const consumptionText = consumptions.length > 0 
          ? ` con ${consumptions.length} consumo(s)` 
          : '';
        
        Swal.fire({
          title: '¡Agregado!',
          text: `¡${this.quantity} entrada(s) agregada(s) al carrito${consumptionText}!`,
          icon: 'success',
          timer: 1500,
          showConfirmButton: false
        }).then(() => {
          this.isAddingToCart = false;
          // Resetear la cantidad y consumptions seleccionadas
          this.quantity = 1;
          this.selectedConsumptions.clear();
          this.router.navigate(['/customer/dashboard']);
        });
      },
      error: (error) => {
        console.error('Error al agregar al carrito:', error);
        
        // Mostrar el mensaje específico del backend si está disponible
        const errorMessage = error?.error?.message || error?.message || 'Error desconocido';
        
        if (errorMessage.includes('Cannot add more than')) {
          // Error de límite máximo desde el backend
          const maxTickets = this.cartService.MAX_TICKETS_PER_PERSON;
          const currentCount = this.cartService.getTicketCountForEvent(this.event!.id!);
          Swal.fire('Límite alcanzado', `Has alcanzado el límite máximo de ${maxTickets} entradas por persona. Actualmente tienes ${currentCount} entradas de este evento en tu carrito.`, 'error');
        } else {
          Swal.fire('Error', `Error al agregar al carrito: ${errorMessage}`, 'error');
        }
        
        this.isAddingToCart = false;
      }
    });
  }

  // Consumption selection methods
  toggleConsumption(consumptionId: number): void {
    if (this.selectedConsumptions.has(consumptionId)) {
      this.selectedConsumptions.delete(consumptionId);
    } else {
      this.selectedConsumptions.set(consumptionId, 1);
    }
  }

  isConsumptionSelected(consumptionId: number): boolean {
    return this.selectedConsumptions.has(consumptionId);
  }

  getConsumptionQuantity(consumptionId: number): number {
    return this.selectedConsumptions.get(consumptionId) || 0;
  }

  updateConsumptionQuantity(consumptionId: number, quantity: number): void {
    if (quantity > 0 && quantity <= 10) {
      this.selectedConsumptions.set(consumptionId, quantity);
    } else if (quantity <= 0) {
      this.selectedConsumptions.delete(consumptionId);
    }
  }

  incrementConsumption(consumptionId: number): void {
    const current = this.getConsumptionQuantity(consumptionId);
    if (current < 10) {
      this.updateConsumptionQuantity(consumptionId, current + 1);
    }
  }

  decrementConsumption(consumptionId: number): void {
    const current = this.getConsumptionQuantity(consumptionId);
    if (current > 1) {
      this.updateConsumptionQuantity(consumptionId, current - 1);
    } else {
      this.selectedConsumptions.delete(consumptionId);
    }
  }

  getSelectedConsumptionsTotal(): number {
    if (!this.event?.availableConsumptions) return 0;
    
    let total = 0;
    this.selectedConsumptions.forEach((quantity, consumptionId) => {
      const consumption = this.event!.availableConsumptions!.find(c => c.id === consumptionId);
      if (consumption) {
        total += consumption.price * quantity;
      }
    });
    return total;
  }

  getTotalPrice(): number {
    const ticketsTotal = this.event ? this.event.basePrice * this.quantity : 0;
    const consumptionsTotal = this.getSelectedConsumptionsTotal();
    return ticketsTotal + consumptionsTotal;
  }

  goBack(): void {
    this.router.navigate(['/customer/dashboard']);
  }

  // ==================== GEOCODING & MAP ====================
  getEventAddress(): string {
    if (!this.event) return 'Cargando ubicación...';
    
    const key = `${this.event.lat},${this.event.lng}`;
    
    if (this.addressCache.has(key)) {
      return this.addressCache.get(key)!;
    }
    
    this.geocodeLocation(this.event.lat, this.event.lng, key);
    return 'Cargando ubicación...';
  }

  private geocodeLocation(lat: number, lng: number, key: string): void {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&addressdetails=1&zoom=18`;
    
    this.http.get<any>(url).subscribe({
      next: (data) => {
        if (data) {
          const addr = data.address || {};
          let placeName: string | undefined;
          
          if (data.name) {
            const roadTypes = ['Avenida', 'Calle', 'Ruta', 'Autovía', 'Boulevard', 'Paseo', 'Camino'];
            const isRoadName = roadTypes.some(type => data.name.startsWith(type));
            
            if (!isRoadName && data.name !== addr.road && data.name !== addr.highway) {
              placeName = data.name;
            }
          }
          
          if (!placeName) {
            placeName = addr.amenity || addr.shop || addr.leisure || 
                       addr.tourism || addr.office || addr.club_venue;
          }
          
          if (!placeName && addr.building && typeof addr.building === 'string' && 
              addr.building !== 'yes' && addr.building !== 'house') {
            placeName = addr.building;
          }
          
          if (!placeName && data.display_name) {
            const firstPart = data.display_name.split(',')[0].trim();
            const roadTypes = ['Avenida', 'Calle', 'Ruta', 'Autovía', 'Boulevard', 'Paseo', 'Camino'];
            const isRoadName = roadTypes.some(type => firstPart.startsWith(type));
            
            if (!isRoadName && firstPart !== addr.road) {
              placeName = firstPart;
            }
          }
          
          if (!placeName) {
            const parts = [
              addr.neighbourhood || addr.suburb || addr.quarter,
              addr.city || addr.town || addr.village
            ].filter(p => p);
            placeName = parts.join(', ') || 'Ubicación desconocida';
          }
          
          this.addressCache.set(key, placeName);
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('Error geocoding:', err);
        this.addressCache.set(key, 'Ubicación no disponible');
        this.cdr.detectChanges();
      }
    });
  }

  openMapModal(): void {
    this.showMapModal = true;
  }

  closeMapModal(): void {
    this.showMapModal = false;
  }

  onMapModalKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.closeMapModal();
    }
  }
}
