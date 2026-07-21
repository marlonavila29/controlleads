import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { PingResponse, PingService } from './core/api/ping.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('ControlLeads');
  protected readonly ping = signal<PingResponse | null>(null);
  protected readonly pingError = signal(false);

  private readonly pingService = inject(PingService);

  constructor() {
    this.pingService.ping().subscribe({
      next: (res) => this.ping.set(res),
      error: () => this.pingError.set(true)
    });
  }
}
