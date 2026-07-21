import { CdkDrag, CdkDragDrop, CdkDropList } from '@angular/cdk/drag-drop';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../../core/api/catalog.service';
import { Lead, LeadStatus, LeadsService, STATUS_LABELS } from '../../core/api/leads.service';
import { Topbar } from '../../shared/topbar';

const COLUMNS: LeadStatus[] = ['LEAD', 'HOT_LEAD', 'APPLICATION', 'STUDENT', 'STALLED'];

@Component({
  selector: 'app-board',
  imports: [CdkDropList, CdkDrag, FormsModule, RouterLink, Topbar],
  templateUrl: './board.html',
  styleUrl: './board.scss'
})
export class Board {
  protected readonly catalogs = inject(CatalogService);
  private readonly leadsService = inject(LeadsService);

  protected readonly columns = COLUMNS;
  protected readonly statusLabels = STATUS_LABELS;
  protected readonly byStatus = signal<Record<LeadStatus, Lead[]>>(emptyBoard());
  protected readonly error = signal<string | null>(null);

  // Pending stall: drop into STALLED asks for a reason before committing.
  protected readonly pendingStall = signal<Lead | null>(null);
  protected stallReasonId = '';

  constructor() {
    this.catalogs.loadAll();
    this.reload();
  }

  protected reload(): void {
    this.leadsService.list({ size: 100 }).subscribe((page) => {
      const grouped = emptyBoard();
      for (const lead of page.content) {
        grouped[lead.status].push(lead);
      }
      this.byStatus.set(grouped);
    });
  }

  protected drop(event: CdkDragDrop<LeadStatus>): void {
    const lead = event.item.data as Lead;
    const to = event.container.data;
    if (to === lead.status) return;

    if (to === 'STALLED') {
      this.pendingStall.set(lead);
      return;
    }
    this.transition(lead, to);
  }

  protected confirmStall(): void {
    const lead = this.pendingStall();
    if (!lead || !this.stallReasonId) return;
    this.transition(lead, 'STALLED', this.stallReasonId);
    this.pendingStall.set(null);
    this.stallReasonId = '';
  }

  private transition(lead: Lead, to: LeadStatus, reasonId?: string): void {
    this.error.set(null);
    this.leadsService.transition(lead.id, to, reasonId).subscribe({
      next: () => this.reload(),
      error: (err) => {
        this.error.set(err.error?.title ?? 'Move not allowed.');
        this.reload();
      }
    });
  }
}

function emptyBoard(): Record<LeadStatus, Lead[]> {
  return { LEAD: [], HOT_LEAD: [], APPLICATION: [], STUDENT: [], STALLED: [] };
}
