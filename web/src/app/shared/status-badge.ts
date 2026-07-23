import { Component, input } from '@angular/core';
import { LeadStatus, STATUS_LABELS } from '../core/api/leads.service';

@Component({
  selector: 'app-status-badge',
  template: `
    <span class="badge" [class]="'badge badge--' + status().toLowerCase()">
      <span class="badge-dot"></span>
      <span class="badge-label">{{ label() }}</span>
    </span>
  `,
  styles: `
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 10px;
      border-radius: 9999px;
      font-family: var(--cl-font-family-display);
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.02em;
      line-height: 1.2;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
    }
    .badge-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .badge--lead {
      background: rgba(148, 163, 184, 0.12);
      color: #94A3B8;
      border: 1px solid rgba(148, 163, 184, 0.3);
      .badge-dot { background: #94A3B8; box-shadow: 0 0 8px rgba(148, 163, 184, 0.8); }
    }
    .badge--hot_lead {
      background: rgba(245, 158, 11, 0.15);
      color: #FBBF24;
      border: 1px solid rgba(245, 158, 11, 0.35);
      .badge-dot { background: #F59E0B; box-shadow: 0 0 8px rgba(245, 158, 11, 0.9); }
    }
    .badge--application {
      background: rgba(14, 165, 233, 0.15);
      color: #38BDF8;
      border: 1px solid rgba(14, 165, 233, 0.35);
      .badge-dot { background: #0EA5E9; box-shadow: 0 0 8px rgba(14, 165, 233, 0.9); }
    }
    .badge--student {
      background: rgba(16, 185, 129, 0.15);
      color: #34D399;
      border: 1px solid rgba(16, 185, 129, 0.35);
      .badge-dot { background: #10B981; box-shadow: 0 0 8px rgba(16, 185, 129, 0.9); }
    }
    .badge--stalled {
      background: rgba(239, 68, 68, 0.15);
      color: #F87171;
      border: 1px solid rgba(239, 68, 68, 0.35);
      .badge-dot { background: #EF4444; box-shadow: 0 0 8px rgba(239, 68, 68, 0.9); }
    }
  `
})
export class StatusBadge {
  readonly status = input.required<LeadStatus>();

  label(): string {
    return STATUS_LABELS[this.status()];
  }
}
