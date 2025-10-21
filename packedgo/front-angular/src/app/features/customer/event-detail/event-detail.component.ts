import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { EventService } from '../../../core/services/event.service';
import { CartService } from '../../../core/services/cart.service';
import { Event } from '../../../shared/models/event.model';
import { AddToCartRequest } from '../../../shared/models/cart.model';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './event-detail.component.html',
  styleUrls: ['./event-detail.component.css']
})
export class EventDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private eventService = inject(EventService);
  private cartService = inject(CartService);

  event: Event | null = null;
  isLoading = true;
  errorMessage = '';
  quantity = 1;
  isAddingToCart = false;

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
        this.event = event;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error al cargar evento:', error);
        this.errorMessage = 'No se pudo cargar el evento';
        this.isLoading = false;
      }
    });
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
      alert(`Ya tienes ${currentInCart} entrada(s) en tu carrito. Solo puedes agregar ${maxCanAdd} más (máximo ${maxTickets} por persona).`);
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
      alert(`Ya tienes ${currentCount} entrada(s) de este evento en tu carrito. Solo puedes agregar ${remaining} más (máximo ${maxTickets} por persona).`);
      return;
    }

    this.isAddingToCart = true;
    
    // Create request with eventId, quantity, and empty consumptions array
    const request: AddToCartRequest = {
      eventId: this.event.id!,
      quantity: this.quantity, // Enviar la cantidad seleccionada
      consumptions: [] // No consumptions for now, just event ticket
    };
    
    this.cartService.addToCart(request).subscribe({
      next: (cart) => {
        alert(`¡${this.quantity} entrada(s) agregada(s) al carrito!`);
        this.isAddingToCart = false;
        // Resetear la cantidad a 1 después de agregar
        this.quantity = 1;
        this.router.navigate(['/customer/dashboard']);
      },
      error: (error) => {
        console.error('Error al agregar al carrito:', error);
        
        // Mostrar el mensaje específico del backend si está disponible
        const errorMessage = error?.error?.message || error?.message || 'Error desconocido';
        
        if (errorMessage.includes('Cannot add more than')) {
          // Error de límite máximo desde el backend
          const maxTickets = this.cartService.MAX_TICKETS_PER_PERSON;
          const currentCount = this.cartService.getTicketCountForEvent(this.event!.id!);
          alert(`Has alcanzado el límite máximo de ${maxTickets} entradas por persona. Actualmente tienes ${currentCount} entradas de este evento en tu carrito.`);
        } else {
          alert(`Error al agregar al carrito: ${errorMessage}`);
        }
        
        this.isAddingToCart = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/customer/dashboard']);
  }
}
