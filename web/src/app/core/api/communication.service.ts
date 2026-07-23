import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface CommunicationLogDto {
  id: string;
  leadId: string;
  leadName: string;
  sentByName: string;
  channel: 'EMAIL' | 'WHATSAPP';
  recipientAddress: string;
  subject: string | null;
  body: string;
  status: 'SENT' | 'FAILED';
  createdAt: string;
}

export interface SendBatchRequest {
  leadIds: string[];
  channel: 'EMAIL' | 'WHATSAPP';
  subject?: string;
  body: string;
}

export interface SendResultDto {
  total: number;
  sent: number;
  failed: number;
}

@Injectable({ providedIn: 'root' })
export class CommunicationService {
  private readonly http = inject(HttpClient);

  sendBatch(request: SendBatchRequest): Observable<SendResultDto> {
    return this.http.post<SendResultDto>('/api/communications/send', request);
  }

  getAllLogs(): Observable<CommunicationLogDto[]> {
    return this.http.get<CommunicationLogDto[]>('/api/communications/logs');
  }

  getLogsForLead(leadId: string): Observable<CommunicationLogDto[]> {
    return this.http.get<CommunicationLogDto[]>(`/api/communications/logs/lead/${leadId}`);
  }
}
