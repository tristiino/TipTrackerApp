import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';


@Injectable({
  providedIn: 'root'
})
export class TipService {
  // Use the environment variable for the base URL.
  private apiUrl = `${environment.apiUrl}/tips`;

  constructor(private http: HttpClient) {}

  /**
   * Sends a new tip entry to the backend.
   * @param tip The tip data to be saved.
   */
  addTip(tip: any): Observable<any> {
    return this.http.post(this.apiUrl, tip);
  }

  /**
   * Sends an update request for an existing tip.
   * @param id The ID of the tip to update.
   * @param tip The updated tip data.
   */
  updateTip(id: number, tip: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, tip);
  }

  /**
   * Sends a request to delete a tip by its ID.
   * @param id The ID of the tip to delete.
   */
  deleteTip(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  /**
   * Fetches the most recent tip entries for the logged-in user.
   */
  getRecentTips(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/recent`);
  }
}
