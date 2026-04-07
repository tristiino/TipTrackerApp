import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Job } from '../models/job.model';

@Injectable({ providedIn: 'root' })
export class JobService {

  private baseUrl = `${environment.apiUrl}/jobs`;

  constructor(private http: HttpClient) {}

  getJobs(): Observable<Job[]> {
    return this.http.get<Job[]>(this.baseUrl);
  }

  createJob(job: Omit<Job, 'id'>): Observable<Job> {
    return this.http.post<Job>(this.baseUrl, job);
  }

  updateJob(id: number, job: Omit<Job, 'id'>): Observable<Job> {
    return this.http.put<Job>(`${this.baseUrl}/${id}`, job);
  }

  deleteJob(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
