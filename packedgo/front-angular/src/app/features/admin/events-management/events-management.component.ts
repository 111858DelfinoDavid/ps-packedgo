import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import Swal from 'sweetalert2';
import { EventService } from '../../../core/services/event.service';
import { AuthService } from '../../../core/services/auth.service';
import { Event, EventCategory, Consumption } from '../../../shared/models/event.model';

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

  // Tab Management
  activeTab: 'events' | 'categories' = 'events';
  
  // Events Tab
  events: Event[] = [];
  filteredEvents: Event[] = [];
  selectedConsumptions: number[] = [];
  eventForm: FormGroup;
  searchTerm = '';
  showModal = false;
  isEditMode = false;
  currentEventId?: number;
  
  // Image Upload
  imageUploadOption: 'url' | 'upload' = 'url';
  selectedImageFile: File | null = null;
  imagePreviewUrl: string | null = null;
  
  // Categories Tab
  categories: EventCategory[] = [];
  filteredCategories: EventCategory[] = [];
  categorySearchTerm = '';
  showCategoryModal = false;
  isCategoryEditMode = false;
  categoryForm: any = {
    id: undefined,
    name: '',
    description: '',
    active: true
  };
  
  // Shared Data
  consumptions: Consumption[] = [];
  
  // Loading & Messages
  isLoading = true;
  isSubmitting = false;
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

    // Cargar consumptions
    this.eventService.getConsumptions().subscribe({
      next: (consumptions: any) => {
        this.consumptions = consumptions.filter((c: Consumption) => c.active);
      },
      error: (error: any) => console.error('Error al cargar consumptions:', error)
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
    this.selectedConsumptions = [];
    this.eventForm.reset();
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  openEditModal(event: Event): void {
    this.isEditMode = true;
    this.currentEventId = event.id;
    
    // Cargar consumiciones existentes del evento
    this.selectedConsumptions = event.availableConsumptions 
      ? event.availableConsumptions.map(c => c.id).filter((id): id is number => id !== undefined) 
      : [];
    
    // Convertir fecha al formato YYYY-MM-DD para el input date
    // El servidor envía la fecha en UTC sin 'Z', así que la agregamos para parsear correctamente
    const eventDate = new Date(event.eventDate.endsWith('Z') ? event.eventDate : event.eventDate + 'Z');
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
    
    // Configurar modo de imagen (si tiene URL externa usar 'url', sino dejar en 'url' por defecto)
    this.imageUploadOption = event.imageUrl ? 'url' : 'url';
    this.clearImageFile();
    
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeModal(): void {
    this.showModal = false;
    this.selectedConsumptions = [];
    this.eventForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
    this.isSubmitting = false;
    
    // Limpiar imagen
    this.clearImageFile();
    this.imageUploadOption = 'url';
  }

  onSubmit(): void {
    if (this.eventForm.invalid) {
      this.eventForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    // Convertir la fecha (YYYY-MM-DD) a LocalDateTime (YYYY-MM-DDTHH:mm:ss)
    const eventDateString = this.eventForm.value.eventDate;
    const eventDateTime = `${eventDateString}T20:00:00`; // Agregar hora por defecto (8 PM)

    const eventData: Event & { consumptionIds?: number[] } = {
      ...this.eventForm.value,
      eventDate: eventDateTime, // Usar la fecha con hora
      lat: Number(this.eventForm.value.lat),
      lng: Number(this.eventForm.value.lng),
      maxCapacity: Number(this.eventForm.value.maxCapacity),
      basePrice: Number(this.eventForm.value.basePrice),
      categoryId: Number(this.eventForm.value.categoryId),
      consumptionIds: this.selectedConsumptions
    };

    if (this.isEditMode && this.currentEventId) {
      // Actualizar evento existente
      this.eventService.updateEvent(this.currentEventId, eventData).subscribe({
        next: () => {
          // Si hay imagen seleccionada, subirla
          if (this.selectedImageFile) {
            this.uploadEventImage(this.currentEventId!);
          } else {
            this.isSubmitting = false;
            this.successMessage = 'Evento actualizado exitosamente';
            setTimeout(() => {
              this.closeModal();
              this.loadData();
            }, 1500);
          }
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
        next: (response: any) => {
          const newEventId = response.id;
          
          // Si hay imagen seleccionada, subirla
          if (this.selectedImageFile && newEventId) {
            this.uploadEventImage(newEventId);
          } else {
            this.isSubmitting = false;
            this.successMessage = 'Evento creado exitosamente';
            setTimeout(() => {
              this.closeModal();
              this.loadData();
            }, 1500);
          }
        },
        error: (error: any) => {
          console.error('Error al crear evento:', error);
          this.errorMessage = error.error?.message || 'Error al crear el evento. Por favor, intenta nuevamente.';
          this.isSubmitting = false;
        }
      });
    }
  }

  uploadEventImage(eventId: number): void {
    if (!this.selectedImageFile) {
      return;
    }

    this.eventService.uploadEventImage(eventId, this.selectedImageFile).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.successMessage = `Evento ${this.isEditMode ? 'actualizado' : 'creado'} exitosamente con imagen`;
        setTimeout(() => {
          this.closeModal();
          this.loadData();
        }, 1500);
      },
      error: (error: any) => {
        console.error('Error al subir imagen:', error);
        this.isSubmitting = false;
        this.successMessage = `Evento ${this.isEditMode ? 'actualizado' : 'creado'} exitosamente`;
        this.errorMessage = 'Advertencia: Error al subir la imagen. El evento fue guardado sin imagen.';
        setTimeout(() => {
          this.closeModal();
          this.loadData();
        }, 2500);
      }
    });
  }

  deleteEvent(eventId: number, eventName: string): void {
    Swal.fire({
      title: '¿Eliminar evento?',
      text: `¿Estás seguro de que deseas eliminar el evento "${eventName}"?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        this.eventService.deleteEvent(eventId).subscribe({
          next: () => {
            Swal.fire('Eliminado', 'Evento eliminado exitosamente', 'success');
            this.loadData();
          },
          error: (error: any) => {
            console.error('Error al eliminar evento:', error);
            Swal.fire('Error', error.error?.message || 'Error al eliminar el evento. Por favor, intenta nuevamente.', 'error');
          }
        });
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

  // Métodos para manejo de consumptions
  toggleConsumptionSelection(consumptionId: number): void {
    const index = this.selectedConsumptions.indexOf(consumptionId);
    if (index > -1) {
      this.selectedConsumptions.splice(index, 1);
    } else {
      this.selectedConsumptions.push(consumptionId);
    }
  }

  isConsumptionSelected(consumptionId: number): boolean {
    return this.selectedConsumptions.includes(consumptionId);
  }

  getSelectedConsumptionsList(): Consumption[] {
    return this.consumptions.filter(c => this.isConsumptionSelected(c.id!));
  }

  getAvailableConsumptionsList(): Consumption[] {
    return this.consumptions.filter(c => !this.isConsumptionSelected(c.id!));
  }

  getConsumptionCategoryName(categoryId: number): string {
    const consumption = this.consumptions.find(c => c.id === categoryId);
    return consumption?.categoryId ? `Categoría ${consumption.categoryId}` : 'Sin categoría';
  }

  // ===== IMAGE UPLOAD METHODS =====
  setImageOption(option: 'url' | 'upload'): void {
    this.imageUploadOption = option;
    
    if (option === 'url') {
      // Limpiar archivo seleccionado
      this.clearImageFile();
    } else {
      // Limpiar URL ingresada
      this.eventForm.patchValue({ imageUrl: '' });
    }
  }

  onImageFileSelected(event: any): void {
    const input = event.target as HTMLInputElement;
    
    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];
    
    // Validar tipo de archivo
    const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg'];
    if (!allowedTypes.includes(file.type)) {
      this.errorMessage = 'Formato de archivo no válido. Use PNG, JPG o JPEG';
      return;
    }

    // Validar tamaño (5MB)
    const maxSize = 5 * 1024 * 1024; // 5MB en bytes
    if (file.size > maxSize) {
      this.errorMessage = `El archivo excede el tamaño máximo de 5MB (${(file.size / 1024 / 1024).toFixed(2)} MB)`;
      return;
    }

    this.selectedImageFile = file;
    this.errorMessage = '';

    // Crear preview
    const reader = new FileReader();
    reader.onload = (e: ProgressEvent<FileReader>) => {
      this.imagePreviewUrl = e.target?.result as string;
    };
    reader.readAsDataURL(file);
  }

  clearImageFile(): void {
    this.selectedImageFile = null;
    this.imagePreviewUrl = null;
    
    // Limpiar el input file
    const fileInput = document.getElementById('imageFile') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  getEventImageSrc(event: Event): string {
    // Prioridad: imagen subida > imagen URL externa
    if (event.hasImageData && event.id) {
      return this.eventService.getEventImageUrl(event.id);
    }
    return event.imageUrl || '';
  }

  handleImageError(event: Event): void {
    // Si falla la imagen subida, intentar con la URL externa
    if (event.hasImageData && event.imageUrl) {
      event.hasImageData = false;
    }
  }

  // ===== TAB MANAGEMENT =====
  switchTab(tab: 'events' | 'categories'): void {
    this.activeTab = tab;
    
    // Limpiar mensajes
    this.errorMessage = '';
    this.successMessage = '';
    
    // Si cambiamos a categorías y no están cargadas, cargarlas
    if (tab === 'categories') {
      this.loadCategories();
    }
  }

  // ===== CATEGORIES TAB METHODS =====
  loadCategories(): void {
    this.eventService.getEventCategories().subscribe({
      next: (categories: any) => {
        this.categories = categories;
        this.filteredCategories = categories;
      },
      error: (error: any) => {
        console.error('Error al cargar categorías:', error);
        this.errorMessage = 'Error al cargar categorías. Por favor, intenta nuevamente.';
      }
    });
  }

  searchCategories(): void {
    if (!this.categorySearchTerm.trim()) {
      this.filteredCategories = this.categories;
      return;
    }

    const term = this.categorySearchTerm.toLowerCase();
    this.filteredCategories = this.categories.filter(category => 
      category.name.toLowerCase().includes(term) ||
      (category.description && category.description.toLowerCase().includes(term))
    );
  }

  openCreateCategoryModal(): void {
    this.isCategoryEditMode = false;
    this.categoryForm = {
      id: undefined,
      name: '',
      description: '',
      active: true
    };
    this.showCategoryModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  openEditCategoryModal(category: EventCategory): void {
    this.isCategoryEditMode = true;
    this.categoryForm = { ...category };
    this.showCategoryModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeCategoryModal(): void {
    this.showCategoryModal = false;
    this.categoryForm = {
      id: undefined,
      name: '',
      description: '',
      active: true
    };
    this.errorMessage = '';
    this.successMessage = '';
    this.isSubmitting = false;
  }

  onCategorySubmit(): void {
    if (!this.categoryForm.name || this.categoryForm.name.trim() === '') {
      this.errorMessage = 'El nombre de la categoría es obligatorio';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const categoryData = {
      name: this.categoryForm.name.trim(),
      description: this.categoryForm.description?.trim() || '',
      active: this.categoryForm.active
    };

    if (this.isCategoryEditMode && this.categoryForm.id) {
      // Actualizar categoría existente
      this.eventService.updateEventCategory(this.categoryForm.id, categoryData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.successMessage = 'Categoría actualizada exitosamente';
          setTimeout(() => {
            this.closeCategoryModal();
            this.loadCategories();
          }, 1500);
        },
        error: (error: any) => {
          this.isSubmitting = false;
          console.error('Error al actualizar categoría:', error);
          this.errorMessage = error.error?.message || 'Error al actualizar la categoría. Por favor, intenta nuevamente.';
        }
      });
    } else {
      // Crear nueva categoría
      this.eventService.createEventCategory(categoryData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.successMessage = 'Categoría creada exitosamente';
          setTimeout(() => {
            this.closeCategoryModal();
            this.loadCategories();
          }, 1500);
        },
        error: (error: any) => {
          this.isSubmitting = false;
          console.error('Error al crear categoría:', error);
          this.errorMessage = error.error?.message || 'Error al crear la categoría. Por favor, intenta nuevamente.';
        }
      });
    }
  }

  deleteCategory(categoryId: number, categoryName: string): void {
    Swal.fire({
      title: '¿Eliminar categoría?',
      text: `¿Estás seguro de que deseas eliminar la categoría "${categoryName}"?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        this.eventService.deleteEventCategory(categoryId).subscribe({
          next: () => {
            Swal.fire('Eliminado', 'Categoría eliminada exitosamente', 'success');
            this.loadCategories();
          },
          error: (error: any) => {
            console.error('Error al eliminar categoría:', error);
            Swal.fire('Error', error.error?.message || 'Error al eliminar la categoría. Por favor, intenta nuevamente.', 'error');
          }
        });
      }
    });
  }

  toggleCategoryStatus(category: EventCategory): void {
    const updatedCategory = {
      ...category,
      active: !category.active
    };

    this.eventService.updateEventCategory(category.id!, updatedCategory).subscribe({
      next: () => {
        this.successMessage = `Categoría ${updatedCategory.active ? 'activada' : 'desactivada'} exitosamente`;
        this.loadCategories();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (error: any) => {
        console.error('Error al cambiar estado de categoría:', error);
        this.errorMessage = error.error?.message || 'Error al cambiar el estado de la categoría.';
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
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
