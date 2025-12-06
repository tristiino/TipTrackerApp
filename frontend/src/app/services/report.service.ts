import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../environments/environment";


@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private baseUrl = `${environment.apiUrl}/tips`;

  constructor(private http: HttpClient) { }

  /**
   * Fetches a calculated report summary for a given user and date range.
   * @param userId The ID of the user.
   * @param startDate The start date of the report period (YYYY-MM-DD).
   * @param endDate The end date of the report period (YYYY-MM-DD).
   */
  getReportSummary(userId: number, startDate: string, endDate: string): Observable<any> {
    const url = `${this.baseUrl}/user/${userId}/report?start=${startDate}&end=${endDate}`;
    return this.http.get(url);
  }
}
