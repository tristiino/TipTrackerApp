import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TipOutRole, TipOutRecord } from '../models/tip-out-role.model';

/**
 * Handles all HTTP communication for tip-out roles and per-shift record overrides.
 *
 * Endpoints backed by TipOutController:
 *   GET    /api/tip-out-roles                         → list user's roles
 *   POST   /api/tip-out-roles                         → create a role
 *   PUT    /api/tip-out-roles/:id                     → update a role
 *   DELETE /api/tip-out-roles/:id                     → delete a role
 *   PATCH  /api/tip-out-roles/records/:id/override    → override a record's finalAmount
 */
@Injectable({
  providedIn: 'root'
})
export class TipOutRoleService {

  private baseUrl = `${environment.apiUrl}/tip-out-roles`;

  constructor(private http: HttpClient) {}

  /** Fetches all tip-out roles for the authenticated user, sorted A→Z. */
  getRoles(): Observable<TipOutRole[]> {
    return this.http.get<TipOutRole[]>(this.baseUrl);
  }

  /** Creates a new tip-out role. Returns the saved role with its new id. */
  createRole(role: Omit<TipOutRole, 'id'>): Observable<TipOutRole> {
    return this.http.post<TipOutRole>(this.baseUrl, role);
  }

  /** Updates an existing role. Returns the updated role. */
  updateRole(id: number, role: Partial<TipOutRole>): Observable<TipOutRole> {
    return this.http.put<TipOutRole>(`${this.baseUrl}/${id}`, role);
  }

  /** Deletes a role by id. */
  deleteRole(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /**
   * Overrides the finalAmount on a specific TipOutRecord (P2-004).
   * Sets isOverridden = true on the backend so the UI can flag it visually.
   */
  overrideRecord(recordId: number, finalAmount: number): Observable<TipOutRecord> {
    return this.http.patch<TipOutRecord>(
      `${this.baseUrl}/records/${recordId}/override`,
      { finalAmount }
    );
  }
}
