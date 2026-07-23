import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  AppNotification,
  NotificationsService,
  notificationText
} from '../core/api/notifications.service';

@Component({
  selector: 'app-notification-bell',
  imports: [DatePipe],
  template: `
    <div class="bell">
      <button type="button" class="trigger" (click)="toggle()" [attr.aria-label]="'Notifications'">
        🔔
        @if (unread() > 0) {
          <span class="badge">{{ unread() > 9 ? '9+' : unread() }}</span>
        }
      </button>

      @if (open()) {
        <div class="backdrop" (click)="open.set(false)"></div>
        <div class="panel">
          <header>
            <span>Notifications</span>
            @if (unread() > 0) {
              <button type="button" class="link" (click)="markAllRead()">Mark all read</button>
            }
          </header>
          @for (n of items(); track n.id) {
            <button type="button" class="item" [class.unread]="!n.readAt" (click)="openLead(n)">
              <span class="dot" [class]="'dot dot--' + n.type.toLowerCase()"></span>
              <span class="body">
                <span class="text">{{ text(n) }}</span>
                <span class="time">{{ n.createdAt | date: 'short' }}</span>
              </span>
            </button>
          } @empty {
            <p class="empty">You're all caught up 🎉</p>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .bell { position: relative; }
    .trigger {
      position: relative;
      border: none;
      background: none;
      font-size: 18px;
      cursor: pointer;
      line-height: 1;
      padding: 6px;
      border-radius: 8px;
      transition: background 0.2s ease;
    }
    .trigger:hover {
      background: rgba(255, 255, 255, 0.08);
    }
    .badge {
      position: absolute;
      top: 0px;
      right: -2px;
      min-width: 16px;
      height: 16px;
      padding: 0 4px;
      border-radius: 9999px;
      background: #EF4444;
      color: #fff;
      font-size: 10px;
      font-weight: 700;
      display: grid;
      place-items: center;
      box-shadow: 0 0 8px rgba(239, 68, 68, 0.8);
    }
    .backdrop { position: fixed; inset: 0; z-index: 10; }
    .panel {
      position: absolute;
      top: calc(100% + 12px);
      right: 0;
      z-index: 11;
      width: 340px;
      max-height: 70vh;
      overflow-y: auto;
      background: #161F33;
      border: 1px solid rgba(255, 255, 255, 0.12);
      border-radius: 14px;
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5);
    }
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.06);
      font-size: 13px;
      font-weight: 700;
      color: #F8FAFC;
    }
    .link {
      border: none; background: none; padding: 0;
      font-family: var(--cl-font-family-display); font-size: 11px;
      color: #818CF8; cursor: pointer; font-weight: 600;
    }
    .link:hover { text-decoration: underline; }
    .item {
      display: flex;
      gap: 12px;
      width: 100%;
      text-align: left;
      border: none;
      background: none;
      padding: 12px 16px;
      cursor: pointer;
      border-bottom: 1px solid rgba(255, 255, 255, 0.04);
      transition: background 0.2s ease;
    }
    .item:hover { background: rgba(255, 255, 255, 0.04); }
    .item.unread { background: rgba(99, 102, 241, 0.1); }
    .item.unread:hover { background: rgba(99, 102, 241, 0.18); }
    .dot {
      width: 8px; height: 8px; margin-top: 5px; border-radius: 50%; flex-shrink: 0;
    }
    .dot--sla_breach { background: #EF4444; box-shadow: 0 0 6px #EF4444; }
    .dot--follow_up_due { background: #F59E0B; box-shadow: 0 0 6px #F59E0B; }
    .body { display: grid; gap: 2px; }
    .text { font-size: 13px; color: #F8FAFC; }
    .time { font-size: 11px; color: #64748B; }
    .empty {
      margin: 0; padding: 24px;
      text-align: center; color: #64748B;
      font-size: 13px;
    }
  `
})
export class NotificationBell {
  private readonly service = inject(NotificationsService);
  private readonly router = inject(Router);

  protected readonly unread = signal(0);
  protected readonly items = signal<AppNotification[]>([]);
  protected readonly open = signal(false);

  protected readonly text = notificationText;

  constructor() {
    this.refreshCount();
    const timer = setInterval(() => this.refreshCount(), 60_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected toggle(): void {
    const next = !this.open();
    this.open.set(next);
    if (next) {
      this.service.list().subscribe((list) => this.items.set(list));
    }
  }

  protected openLead(n: AppNotification): void {
    if (!n.readAt) {
      this.service.markRead(n.id).subscribe(() => this.refreshCount());
    }
    this.open.set(false);
    if (n.leadId) {
      this.router.navigate(['/leads', n.leadId]);
    }
  }

  protected markAllRead(): void {
    this.service.markAllRead().subscribe(() => {
      this.items.update((list) => list.map((n) => ({ ...n, readAt: n.readAt ?? new Date().toISOString() })));
      this.unread.set(0);
    });
  }

  private refreshCount(): void {
    this.service.unreadCount().subscribe({
      next: (r) => this.unread.set(r.count),
      error: () => {}
    });
  }
}
