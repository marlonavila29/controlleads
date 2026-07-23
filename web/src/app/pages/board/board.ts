import { CdkDrag, CdkDragDrop, CdkDropList, CdkDropListGroup } from '@angular/cdk/drag-drop';
import { Component, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../../core/api/catalog.service';
import { Lead, LeadStatus, LeadsService, STATUS_LABELS } from '../../core/api/leads.service';
import { AddButton } from '../../shared/add-button';
import { CustomSelect } from '../../shared/custom-select';
import { Sidebar } from '../../shared/sidebar';

const COLUMNS: LeadStatus[] = ['LEAD', 'HOT_LEAD', 'APPLICATION', 'STUDENT', 'STALLED'];

@Component({
  selector: 'app-board',
  imports: [CdkDropList, CdkDrag, CdkDropListGroup, FormsModule, RouterLink, Sidebar, AddButton, CustomSelect],
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

  // Tab and list state
  protected readonly activeTab = signal<'board' | 'list'>('board');
  protected readonly searchQuery = signal('');
  protected readonly selectedCourseId = signal<string>('');
  protected readonly selectedCounselorId = signal<string>('');
  protected readonly selectedChannelId = signal<string>('');
  protected readonly allLeads = signal<Lead[]>([]);

  // Filtered leads list across search & column select options
  protected readonly filteredLeads = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const courseId = this.selectedCourseId();
    const counselorId = this.selectedCounselorId();
    const channelId = this.selectedChannelId();

    return this.allLeads().filter((l) => {
      if (courseId && l.courseId !== courseId) return false;
      if (counselorId && l.assignedTo !== counselorId) return false;
      if (channelId && l.channelId !== channelId) return false;

      if (query) {
        const courseName = this.catalogs.nameOf('courses', l.courseId).toLowerCase();
        const channelName = this.catalogs.nameOf('channels', l.channelId).toLowerCase();
        const matchName = l.fullName.toLowerCase().includes(query);
        const matchCounselor = l.assignedToName.toLowerCase().includes(query);
        const matchCourse = courseName.includes(query);
        const matchChannel = channelName.includes(query);

        if (!matchName && !matchCounselor && !matchCourse && !matchChannel) {
          return false;
        }
      }

      return true;
    });
  });

  // Grouped filtered leads by status for the drag & drop board
  protected readonly filteredByStatus = computed(() => {
    const grouped = emptyBoard();
    for (const lead of this.filteredLeads()) {
      grouped[lead.status].push(lead);
    }
    return grouped;
  });

  protected readonly hasActiveFilters = computed(() => {
    return !!(this.searchQuery() || this.selectedCourseId() || this.selectedCounselorId() || this.selectedChannelId());
  });

  // Derived counselors options list for filtering
  protected readonly counselorOptions = computed(() => {
    const map = new Map<string, string>();
    for (const lead of this.allLeads()) {
      if (lead.assignedTo && lead.assignedToName) {
        map.set(lead.assignedTo, lead.assignedToName);
      }
    }
    return Array.from(map.entries()).map(([id, name]) => ({ id, name, active: true }));
  });

  protected resetFilters(): void {

    this.searchQuery.set('');
    this.selectedCourseId.set('');
    this.selectedCounselorId.set('');
    this.selectedChannelId.set('');
  }

  // Pending status transition confirmation modal state
  protected readonly pendingTransition = signal<{ lead: Lead; targetStatus: LeadStatus } | null>(null);

  // Pending stall: drop into STALLED asks for a reason before committing.
  protected readonly pendingStall = signal<Lead | null>(null);
  protected stallReasonId = '';

  constructor() {
    this.catalogs.loadAll();
    this.reload();
  }

  protected reload(): void {
    this.leadsService.list({ size: 200 }).subscribe((page) => {
      this.allLeads.set(page.content);
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
    this.pendingTransition.set({ lead, targetStatus: to });
  }

  protected onStatusChange(lead: Lead, to: LeadStatus): void {
    if (to === lead.status) return;

    if (to === 'STALLED') {
      this.pendingStall.set(lead);
      return;
    }
    this.pendingTransition.set({ lead, targetStatus: to });
  }

  protected confirmTransition(): void {
    const pending = this.pendingTransition();
    if (!pending) return;
    this.transition(pending.lead, pending.targetStatus);
    this.pendingTransition.set(null);
  }

  protected cancelTransition(): void {
    this.pendingTransition.set(null);
    this.reload();
  }


  protected confirmStall(): void {
    const lead = this.pendingStall();
    if (!lead || !this.stallReasonId) return;
    this.transition(lead, 'STALLED', this.stallReasonId);
    this.pendingStall.set(null);
    this.stallReasonId = '';
  }

  protected cancelStall(): void {
    this.pendingStall.set(null);
    this.stallReasonId = '';
    this.reload(); // Reset the select dropdown to the previous status value
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
