import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export type NotificationType = 'SLA_BREACH' | 'FOLLOW_UP_DUE';

export interface AppNotification {
  id: string;
  type: NotificationType;
  leadId: string | null;
  leadName: string | null;
  readAt: string | null;
  createdAt: string;
}

export function notificationText(n: AppNotification): string {
  const name = n.leadName ?? 'A lead';
  switch (n.type) {
    case 'SLA_BREACH':
      return `${name} is going cold — no contact within SLA`;
    case 'FOLLOW_UP_DUE':
      return `Follow-up due for ${name}`;
  }
}

@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<AppNotification[]>('/api/notifications');
  }

  unreadCount() {
    return this.http.get<{ count: number }>('/api/notifications/unread-count');
  }

  markRead(id: string) {
    return this.http.post<void>(`/api/notifications/${id}/read`, {});
  }

  markAllRead() {
    return this.http.post<void>('/api/notifications/read-all', {});
  }
}
