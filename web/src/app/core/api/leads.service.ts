import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export type LeadStatus = 'LEAD' | 'HOT_LEAD' | 'APPLICATION' | 'STUDENT' | 'STALLED';

export const FUNNEL_STAGES: LeadStatus[] = ['LEAD', 'HOT_LEAD', 'APPLICATION', 'STUDENT'];

export const STATUS_LABELS: Record<LeadStatus, string> = {
  LEAD: 'Lead',
  HOT_LEAD: 'Hot lead',
  APPLICATION: 'Application',
  STUDENT: 'Student',
  STALLED: 'Stalled'
};

export interface Lead {
  id: string;
  fullName: string;
  countryCode: string;
  email: string | null;
  phone: string | null;
  courseId: string;
  channelId: string;
  utmSource: string | null;
  utmMedium: string | null;
  utmCampaign: string | null;
  status: LeadStatus;
  stalledFromStatus: LeadStatus | null;
  stallReasonId: string | null;
  assignedTo: string;
  assignedToName: string;
  lastContactedAt: string | null;
  createdAt: string;
}

export interface StatusEvent {
  id: string;
  fromStatus: LeadStatus | null;
  toStatus: LeadStatus;
  stallReasonId: string | null;
  note: string | null;
  changedBy: string;
  changedByName: string;
  changedAt: string;
}

export interface LeadDetail {
  lead: Lead;
  statusHistory: StatusEvent[];
}

export interface LeadPage {
  content: Lead[];
  totalElements: number;
  totalPages: number;
  page: number;
}

export interface Duplicate {
  id: string;
  fullName: string;
  status: LeadStatus;
  assignedToName: string;
}

export interface LeadFilters {
  status?: LeadStatus;
  countryCode?: string;
  courseId?: string;
  channelId?: string;
  q?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class LeadsService {
  private readonly http = inject(HttpClient);

  list(filters: LeadFilters = {}) {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<LeadPage>('/api/leads', { params });
  }

  detail(id: string) {
    return this.http.get<LeadDetail>(`/api/leads/${id}`);
  }

  create(payload: Partial<Lead>) {
    return this.http.post<Lead>('/api/leads', payload);
  }

  update(id: string, payload: Partial<Lead>) {
    return this.http.patch<Lead>(`/api/leads/${id}`, payload);
  }

  transition(id: string, toStatus: LeadStatus, stallReasonId?: string, note?: string) {
    return this.http.post<Lead>(`/api/leads/${id}/transition`, { toStatus, stallReasonId, note });
  }

  duplicates(email: string | null, phone: string | null) {
    let params = new HttpParams();
    if (email) params = params.set('email', email);
    if (phone) params = params.set('phone', phone);
    return this.http.get<Duplicate[]>('/api/leads/duplicates', { params });
  }

  exportCsv(filters: LeadFilters = {}) {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get('/api/leads/export.csv', { params, responseType: 'blob' });
  }
}
