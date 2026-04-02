import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';


@Injectable({
  providedIn: 'root'
})
export class LanguageService {
  private translations: any = {
    english: {
      settingsTitle: 'Settings',
      taxRate: 'Default Tax Rate (%)',
      theme: 'Theme Preference',
      language: 'Language Preference',
      save: 'Save Settings',
      reset: 'Reset'
    },
    spanish: {
      settingsTitle: 'Configuración',
      taxRate: 'Tasa de impuesto predeterminada (%)',
      theme: 'Tema',
      language: 'Idioma',
      save: 'Guardar configuración',
      reset: 'Restablecer'
    }
  };

  private languageSubject = new BehaviorSubject<string>('english');
  /** An observable that components can subscribe to to receive language updates. */
  public language$ = this.languageSubject.asObservable();

  constructor() {}

  /**
   * Loads the saved language from local storage when the application starts.
   */
  loadLanguage(): void {
    const savedLang = localStorage.getItem('language') || 'english';
    this.setLanguage(savedLang);
  }

  /**
   * Sets the application's current language and notifies all subscribers of the change.
   * @param language The language to set (e.g., 'english' or 'spanish').
   */
  setLanguage(language: string): void {
    this.languageSubject.next(language);
  }

  /**
   * Retrieves the dictionary of translated strings for a given language.
   * @param language The desired language.
   * @returns An object containing the translated strings.
   */
  getTranslations(language: string): any {
    return this.translations[language] || this.translations['english'];
  }
}
