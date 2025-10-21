import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EventService } from '../../../core/services/event.service';
import { Consumption, ConsumptionCategory } from '../../../shared/models/event.model';

@Component({
  selector: 'app-consumptions-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './consumptions-management.component.html',
  styleUrls: ['./consumptions-management.component.css']
})
export class ConsumptionsManagementComponent implements OnInit {
  // Consumptions
  consumptions: Consumption[] = [];
  filteredConsumptions: Consumption[] = [];
  
  // Consumption Categories
  consumptionCategories: ConsumptionCategory[] = [];
  activeConsumptionCategories: ConsumptionCategory[] = [];
  
  // UI State
  activeTab: 'consumptions' | 'categories' = 'consumptions';
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  
  // Modals
  showConsumptionModal = false;
  showCategoryModal = false;
  
  // Forms
  consumptionForm: Consumption = this.getEmptyConsumption();
  categoryForm: ConsumptionCategory = this.getEmptyCategory();
  isEditMode = false;
  
  // Search
  searchTerm = '';

  constructor(private eventService: EventService) {}

  ngOnInit(): void {
    this.loadConsumptions();
    this.loadCategories();
  }

  // ============================================
  // CONSUMPTIONS CRUD
  // ============================================

  loadConsumptions(): void {
    this.isLoading = true;
    this.eventService.getConsumptions().subscribe({
      next: (data) => {
        this.consumptions = data;
        this.filteredConsumptions = data;
        this.isLoading = false;
      },
      error: (error) => {
        this.showError('Error al cargar consumos: ' + error.message);
        this.isLoading = false;
      }
    });
  }

  loadCategories(): void {
    this.eventService.getConsumptionCategories().subscribe({
      next: (data) => {
        this.consumptionCategories = data;
      },
      error: (error) => {
        this.showError('Error al cargar categorías: ' + error.message);
      }
    });

    this.eventService.getActiveConsumptionCategories().subscribe({
      next: (data) => {
        this.activeConsumptionCategories = data;
      },
      error: (error) => console.error('Error loading active categories', error)
    });
  }

  openConsumptionModal(consumption?: Consumption): void {
    if (consumption) {
      this.consumptionForm = { ...consumption };
      this.isEditMode = true;
    } else {
      this.consumptionForm = this.getEmptyConsumption();
      this.isEditMode = false;
    }
    this.showConsumptionModal = true;
  }

  closeConsumptionModal(): void {
    this.showConsumptionModal = false;
    this.consumptionForm = this.getEmptyConsumption();
    this.isEditMode = false;
  }

  saveConsumption(): void {
    if (!this.validateConsumptionForm()) {
      return;
    }

    this.isLoading = true;

    const operation = this.isEditMode
      ? this.eventService.updateConsumption(this.consumptionForm.id!, this.consumptionForm)
      : this.eventService.createConsumption(this.consumptionForm);

    operation.subscribe({
      next: () => {
        this.showSuccess(
          this.isEditMode
            ? 'Consumo actualizado exitosamente'
            : 'Consumo creado exitosamente'
        );
        this.closeConsumptionModal();
        this.loadConsumptions();
        this.isLoading = false;
      },
      error: (error) => {
        this.showError('Error al guardar consumo: ' + error.message);
        this.isLoading = false;
      }
    });
  }

  deleteConsumption(id: number): void {
    if (!confirm('¿Estás seguro de que deseas eliminar este consumo?')) {
      return;
    }

    this.eventService.deleteConsumption(id).subscribe({
      next: () => {
        this.showSuccess('Consumo eliminado exitosamente');
        this.loadConsumptions();
      },
      error: (error) => {
        this.showError('Error al eliminar consumo: ' + error.message);
      }
    });
  }

  // ============================================
  // CATEGORIES CRUD
  // ============================================

  openCategoryModal(category?: ConsumptionCategory): void {
    if (category) {
      this.categoryForm = { ...category };
      this.isEditMode = true;
    } else {
      this.categoryForm = this.getEmptyCategory();
      this.isEditMode = false;
    }
    this.showCategoryModal = true;
  }

  closeCategoryModal(): void {
    this.showCategoryModal = false;
    this.categoryForm = this.getEmptyCategory();
    this.isEditMode = false;
  }

  saveCategory(): void {
    if (!this.validateCategoryForm()) {
      return;
    }

    this.isLoading = true;

    const operation = this.isEditMode
      ? this.eventService.updateConsumptionCategory(this.categoryForm.id!, this.categoryForm)
      : this.eventService.createConsumptionCategory(this.categoryForm);

    operation.subscribe({
      next: () => {
        this.showSuccess(
          this.isEditMode
            ? 'Categoría actualizada exitosamente'
            : 'Categoría creada exitosamente'
        );
        this.closeCategoryModal();
        this.loadCategories();
        this.isLoading = false;
      },
      error: (error) => {
        this.showError('Error al guardar categoría: ' + error.message);
        this.isLoading = false;
      }
    });
  }

  deleteCategory(id: number): void {
    if (!confirm('¿Estás seguro de que deseas eliminar esta categoría?')) {
      return;
    }

    this.eventService.deleteConsumptionCategory(id).subscribe({
      next: () => {
        this.showSuccess('Categoría eliminada exitosamente');
        this.loadCategories();
      },
      error: (error) => {
        this.showError('Error al eliminar categoría: ' + error.message);
      }
    });
  }

  toggleCategoryStatus(id: number): void {
    this.eventService.toggleConsumptionCategoryStatus(id).subscribe({
      next: () => {
        this.showSuccess('Estado de categoría actualizado');
        this.loadCategories();
      },
      error: (error) => {
        this.showError('Error al cambiar estado: ' + error.message);
      }
    });
  }

  // ============================================
  // UTILITIES
  // ============================================

  switchTab(tab: 'consumptions' | 'categories'): void {
    this.activeTab = tab;
    this.clearMessages();
  }

  filterConsumptions(): void {
    if (!this.searchTerm.trim()) {
      this.filteredConsumptions = this.consumptions;
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredConsumptions = this.consumptions.filter(c =>
      c.name.toLowerCase().includes(term) ||
      c.description?.toLowerCase().includes(term)
    );
  }

  getCategoryName(categoryId: number): string {
    const category = this.consumptionCategories.find(c => c.id === categoryId);
    return category?.name || 'Sin categoría';
  }

  validateConsumptionForm(): boolean {
    if (!this.consumptionForm.name || !this.consumptionForm.name.trim()) {
      this.showError('El nombre es requerido');
      return false;
    }
    if (!this.consumptionForm.categoryId) {
      this.showError('La categoría es requerida');
      return false;
    }
    if (!this.consumptionForm.price || this.consumptionForm.price <= 0) {
      this.showError('El precio debe ser mayor a 0');
      return false;
    }
    return true;
  }

  validateCategoryForm(): boolean {
    if (!this.categoryForm.name || !this.categoryForm.name.trim()) {
      this.showError('El nombre es requerido');
      return false;
    }
    return true;
  }

  getEmptyConsumption(): Consumption {
    return {
      categoryId: 0,
      name: '',
      description: '',
      price: 0,
      imageUrl: '',
      active: true
    };
  }

  getEmptyCategory(): ConsumptionCategory {
    return {
      name: '',
      description: '',
      active: true
    };
  }

  showSuccess(message: string): void {
    this.successMessage = message;
    this.errorMessage = '';
    setTimeout(() => this.successMessage = '', 3000);
  }

  showError(message: string): void {
    this.errorMessage = message;
    this.successMessage = '';
    setTimeout(() => this.errorMessage = '', 5000);
  }

  clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  handleImageError(event: any): void {
    event.target.style.display = 'none';
    event.target.parentElement.classList.add('image-error');
  }
}
