import { Injectable, Renderer2, RendererFactory2 } from '@angular/core';


@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private renderer: Renderer2;
  private currentTheme: string = 'light';

  constructor(rendererFactory: RendererFactory2) {
    this.renderer = rendererFactory.createRenderer(null, null);
  }

  /**
   * Loads the saved theme from local storage when the application starts.
   * Defaults to 'light' if no theme is saved.
   */
  loadTheme(): void {
    const savedTheme = localStorage.getItem('theme') || 'light';
    this.setTheme(savedTheme);
  }

  /**
   * Applies a new theme to the application by adding a class to the body element
   * and saves the user's choice to local storage.
   * @param theme The name of the theme to apply (e.g., 'light' or 'dark').
   */
  setTheme(theme: string): void {
    const oldTheme = this.currentTheme;
    this.currentTheme = theme;

    this.renderer.removeClass(document.body, `${oldTheme}-theme`);
    this.renderer.addClass(document.body, `${theme}-theme`);
  }
}
