import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

export interface CatalogItem {
  id: string;
  name: string;
  active: boolean;
}

export interface Country {
  code: string;
  name: string;
}

export interface SystemSettings {
  hotLeadMaxHours: number;
  shareLeadsVisibility: boolean;
}

export type CatalogKind = 'courses' | 'channels' | 'stall-reasons' | 'campaigns';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  readonly courses = signal<CatalogItem[]>([]);
  readonly channels = signal<CatalogItem[]>([]);
  readonly stallReasons = signal<CatalogItem[]>([]);
  readonly campaigns = signal<CatalogItem[]>([]);
  readonly countries = signal<Country[]>([]);
  readonly settings = signal<SystemSettings | null>(null);

  readonly activeCourses = computed(() => this.courses().filter((c) => c.active));
  readonly activeChannels = computed(() => this.channels().filter((c) => c.active));
  readonly activeStallReasons = computed(() => this.stallReasons().filter((c) => c.active));
  readonly activeCampaigns = computed(() => this.campaigns().filter((c) => c.active));

  loadAll(): void {
    this.reload('courses');
    this.reload('channels');
    this.reload('stall-reasons');
    this.reload('campaigns');
    this.loadCountries();
    this.loadSettings();
  }

  loadCountries(): void {
    this.http.get<Country[]>('/api/countries').subscribe({
      next: (list) => this.countries.set(list),
      error: () => {}
    });
  }

  loadSettings(): void {
    this.http.get<SystemSettings>('/api/settings').subscribe({
      next: (val) => this.settings.set(val),
      error: () => {}
    });
  }

  updateSettings(patch: Partial<SystemSettings>) {
    return this.http.patch<SystemSettings>('/api/settings', patch).subscribe({
      next: (val) => this.settings.set(val),
      error: () => {}
    });
  }

  reload(kind: CatalogKind): void {
    this.http.get<CatalogItem[]>(`/api/${kind}`).subscribe((items) => this.target(kind).set(items));
  }

  create(kind: CatalogKind, name: string) {
    return this.http.post<CatalogItem>(`/api/${kind}`, { name });
  }

  update(kind: CatalogKind, id: string, patch: Partial<Pick<CatalogItem, 'name' | 'active'>>) {
    return this.http.patch<CatalogItem>(`/api/${kind}/${id}`, patch);
  }

  nameOf(kind: CatalogKind, id: string | null): string {
    if (!id) return '—';
    return this.target(kind)().find((c) => c.id === id)?.name ?? '—';
  }

  countryNameOf(code: string | null): string {
    if (!code) return '—';
    const match = this.countries().find((c) => c.code.toUpperCase() === code.toUpperCase());
    return match ? `${match.name} (${match.code})` : code;
  }

  private target(kind: CatalogKind) {
    switch (kind) {
      case 'courses':
        return this.courses;
      case 'channels':
        return this.channels;
      case 'stall-reasons':
        return this.stallReasons;
      case 'campaigns':
        return this.campaigns;
    }
  }
}
