import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CatalogService } from '../../core/api/catalog.service';
import { Topbar } from '../../shared/topbar';

type Kind = 'courses' | 'channels' | 'stall-reasons';

@Component({
  selector: 'app-catalogs',
  imports: [FormsModule, Topbar],
  templateUrl: './catalogs.html',
  styleUrl: './catalogs.scss'
})
export class Catalogs {
  protected readonly catalogs = inject(CatalogService);
  protected readonly error = signal<string | null>(null);

  protected readonly sections: { kind: Kind; title: string; hint: string }[] = [
    { kind: 'courses', title: 'Courses', hint: 'Programs leads can be interested in' },
    { kind: 'channels', title: 'Channels', hint: 'Where leads come from' },
    { kind: 'stall-reasons', title: 'Stall reasons', hint: 'Why leads stop — feeds the drop-off report' }
  ];

  protected drafts: Record<Kind, string> = { courses: '', channels: '', 'stall-reasons': '' };

  constructor() {
    this.catalogs.loadAll();
  }

  protected itemsOf(kind: Kind) {
    switch (kind) {
      case 'courses':
        return this.catalogs.courses();
      case 'channels':
        return this.catalogs.channels();
      case 'stall-reasons':
        return this.catalogs.stallReasons();
    }
  }

  protected add(kind: Kind): void {
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

  protected toggle(kind: Kind, id: string, active: boolean): void {
    this.catalogs.update(kind, id, { active: !active }).subscribe(() => this.catalogs.reload(kind));
  }
}
