import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserRole } from '../../core/auth/auth.service';
import { AddButton } from '../../shared/add-button';
import { CustomSelect, SelectOption } from '../../shared/custom-select';
import { Sidebar } from '../../shared/sidebar';
import { TeamMember, UsersService } from '../../core/api/users.service';

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule, FormsModule, DatePipe, Sidebar, AddButton, CustomSelect],
  templateUrl: './users.html',
  styleUrl: './users.scss'
})
export class Users {
  private readonly fb = inject(FormBuilder);
  private readonly usersService = inject(UsersService);

  protected readonly members = signal<TeamMember[]>([]);
  protected readonly loading = signal(false);
  protected readonly creating = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly reassigningId = signal<string | null>(null);
  protected reassignTargetId = '';

  protected readonly roleOptions: SelectOption[] = [
    { id: 'MARKETING_TEAM', name: 'Marketing Team (Counselor)' },
    { id: 'ADMINISTRATOR', name: 'Administrator' }
  ];

  protected readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    role: ['MARKETING_TEAM' as UserRole, Validators.required]
  });

  constructor() {
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.usersService.list().subscribe({
      next: (list) => {
        this.members.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.title ?? 'Failed to load team members.');
        this.loading.set(false);
      }
    });
  }

  protected create(): void {
    if (this.form.invalid || this.creating()) {
      this.form.markAllAsTouched();
      return;
    }
    this.creating.set(true);
    this.error.set(null);

    const val = this.form.getRawValue();
    this.usersService.create({
      name: val.name,
      email: val.email,
      password: val.password,
      role: val.role
    }).subscribe({
      next: () => {
        this.form.reset({ role: 'MARKETING_TEAM' });
        this.creating.set(false);
        this.reload();
      },
      error: (err) => {
        this.error.set(err.error?.title ?? 'Failed to create user.');
        this.creating.set(false);
      }
    });
  }

  protected toggleActive(member: TeamMember): void {
    this.usersService.update(member.id, { active: !member.active }).subscribe({
      next: () => this.reload(),
      error: (err) => this.error.set(err.error?.title ?? 'Failed to toggle status.')
    });
  }

  protected startReassign(member: TeamMember): void {
    this.reassigningId.set(member.id);
    this.reassignTargetId = '';
  }

  protected otherActiveMembers(memberId: string): SelectOption[] {
    return this.members()
      .filter((m) => m.id !== memberId && m.active)
      .map((m) => ({ id: m.id, name: m.name }));
  }

  protected confirmReassign(member: TeamMember): void {
    if (!this.reassignTargetId) return;
    this.usersService.reassignLeads(member.id, this.reassignTargetId).subscribe({
      next: () => {
        this.reassigningId.set(null);
        this.reassignTargetId = '';
        this.reload();
      },
      error: (err) => this.error.set(err.error?.title ?? 'Failed to reassign leads.')
    });
  }
}
