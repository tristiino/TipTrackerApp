import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { ThemeService } from './services/theme.service';
import { LanguageService } from './services/language.service';
import { AuthService } from './services/auth.service';
import { SettingsService } from './services/settings.service';

/**
 * The root component of the application, serving as the main app shell.
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  isLoginOrRegister = false;

  constructor(
    private router: Router,
    private themeService: ThemeService,
    private languageService: LanguageService,
    private authService: AuthService,
    private settingsService: SettingsService
  ) {}

  ngOnInit(): void {
    // Apply cached theme and language immediately so there is no flash on cold start
    this.themeService.loadTheme();
    this.languageService.loadLanguage();

    // Whenever the user is authenticated (on login or on page refresh with an active session),
    // fetch the latest settings from the server and apply them.
    this.authService.isLoggedIn$.pipe(
      filter(isLoggedIn => isLoggedIn)
    ).subscribe(() => {
      this.settingsService.loadSettings().subscribe({
        error: () => { /* silently keep the cached values on network failure */ }
      });
    });

    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.isLoginOrRegister = event.urlAfterRedirects === '/' || event.urlAfterRedirects === '/login' || event.urlAfterRedirects === '/register';
    });
  }

  isLoginOrRegisterRoute(): boolean {
    return this.isLoginOrRegister;
  }
}
