import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { NotificationBell } from './notification-bell';

@Component({
  selector: 'app-topbar',
  imports: [RouterLink, RouterLinkActive, NotificationBell],
  template: `
    <header class="topbar">
      <a routerLink="/" class="brand">ControlLeads</a>
      <nav>
        <a routerLink="/leads" routerLinkActive="active">Leads</a>
        <a routerLink="/board" routerLinkActive="active">Board</a>
        @if (auth.isAdmin()) {
          <a routerLink="/users" routerLinkActive="active">Team</a>
          <a routerLink="/catalogs" routerLinkActive="active">Catalogs</a>
        }
      </nav>
      <div class="account">
        <app-notification-bell />
        <span class="who">{{ auth.user()?.name }}</span>
        <button type="button" (click)="auth.logout()">Sign out</button>
      </div>
    </header>
  `,
  styles: `
    .topbar {
      display: flex;
      align-items: center;
      gap: var(--cl-space-5);
      padding: var(--cl-space-3) var(--cl-space-5);
      background: var(--cl-color-neutral-0);
      box-shadow: var(--cl-elevation-card);
    }
    .brand {
      font-weight: var(--cl-font-weight-bold);
      color: var(--cl-color-brand-primary);
      text-decoration: none;
    }
    nav {
      flex: 1;
      display: flex;
      gap: var(--cl-space-4);
    }
    nav a {
      color: var(--cl-color-neutral-600);
      text-decoration: none;
      font-size: var(--cl-font-size-sm);
      font-weight: var(--cl-font-weight-medium);
      padding-bottom: 2px;
      border-bottom: 2px solid transparent;
      transition: color var(--cl-motion-duration-fast) var(--cl-motion-easing);
    }
    nav a:hover { color: var(--cl-color-brand-primary); }
    nav a.active {
      color: var(--cl-color-brand-primary);
      border-bottom-color: var(--cl-color-brand-primary);
    }
    .account { display: flex; align-items: center; gap: var(--cl-space-3); }
    .who { font-size: var(--cl-font-size-sm); color: var(--cl-color-neutral-800); }
    .account button {
      padding: var(--cl-space-1) var(--cl-space-3);
      border: 1px solid var(--cl-color-neutral-200);
      border-radius: var(--cl-radius-full);
      background: transparent;
      font: inherit;
      font-size: var(--cl-font-size-sm);
      color: var(--cl-color-neutral-600);
      cursor: pointer;
    }
    .account button:hover {
      border-color: var(--cl-color-semantic-danger);
      color: var(--cl-color-semantic-danger);
    }
  `
})
export class Topbar {
  protected readonly auth = inject(AuthService);
}
