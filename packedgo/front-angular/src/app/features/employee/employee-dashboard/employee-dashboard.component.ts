import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import Swal from 'sweetalert2';
import { AuthService } from '../../../core/services/auth.service';
import { EmployeeService } from '../../../core/services/employee.service';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { BarcodeFormat } from '@zxing/library';

@Component({
  selector: 'app-employee-dashboard',
  standalone: true,
  imports: [CommonModule, ZXingScannerModule],
  templateUrl: './employee-dashboard.component.html',
  styleUrl: './employee-dashboard.component.css'
})
export class EmployeeDashboardComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private employeeService = inject(EmployeeService);
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

  // Scanner configuration
  availableDevices: MediaDeviceInfo[] = [];
  currentDevice: MediaDeviceInfo | undefined;
  hasDevices: boolean = false;
  hasPermission: boolean | null = null;
  allowedFormats = [BarcodeFormat.QR_CODE];

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
    console.log('Cargando eventos asignados al empleado...');

    this.employeeService.getAssignedEvents().subscribe({
      next: (events) => {
        this.assignedEvents = events.map(e => ({
          id: e.id,
          name: e.name,
          date: e.eventDate ? new Date(e.eventDate) : new Date(),
          location: e.location || 'Sin ubicación',
          status: e.status || 'ACTIVE'
        }));

        // Auto-select first event if available
        if (this.assignedEvents.length > 0) {
          this.selectedEventId = this.assignedEvents[0].id;
        }
      },
      error: (err) => {
        console.error('Error cargando eventos:', err);
        Swal.fire('Error', 'No se pudieron cargar los eventos asignados', 'error');
      }
    });
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
      Swal.fire('Atención', 'Por favor, selecciona un evento primero', 'warning');
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
      this.handleConsumptionScan(result);
    }
  }

  onCamerasFound(devices: MediaDeviceInfo[]): void {
    this.availableDevices = devices;
    this.hasDevices = devices && devices.length > 0;

    // Select back camera by default if available
    const backCamera = devices.find(device =>
      device.label.toLowerCase().includes('back') ||
      device.label.toLowerCase().includes('rear')
    );
    this.currentDevice = backCamera || devices[0];
  }

  onCameraPermission(hasPermission: boolean): void {
    this.hasPermission = hasPermission;
    if (!hasPermission) {
      Swal.fire('Permiso denegado', 'Necesitas permitir el acceso a la cámara para escanear QR', 'warning');
    }
  }

  onScanError(error: any): void {
    console.error('Error en scanner:', error);
  }

  private validateTicket(qrCode: string): void {
    if (!this.selectedEventId) return;

    console.log('Validando ticket:', qrCode, 'para evento:', this.selectedEventId);

    // Pre-validación del formato y evento
    const qrPattern = /PACKEDGO\|T:(\d+)(?:\|TC:(\d+))?\|E:(\d+)\|U:(\d+)\|TS:(\d+)/;
    const match = qrCode.match(qrPattern);

    if (!match) {
      Swal.fire('QR Inválido', 'El código QR no tiene el formato correcto', 'error');
      return;
    }

    const eventIdFromQR = parseInt(match[3]);
    if (eventIdFromQR !== this.selectedEventId) {
      Swal.fire({
        icon: 'warning',
        title: 'Evento Incorrecto',
        text: `Este QR pertenece al evento #${eventIdFromQR}, pero estás operando en el evento #${this.selectedEventId}. Por favor cambia de evento o escanea el QR correcto.`
      });
      return;
    }

    this.employeeService.validateTicket(qrCode, this.selectedEventId).subscribe({
      next: (response) => {
        const scanRecord: ScanRecord = {
          id: Date.now(),
          type: 'ticket',
          qrCode: qrCode,
          timestamp: new Date(),
          status: response.valid ? 'success' : 'error',
          message: response.message,
          eventName: response.ticketInfo?.eventName || this.getSelectedEvent()?.name
        };

        this.scanHistory.unshift(scanRecord);

        if (response.valid) {
          this.stats.ticketsScannedToday++;
          this.stats.totalScannedToday++;
          Swal.fire({
            icon: 'success',
            title: '¡Entrada autorizada!',
            html: `
              <p><strong>Cliente:</strong> ${response.ticketInfo?.customerName}</p>
              <p><strong>Pass:</strong> ${response.ticketInfo?.passType}</p>
            `,
            timer: 2000,
            showConfirmButton: false
          });
        } else {
          Swal.fire('Entrada denegada', response.message, 'error');
        }
      },
      error: (err) => {
        console.error('Error validando ticket:', err);
        const scanRecord: ScanRecord = {
          id: Date.now(),
          type: 'ticket',
          qrCode: qrCode,
          timestamp: new Date(),
          status: 'error',
          message: err.error?.message || 'Error al validar ticket',
          eventName: this.getSelectedEvent()?.name
        };
        this.scanHistory.unshift(scanRecord);
        Swal.fire('Error', err.error?.message || 'No se pudo validar el ticket', 'error');
      }
    });
  }

  private handleConsumptionScan(qrCode: string): void {
    if (!this.selectedEventId) return;

    console.log('Escaneando consumo:', qrCode);

    // Extract ticketConsumptionId from QR
    // Allow TC to be optional (some QRs might only have T which we'll use as ID)
    const qrPattern = /PACKEDGO\|T:(\d+)(?:\|TC:(\d+))?\|E:(\d+)\|U:(\d+)\|TS:(\d+)/;
    const match = qrCode.match(qrPattern);

    if (!match) {
      Swal.fire('QR Inválido', 'El código QR no tiene el formato correcto', 'error');
      return;
    }

    const ticketId = parseInt(match[1]);
    const ticketConsumptionId = match[2] ? parseInt(match[2]) : null;
    const eventIdFromQR = parseInt(match[3]);

    if (eventIdFromQR !== this.selectedEventId) {
      Swal.fire({
        icon: 'warning',
        title: 'Evento Incorrecto',
        text: `Este QR pertenece al evento #${eventIdFromQR}, pero estás operando en el evento #${this.selectedEventId}.`
      });
      return;
    }

    let requestObservable;

    if (ticketConsumptionId) {
      requestObservable = this.employeeService.getTicketConsumptions(ticketConsumptionId);
    } else {
      requestObservable = this.employeeService.getTicketConsumptionsByTicket(ticketId);
    }

    // Get available consumptions for this ticket
    requestObservable.subscribe({
      next: (consumptions) => {
        const availableConsumptions = consumptions.filter(c => c.active && !c.redeem && c.quantity > 0);

        if (availableConsumptions.length === 0) {
          Swal.fire('Sin consumiciones', 'Este ticket no tiene consumiciones disponibles', 'warning');
          return;
        }

        // Show list of consumptions for employee to select
        this.showConsumptionSelector(qrCode, availableConsumptions);
      },
      error: (err) => {
        console.error('Error obteniendo consumiciones:', err);
        Swal.fire('Error', 'No se pudieron cargar las consumiciones del ticket', 'error');
      }
    });
  }

  private showConsumptionSelector(qrCode: string, consumptions: any[]): void {
    const options = consumptions.map(c =>
      `<div style="padding: 10px; border: 1px solid #ddd; margin: 5px; border-radius: 5px; cursor: pointer;" data-detail-id="${c.id}">
        <strong>${c.consumptionName || 'Producto sin nombre'}</strong>
        <small>Cantidad disponible: ${c.quantity}</small>
      </div>`
    ).join('');

    Swal.fire({
      title: 'Seleccionar consumición',
      html: `<div id="consumption-list" style="max-height: 400px; overflow-y: auto;">${options}</div>`,
      showCancelButton: true,
      confirmButtonText: 'Cancelar',
      showConfirmButton: false,
      didOpen: () => {
        const listContainer = document.getElementById('consumption-list');
        listContainer?.querySelectorAll('[data-detail-id]').forEach(element => {
          element.addEventListener('click', () => {
            const detailId = parseInt(element.getAttribute('data-detail-id') || '0');
            const consumption = consumptions.find(c => c.id === detailId);
            Swal.close();
            this.confirmConsumption(qrCode, detailId, consumption);
          });
        });
      }
    });
  }

  private confirmConsumption(qrCode: string, detailId: number, consumption: any): void {
    if (!this.selectedEventId) return;

    Swal.fire({
      title: '¿Canjear consumición?',
      html: `
        <p><strong>${consumption.consumptionName || 'Producto'}</strong></p>
        <p>Cantidad disponible: ${consumption.quantity}</p>
      `,
      input: 'number',
      inputValue: 1,
      inputAttributes: {
        min: '1',
        max: consumption.quantity.toString(),
        step: '1'
      },
      inputLabel: 'Cantidad a canjear',
      showCancelButton: true,
      confirmButtonText: 'Canjear',
      cancelButtonText: 'Cancelar',
      inputValidator: (value) => {
        const qty = parseInt(value);
        if (!qty || qty < 1) {
          return 'Ingresa una cantidad válida';
        }
        if (qty > consumption.quantity) {
          return `La cantidad no puede ser mayor a ${consumption.quantity}`;
        }
        return null;
      }
    }).then((result) => {
      if (result.isConfirmed) {
        const quantity = parseInt(result.value);
        this.registerConsumption(qrCode, detailId, quantity, consumption.consumptionName);
      }
    });
  }

  private registerConsumption(qrCode: string, detailId: number, quantity: number, consumptionName: string): void {
    if (!this.selectedEventId) return;

    this.employeeService.registerConsumption({
      qrCode,
      eventId: this.selectedEventId,
      detailId,
      quantity
    }).subscribe({
      next: (response) => {
        const scanRecord: ScanRecord = {
          id: Date.now(),
          type: 'consumption',
          qrCode: qrCode,
          timestamp: new Date(),
          status: response.success ? 'success' : 'error',
          message: `${consumptionName} - ${response.message}`,
          eventName: this.getSelectedEvent()?.name
        };

        this.scanHistory.unshift(scanRecord);

        if (response.success) {
          this.stats.consumptionsToday++;
          this.stats.totalScannedToday++;

          Swal.fire({
            icon: 'success',
            title: '¡Consumición canjeada!',
            html: `
              <p><strong>${response.consumptionInfo?.consumptionName}</strong></p>
              <p>Cantidad: ${response.consumptionInfo?.quantityRedeemed}</p>
              ${response.consumptionInfo?.remainingQuantity ?
                `<p>Restante: ${response.consumptionInfo.remainingQuantity}</p>` :
                '<p>Totalmente canjeado</p>'}
            `,
            timer: 2000,
            showConfirmButton: false
          });
        } else {
          Swal.fire('Error', response.message, 'error');
        }
      },
      error: (err) => {
        console.error('Error registrando consumo:', err);
        Swal.fire('Error', 'No se pudo registrar la consumición', 'error');
      }
    });
  }

  private loadStats(): void {
    this.employeeService.getStats().subscribe({
      next: (stats) => {
        this.stats = stats;
      },
      error: (err) => {
        console.error('Error cargando estadísticas:', err);
        // Keep default values
      }
    });
  }

  clearHistory(): void {
    Swal.fire({
      title: '¿Limpiar historial?',
      text: '¿Estás seguro de que quieres limpiar el historial?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, limpiar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        this.scanHistory = [];
        Swal.fire('Historial limpiado', '', 'success');
      }
    });
  }

  logout(): void {
    Swal.fire({
      title: '¿Cerrar sesión?',
      text: '¿Estás seguro de que quieres cerrar sesión?',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, salir',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.authService.logout();
        this.router.navigate(['/employee/login']);
      }
    });
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
