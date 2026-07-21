import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: '../login/login.scss'
})
export class ResetPassword {
  /** Bound from the ?token= query param (withComponentInputBinding). */
  readonly token = input('');

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirm: ['', Validators.required]
  });

  protected submit(): void {
    const { newPassword, confirm } = this.form.getRawValue();
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    if (newPassword !== confirm) {
      this.error.set('Passwords do not match.');
      return;
    }
    if (!this.token()) {
      this.error.set('This reset link is invalid or incomplete.');
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.auth.resetPassword(this.token(), newPassword).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { reset: '1' } }),
      error: (err) => {
        this.submitting.set(false);
        this.error.set(
          err.status === 400 ? 'This reset link is invalid or has expired.' : 'Something went wrong. Try again.'
        );
      }
    });
  }
}
