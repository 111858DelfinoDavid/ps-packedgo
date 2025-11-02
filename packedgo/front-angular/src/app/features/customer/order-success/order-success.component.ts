import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { TicketService, Ticket } from '../../../core/services/ticket.service';
import { AuthService } from '../../../core/services/auth.service';
import { MultiOrderCheckoutResponse } from '../../../shared/models/order.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-order-success',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-success.component.html',
  styleUrls: ['./order-success.component.css']
})
export class OrderSuccessComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private orderService = inject(OrderService);
  private ticketService = inject(TicketService);
  private authService = inject(AuthService);

  sessionId: string = '';
  sessionData: MultiOrderCheckoutResponse | null = null;
  tickets: Ticket[] = [];
  isLoading = true;
  isLoadingTickets = false;

  ngOnInit(): void {
    // Obtener el sessionId de los query params
    this.route.queryParams.subscribe(params => {
      this.sessionId = params['sessionId'];
      if (this.sessionId) {
        this.loadSessionData();
      } else {
        this.router.navigate(['/customer/dashboard']);
      }
    });
  }

  loadSessionData(): void {
    this.orderService.getSessionStatus(this.sessionId).subscribe({
      next: (data) => {
        this.sessionData = data;
        this.isLoading = false;
        // Load tickets after session data is loaded
        this.loadTickets();
      },
      error: (error) => {
        console.error('Error loading session:', error);
        this.isLoading = false;
      }
    });
  }

  loadTickets(): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      console.error('No user ID available');
      return;
    }

    this.isLoadingTickets = true;
    this.ticketService.getActiveTickets(userId).subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.isLoadingTickets = false;
        console.log('✅ Tickets loaded:', tickets);
      },
      error: (error) => {
        console.error('❌ Error loading tickets:', error);
        this.isLoadingTickets = false;
      }
    });
  }

  downloadTicketQR(ticket: Ticket): void {
    if (ticket.qrCode) {
      this.ticketService.downloadQRCode(
        ticket.qrCode,
        `ticket-${ticket.ticketId}-${ticket.eventName.replace(/\s/g, '-')}.png`
      );
    }
  }

  downloadAllQRCodes(): void {
    const ticketsWithQR = this.tickets.filter(t => t.qrCode);
    if (ticketsWithQR.length > 0) {
      this.ticketService.downloadAllQRCodes(ticketsWithQR);
    }
  }

  hasTicketsWithQR(): boolean {
    return this.tickets.some(t => t.qrCode);
  }

  goToDashboard(): void {
    this.router.navigate(['/customer/dashboard']);
  }

  goToTickets(): void {
    // TODO: Navegar a la sección de tickets cuando esté implementada
    this.router.navigate(['/customer/dashboard'], { queryParams: { tab: 'tickets' } });
  }
}
