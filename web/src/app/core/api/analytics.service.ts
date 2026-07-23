import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { LeadStatus } from './leads.service';

/**
 * Analytics contract (Fase 1d). All aggregations are computed in the backend
 * from the immutable lead_status_events — web and app must show the SAME
 * numbers (CLAUDE.md rule 4). Marketing members receive their own slice;
 * admins the global view (+ leaderboard).
 */

export interface AnalyticsSummary {
  totalLeads: number;
  newLeads: number;
  conversionRate: number; // 0..1
  avgDaysToConvert: number | null;
  slaBreaches: number;
}

export interface FunnelStage {
  status: LeadStatus;
  count: number;
}

export interface DropOffRow {
  stage: LeadStatus;
  reasonId: string;
  reasonName: string;
  count: number;
}

export interface TimePoint {
  period: string; // e.g. 2026-07-01 (week/month start)
  created: number;
  converted: number;
}

export interface BreakdownRow {
  id: string;
  name: string;
  total: number;
  students: number;
}

export interface CountryRow {
  countryCode: string;
  total: number;
  students: number;
}

export interface LeaderboardRow {
  userId: string;
  name: string;
  totalLeads: number;
  students: number;
}

export interface DateRange {
  from?: string; // ISO date
  to?: string;
  assignedTo?: string;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);

  summary(range: DateRange = {}) {
    return this.http.get<AnalyticsSummary>('/api/analytics/summary', { params: toParams(range) });
  }

  funnel(range: DateRange = {}) {
    return this.http.get<FunnelStage[]>('/api/analytics/funnel', { params: toParams(range) });
  }

  dropOff(range: DateRange = {}) {
    return this.http.get<DropOffRow[]>('/api/analytics/drop-off', { params: toParams(range) });
  }

  timeseries(interval: 'week' | 'month', range: DateRange = {}) {
    return this.http.get<TimePoint[]>('/api/analytics/timeseries', {
      params: toParams(range).set('interval', interval)
    });
  }

  byChannel(range: DateRange = {}) {
    return this.http.get<BreakdownRow[]>('/api/analytics/by-channel', { params: toParams(range) });
  }

  byCourse(range: DateRange = {}) {
    return this.http.get<BreakdownRow[]>('/api/analytics/by-course', { params: toParams(range) });
  }

  byCountry(range: DateRange = {}) {
    return this.http.get<CountryRow[]>('/api/analytics/by-country', { params: toParams(range) });
  }

  /** Admin only. */
  leaderboard(range: DateRange = {}) {
    return this.http.get<LeaderboardRow[]>('/api/analytics/leaderboard', { params: toParams(range) });
  }
}

function toParams(range: DateRange): HttpParams {
  let params = new HttpParams();
  if (range.from) params = params.set('from', range.from);
  if (range.to) params = params.set('to', range.to);
  if (range.assignedTo) params = params.set('assignedTo', range.assignedTo);
  return params;
}
