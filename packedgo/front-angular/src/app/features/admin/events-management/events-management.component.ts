import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { EventService } from '../../../core/services/event.service';
import { AuthService } from '../../../core/services/auth.service';
import { Event, EventCategory } from '../../../shared/models/event.model';

@Component({
  selector: 'app-events-management',
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule],
  templateUrl: './events-management.component.html',
  styleUrl: './events-management.component.css'
})
export class EventsManagementComponent implements OnInit {
  private fb = inject(FormBuilder);
  private eventService = inject(EventService);
  private authService = inject(AuthService);
  private router = inject(Router);

  events: Event[] = [];
  categories: EventCategory[] = [];
  filteredEvents: Event[] = [];
  
  eventForm: FormGroup;
  isLoading = true;
  isSubmitting = false;
  showModal = false;
  isEditMode = false;
  currentEventId?: number;
  
  searchTerm = '';
  errorMessage = '';
  successMessage = '';

  constructor() {
    this.eventForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      eventDate: ['', [Validators.required]],
      lat: ['', [Validators.required, Validators.pattern(/^-?\d+\.?\d*$/)]],
      lng: ['', [Validators.required, Validators.pattern(/^-?\d+\.?\d*$/)]],
      maxCapacity: ['', [Validators.required, Validators.min(1)]],
      basePrice: ['', [Validators.required, Validators.min(0)]],
      imageUrl: [''],
      categoryId: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    
    // Cargar categorías primero
    this.eventService.getEventCategories().subscribe({
      next: (categories: any) => {
        this.categories = categories;
      },
      error: (error: any) => console.error('Error al cargar categorías:', error)
    });

    // Cargar eventos
    this.eventService.getEvents().subscribe({
      next: (events: any) => {
        this.events = events;
        this.filteredEvents = events;
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Error al cargar eventos:', error);
        this.isLoading = false;
        this.errorMessage = 'Error al cargar eventos. Por favor, intenta nuevamente.';
      }
    });
  }

  searchEvents(): void {
    if (!this.searchTerm.trim()) {
      this.filteredEvents = this.events;
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredEvents = this.events.filter(event => 
      event.name.toLowerCase().includes(term) ||
      event.description.toLowerCase().includes(term)
    );
  }

  openCreateModal(): void {
    this.isEditMode = false;
    this.currentEventId = undefined;
    this.eventForm.reset();
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  openEditModal(event: Event): void {
    this.isEditMode = true;
    this.currentEventId = event.id;
    
    // Convertir fecha al formato YYYY-MM-DD para el input date
    const eventDate = new Date(event.eventDate);
    const formattedDate = eventDate.toISOString().split('T')[0];
    
    this.eventForm.patchValue({
      name: event.name,
      description: event.description,
      eventDate: formattedDate,
      lat: event.lat,
      lng: event.lng,
      maxCapacity: event.maxCapacity,
      basePrice: event.basePrice,
      imageUrl: event.imageUrl || '',
      categoryId: event.categoryId
    });
    
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeModal(): void {
    this.showModal = false;
    this.eventForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
  }

  onSubmit(): void {
    if (this.eventForm.invalid) {
      this.eventForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const eventData: Event = {
      ...this.eventForm.value,
      lat: Number(this.eventForm.value.lat),
      lng: Number(this.eventForm.value.lng),
      maxCapacity: Number(this.eventForm.value.maxCapacity),
      basePrice: Number(this.eventForm.value.basePrice),
      categoryId: Number(this.eventForm.value.categoryId)
    };

    if (this.isEditMode && this.currentEventId) {
      // Actualizar evento existente
      this.eventService.updateEvent(this.currentEventId, eventData).subscribe({
        next: () => {
          this.successMessage = 'Evento actualizado exitosamente';
          setTimeout(() => {
            this.closeModal();
            this.loadData();
          }, 1500);
        },
        error: (error: any) => {
          console.error('Error al actualizar evento:', error);
          this.errorMessage = error.error?.message || 'Error al actualizar el evento. Por favor, intenta nuevamente.';
          this.isSubmitting = false;
        }
      });
    } else {
      // Crear nuevo evento
      this.eventService.createEvent(eventData).subscribe({
        next: () => {
          this.successMessage = 'Evento creado exitosamente';
          setTimeout(() => {
            this.closeModal();
            this.loadData();
          }, 1500);
        },
        error: (error: any) => {
          console.error('Error al crear evento:', error);
          this.errorMessage = error.error?.message || 'Error al crear el evento. Por favor, intenta nuevamente.';
          this.isSubmitting = false;
        }
      });
    }
  }

  deleteEvent(eventId: number, eventName: string): void {
    if (!confirm(`¿Estás seguro de que deseas eliminar el evento "${eventName}"?`)) {
      return;
    }

    this.eventService.deleteEvent(eventId).subscribe({
      next: () => {
        this.successMessage = 'Evento eliminado exitosamente';
        this.loadData();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (error: any) => {
        console.error('Error al eliminar evento:', error);
        this.errorMessage = error.error?.message || 'Error al eliminar el evento. Por favor, intenta nuevamente.';
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  logout(): void {
    this.authService.logout();
  }

  getCategoryName(categoryId: number): string {
    const category = this.categories.find(c => c.id === categoryId);
    return category ? category.name : 'Sin categoría';
  }

  // Getters para validación en template
  get name() { return this.eventForm.get('name'); }
  get description() { return this.eventForm.get('description'); }
  get eventDate() { return this.eventForm.get('eventDate'); }
  get lat() { return this.eventForm.get('lat'); }
  get lng() { return this.eventForm.get('lng'); }
  get maxCapacity() { return this.eventForm.get('maxCapacity'); }
  get basePrice() { return this.eventForm.get('basePrice'); }
  get imageUrl() { return this.eventForm.get('imageUrl'); }
  get categoryId() { return this.eventForm.get('categoryId'); }
}
