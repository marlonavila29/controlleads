import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { PingResponse, PingService } from '../../core/api/ping.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard {
  protected readonly auth = inject(AuthService);

  protected readonly ping = signal<PingResponse | null>(null);

  private readonly pingService = inject(PingService);

  constructor() {
    this.pingService.ping().subscribe({
      next: (res) => this.ping.set(res),
      error: () => this.ping.set(null)
    });
  }
}
