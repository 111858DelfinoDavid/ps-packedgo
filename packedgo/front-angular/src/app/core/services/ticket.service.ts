import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export interface TicketConsumptionDetail {
  id: number;
  consumptionId: number;
  consumptionName: string;
  quantity: number;
  priceAtPurchase: number;
  active: boolean;
  redeem: boolean;
}

export interface TicketConsumption {
  id: number;
  redeem: boolean;
  ticketDetails: TicketConsumptionDetail[];
}

export interface Ticket {
  ticketId: number;
  userId: number;
  passCode: string;
  passId: number;
  qrCode?: string; // Base64 PNG image
  eventId: number;
  eventName: string;
  eventDate: string;
  eventLocation: string;
  active: boolean;
  redeemed: boolean;
  createdAt: string;
  purchasedAt: string;
  redeemedAt: string | null;
  ticketConsumption: TicketConsumption;
}

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private apiUrl = `${environment.eventsServiceUrl}/event-service/tickets`;

  /**
   * Obtiene todos los tickets de un usuario
   */
  getUserTickets(userId: number): Observable<Ticket[]> {
    const headers = this.getHeaders();
    return this.http.get<Ticket[]>(`${this.apiUrl}/user/${userId}`, { headers }).pipe(
      tap(tickets => console.log('✅ Tickets obtenidos:', tickets)),
      catchError(error => {
        console.error('❌ Error obteniendo tickets:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Obtiene solo los tickets activos de un usuario
   */
  getActiveTickets(userId: number): Observable<Ticket[]> {
    const headers = this.getHeaders();
    return this.http.get<Ticket[]>(`${this.apiUrl}/user/${userId}/active`, { headers }).pipe(
      tap(tickets => console.log('✅ Tickets activos obtenidos:', tickets)),
      catchError(error => {
        console.error('❌ Error obteniendo tickets activos:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Obtiene solo los tickets canjeados de un usuario
   */
  getRedeemedTickets(userId: number): Observable<Ticket[]> {
    const headers = this.getHeaders();
    return this.http.get<Ticket[]>(`${this.apiUrl}/user/${userId}/redeemed`, { headers }).pipe(
      tap(tickets => console.log('✅ Tickets canjeados obtenidos:', tickets)),
      catchError(error => {
        console.error('❌ Error obteniendo tickets canjeados:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Genera los headers con el token JWT
   */
  private getHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  /**
   * Download QR code as image
   */
  downloadQRCode(qrCode: string, fileName: string): void {
    if (!qrCode) {
      console.error('❌ No QR code available');
      return;
    }
    const link = document.createElement('a');
    link.href = qrCode;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  /**
   * Download all QR codes from a list of tickets
   */
  downloadAllQRCodes(tickets: Ticket[]): void {
    tickets.forEach((ticket, index) => {
      if (ticket.qrCode) {
        setTimeout(() => {
          this.downloadQRCode(
            ticket.qrCode!,
            `ticket-${ticket.ticketId}-${ticket.eventName.replace(/\s/g, '-')}.png`
          );
        }, index * 500); // Delay between downloads to avoid browser blocking
      }
    });
  }
}
