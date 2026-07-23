import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CatalogKind, CatalogService } from '../../core/api/catalog.service';
import { AddButton } from '../../shared/add-button';
import { Sidebar } from '../../shared/sidebar';

@Component({
  selector: 'app-catalogs',
  imports: [FormsModule, Sidebar, AddButton],
  templateUrl: './catalogs.html',
  styleUrl: './catalogs.scss'
})
export class Catalogs {
  protected readonly catalogs = inject(CatalogService);
  protected readonly error = signal<string | null>(null);

  protected readonly sections: { kind: CatalogKind; title: string; hint: string }[] = [
    { kind: 'courses', title: 'Courses', hint: 'Programs leads can be interested in' },
    { kind: 'channels', title: 'Channels', hint: 'Where leads come from' },
    { kind: 'stall-reasons', title: 'Stall reasons', hint: 'Why leads stop — feeds the drop-off report' },
    { kind: 'campaigns', title: 'Marketing Campaigns', hint: 'UTM marketing campaigns for lead intake' }
  ];

  protected drafts: Record<CatalogKind, string> = {
    courses: '',
    channels: '',
    'stall-reasons': '',
    campaigns: ''
  };

  constructor() {
    this.catalogs.loadAll();
  }

  protected itemsOf(kind: CatalogKind) {
    switch (kind) {
      case 'courses':
        return this.catalogs.courses();
      case 'channels':
        return this.catalogs.channels();
      case 'stall-reasons':
        return this.catalogs.stallReasons();
      case 'campaigns':
        return this.catalogs.campaigns();
      default:
        return [];
    }
  }

  protected add(kind: CatalogKind): void {
    const name = this.drafts[kind].trim();
    if (!name) return;
    this.error.set(null);
    this.catalogs.create(kind, name).subscribe({
      next: () => {
        this.drafts[kind] = '';
        this.catalogs.reload(kind);
      },
      error: (err) => this.error.set(err.error?.title ?? 'Could not add item.')
    });
  }

  protected toggle(kind: CatalogKind, id: string, active: boolean): void {
    this.catalogs.update(kind, id, { active: !active }).subscribe(() => this.catalogs.reload(kind));
  }

  protected toggleShareVisibility(currentValue: boolean): void {
    this.catalogs.updateSettings({ shareLeadsVisibility: !currentValue });
  }

  protected updateSlaHours(hours: number): void {
    if (!hours || hours < 1) return;
    this.catalogs.updateSettings({ hotLeadMaxHours: hours });
  }
}
