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

  /**
   * Fetches daily aggregated tip earnings for the last N days.
   * @param days Number of days to look back (default 30).
   */
  getDailyEarnings(days: number = 30, groupBy: string = 'day'): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/earnings/daily?days=${days}&groupBy=${groupBy}`);
  }

  /**
   * Fetches aggregated summary stats for the dashboard (totals, shifts, hourly wage).
   * @param days Number of days to look back (default 30).
   */
  getDashboardSummary(days: number = 30): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/summary?days=${days}`);
  }

  /**
   * Fetches daily aggregated earnings for an explicit date range.
   * @param startDate ISO date string (YYYY-MM-DD).
   * @param endDate   ISO date string (YYYY-MM-DD).
   * @param groupBy   Aggregation period: 'day', 'week', or 'month'.
   */
  getDailyEarningsByDateRange(startDate: string, endDate: string, groupBy: string = 'day'): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/earnings/daily?startDate=${startDate}&endDate=${endDate}&groupBy=${groupBy}`);
  }

  /**
   * Fetches dashboard summary stats for an explicit date range.
   * @param startDate ISO date string (YYYY-MM-DD).
   * @param endDate   ISO date string (YYYY-MM-DD).
   */
  getDashboardSummaryByDateRange(startDate: string, endDate: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/summary?startDate=${startDate}&endDate=${endDate}`);
  }
}
