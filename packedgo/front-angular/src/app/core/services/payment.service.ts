import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { 
  PaymentPreference, 
  CreatePaymentRequest 
} from '../../shared/models/order.model';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private http = inject(HttpClient);
  private apiUrl = environment.paymentsServiceUrl;

  /**
   * Obtiene los headers con el token JWT
   */
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  /**
   * Crea una preferencia de pago en Mercado Pago para una orden específica
   * @param adminId ID del administrador (dueño del evento)
   * @param orderNumber Número de la orden
   * @param amount Monto a pagar
   * @returns Observable con la preferencia de pago (incluye QR y URL de checkout)
   */
  createPaymentPreference(
    adminId: number, 
    orderNumber: string, 
    amount: number
  ): Observable<PaymentPreference> {
    const request: CreatePaymentRequest = {
      adminId,
      orderId: orderNumber,
      amount
    };

    return this.http.post<PaymentPreference>(
      `${this.apiUrl}/payments/create`,
      request,
      { headers: this.getHeaders() }
    ).pipe(
      tap(preference => {
        console.log('Preferencia de pago creada:', preference);
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Obtiene el estado de un pago
   * @param preferenceId ID de la preferencia de Mercado Pago
   * @returns Observable con el estado del pago
   */
  getPaymentStatus(preferenceId: string): Observable<any> {
    return this.http.get(
      `${this.apiUrl}/payments/status/${preferenceId}`,
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Maneja el callback de Mercado Pago (generalmente se maneja en el backend)
   * Este método puede ser usado para verificar manualmente el estado
   */
  verifyPaymentCallback(paymentId: string): Observable<any> {
    return this.http.get(
      `${this.apiUrl}/payments/verify/${paymentId}`,
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Maneja errores HTTP
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Ocurrió un error desconocido';
    
    if (error.error instanceof ErrorEvent) {
      // Error del lado del cliente
      errorMessage = `Error: ${error.error.message}`;
    } else {
      // Error del lado del servidor
      switch (error.status) {
        case 400:
          errorMessage = error.error?.message || 'Datos de pago inválidos';
          break;
        case 401:
          errorMessage = 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.';
          localStorage.removeItem('token');
          localStorage.removeItem('userRole');
          window.location.href = '/customer/login';
          break;
        case 404:
          errorMessage = error.error?.message || 'Pago no encontrado';
          break;
        case 500:
          errorMessage = error.error?.message || 'Error al procesar el pago';
          break;
        case 503:
          errorMessage = 'Servicio de pagos no disponible. Intenta más tarde';
          break;
        default:
          errorMessage = error.error?.message || `Error del servidor: ${error.status}`;
      }
    }
    
    console.error('PaymentService Error:', errorMessage, error);
    return throwError(() => new Error(errorMessage));
  }
}
