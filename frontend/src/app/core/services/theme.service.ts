import { Injectable, signal } from '@angular/core';

export type Theme = 'dark' | 'light';

@Injectable({ providedIn: 'root' })
export class ThemeService {

  // Reactive signal — bind to this in templates
  theme = signal<Theme>(this.getInitialTheme());

  constructor() {
    this.applyTheme(this.theme());
  }

  toggle(): void {
    this.setTheme(this.theme() === 'dark' ? 'light' : 'dark');
  }

  setTheme(theme: Theme): void {
    this.theme.set(theme);
    this.applyTheme(theme);
    localStorage.setItem('ctrl-theme', theme); // persist across sessions
  }

  private applyTheme(theme: Theme): void {
    document.body.classList.remove('dark', 'light');
    document.body.classList.add(theme);
  }

  private getInitialTheme(): Theme {
    // 1. persisted preference
    const stored = localStorage.getItem('ctrl-theme') as Theme | null;
    if (stored === 'dark' || stored === 'light') return stored;

    // 2. system preference
    return window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light';
  }
}
