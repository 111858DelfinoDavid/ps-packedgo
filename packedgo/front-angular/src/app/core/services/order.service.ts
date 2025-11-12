import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { 
  MultiOrderCheckoutResponse, 
  Order, 
  MultiOrderSession,
  SessionStateResponse
} from '../../shared/models/order.model';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private http = inject(HttpClient);
  private apiUrl = environment.ordersServiceUrl;

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
   * Realiza el checkout multi-admin
   * Agrupa los items del carrito por adminId y crea múltiples órdenes
   * @returns Observable con la respuesta del checkout incluyendo los grupos de pago
   */
  checkoutMulti(): Observable<MultiOrderCheckoutResponse> {
    return this.http.post<MultiOrderCheckoutResponse>(
      `${this.apiUrl}/orders/checkout/multi`,
      {}, // Body vacío, el backend obtiene el userId del token
      { headers: this.getHeaders() }
    ).pipe(
      tap(response => {
        console.log('Checkout multi completado:', response);
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Obtiene el estado de una sesión multi-orden
   * @param sessionId ID de la sesión
   * @returns Observable con el estado actualizado de la sesión
   */
  getSessionStatus(sessionId: string): Observable<MultiOrderCheckoutResponse> {
    return this.http.get<MultiOrderCheckoutResponse>(
      `${this.apiUrl}/orders/sessions/${sessionId}`,
      { headers: this.getHeaders() }
    ).pipe(
      tap(response => {
        console.log('Estado de sesión:', response);
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Recupera una sesión usando el token de recuperación (sin JWT)
   * @param sessionToken Token de recuperación de la sesión
   * @returns Observable con el estado de la sesión
   */
  recoverSession(sessionToken: string): Observable<MultiOrderCheckoutResponse> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'X-Session-Token': sessionToken
    });

    return this.http.get<MultiOrderCheckoutResponse>(
      `${this.apiUrl}/orders/session/recover`,
      { headers }
    ).pipe(
      tap(response => {
        console.log('Sesión recuperada:', response);
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Backend State Authority: Obtiene o crea la sesión actual del usuario
   * El frontend NO guarda sessionId, el backend lo maneja TODO automáticamente
   * Usa JWT del usuario para identificar sesión activa o crear nueva
   * @returns Observable con el estado completo de la sesión (NUNCA falla)
   */
  getCurrentCheckoutState(): Observable<SessionStateResponse> {
    return this.http.get<SessionStateResponse>(
      `${this.apiUrl}/orders/checkout/current`
    ).pipe(
      tap(response => {
        console.log('✅ Estado actual de checkout:', response);
        console.log(`📊 Session: ${response.sessionId}, Status: ${response.sessionStatus}`);
        console.log(`💰 ${response.paidGroups}/${response.totalGroups} pagos completados`);
        console.log(`⏱️  ${response.secondsUntilExpiration}s restantes`);
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Obtiene todos los tickets generados en una sesión
   * @param sessionId ID de la sesión
   * @returns Observable con la lista de tickets
   */
  getSessionTickets(sessionId: string): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/orders/session/${sessionId}/tickets`
    ).pipe(
      tap(response => {
        console.log('Tickets de sesión:', response);
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Abandona una sesión de checkout y devuelve los items al carrito
   * Solo funciona si no hay pagos completados en la sesión
   * @param sessionId ID de la sesión a abandonar
   * @returns Observable con el resultado de la operación
   */
  abandonSession(sessionId: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/orders/sessions/${sessionId}/abandon`,
      {}, // Body vacío
      { headers: this.getHeaders() }
    ).pipe(
      tap(response => {
        console.log('Sesión abandonada:', response);
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Obtiene todas las órdenes del usuario actual
   * @returns Observable con el array de órdenes
   */
  getUserOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(
      `${this.apiUrl}/orders/user`,
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Obtiene una orden específica por ID
   * @param orderId ID de la orden
   * @returns Observable con los detalles de la orden
   */
  getOrderById(orderId: number): Observable<Order> {
    return this.http.get<Order>(
      `${this.apiUrl}/orders/${orderId}`,
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
          errorMessage = error.error?.message || 'Datos inválidos';
          break;
        case 401:
          errorMessage = 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.';
          localStorage.removeItem('token');
          localStorage.removeItem('userRole');
          window.location.href = '/customer/login';
          break;
        case 404:
          errorMessage = error.error?.message || 'Carrito no encontrado';
          break;
        case 409:
          errorMessage = error.error?.message || 'Conflicto al procesar la orden';
          break;
        case 410:
          errorMessage = error.error?.message || 'El carrito ha expirado';
          break;
        case 500:
          errorMessage = error.error?.message || 'Error interno del servidor';
          break;
        default:
          errorMessage = error.error?.message || `Error del servidor: ${error.status}`;
      }
    }
    
    console.error('OrderService Error:', errorMessage, error);
    return throwError(() => new Error(errorMessage));
  }
}
