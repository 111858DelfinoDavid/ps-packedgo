import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-employee-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './employee-dashboard.component.html',
  styleUrl: './employee-dashboard.component.css'
})
export class EmployeeDashboardComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);

  employeeName: string = '';
  currentTime: Date = new Date();
  private timeInterval: any;

  // Employee assigned events
  assignedEvents: AssignedEvent[] = [];
  selectedEventId: number | null = null;

  // QR Scanner state
  scanMode: 'ticket' | 'consumption' | null = null;
  isScanning: boolean = false;
  lastScanResult: string | null = null;
  scanHistory: ScanRecord[] = [];

  // Stats
  stats = {
    ticketsScannedToday: 0,
    consumptionsToday: 0,
    totalScannedToday: 0
  };

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.employeeName = user.username || user.email;
    }

    // Update time every second
    this.timeInterval = setInterval(() => {
      this.currentTime = new Date();
    }, 1000);

    // Load assigned events
    this.loadAssignedEvents();

    // Load stats (in a real app, this would come from backend)
    this.loadStats();
  }

  loadAssignedEvents(): void {
    // TODO: Llamar al backend para obtener eventos asignados al empleado
    // GET /employee/assigned-events con JWT
    console.log('Cargando eventos asignados al empleado...');
    
    // Mock data
    this.assignedEvents = [
      {
        id: 1,
        name: 'Fiesta de Año Nuevo 2025',
        date: new Date('2024-12-31'),
        location: 'Club Central',
        status: 'ACTIVE'
      },
      {
        id: 2,
        name: 'Concierto Rock en Vivo',
        date: new Date('2024-12-15'),
        location: 'Estadio Municipal',
        status: 'ACTIVE'
      }
    ];

    // Auto-select first event if available
    if (this.assignedEvents.length > 0) {
      this.selectedEventId = this.assignedEvents[0].id;
    }
  }

  selectEvent(eventId: number): void {
    this.selectedEventId = eventId;
    this.stopScanning();
    console.log('Evento seleccionado:', eventId);
  }

  getSelectedEvent(): AssignedEvent | undefined {
    return this.assignedEvents.find(e => e.id === this.selectedEventId);
  }

  ngOnDestroy(): void {
    if (this.timeInterval) {
      clearInterval(this.timeInterval);
    }
    this.stopScanning();
  }

  startScanMode(mode: 'ticket' | 'consumption'): void {
    if (!this.selectedEventId) {
      alert('Por favor, selecciona un evento primero');
      return;
    }

    this.scanMode = mode;
    this.isScanning = true;
    this.lastScanResult = null;
    // TODO: Iniciar cámara para escaneo QR
    console.log(`Iniciando modo de escaneo: ${mode} para evento ${this.selectedEventId}`);
  }

  stopScanning(): void {
    this.isScanning = false;
    this.scanMode = null;
    // TODO: Detener cámara
    console.log('Deteniendo escaneo');
  }

  onScanSuccess(result: string): void {
    console.log('QR escaneado:', result);
    this.lastScanResult = result;
    this.isScanning = false;

    // Process scan based on mode
    if (this.scanMode === 'ticket') {
      this.validateTicket(result);
    } else if (this.scanMode === 'consumption') {
      this.registerConsumption(result);
    }
  }

  private validateTicket(qrCode: string): void {
    // TODO: Llamar al backend para validar el ticket del evento seleccionado
    // POST /employee/validate-ticket con { qrCode, eventId: this.selectedEventId }
    console.log('Validando ticket:', qrCode, 'para evento:', this.selectedEventId);
    
    // Mock response
    const scanRecord: ScanRecord = {
      id: Date.now(),
      type: 'ticket',
      qrCode: qrCode,
      timestamp: new Date(),
      status: 'success',
      message: 'Ticket válido - Entrada autorizada',
      eventName: this.getSelectedEvent()?.name
    };

    this.scanHistory.unshift(scanRecord);
    this.stats.ticketsScannedToday++;
    this.stats.totalScannedToday++;
  }

  private registerConsumption(qrCode: string): void {
    // TODO: Llamar al backend para registrar consumo del evento seleccionado
    // POST /employee/register-consumption con { qrCode, eventId: this.selectedEventId }
    console.log('Registrando consumo:', qrCode, 'para evento:', this.selectedEventId);

    // Mock response
    const scanRecord: ScanRecord = {
      id: Date.now(),
      type: 'consumption',
      qrCode: qrCode,
      timestamp: new Date(),
      status: 'success',
      message: 'Consumo registrado correctamente',
      eventName: this.getSelectedEvent()?.name
    };

    this.scanHistory.unshift(scanRecord);
    this.stats.consumptionsToday++;
    this.stats.totalScannedToday++;
  }

  private loadStats(): void {
    // TODO: Cargar estadísticas reales del backend
    // Por ahora, valores de ejemplo
    this.stats = {
      ticketsScannedToday: 0,
      consumptionsToday: 0,
      totalScannedToday: 0
    };
  }

  clearHistory(): void {
    if (confirm('¿Estás seguro de que quieres limpiar el historial?')) {
      this.scanHistory = [];
    }
  }

  logout(): void {
    if (confirm('¿Estás seguro de que quieres cerrar sesión?')) {
      this.authService.logout();
      this.router.navigate(['/employee/login']);
    }
  }
}

interface ScanRecord {
  id: number;
  type: 'ticket' | 'consumption';
  qrCode: string;
  timestamp: Date;
  status: 'success' | 'error' | 'warning';
  message: string;
  eventName?: string;
}

interface AssignedEvent {
  id: number;
  name: string;
  date: Date;
  location: string;
  status: string;
}
