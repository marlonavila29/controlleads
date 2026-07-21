import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ACTIVITY_ICONS,
  ACTIVITY_LABELS,
  Activity,
  ActivitiesService,
  ActivityType
} from '../../core/api/activities.service';
import { CatalogService } from '../../core/api/catalog.service';
import {
  LeadDetail as LeadDetailData,
  LeadStatus,
  LeadsService,
  STATUS_LABELS,
  StatusEvent
} from '../../core/api/leads.service';
import { StatusBadge } from '../../shared/status-badge';
import { Topbar } from '../../shared/topbar';

/** One row of the unified timeline: a status change or an activity. */
interface TimelineEntry {
  when: string;
  status?: StatusEvent;
  activity?: Activity;
}

@Component({
  selector: 'app-lead-detail',
  imports: [DatePipe, FormsModule, RouterLink, StatusBadge, Topbar],
  templateUrl: './lead-detail.html',
  styleUrl: './lead-detail.scss'
})
export class LeadDetail {
  readonly id = input.required<string>();

  protected readonly catalogs = inject(CatalogService);
  private readonly leadsService = inject(LeadsService);
  private readonly activitiesService = inject(ActivitiesService);

  protected readonly detail = signal<LeadDetailData | null>(null);
  protected readonly activities = signal<Activity[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly stalling = signal(false);
  protected stallReasonId = '';
  protected stallNote = '';

  // Activity composer state
  protected readonly activityTypes = Object.keys(ACTIVITY_LABELS) as ActivityType[];
  protected readonly activityLabels = ACTIVITY_LABELS;
  protected readonly activityIcons = ACTIVITY_ICONS;
  protected newActivityType: ActivityType = 'NOTE';
  protected newActivityContent = '';
  protected newActivityDue = '';
  protected readonly savingActivity = signal(false);

  protected readonly statusLabels = STATUS_LABELS;

  protected readonly timeline = computed<TimelineEntry[]>(() => {
    const statusEntries: TimelineEntry[] = (this.detail()?.statusHistory ?? []).map((s) => ({
      when: s.changedAt,
      status: s
    }));
    const activityEntries: TimelineEntry[] = this.activities().map((a) => ({
      when: a.createdAt,
      activity: a
    }));
    return [...statusEntries, ...activityEntries].sort((a, b) => b.when.localeCompare(a.when));
  });

  protected readonly nextStage = computed<LeadStatus | null>(() => {
    const lead = this.detail()?.lead;
    if (!lead) return null;
    switch (lead.status) {
      case 'LEAD':
        return 'HOT_LEAD';
      case 'HOT_LEAD':
        return 'APPLICATION';
      case 'APPLICATION':
        return 'STUDENT';
      default:
        return null;
    }
  });

  constructor() {
    this.catalogs.loadAll();
  }

  ngOnInit(): void {
    this.reload();
  }

  protected reload(): void {
    this.leadsService.detail(this.id()).subscribe({
      next: (detail) => this.detail.set(detail),
      error: () => this.error.set('Lead not found.')
    });
    this.activitiesService.listByLead(this.id()).subscribe({
      next: (activities) => this.activities.set(activities),
      error: () => {} // activities endpoint may not be live yet
    });
  }

  protected addActivity(): void {
    const content = this.newActivityContent.trim();
    if (!content || this.savingActivity()) return;
    this.savingActivity.set(true);
    this.activitiesService
      .add(this.id(), {
        type: this.newActivityType,
        content,
        dueAt:
          this.newActivityType === 'FOLLOW_UP' && this.newActivityDue
            ? new Date(this.newActivityDue).toISOString()
            : undefined
      })
      .subscribe({
        next: () => {
          this.savingActivity.set(false);
          this.newActivityContent = '';
          this.newActivityDue = '';
          this.reload();
        },
        error: (err) => {
          this.savingActivity.set(false);
          this.error.set(err.error?.title ?? 'Could not save the activity.');
        }
      });
  }

  protected completeActivity(activity: Activity): void {
    this.activitiesService.complete(activity.id).subscribe(() => this.reload());
  }

  protected advance(): void {
    const to = this.nextStage();
    if (to) this.transition(to);
  }

  protected reactivate(): void {
    const from = this.detail()?.lead.stalledFromStatus;
    if (from) this.transition(from);
  }

  protected confirmStall(): void {
    if (!this.stallReasonId) return;
    this.transition('STALLED', this.stallReasonId, this.stallNote || undefined);
    this.stalling.set(false);
    this.stallReasonId = '';
    this.stallNote = '';
  }

  private transition(to: LeadStatus, reasonId?: string, note?: string): void {
    this.error.set(null);
    this.leadsService.transition(this.id(), to, reasonId, note).subscribe({
      next: () => this.reload(),
      error: (err) => this.error.set(err.error?.title ?? 'Transition failed.')
    });
  }
}
