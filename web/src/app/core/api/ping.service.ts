import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Fase 0 hand-written stand-in. Replaced by the generated OpenAPI client
 * (shared/api-contract) once the contract pipeline runs in CI — never
 * hand-write API models after that (see CLAUDE.md).
 */
export interface PingResponse {
  service: string;
  status: string;
  serverTime: string;
}

@Injectable({ providedIn: 'root' })
export class PingService {
  private readonly http = inject(HttpClient);

  ping(): Observable<PingResponse> {
    return this.http.get<PingResponse>('/api/ping');
  }
}
