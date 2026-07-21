import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export type ActivityType = 'NOTE' | 'CALL' | 'EMAIL' | 'WHATSAPP' | 'MEETING' | 'FOLLOW_UP';

export const ACTIVITY_LABELS: Record<ActivityType, string> = {
  NOTE: 'Note',
  CALL: 'Call',
  EMAIL: 'Email',
  WHATSAPP: 'WhatsApp',
  MEETING: 'Meeting',
  FOLLOW_UP: 'Follow-up'
};

export const ACTIVITY_ICONS: Record<ActivityType, string> = {
  NOTE: '📝',
  CALL: '📞',
  EMAIL: '✉️',
  WHATSAPP: '💬',
  MEETING: '🤝',
  FOLLOW_UP: '⏰'
};

export interface Activity {
  id: string;
  leadId: string;
  type: ActivityType;
  content: string;
  dueAt: string | null;
  completedAt: string | null;
  createdBy: string;
  createdByName: string;
  createdAt: string;
}

export interface FollowUpTask extends Activity {
  leadName: string;
}

@Injectable({ providedIn: 'root' })
export class ActivitiesService {
  private readonly http = inject(HttpClient);

  listByLead(leadId: string) {
    return this.http.get<Activity[]>(`/api/leads/${leadId}/activities`);
  }

  add(leadId: string, payload: { type: ActivityType; content: string; dueAt?: string }) {
    return this.http.post<Activity>(`/api/leads/${leadId}/activities`, payload);
  }

  complete(activityId: string) {
    return this.http.post<Activity>(`/api/activities/${activityId}/complete`, {});
  }

  /** Open follow-ups for the current user, ordered by due date. */
  myFollowUps() {
    return this.http.get<FollowUpTask[]>('/api/my/follow-ups');
  }
}
