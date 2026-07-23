import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthUser, UserRole } from '../auth/auth.service';

export interface TeamMember extends AuthUser {
  active: boolean;
  createdAt: string;
  leadCount: number;
}

export interface CreateUserRequest {
  name: string;
  email: string;
  password: string;
  role: UserRole;
}

export interface UpdateUserRequest {
  name?: string;
  role?: UserRole;
  active?: boolean;
}

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);

  list(): Observable<TeamMember[]> {
    return this.http.get<TeamMember[]>('/api/users');
  }

  create(request: CreateUserRequest): Observable<AuthUser> {
    return this.http.post<AuthUser>('/api/users', request);
  }

  update(id: string, request: UpdateUserRequest): Observable<AuthUser> {
    return this.http.patch<AuthUser>(`/api/users/${id}`, request);
  }

  reassignLeads(fromUserId: string, toUserId: string): Observable<{ reassigned: number }> {
    return this.http.post<{ reassigned: number }>(`/api/users/${fromUserId}/reassign-leads`, { toUserId });
  }
}
