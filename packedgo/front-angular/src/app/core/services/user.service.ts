import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserProfile, UpdateUserProfileRequest } from '../../shared/models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = environment.usersServiceUrl;

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

  // Get user profile by authUserId
  getUserProfile(authUserId: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(
      `${this.apiUrl}/user-profiles/by-auth-user/${authUserId}`,
      this.getHeaders()
    );
  }

  // Get current logged user profile
  getMyProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/user-profiles/me`, this.getHeaders());
  }

  // Update user profile
  updateUserProfile(authUserId: number, profile: UpdateUserProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(
      `${this.apiUrl}/user-profiles/by-auth-user/${authUserId}`,
      profile,
      this.getHeaders()
    );
  }

  // Update my profile
  updateMyProfile(profile: UpdateUserProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/user-profiles/me`, profile, this.getHeaders());
  }

  // Get all users (admin only)
  getAllUsers(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${this.apiUrl}/user-profiles`, this.getHeaders());
  }
}
