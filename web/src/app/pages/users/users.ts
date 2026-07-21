import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthUser, UserRole } from '../../core/auth/auth.service';

interface TeamMember extends AuthUser {
  active: boolean;
  createdAt: string;
  leadCount: number;
}

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule, FormsModule, RouterLink, DatePipe],
  templateUrl: './users.html',
  styleUrl: './users.scss'
})
export class Users {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);

  protected readonly members = signal<TeamMember[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly creating = signal(false);

  // Inline "reassign this user's leads" state.
  protected readonly reassigningId = signal<string | null>(null);
  protected reassignTargetId = '';

  protected otherActiveMembers(memberId: string): TeamMember[] {
    return this.members().filter((m) => m.id !== memberId && m.active);
  }

  protected readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['MARKETING_TEAM' as UserRole, Validators.required]
  });

  constructor() {
    this.reload();
  }

  protected reload(): void {
    this.http.get<TeamMember[]>('/api/users').subscribe((list) => this.members.set(list));
  }

  protected create(): void {
    if (this.form.invalid || this.creating()) {
      this.form.markAllAsTouched();
      return;
    }
    this.creating.set(true);
    this.error.set(null);
    this.http.post<TeamMember>('/api/users', this.form.getRawValue()).subscribe({
      next: () => {
        this.creating.set(false);
        this.form.reset({ name: '', email: '', password: '', role: 'MARKETING_TEAM' });
        this.reload();
      },
      error: (err) => {
        this.creating.set(false);
        this.error.set(err.status === 409 ? 'A user with this email already exists.' : 'Could not create user.');
      }
    });
  }

  protected toggleActive(member: TeamMember): void {
    this.http
      .patch<TeamMember>(`/api/users/${member.id}`, { active: !member.active })
      .subscribe(() => this.reload());
  }

  protected startReassign(member: TeamMember): void {
    this.reassignTargetId = '';
    this.reassigningId.set(member.id);
  }

  protected confirmReassign(member: TeamMember): void {
    if (!this.reassignTargetId) return;
    this.http
      .post<{ reassigned: number }>(`/api/users/${member.id}/reassign-leads`, {
        toUserId: this.reassignTargetId
      })
      .subscribe({
        next: () => {
          this.reassigningId.set(null);
          this.reload();
        },
        error: (err) => this.error.set(err.error?.title ?? 'Could not reassign leads.')
      });
  }
}
