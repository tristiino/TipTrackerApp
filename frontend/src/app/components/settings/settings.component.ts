import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';
import { LanguageService } from 'src/app/services/language.service';
import { ThemeService } from 'src/app/services/theme.service';


@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  settingsForm!: FormGroup;
  translations: { [key: string]: string } = {};

  constructor(
    private fb: FormBuilder,
    private languageService: LanguageService,
    private themeService: ThemeService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const savedTheme = localStorage.getItem('theme') || 'light';
    const savedLanguage = localStorage.getItem('language') || 'english';

    this.settingsForm = this.fb.group({
      taxRate: [3, [Validators.required, Validators.min(0)]],
      theme: [savedTheme, Validators.required],
      language: [savedLanguage, Validators.required]
    });

    this.languageService.language$.subscribe(lang => {
      this.translations = this.languageService.getTranslations(lang);
    });
  }

  /**
   * Applies the selected theme by calling the ThemeService.
   * Triggered by the (change) event on the theme dropdown.
   * @param event The browser event from the select element.
   */
  applyTheme(event: Event): void {
    const theme = (event.target as HTMLSelectElement).value;
    this.themeService.setTheme(theme);
  }

  /**
   * Applies the selected language by calling the LanguageService.
   * Triggered by the (change) event on the language dropdown.
   * @param event The browser event from the select element.
   */
  applyLanguage(event: Event): void {
    const language = (event.target as HTMLSelectElement).value;
    this.languageService.setLanguage(language);
  }

  /**
   * Handles the form submission to save settings.
   * Note: Currently logs to console; would be extended to save to a backend.
   */
  onSubmit(): void {
    if (this.settingsForm.valid) {
      console.log('Settings saved:', this.settingsForm.value);
      alert('Settings saved!'); // Simple user feedback
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  onReset(): void {
    const defaultLanguage = 'english';
    const defaultTheme = 'light';
    this.settingsForm.reset({
      taxRate: 3,
      theme: defaultTheme,
      language: defaultLanguage
    });
    this.themeService.setTheme(defaultTheme);
    this.languageService.setLanguage(defaultLanguage);
  }
}
