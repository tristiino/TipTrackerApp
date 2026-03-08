import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { ThemeService } from './services/theme.service';
import { LanguageService } from './services/language.service';

/**
 * The root component of the application, serving as the main app shell.
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  // A flag to determine if the current route is for login/register.
  isLoginOrRegister = false;

  constructor(
    private router: Router,
    private themeService: ThemeService,
    private languageService: LanguageService
  ) {}

  /**
   * On component initialization, load saved user preferences and subscribe
   * to router events to determine which navbar to display.
   */
  ngOnInit(): void {
    this.themeService.loadTheme();
    this.languageService.loadLanguage();

    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.isLoginOrRegister = event.urlAfterRedirects === '/' || event.urlAfterRedirects === '/login' || event.urlAfterRedirects === '/register';
    });
  }

  /**
   * A helper method used by the template to check the current route status.
   * @returns True if the current route is /login or /register.
   */
  isLoginOrRegisterRoute(): boolean {
    return this.isLoginOrRegister;
  }
}
