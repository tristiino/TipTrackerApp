import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { ThemeService } from './theme.service';
import { LanguageService } from './language.service';
import { PayPeriodService } from './pay-period.service';

export interface UserSettings {
  theme: string;
  language: string;
  taxRate: number;              // decimal, e.g. 0.03 for 3%
  payPeriodStartAnchor: string | null;  // YYYY-MM-DD
  payPeriodLengthDays: number;
}

@Injectable({
  providedIn: 'root'
})
export class SettingsService {
  private readonly CACHE_KEY = 'userSettings';
  private baseUrl = `${environment.apiUrl}/settings`;

  constructor(
    private http: HttpClient,
    private themeService: ThemeService,
    private languageService: LanguageService,
    private payPeriodService: PayPeriodService
  ) {}

  /**
   * Fetches the current user's settings from the backend.
   * User is resolved server-side from the JWT — no userId param needed.
   */
  getSettings(): Observable<UserSettings> {
    return this.http.get<UserSettings>(this.baseUrl);
  }

  /**
   * Persists updated settings to the backend and refreshes the local cache.
   */
  updateSettings(settings: UserSettings): Observable<UserSettings> {
    return this.http.put<UserSettings>(this.baseUrl, settings).pipe(
      tap(saved => this.cacheSettings(saved))
    );
  }

  /**
   * Fetches settings from the API, applies theme and language immediately,
   * and writes to the local cache. Call this once after login.
   */
  loadSettings(): Observable<UserSettings> {
    return this.getSettings().pipe(
      tap(settings => {
        this.themeService.setTheme(settings.theme);
        this.languageService.setLanguage(settings.language);
        if (settings.payPeriodStartAnchor && settings.payPeriodLengthDays > 0) {
          this.payPeriodService.setConfig({
            startAnchor: settings.payPeriodStartAnchor,
            lengthDays: settings.payPeriodLengthDays,
          });
        } else {
          this.payPeriodService.clearConfig();
        }
        this.cacheSettings(settings);
      })
    );
  }

  /**
   * Reads the locally cached settings synchronously.
   * Useful for initialising forms without waiting for an API round-trip.
   */
  getCachedSettings(): UserSettings | null {
    const stored = localStorage.getItem(this.CACHE_KEY);
    return stored ? JSON.parse(stored) : null;
  }

  private cacheSettings(settings: UserSettings): void {
    localStorage.setItem(this.CACHE_KEY, JSON.stringify(settings));
    // Keep individual keys in sync so ThemeService/LanguageService cold-start reads still work
    localStorage.setItem('theme', settings.theme);
    localStorage.setItem('language', settings.language);
  }
}
