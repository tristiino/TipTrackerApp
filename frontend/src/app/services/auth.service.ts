import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';


@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;

  private isLoggedInSubject = new BehaviorSubject<boolean>(this.isAuthenticated());
  public isLoggedIn$ = this.isLoggedInSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Sends login credentials to the backend and stores the returned
   * token and user data upon successful authentication.
   * @param credentials The user's email and password.
   */
  login(credentials: { usernameOrEmail: string; password: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      map((response: any) => {
        if (response.token && response.user) {
          sessionStorage.setItem('token', response.token);
          sessionStorage.setItem('user', JSON.stringify(response.user));
          this.isLoggedInSubject.next(true);
        }
        return response;
      })
    );
  }

  /**
   * Logs the user out by clearing their token and user data from local storage
   * and updating the application's authentication state.
   */
  logout(): void {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    this.isLoggedInSubject.next(false);
  }

  /**
   * Retrieves the JWT from local storage.
   * @returns The JWT string or null if not present.
   */
  getToken(): string | null {
    return sessionStorage.getItem('token');
  }

  /**
   * Retrieves the stored user object from local storage.
   * @returns The user object or null if not present.
   */
  getUser(): any {
    const user = sessionStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }

  /**
   * Checks if the user is currently authenticated by verifying the presence of a token.
   * @returns True if a token exists, false otherwise.
   */
  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  /**
   * Sends new user registration data to the backend.
   * @param data The user's username, email, and password.
   */
  register(data: { username: string, email: string, password: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, data);
  }
}
