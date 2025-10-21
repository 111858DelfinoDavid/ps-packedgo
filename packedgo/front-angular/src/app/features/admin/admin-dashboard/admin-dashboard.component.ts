import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { EventService } from '../../../core/services/event.service';
import { UserService } from '../../../core/services/user.service';

interface DashboardStats {
  totalEvents: number;
  activeEvents: number;
  totalUsers: number;
  totalCategories: number;
}

@Component({
  selector: 'app-admin-dashboard',
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private eventService = inject(EventService);
  private userService = inject(UserService);
  private router = inject(Router);

  currentUser = this.authService.getCurrentUser();
  isLoading = true;
  
  stats: DashboardStats = {
    totalEvents: 0,
    activeEvents: 0,
    totalUsers: 0,
    totalCategories: 0
  };

  quickActions = [
    {
      title: 'Gestionar Eventos',
      description: 'Crear, editar y eliminar eventos',
      icon: 'bi-calendar-event',
      route: '/admin/events',
      color: 'primary'
    },
    {
      title: 'Gestionar Consumos',
      description: 'Administrar consumos y categorías',
      icon: 'bi-cup-straw',
      route: '/admin/consumptions',
      color: 'danger'
    },
    {
      title: 'Gestionar Usuarios',
      description: 'Ver y administrar usuarios',
      icon: 'bi-people',
      route: '/admin/users',
      color: 'success'
    },
    {
      title: 'Categorías',
      description: 'Gestionar categorías de eventos',
      icon: 'bi-tags',
      route: '/admin/categories',
      color: 'warning'
    },
    {
      title: 'Analíticas',
      description: 'Ver estadísticas y reportes',
      icon: 'bi-graph-up',
      route: '/admin/analytics',
      color: 'info'
    }
  ];

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;

    // Cargar eventos
    this.eventService.getEvents().subscribe({
      next: (events: any) => {
        this.stats.totalEvents = events.length;
        this.stats.activeEvents = events.filter((e: any) => e.isActive).length;
      },
      error: (error: any) => console.error('Error al cargar eventos:', error)
    });

    // Cargar categorías de eventos
    this.eventService.getEventCategories().subscribe({
      next: (categories: any) => {
        this.stats.totalCategories = categories.length;
      },
      error: (error: any) => console.error('Error al cargar categorías:', error)
    });

    // Cargar usuarios
    this.userService.getAllUsers().subscribe({
      next: (users: any) => {
        this.stats.totalUsers = users.length;
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Error al cargar usuarios:', error);
        this.isLoading = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }
}
