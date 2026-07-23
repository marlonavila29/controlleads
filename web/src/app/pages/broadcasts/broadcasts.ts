import { DatePipe } from '@angular/common';
import { Component, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../../core/api/catalog.service';
import { Lead, LeadStatus, LeadsService, STATUS_LABELS } from '../../core/api/leads.service';
import { CommunicationService, CommunicationLogDto } from '../../core/api/communication.service';
import { Sidebar } from '../../shared/sidebar';
import { CustomSelect, SelectOption } from '../../shared/custom-select';

@Component({
  selector: 'app-broadcasts',
  imports: [FormsModule, DatePipe, RouterLink, Sidebar, CustomSelect],
  templateUrl: './broadcasts.html',
  styleUrl: './broadcasts.scss'
})
export class Broadcasts {
  protected readonly catalogs = inject(CatalogService);
  private readonly leadsService = inject(LeadsService);
  private readonly commsService = inject(CommunicationService);

  // Form State
  protected channel: 'EMAIL' | 'WHATSAPP' = 'EMAIL';
  protected subject = '';
  protected body = 'Hi {name},\n\nWe would like to share updates regarding the {course} program. Please let us know if you would like to schedule a call with {counselor}.\n\nBest regards,\nYour Admissions Team';
  
  protected targetType: 'all' | 'status' | 'individual' = 'all';
  protected selectedStatus: LeadStatus = 'LEAD';
  protected searchQuery = '';
  
  // Lists
  protected readonly leads = signal<Lead[]>([]);
  protected readonly logs = signal<CommunicationLogDto[]>([]);
  protected readonly selectedLeadIds = signal<Record<string, boolean>>({});

  // UI state
  protected sending = false;
  protected successMessage = '';
  protected errorMessage = '';

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly statusOptions: SelectOption[] = [
    { id: 'LEAD', name: 'Lead' },
    { id: 'HOT_LEAD', name: 'Hot Lead' },
    { id: 'APPLICATION', name: 'Application' },
    { id: 'STUDENT', name: 'Student' },
    { id: 'STALLED', name: 'Stalled' }
  ];

  // Computed recipients based on target type
  protected readonly resolvedRecipients = computed(() => {
    const list = this.leads();
    const type = this.targetType;
    if (type === 'all') {
      return list;
    } else if (type === 'status') {
      const status = this.selectedStatus;
      return list.filter(l => l.status === status);
    } else {
      const selectedMap = this.selectedLeadIds();
      return list.filter(l => selectedMap[l.id]);
    }
  });

  // Filtered list of leads for individual selection checkboxes
  protected readonly filteredLeads = computed(() => {
    const query = this.searchQuery.toLowerCase().trim();
    const list = this.leads();
    if (!query) return list;
    return list.filter(l => 
      l.fullName.toLowerCase().includes(query) ||
      l.email?.toLowerCase().includes(query) ||
      l.phone?.toLowerCase().includes(query)
    );
  });

  // Preview generated for the first resolved recipient
  protected readonly previewText = computed(() => {
    const recipients = this.resolvedRecipients();
    if (recipients.length === 0) return 'No candidates selected yet.';
    
    const lead = recipients[0];
    let courseName = 'N/A';
    if (lead.courseId) {
      courseName = this.catalogs.nameOf('courses', lead.courseId);
    }
    
    return this.body
      .replace('{name}', lead.fullName)
      .replace('{course}', courseName)
      .replace('{counselor}', lead.assignedToName || 'Your Counselor');
  });

  constructor() {
    this.catalogs.loadAll();
    this.reloadLeads();
    this.reloadLogs();
  }

  protected reloadLeads(): void {
    this.leadsService.list({ size: 200 }).subscribe({
      next: (page) => this.leads.set(page.content),
      error: () => this.errorMessage = 'Failed to load leads list.'
    });
  }

  protected reloadLogs(): void {
    this.commsService.getAllLogs().subscribe({
      next: (data) => this.logs.set(data),
      error: () => this.errorMessage = 'Failed to load message log history.'
    });
  }

  protected toggleLeadSelection(leadId: string): void {
    this.selectedLeadIds.update(map => {
      const copy = { ...map };
      copy[leadId] = !copy[leadId];
      return copy;
    });
  }

  protected selectAllFiltered(): void {
    const visible = this.filteredLeads();
    this.selectedLeadIds.update(map => {
      const copy = { ...map };
      visible.forEach(l => copy[l.id] = true);
      return copy;
    });
  }

  protected deselectAll(): void {
    this.selectedLeadIds.set({});
  }

  protected send(): void {
    const recipients = this.resolvedRecipients();
    if (recipients.length === 0) {
      this.errorMessage = 'Please select at least one recipient candidate.';
      return;
    }
    if (!this.body.trim()) {
      this.errorMessage = 'Message body is required.';
      return;
    }
    if (this.channel === 'EMAIL' && !this.subject.trim()) {
      this.errorMessage = 'Subject is required for Email communications.';
      return;
    }

    this.sending = true;
    this.errorMessage = '';
    this.successMessage = '';

    const leadIds = recipients.map(r => r.id);
    
    this.commsService.sendBatch({
      leadIds,
      channel: this.channel,
      subject: this.channel === 'EMAIL' ? this.subject : undefined,
      body: this.body
    }).subscribe({
      next: (res) => {
        this.sending = false;
        this.successMessage = `Successfully dispatched message queue! Sent: ${res.sent}, Failed: ${res.failed} (due to missing ${this.channel === 'EMAIL' ? 'email address' : 'phone number'}).`;
        this.subject = '';
        this.deselectAll();
        this.reloadLogs();
      },
      error: (err) => {
        this.sending = false;
        this.errorMessage = err.error?.title ?? 'Failed to send campaign.';
      }
    });
  }
}
