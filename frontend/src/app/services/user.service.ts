import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../environments/environment";

interface User {
  id?: number;
  email: string;
  password?: string;
  name?: string; // if your backend supports it
}

interface LoginPayload {
  email: string;
  password: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private baseUrl: string = `${environment.apiUrl}/users`; // adjust if needed

  constructor(private http: HttpClient) {}

  register(user: User): Observable<any> {
    return this.http.post(`${this.baseUrl}/register`, user);
  }

  login(credentials: LoginPayload): Observable<any> {
    return this.http.post(`${this.baseUrl}/login`, credentials);
  }

  getUserProfile(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/me`);
  }

  logout(): void {
    localStorage.removeItem('token'); // optional if you're using tokens
  }
}
