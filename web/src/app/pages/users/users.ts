import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthUser, UserRole } from '../../core/auth/auth.service';

interface TeamMember extends AuthUser {
  active: boolean;
  createdAt: string;
}

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './users.html',
  styleUrl: './users.scss'
})
export class Users {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);

  protected readonly members = signal<TeamMember[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly creating = signal(false);

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
}
