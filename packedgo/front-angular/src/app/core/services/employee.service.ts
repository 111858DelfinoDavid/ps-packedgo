import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

// Interfaces
export interface Employee {
  id: number;
  email: string;
  username: string;
  document: number;
  assignedEventIds: number[];
  createdAt: string;
  isActive: boolean;
}

export interface EmployeeWithEvents {
  id: number;
  email: string;
  username: string;
  document: number;
  assignedEvents: AssignedEvent[];
  createdAt: string;
  isActive: boolean;
}

export interface AssignedEvent {
  id: number;
  name: string;
  location?: string;
  eventDate?: string;
  status?: string;
}

export interface CreateEmployeeRequest {
  email: string;
  username: string;
  password: string;
  document: number;
  assignedEventIds: number[];
}

export interface UpdateEmployeeRequest {
  email: string;
  username: string;
  document: number;
  assignedEventIds: number[];
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.usersServiceUrl}/admin/employees`;

  /**
   * Obtiene los headers con el token JWT
   */
  private getHeaders(): { headers: { Authorization: string } } {
    const token = localStorage.getItem('token');
    return {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    };
  }

  /**
   * Obtiene todos los empleados del admin actual (con sus eventos asignados)
   */
  getMyEmployees(): Observable<EmployeeWithEvents[]> {
    return this.http.get<ApiResponse<EmployeeWithEvents[]>>(this.apiUrl, this.getHeaders())
      .pipe(map(response => response.data));
  }

  /**
   * Obtiene un empleado específico por ID
   */
  getEmployeeById(id: number): Observable<EmployeeWithEvents> {
    return this.http.get<ApiResponse<EmployeeWithEvents>>(`${this.apiUrl}/${id}`, this.getHeaders())
      .pipe(map(response => response.data));
  }

  /**
   * Crea un nuevo empleado
   */
  createEmployee(request: CreateEmployeeRequest): Observable<Employee> {
    return this.http.post<ApiResponse<Employee>>(this.apiUrl, request, this.getHeaders())
      .pipe(map(response => response.data));
  }

  /**
   * Actualiza un empleado existente
   */
  updateEmployee(id: number, request: UpdateEmployeeRequest): Observable<Employee> {
    return this.http.put<ApiResponse<Employee>>(`${this.apiUrl}/${id}`, request, this.getHeaders())
      .pipe(map(response => response.data));
  }

  /**
   * Cambia el estado activo/inactivo de un empleado
   */
  toggleEmployeeStatus(id: number): Observable<void> {
    return this.http.patch<ApiResponse<void>>(`${this.apiUrl}/${id}/toggle-status`, {}, this.getHeaders())
      .pipe(map(() => undefined));
  }

  /**
   * Elimina un empleado (soft delete - marca como inactivo)
   */
  deleteEmployee(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`, this.getHeaders())
      .pipe(map(() => undefined));
  }

  /**
   * Obtiene empleados asignados a un evento específico
   */
  getEmployeesByEvent(eventId: number): Observable<EmployeeWithEvents[]> {
    return this.http.get<ApiResponse<EmployeeWithEvents[]>>(
      `${this.apiUrl}/by-event/${eventId}`,
      this.getHeaders()
    ).pipe(map(response => response.data));
  }
}
