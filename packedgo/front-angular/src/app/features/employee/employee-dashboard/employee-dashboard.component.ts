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
      `<div style="padding: 10px; border: 1px solid #ddd; margin: 5px; border-radius: 5px; display: flex; align-items: center; gap: 10px;">
        <input type="checkbox" id="consumption-${c.id}" data-detail-id="${c.id}" style="width: 20px; height: 20px; cursor: pointer;">
        <label for="consumption-${c.id}" style="flex: 1; cursor: pointer; margin: 0;">
          <strong>${c.consumptionName || 'Producto sin nombre'}</strong><br>
          <small>Disponible: ${c.quantity}</small>
        </label>
        <input type="number" id="quantity-${c.id}" min="1" max="${c.quantity}" value="1" 
               style="width: 60px; padding: 5px; border: 1px solid #ddd; border-radius: 4px;" 
               disabled data-max="${c.quantity}">
      </div>`
    ).join('');

    Swal.fire({
      title: 'Seleccionar consumiciones',
      html: `
        <div style="text-align: left; margin-bottom: 15px;">
          <p style="margin: 0; color: #666; font-size: 14px;">✓ Marca las consumiciones que deseas canjear</p>
        </div>
        <div id="consumption-list" style="max-height: 400px; overflow-y: auto; text-align: left;">
          ${options}
        </div>
      `,
      showCancelButton: true,
      confirmButtonText: 'Canjear Seleccionadas',
      cancelButtonText: 'Cancelar',
      preConfirm: () => {
        const selected: Array<{detailId: number, quantity: number, consumption: any}> = [];
        consumptions.forEach(c => {
          const checkbox = document.getElementById(`consumption-${c.id}`) as HTMLInputElement;
          const quantityInput = document.getElementById(`quantity-${c.id}`) as HTMLInputElement;
          
          if (checkbox?.checked) {
            const quantity = parseInt(quantityInput.value);
            if (quantity > 0 && quantity <= c.quantity) {
              selected.push({
                detailId: c.id,
                quantity: quantity,
                consumption: c
              });
            }
          }
        });

        if (selected.length === 0) {
          Swal.showValidationMessage('Selecciona al menos una consumición');
          return false;
        }

        return selected;
      },
      didOpen: () => {
        // Enable/disable quantity inputs based on checkbox state
        consumptions.forEach(c => {
          const checkbox = document.getElementById(`consumption-${c.id}`) as HTMLInputElement;
          const quantityInput = document.getElementById(`quantity-${c.id}`) as HTMLInputElement;
          
          checkbox?.addEventListener('change', () => {
            if (quantityInput) {
              quantityInput.disabled = !checkbox.checked;
              if (checkbox.checked) {
                quantityInput.focus();
              }
            }
          });

          // Validate quantity on input
          quantityInput?.addEventListener('input', () => {
            const max = parseInt(quantityInput.getAttribute('data-max') || '1');
            const value = parseInt(quantityInput.value);
            if (value > max) {
              quantityInput.value = max.toString();
            }
            if (value < 1) {
              quantityInput.value = '1';
            }
          });
        });
      }
    }).then((result) => {
      if (result.isConfirmed && result.value) {
        this.confirmMultipleConsumptions(qrCode, result.value);
      }
    });
  }

  private confirmMultipleConsumptions(qrCode: string, selections: Array<{detailId: number, quantity: number, consumption: any}>): void {
    if (!this.selectedEventId) return;

    // Mostrar confirmación con resumen
    const summaryHtml = selections.map(s => 
      `<div style="padding: 8px; border-bottom: 1px solid #eee;">
        <strong>${s.consumption.consumptionName}</strong>
        <span style="float: right; color: #667eea; font-weight: bold;">x${s.quantity}</span>
      </div>`
    ).join('');

    Swal.fire({
      title: '¿Confirmar canje?',
      html: `
        <div style="text-align: left; max-height: 300px; overflow-y: auto; margin: 10px 0;">
          ${summaryHtml}
        </div>
        <p style="margin-top: 15px; color: #666;">Se canjearán ${selections.length} consumición(es)</p>
      `,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, canjear',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#667eea'
    }).then((result) => {
      if (result.isConfirmed) {
        this.registerMultipleConsumptions(qrCode, selections);
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

  private registerMultipleConsumptions(qrCode: string, selections: Array<{detailId: number, quantity: number, consumption: any}>): void {
    if (!this.selectedEventId) return;

    let successCount = 0;
    let failCount = 0;
    const results: string[] = [];

    // Mostrar loading
    Swal.fire({
      title: 'Procesando canjes...',
      html: 'Por favor espera',
      allowOutsideClick: false,
      didOpen: () => {
        Swal.showLoading();
      }
    });

    // Procesar cada selección secuencialmente
    const processNext = (index: number) => {
      if (index >= selections.length) {
        // Todas las consumiciones procesadas
        this.showMultipleConsumptionsResult(successCount, failCount, results, qrCode);
        return;
      }

      const selection = selections[index];
      this.employeeService.registerConsumption({
        qrCode,
        eventId: this.selectedEventId!,
        detailId: selection.detailId,
        quantity: selection.quantity
      }).subscribe({
        next: (response) => {
          if (response.success) {
            successCount++;
            this.stats.consumptionsToday++;
            this.stats.totalScannedToday++;
            results.push(`✓ ${selection.consumption.consumptionName} (x${selection.quantity})`);
          } else {
            failCount++;
            results.push(`✗ ${selection.consumption.consumptionName}: ${response.message}`);
          }
          
          const scanRecord: ScanRecord = {
            id: Date.now() + index,
            type: 'consumption',
            qrCode: qrCode,
            timestamp: new Date(),
            status: response.success ? 'success' : 'error',
            message: `${selection.consumption.consumptionName} (x${selection.quantity}) - ${response.message}`,
            eventName: this.getSelectedEvent()?.name
          };
          this.scanHistory.unshift(scanRecord);
          
          processNext(index + 1);
        },
        error: (err) => {
          failCount++;
          results.push(`✗ ${selection.consumption.consumptionName}: Error de conexión`);
          
          const scanRecord: ScanRecord = {
            id: Date.now() + index,
            type: 'consumption',
            qrCode: qrCode,
            timestamp: new Date(),
            status: 'error',
            message: `${selection.consumption.consumptionName} - Error: ${err.error?.message || 'Error de conexión'}`,
            eventName: this.getSelectedEvent()?.name
          };
          this.scanHistory.unshift(scanRecord);
          
          processNext(index + 1);
        }
      });
    };

    processNext(0);
  }

  private showMultipleConsumptionsResult(successCount: number, failCount: number, results: string[], qrCode: string): void {
    const resultsHtml = results.map(r => `<div style="padding: 5px; text-align: left;">${r}</div>`).join('');
    
    if (failCount === 0) {
      Swal.fire({
        icon: 'success',
        title: '¡Canjes completados!',
        html: `
          <p><strong>${successCount} consumición(es) canjeadas exitosamente</strong></p>
          <div style="max-height: 200px; overflow-y: auto; margin-top: 10px;">
            ${resultsHtml}
          </div>
        `,
        timer: 3000,
        showConfirmButton: false
      });
    } else if (successCount === 0) {
      Swal.fire({
        icon: 'error',
        title: 'Error en los canjes',
        html: `
          <p>No se pudo canjear ninguna consumición</p>
          <div style="max-height: 200px; overflow-y: auto; margin-top: 10px;">
            ${resultsHtml}
          </div>
        `
      });
    } else {
      Swal.fire({
        icon: 'warning',
        title: 'Canjes parcialmente completados',
        html: `
          <p><strong>Exitosos:</strong> ${successCount} | <strong>Fallidos:</strong> ${failCount}</p>
          <div style="max-height: 200px; overflow-y: auto; margin-top: 10px;">
            ${resultsHtml}
          </div>
        `
      });
    }
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
