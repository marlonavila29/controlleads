import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, shareReplay, tap, throwError } from 'rxjs';

export type UserRole = 'ADMINISTRATOR' | 'MARKETING_TEAM';

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: UserRole;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

const STORAGE_KEY = 'cl.auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly state = signal<AuthResponse | null>(readStoredAuth());

  readonly user = computed(() => this.state()?.user ?? null);
  readonly isAuthenticated = computed(() => this.state() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMINISTRATOR');

  // Shared in-flight refresh so many parallel 401s trigger a single call.
  private refresh$: Observable<AuthResponse> | null = null;

  get accessToken(): string | null {
    return this.state()?.accessToken ?? null;
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { email, password })
      .pipe(tap((auth) => this.store(auth)));
  }

  /** Exchange the stored refresh token for a fresh pair (single-flight). */
  refresh(): Observable<AuthResponse> {
    if (this.refresh$) {
      return this.refresh$;
    }
    const refreshToken = this.state()?.refreshToken;
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token'));
    }
    this.refresh$ = this.http.post<AuthResponse>('/api/auth/refresh', { refreshToken }).pipe(
      tap((auth) => this.store(auth)),
      catchError((err) => {
        this.logout();
        return throwError(() => err);
      }),
      finalize(() => (this.refresh$ = null)),
      shareReplay(1)
    );
    return this.refresh$;
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>('/api/auth/forgot-password', { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>('/api/auth/reset-password', { token, newPassword });
  }

  logout(): void {
    this.state.set(null);
    localStorage.removeItem(STORAGE_KEY);
    this.router.navigateByUrl('/login');
  }

  private store(auth: AuthResponse): void {
    this.state.set(auth);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  }
}

function readStoredAuth(): AuthResponse | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  } catch {
    return null;
  }
}
