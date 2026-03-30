import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';
import { LanguageService } from 'src/app/services/language.service';
import { ThemeService } from 'src/app/services/theme.service';
import { PayPeriodService } from 'src/app/services/pay-period.service';


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
    private router: Router,
    private payPeriodService: PayPeriodService,
  ) {}

  ngOnInit(): void {
    const savedTheme = localStorage.getItem('theme') || 'light';
    const savedLanguage = localStorage.getItem('language') || 'english';
    const savedPeriod = this.payPeriodService.getPayPeriod();

    this.settingsForm = this.fb.group({
      taxRate:         [3, [Validators.required, Validators.min(0)]],
      theme:           [savedTheme, Validators.required],
      language:        [savedLanguage, Validators.required],
      payPeriodStart:  [savedPeriod?.startDate ?? ''],
      payPeriodEnd:    [savedPeriod?.endDate   ?? ''],
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
      const { payPeriodStart, payPeriodEnd } = this.settingsForm.value;

      if (payPeriodStart && payPeriodEnd) {
        this.payPeriodService.setPayPeriod({ startDate: payPeriodStart, endDate: payPeriodEnd });
      } else {
        this.payPeriodService.clearPayPeriod();
      }

      console.log('Settings saved:', this.settingsForm.value);
      alert('Settings saved!');
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
      taxRate:        3,
      theme:          defaultTheme,
      language:       defaultLanguage,
      payPeriodStart: '',
      payPeriodEnd:   '',
    });
    this.themeService.setTheme(defaultTheme);
    this.languageService.setLanguage(defaultLanguage);
    this.payPeriodService.clearPayPeriod();
  }
}
