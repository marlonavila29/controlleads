import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  AppNotification,
  NotificationsService,
  notificationText
} from '../core/api/notifications.service';

/**
 * Bell + unread badge in the top bar. Polls the unread count and, when opened,
 * shows the recent list; clicking an item marks it read and jumps to the lead.
 */
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
      padding: var(--cl-space-1);
    }
    .badge {
      position: absolute;
      top: -2px;
      right: -4px;
      min-width: 16px;
      height: 16px;
      padding: 0 4px;
      border-radius: var(--cl-radius-full);
      background: var(--cl-color-semantic-danger);
      color: #fff;
      font-size: 10px;
      font-weight: var(--cl-font-weight-bold);
      display: grid;
      place-items: center;
    }
    .backdrop { position: fixed; inset: 0; z-index: 10; }
    .panel {
      position: absolute;
      top: calc(100% + 8px);
      right: 0;
      z-index: 11;
      width: 340px;
      max-height: 70vh;
      overflow-y: auto;
      background: var(--cl-color-neutral-0);
      border-radius: var(--cl-radius-lg);
      box-shadow: var(--cl-elevation-raised);
    }
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--cl-space-3) var(--cl-space-4);
      border-bottom: 1px solid var(--cl-color-neutral-100);
      font-size: var(--cl-font-size-sm);
      font-weight: var(--cl-font-weight-semibold);
    }
    .link {
      border: none; background: none; padding: 0;
      font: inherit; font-size: var(--cl-font-size-xs);
      color: var(--cl-color-brand-primary); cursor: pointer;
    }
    .item {
      display: flex;
      gap: var(--cl-space-3);
      width: 100%;
      text-align: left;
      border: none;
      background: none;
      padding: var(--cl-space-3) var(--cl-space-4);
      cursor: pointer;
      border-bottom: 1px solid var(--cl-color-neutral-50);
    }
    .item:hover { background: var(--cl-color-neutral-50); }
    .item.unread { background: var(--cl-color-brand-primary-soft); }
    .item.unread:hover { background: var(--cl-color-brand-primary-soft); }
    .dot {
      width: 8px; height: 8px; margin-top: 5px; border-radius: var(--cl-radius-full); flex-shrink: 0;
    }
    .dot--sla_breach { background: var(--cl-color-status-hot-lead); }
    .dot--follow_up_due { background: var(--cl-color-brand-accent); }
    .body { display: grid; gap: 2px; }
    .text { font-size: var(--cl-font-size-sm); color: var(--cl-color-neutral-800); }
    .time { font-size: var(--cl-font-size-xs); color: var(--cl-color-neutral-400); }
    .empty {
      margin: 0; padding: var(--cl-space-5);
      text-align: center; color: var(--cl-color-neutral-400);
      font-size: var(--cl-font-size-sm);
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
