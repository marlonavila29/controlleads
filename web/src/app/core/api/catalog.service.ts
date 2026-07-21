import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

export interface CatalogItem {
  id: string;
  name: string;
  active: boolean;
}

type CatalogKind = 'courses' | 'channels' | 'stall-reasons';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  readonly courses = signal<CatalogItem[]>([]);
  readonly channels = signal<CatalogItem[]>([]);
  readonly stallReasons = signal<CatalogItem[]>([]);

  readonly activeCourses = computed(() => this.courses().filter((c) => c.active));
  readonly activeChannels = computed(() => this.channels().filter((c) => c.active));
  readonly activeStallReasons = computed(() => this.stallReasons().filter((c) => c.active));

  loadAll(): void {
    this.reload('courses');
    this.reload('channels');
    this.reload('stall-reasons');
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

  private target(kind: CatalogKind) {
    switch (kind) {
      case 'courses':
        return this.courses;
      case 'channels':
        return this.channels;
      case 'stall-reasons':
        return this.stallReasons;
    }
  }
}
