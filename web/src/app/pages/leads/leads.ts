import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CatalogService } from '../../core/api/catalog.service';
import { Lead, LeadPage, LeadStatus, LeadsService, STATUS_LABELS } from '../../core/api/leads.service';
import { StatusBadge } from '../../shared/status-badge';
import { Topbar } from '../../shared/topbar';

@Component({
  selector: 'app-leads',
  imports: [FormsModule, RouterLink, DatePipe, StatusBadge, Topbar],
  templateUrl: './leads.html',
  styleUrl: './leads.scss'
})
export class Leads {
  protected readonly catalogs = inject(CatalogService);
  private readonly leadsService = inject(LeadsService);
  private readonly router = inject(Router);

  protected readonly statuses = Object.keys(STATUS_LABELS) as LeadStatus[];
  protected readonly statusLabels = STATUS_LABELS;

  protected readonly page = signal<LeadPage | null>(null);
  protected readonly loading = signal(false);

  protected q = '';
  protected status: LeadStatus | '' = '';
  protected courseId = '';
  protected channelId = '';
  protected pageIndex = 0;

  constructor() {
    this.catalogs.loadAll();
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.leadsService
      .list({
        q: this.q || undefined,
        status: (this.status || undefined) as LeadStatus | undefined,
        courseId: this.courseId || undefined,
        channelId: this.channelId || undefined,
        page: this.pageIndex
      })
      .subscribe((page) => {
        this.page.set(page);
        this.loading.set(false);
      });
  }

  protected applyFilters(): void {
    this.pageIndex = 0;
    this.reload();
  }

  protected goTo(lead: Lead): void {
    this.router.navigate(['/leads', lead.id]);
  }

  protected move(delta: number): void {
    this.pageIndex += delta;
    this.reload();
  }
}
