import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { NotificationBell } from './notification-bell';

@Component({
  selector: 'app-topbar',
  imports: [RouterLink, RouterLinkActive, NotificationBell],
  template: `
    <header class="topbar">
      <div class="topbar-inner">
        <a routerLink="/" class="brand">
          <div class="brand-logo">⚡</div>
          <span class="brand-text">ControlLeads</span>
        </a>
        <nav class="nav-links">
          <a routerLink="/leads" routerLinkActive="active" class="nav-item">
            <span class="nav-icon">📋</span>
            <span>Leads</span>
          </a>
          <a routerLink="/board" routerLinkActive="active" class="nav-item">
            <span class="nav-icon">📊</span>
            <span>Board</span>
          </a>
          @if (auth.isAdmin()) {
            <a routerLink="/users" routerLinkActive="active" class="nav-item">
              <span class="nav-icon">👥</span>
              <span>Team</span>
            </a>
            <a routerLink="/catalogs" routerLinkActive="active" class="nav-item">
              <span class="nav-icon">⚙️</span>
              <span>Catalogs</span>
            </a>
          }
        </nav>
        <div class="account-area">
          <app-notification-bell />
          <div class="user-profile">
            <div class="user-avatar">
              {{ auth.user()?.name?.charAt(0) || 'U' }}
            </div>
            <div class="user-meta">
              <span class="user-name">{{ auth.user()?.name }}</span>
              <span class="user-role" [class.admin]="auth.isAdmin()">
                {{ auth.isAdmin() ? 'Admin' : 'Counselor' }}
              </span>
            </div>
          </div>
          <button type="button" class="btn-logout" (click)="auth.logout()" title="Sign out">
            <span>Sign out</span>
            <span class="icon">↵</span>
          </button>
        </div>
      </div>
    </header>
  `,
  styles: `
    .topbar {
      position: sticky;
      top: 0;
      z-index: 100;
      background: var(--cl-glass-bg);
      backdrop-filter: var(--cl-glass-blur);
      -webkit-backdrop-filter: var(--cl-glass-blur);
      border-bottom: 1px solid var(--cl-glass-border);
      box-shadow: 0 1px 3px 0 rgba(15, 23, 42, 0.04);
    }
    .topbar-inner {
      max-width: 1280px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--cl-space-5);
      padding: var(--cl-space-2) var(--cl-space-5);
    }
    .brand {
      display: flex;
      align-items: center;
      gap: var(--cl-space-2);
      text-decoration: none;
    }
    .brand-logo {
      width: 32px;
      height: 32px;
      border-radius: var(--cl-radius-md);
      background: linear-gradient(135deg, var(--cl-color-brand-primary) 0%, #6366F1 100%);
      color: #fff;
      display: grid;
      place-items: center;
      font-size: 16px;
      box-shadow: 0 2px 8px rgba(79, 70, 229, 0.3);
    }
    .brand-text {
      font-family: var(--cl-font-family-display);
      font-weight: 800;
      font-size: var(--cl-font-size-lg);
      background: linear-gradient(135deg, var(--cl-color-neutral-900) 0%, var(--cl-color-brand-primary) 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      letter-spacing: -0.03em;
    }
    .nav-links {
      display: flex;
      align-items: center;
      gap: var(--cl-space-1);
      background: rgba(241, 245, 249, 0.6);
      padding: 4px;
      border-radius: var(--cl-radius-full);
      border: 1px solid var(--cl-color-neutral-200);
    }
    .nav-item {
      display: flex;
      align-items: center;
      gap: var(--cl-space-2);
      padding: 6px 16px;
      border-radius: var(--cl-radius-full);
      color: var(--cl-color-neutral-600);
      text-decoration: none;
      font-family: var(--cl-font-family-display);
      font-size: var(--cl-font-size-sm);
      font-weight: var(--cl-font-weight-medium);
      transition: all var(--cl-motion-duration-fast) var(--cl-motion-easing);
    }
    .nav-item:hover {
      color: var(--cl-color-brand-primary);
      background: rgba(255, 255, 255, 0.6);
    }
    .nav-item.active {
      color: var(--cl-color-brand-primary);
      background: var(--cl-color-neutral-0);
      font-weight: var(--cl-font-weight-semibold);
      box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
    }
    .nav-icon {
      font-size: 14px;
    }
    .account-area {
      display: flex;
      align-items: center;
      gap: var(--cl-space-4);
    }
    .user-profile {
      display: flex;
      align-items: center;
      gap: var(--cl-space-2);
      padding: 4px 10px 4px 4px;
      background: var(--cl-color-neutral-0);
      border: 1px solid var(--cl-color-neutral-200);
      border-radius: var(--cl-radius-full);
      box-shadow: var(--cl-shadow-subtle);
    }
    .user-avatar {
      width: 28px;
      height: 28px;
      border-radius: var(--cl-radius-full);
      background: linear-gradient(135deg, var(--cl-color-brand-primary) 0%, var(--cl-color-brand-accent) 100%);
      color: #fff;
      font-family: var(--cl-font-family-display);
      font-weight: 700;
      font-size: 13px;
      display: grid;
      place-items: center;
    }
    .user-meta {
      display: flex;
      flex-direction: column;
      line-height: 1.1;
    }
    .user-name {
      font-size: var(--cl-font-size-xs);
      font-weight: var(--cl-font-weight-semibold);
      color: var(--cl-color-neutral-900);
    }
    .user-role {
      font-size: 10px;
      color: var(--cl-color-neutral-600);
      font-weight: var(--cl-font-weight-medium);
    }
    .user-role.admin {
      color: var(--cl-color-brand-primary);
      font-weight: var(--cl-font-weight-semibold);
    }
    .btn-logout {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 6px 12px;
      border: 1px solid var(--cl-color-neutral-200);
      border-radius: var(--cl-radius-full);
      background: var(--cl-color-neutral-0);
      font-family: var(--cl-font-family-base);
      font-size: var(--cl-font-size-xs);
      font-weight: var(--cl-font-weight-medium);
      color: var(--cl-color-neutral-600);
      cursor: pointer;
      transition: all var(--cl-motion-duration-fast) var(--cl-motion-easing);
    }
    .btn-logout:hover {
      border-color: var(--cl-color-semantic-danger);
      color: var(--cl-color-semantic-danger);
      background: #FEF2F2;
    }
  `
})
export class Topbar {
  protected readonly auth = inject(AuthService);
}
