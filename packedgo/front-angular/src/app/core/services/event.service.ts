import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Event, EventCategory, ConsumptionCategory } from '../../shared/models/event.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EventService {
  private http = inject(HttpClient);
  private apiUrl = environment.eventsServiceUrl;

  // Event CRUD
  getEvents(): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.apiUrl}/events`);
  }

  getEventById(id: number): Observable<Event> {
    return this.http.get<Event>(`${this.apiUrl}/events/${id}`);
  }

  createEvent(event: Event): Observable<Event> {
    return this.http.post<Event>(`${this.apiUrl}/events`, event);
  }

  updateEvent(id: number, event: Event): Observable<Event> {
    return this.http.put<Event>(`${this.apiUrl}/events/${id}`, event);
  }

  deleteEvent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/events/${id}`);
  }

  // Event Categories
  getEventCategories(): Observable<EventCategory[]> {
    return this.http.get<EventCategory[]>(`${this.apiUrl}/event-categories`);
  }

  createEventCategory(category: EventCategory): Observable<EventCategory> {
    return this.http.post<EventCategory>(`${this.apiUrl}/event-categories`, category);
  }

  updateEventCategory(id: number, category: EventCategory): Observable<EventCategory> {
    return this.http.put<EventCategory>(`${this.apiUrl}/event-categories/${id}`, category);
  }

  deleteEventCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/event-categories/${id}`);
  }

  // Consumption Categories
  getConsumptionCategories(): Observable<ConsumptionCategory[]> {
    return this.http.get<ConsumptionCategory[]>(`${this.apiUrl}/consumption-categories`);
  }

  createConsumptionCategory(category: ConsumptionCategory): Observable<ConsumptionCategory> {
    return this.http.post<ConsumptionCategory>(`${this.apiUrl}/consumption-categories`, category);
  }

  updateConsumptionCategory(id: number, category: ConsumptionCategory): Observable<ConsumptionCategory> {
    return this.http.put<ConsumptionCategory>(`${this.apiUrl}/consumption-categories/${id}`, category);
  }

  deleteConsumptionCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/consumption-categories/${id}`);
  }
}
