import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../environments/environment";


interface UserSettings {
  userId: number;
  currency: string;
  taxRate: number;
}

/**
 * Service for managing user-specific settings with the backend.
 * Note: The corresponding backend endpoints for this service have not been built yet.
 */
@Injectable({
  providedIn: 'root'
})
export class SettingsService {
  private baseUrl = `${environment.apiUrl}/settings`;

  constructor(private http: HttpClient) {}

  /**
   * Fetches user settings from the backend.
   * @param userId The ID of the user whose settings are being fetched.
   */
  getSettings(userId: number): Observable<UserSettings> {
    return this.http.get<UserSettings>(`${this.baseUrl}/${userId}`);
  }

  /**
   * Saves updated user settings to the backend.
   * @param userId The ID of the user.
   * @param settings The settings object to be saved.
   */
  updateSettings(userId: number, settings: UserSettings): Observable<any> {
    return this.http.put(`${this.baseUrl}/${userId}`, settings);
  }
}
