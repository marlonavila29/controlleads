import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { ThemeService } from '../core/theme.service';
import { OnboardingModal } from './onboarding-modal';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, OnboardingModal],
  template: `
    <app-onboarding-modal #onboardingModal />
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-logo">
          <span class="logo-icon">⚡</span>
        </div>
        <div class="brand-text">
          <span class="brand-name">ControlLeads</span>
          <span class="brand-space">Workspace</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <span class="nav-section-label">Main Menu</span>
        
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }" class="nav-item">
          <span class="nav-icon">📊</span>
          <span class="nav-label">Dashboard</span>
        </a>

        <a routerLink="/leads" routerLinkActive="active" class="nav-item">
          <span class="nav-icon">📋</span>
          <span class="nav-label">Candidates Directory</span>
        </a>

        <a routerLink="/board" routerLinkActive="active" class="nav-item">
          <span class="nav-icon">🎯</span>
          <span class="nav-label">Pipeline Board</span>
        </a>

        <a routerLink="/broadcasts" routerLinkActive="active" class="nav-item">
          <span class="nav-icon">✉️</span>
          <span class="nav-label">Bulk Messages</span>
        </a>

        @if (auth.isAdmin()) {
          <span class="nav-section-label admin-label">Administration</span>

          <a routerLink="/users" routerLinkActive="active" class="nav-item">
            <span class="nav-icon">👥</span>
            <span class="nav-label">Team Members</span>
          </a>

          <a routerLink="/catalogs" routerLinkActive="active" class="nav-item">
            <span class="nav-icon">⚙️</span>
            <span class="nav-label">System Catalogs</span>
          </a>
        }
      </nav>

      <div class="sidebar-footer">
        <button type="button" class="btn-tour" (click)="onboardingModal.open()" title="Replay Interactive System Tour">
          <span>🎓</span>
          <span>System Tour</span>
        </button>

        <div class="theme-toggle-bar">
          <span class="theme-label">Theme Mode</span>
          <button type="button" class="btn-theme" (click)="theme.toggle()" [title]="theme.isDark() ? 'Switch to Light Theme' : 'Switch to Dark Theme'">
            {{ theme.isDark() ? '🌙 Dark' : '☀️ Light' }}
          </button>
        </div>

        @if (auth.user(); as user) {
          <div class="user-card">
            <div class="avatar-circle">
              {{ user.name.charAt(0).toUpperCase() }}
            </div>
            <div class="user-info">
              <span class="user-name">{{ user.name }}</span>
              <span class="user-role">{{ auth.isAdmin() ? 'Administrator' : 'Counselor' }}</span>
            </div>
            <button type="button" class="btn-logout" (click)="auth.logout()" title="Sign Out">
              🚪
            </button>
          </div>
        }
      </div>
    </aside>
  `,
  styles: [`
    :host {
      display: block;
      height: 100vh;
      position: sticky;
      top: 0;
      z-index: 100;
    }

    .sidebar {
      width: 260px;
      height: 100vh;
      background: var(--cl-bg-sidebar, #0D1322);
      border-right: 1px solid var(--cl-sidebar-border, rgba(255, 255, 255, 0.08));
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      padding: var(--cl-space-5) var(--cl-space-4);
      box-sizing: border-box;
      transition: background-color 0.2s ease, border-color 0.2s ease;
    }

    .sidebar-brand {
      display: flex;
      align-items: center;
      gap: var(--cl-space-3);
      padding: var(--cl-space-2) var(--cl-space-2) var(--cl-space-5);
      border-bottom: 1px solid var(--cl-sidebar-border, rgba(255, 255, 255, 0.06));

      .brand-logo {
        width: 38px;
        height: 38px;
        border-radius: 10px;
        background: linear-gradient(135deg, #6366F1 0%, #3B82F6 100%);
        display: grid;
        place-items: center;
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);

        .logo-icon {
          font-size: 20px;
        }
      }

      .brand-text {
        display: flex;
        flex-direction: column;

        .brand-name {
          font-family: var(--cl-font-family-display);
          font-size: 17px;
          font-weight: 800;
          color: var(--cl-sidebar-brand, #FFFFFF);
          letter-spacing: -0.02em;
        }

        .brand-space {
          font-size: 11px;
          font-weight: 600;
          color: var(--cl-sidebar-section, #64748B);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }
      }
    }

    .sidebar-nav {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 6px;
      margin-top: var(--cl-space-5);

      .nav-section-label {
        font-size: 10px;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        color: var(--cl-sidebar-section, #475569);
        margin: var(--cl-space-3) var(--cl-space-3) 4px;

        &.admin-label {
          margin-top: var(--cl-space-5);
        }
      }

      .nav-item {
        display: flex;
        align-items: center;
        gap: var(--cl-space-3);
        padding: 10px 14px;
        border-radius: var(--cl-radius-md);
        color: var(--cl-sidebar-item, #94A3B8);
        text-decoration: none;
        font-family: var(--cl-font-family-display);
        font-size: 14px;
        font-weight: 600;
        transition: all 0.2s ease;
        position: relative;

        .nav-icon {
          font-size: 16px;
          opacity: 0.8;
          transition: transform 0.2s ease;
        }

        &:hover {
          color: var(--cl-sidebar-item-hover, #F8FAFC);
          background: var(--cl-sidebar-item-hover-bg, rgba(255, 255, 255, 0.05));

          .nav-icon {
            transform: scale(1.15);
          }
        }

        &.active {
          color: var(--cl-sidebar-active, #FFFFFF);
          background: var(--cl-sidebar-active-bg, linear-gradient(90deg, rgba(99, 102, 241, 0.2) 0%, rgba(99, 102, 241, 0.05) 100%));
          border-left: 3px solid var(--cl-sidebar-active-border, #6366F1);
          box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);

          .nav-icon {
            opacity: 1;
          }
        }
      }
    }

    .sidebar-footer {
      padding-top: var(--cl-space-4);
      border-top: 1px solid var(--cl-sidebar-border, rgba(255, 255, 255, 0.06));
      display: flex;
      flex-direction: column;
      gap: 12px;

      .btn-tour {
        width: 100%;
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px 14px;
        background: rgba(99, 102, 241, 0.1);
        border: 1px solid rgba(99, 102, 241, 0.25);
        border-radius: var(--cl-radius-md);
        color: #818CF8;
        font-weight: 700;
        font-size: 13px;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          background: rgba(99, 102, 241, 0.2);
          border-color: #6366F1;
          color: #FFFFFF;
        }
      }

      .theme-toggle-bar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 6px 12px;
        background: var(--cl-sidebar-card-bg, rgba(255, 255, 255, 0.03));
        border: 1px solid var(--cl-sidebar-card-border, rgba(255, 255, 255, 0.06));
        border-radius: var(--cl-radius-lg);

        .theme-label {
          font-size: 10px;
          font-weight: 700;
          color: var(--cl-sidebar-section, #64748B);
          text-transform: uppercase;
          letter-spacing: 0.04em;
        }

        .btn-theme {
          background: var(--cl-sidebar-card-bg, rgba(255, 255, 255, 0.05));
          border: 1px solid var(--cl-sidebar-card-border, rgba(255, 255, 255, 0.1));
          color: var(--cl-sidebar-brand, #FFFFFF);
          padding: 4px 10px;
          border-radius: 6px;
          font-size: 11px;
          font-weight: 700;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover {
            background: var(--cl-sidebar-item-hover-bg);
            border-color: var(--cl-sidebar-card-border);
          }
        }
      }

      .user-card {
        display: flex;
        align-items: center;
        gap: var(--cl-space-3);
        padding: var(--cl-space-2) var(--cl-space-3);
        background: var(--cl-sidebar-card-bg, rgba(255, 255, 255, 0.03));
        border: 1px solid var(--cl-sidebar-card-border, rgba(255, 255, 255, 0.06));
        border-radius: var(--cl-radius-lg);

        .avatar-circle {
          width: 34px;
          height: 34px;
          border-radius: 50%;
          background: linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%);
          color: #FFFFFF;
          font-family: var(--cl-font-family-display);
          font-weight: 700;
          font-size: 14px;
          display: grid;
          place-items: center;
          box-shadow: 0 2px 8px rgba(139, 92, 246, 0.3);
        }

        .user-info {
          flex: 1;
          display: flex;
          flex-direction: column;
          overflow: hidden;

          .user-name {
            font-size: 13px;
            font-weight: 700;
            color: var(--cl-sidebar-brand, #F8FAFC);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          .user-role {
            font-size: 11px;
            color: var(--cl-sidebar-section, #64748B);
          }
        }

        .btn-logout {
          border: none;
          background: transparent;
          font-size: 16px;
          cursor: pointer;
          opacity: 0.6;
          padding: 4px;
          border-radius: var(--cl-radius-sm);
          transition: all 0.2s ease;

          &:hover {
            opacity: 1;
            background: rgba(239, 68, 68, 0.2);
          }
        }
      }
    }
  `]
})
export class Sidebar {
  protected readonly auth = inject(AuthService);
  protected readonly theme = inject(ThemeService);
}
