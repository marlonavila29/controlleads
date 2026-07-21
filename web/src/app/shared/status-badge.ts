import { Component, input } from '@angular/core';
import { LeadStatus, STATUS_LABELS } from '../core/api/leads.service';

@Component({
  selector: 'app-status-badge',
  template: `<span class="badge" [class]="'badge badge--' + status().toLowerCase()">{{ label() }}</span>`,
  styles: `
    .badge {
      display: inline-block;
      padding: 2px var(--cl-space-3);
      border-radius: var(--cl-radius-full);
      font-size: var(--cl-font-size-xs);
      font-weight: var(--cl-font-weight-semibold);
      color: var(--cl-color-neutral-0);
    }
    .badge--lead { background: var(--cl-color-status-lead); }
    .badge--hot_lead { background: var(--cl-color-status-hot-lead); }
    .badge--application { background: var(--cl-color-status-application); }
    .badge--student { background: var(--cl-color-status-student); }
    .badge--stalled { background: var(--cl-color-status-stalled); }
  `
})
export class StatusBadge {
  readonly status = input.required<LeadStatus>();

  label(): string {
    return STATUS_LABELS[this.status()];
  }
}
