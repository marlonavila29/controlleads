import { Injectable, signal, effect } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly isDark = signal<boolean>(true);

  constructor() {
    const saved = localStorage.getItem('theme');
    // Default to dark mode (true), switch to light if explicitly saved
    if (saved === 'light') {
      this.isDark.set(false);
      document.body.classList.add('light-theme');
    } else {
      this.isDark.set(true);
      document.body.classList.remove('light-theme');
    }

    // Effect to auto-apply class to body when signal changes
    effect(() => {
      const dark = this.isDark();
      if (dark) {
        document.body.classList.remove('light-theme');
        localStorage.setItem('theme', 'dark');
      } else {
        document.body.classList.add('light-theme');
        localStorage.setItem('theme', 'light');
      }
    });
  }

  toggle(): void {
    this.isDark.update(d => !d);
  }
}
