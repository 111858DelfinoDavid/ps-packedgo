import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { MultiOrderCheckoutResponse } from '../../../shared/models/order.model';

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

  sessionId: string = '';
  sessionData: MultiOrderCheckoutResponse | null = null;
  isLoading = true;

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
      },
      error: (error) => {
        console.error('Error loading session:', error);
        this.isLoading = false;
      }
    });
  }

  goToDashboard(): void {
    this.router.navigate(['/customer/dashboard']);
  }

  goToTickets(): void {
    // TODO: Navegar a la sección de tickets cuando esté implementada
    this.router.navigate(['/customer/dashboard'], { queryParams: { tab: 'tickets' } });
  }
}
