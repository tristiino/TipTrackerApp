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
  getDailyEarnings(days: number = 30, groupBy: string = 'day', jobId?: number): Observable<any[]> {
    let url = `${this.apiUrl}/earnings/daily?days=${days}&groupBy=${groupBy}`;
    if (jobId != null) url += `&jobId=${jobId}`;
    return this.http.get<any[]>(url);
  }

  /**
   * Fetches aggregated summary stats for the dashboard (totals, shifts, hourly wage).
   * @param days  Number of days to look back (default 30).
   * @param jobId Optional job ID to filter by.
   */
  getDashboardSummary(days: number = 30, jobId?: number): Observable<any> {
    let url = `${this.apiUrl}/summary?days=${days}`;
    if (jobId != null) url += `&jobId=${jobId}`;
    return this.http.get<any>(url);
  }

  /**
   * Fetches daily aggregated earnings for an explicit date range.
   * @param startDate ISO date string (YYYY-MM-DD).
   * @param endDate   ISO date string (YYYY-MM-DD).
   * @param groupBy   Aggregation period: 'day', 'week', or 'month'.
   * @param jobId     Optional job ID to filter by.
   */
  getDailyEarningsByDateRange(startDate: string, endDate: string, groupBy: string = 'day', jobId?: number): Observable<any[]> {
    let url = `${this.apiUrl}/earnings/daily?startDate=${startDate}&endDate=${endDate}&groupBy=${groupBy}`;
    if (jobId != null) url += `&jobId=${jobId}`;
    return this.http.get<any[]>(url);
  }

  /**
   * Fetches dashboard summary stats for an explicit date range.
   * @param startDate ISO date string (YYYY-MM-DD).
   * @param endDate   ISO date string (YYYY-MM-DD).
   * @param jobId     Optional job ID to filter by.
   */
  getDashboardSummaryByDateRange(startDate: string, endDate: string, jobId?: number): Observable<any> {
    let url = `${this.apiUrl}/summary?startDate=${startDate}&endDate=${endDate}`;
    if (jobId != null) url += `&jobId=${jobId}`;
    return this.http.get<any>(url);
  }
}
