import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';
import { LanguageService } from 'src/app/services/language.service';
import { ThemeService } from 'src/app/services/theme.service';
import { PayPeriodService } from 'src/app/services/pay-period.service';
import { SettingsService, UserSettings } from 'src/app/services/settings.service';


@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  settingsForm!: FormGroup;
  translations: { [key: string]: string } = {};
  isSaving = false;

  constructor(
    private fb: FormBuilder,
    private languageService: LanguageService,
    private themeService: ThemeService,
    private authService: AuthService,
    private router: Router,
    private payPeriodService: PayPeriodService,
    private settingsService: SettingsService,
  ) {}

  ngOnInit(): void {
    const cached = this.settingsService.getCachedSettings();
    const config = this.payPeriodService.getConfig();

    this.settingsForm = this.fb.group({
      // taxRate stored as decimal (0.03) — multiply by 100 for the % display field
      taxRate:              [cached ? cached.taxRate * 100 : 3, [Validators.required, Validators.min(0)]],
      theme:                [cached?.theme    ?? 'light',   Validators.required],
      language:             [cached?.language ?? 'english', Validators.required],
      payPeriodStartAnchor: [config?.startAnchor ?? ''],
      payPeriodLengthDays:  [config?.lengthDays  ?? 14, [Validators.min(1)]],
    });

    this.languageService.language$.subscribe(lang => {
      this.translations = this.languageService.getTranslations(lang);
    });
  }

  applyTheme(event: Event): void {
    const theme = (event.target as HTMLSelectElement).value;
    this.themeService.setTheme(theme);
  }

  applyLanguage(event: Event): void {
    const language = (event.target as HTMLSelectElement).value;
    this.languageService.setLanguage(language);
  }

  onSubmit(): void {
    if (this.settingsForm.valid) {
      const { taxRate, theme, language, payPeriodStartAnchor, payPeriodLengthDays } = this.settingsForm.value;

      const dto: UserSettings = {
        theme,
        language,
        taxRate: taxRate / 100,  // convert % → decimal before sending to backend
        payPeriodStartAnchor: payPeriodStartAnchor || null,
        payPeriodLengthDays:  payPeriodLengthDays ?? 14,
      };

      this.isSaving = true;
      this.settingsService.updateSettings(dto).subscribe({
        next: () => {
          if (payPeriodStartAnchor && payPeriodLengthDays > 0) {
            this.payPeriodService.setConfig({ startAnchor: payPeriodStartAnchor, lengthDays: payPeriodLengthDays });
          } else {
            this.payPeriodService.clearConfig();
          }
          this.isSaving = false;
          alert('Settings saved!');
        },
        error: () => {
          this.isSaving = false;
          alert('Failed to save settings. Please try again.');
        }
      });
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
      taxRate:              3,
      theme:                defaultTheme,
      language:             defaultLanguage,
      payPeriodStartAnchor: '',
      payPeriodLengthDays:  14,
    });
    this.themeService.setTheme(defaultTheme);
    this.languageService.setLanguage(defaultLanguage);
    this.payPeriodService.clearConfig();
  }
}
