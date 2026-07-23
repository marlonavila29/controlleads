import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface OnboardingSlide {
  icon: string;
  badge: string;
  badgeColor: string;
  title: string;
  subtitle: string;
}

@Component({
  selector: 'app-onboarding-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (isOpen()) {
      <div class="modal-backdrop" (click)="close()">
        <div class="card onboarding-card" (click)="$event.stopPropagation()">
          <!-- Header -->
          <div class="onboarding-header">
            <span class="slide-count">Step {{ currentSlide() + 1 }} of {{ slides.length }}</span>
            <button type="button" class="btn-skip" (click)="close()">Skip Tour ✕</button>
          </div>

          <!-- Slide Content -->
          <div class="slide-content">
            <div class="icon-circle" [style.background]="current().badgeColor + '20'" [style.border-color]="current().badgeColor">
              <span class="slide-icon">{{ current().icon }}</span>
            </div>

            <span class="badge-pill" [style.color]="current().badgeColor" [style.background]="current().badgeColor + '15'">
              {{ current().badge }}
            </span>

            <h2 class="slide-title">{{ current().title }}</h2>
            <p class="slide-subtitle">{{ current().subtitle }}</p>
          </div>

          <!-- Dots Indicator -->
          <div class="dots-row">
            @for (s of slides; track $index) {
              <div
                class="dot"
                [class.active]="$index === currentSlide()"
                [style.background]="$index === currentSlide() ? current().badgeColor : 'rgba(150, 150, 150, 0.3)'"
                (click)="currentSlide.set($index)"
              ></div>
            }
          </div>

          <!-- Footer Controls -->
          <div class="onboarding-footer">
            @if (currentSlide() > 0) {
              <button type="button" class="btn-secondary" (click)="prev()">← Back</button>
            } @else {
              <div></div>
            }

            <button
              type="button"
              class="btn-primary"
              [style.background]="current().badgeColor"
              (click)="next()"
            >
              {{ currentSlide() === slides.length - 1 ? 'Get Started 🚀' : 'Next →' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .onboarding-card {
      width: 100%;
      max-width: 520px;
      padding: 32px;
      border-radius: 24px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 20px;
      box-shadow: 0 24px 60px rgba(0, 0, 0, 0.5);
      animation: modalPop 0.3s cubic-bezier(0.16, 1, 0.3, 1);
    }

    .onboarding-header {
      width: 100%;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .slide-count {
        font-size: 12px;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        color: var(--cl-text-muted);
      }

      .btn-skip {
        background: transparent;
        border: none;
        color: var(--cl-text-secondary);
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;
        &:hover { color: var(--cl-text-primary); }
      }
    }

    .slide-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: 14px;
      padding: 10px 0;

      .icon-circle {
        width: 100px;
        height: 100px;
        border-radius: 50%;
        display: grid;
        place-items: center;
        border: 2px solid transparent;
        margin-bottom: 6px;
      }

      .slide-icon {
        font-size: 46px;
      }

      .badge-pill {
        padding: 4px 12px;
        border-radius: 20px;
        font-size: 11px;
        font-weight: 800;
        letter-spacing: 0.5px;
      }

      .slide-title {
        font-size: 22px;
        font-weight: 800;
        margin: 0;
        color: var(--cl-text-primary);
      }

      .slide-subtitle {
        font-size: 14px;
        line-height: 1.6;
        margin: 0;
        color: var(--cl-text-secondary);
      }
    }

    .dots-row {
      display: flex;
      gap: 8px;

      .dot {
        width: 8px;
        height: 8px;
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.3s ease;

        &.active {
          width: 28px;
        }
      }
    }

    .onboarding-footer {
      width: 100%;
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 8px;

      button {
        padding: 12px 24px;
        border-radius: 12px;
        font-size: 14px;
        font-weight: 700;
        cursor: pointer;
        transition: all 0.2s ease;
      }
    }

    @keyframes modalPop {
      from { opacity: 0; transform: scale(0.92) translateY(10px); }
      to { opacity: 1; transform: scale(1) translateY(0); }
    }
  `]
})
export class OnboardingModal {
  readonly isOpen = signal(false);
  readonly currentSlide = signal(0);

  readonly slides: OnboardingSlide[] = [
    {
      icon: '🚀',
      badge: 'WELCOME',
      badgeColor: '#6366F1',
      title: 'Welcome to ControlLeads',
      subtitle: 'The ultimate CRM tailored for international student recruitment, pipeline management, and conversion tracking.'
    },
    {
      icon: '📊',
      badge: 'PIPELINE BOARD',
      badgeColor: '#8B5CF6',
      title: 'Visual Kanban & Status Protection',
      subtitle: 'Drag and drop candidate cards across Lead, Hot Lead, Application, Student, and Stalled stages with built-in confirmation alerts.'
    },
    {
      icon: '⏱️',
      badge: 'SLA TRACKING',
      badgeColor: '#F59E0B',
      title: 'SLA Clock & Activity History',
      subtitle: 'Never breach SLAs on prospective students. Log calls, emails, WhatsApp messages, and follow-up deadlines effortlessly.'
    },
    {
      icon: '✉️',
      badge: 'BULK CAMPAIGNS',
      badgeColor: '#10B981',
      title: 'Mass Broadcast Messaging',
      subtitle: 'Reach hundreds of candidates via Email and WhatsApp using dynamic placeholders like {name}, {course}, and {counselor}.'
    },
    {
      icon: '🎨',
      badge: 'SYSTEM TOUR',
      badgeColor: '#06B6D4',
      title: 'Custom Themes & Replay Tour',
      subtitle: 'Switch between Dark & Light themes anytime. You can re-open this tour whenever you want from the sidebar!'
    }
  ];

  constructor() {
    this.checkAutoShow();
  }

  protected checkAutoShow(): void {
    const seen = localStorage.getItem('controlleads_onboarding_seen');
    if (!seen) {
      this.open();
    }
  }

  public open(): void {
    this.currentSlide.set(0);
    this.isOpen.set(true);
  }

  public close(): void {
    localStorage.setItem('controlleads_onboarding_seen', 'true');
    this.isOpen.set(false);
  }

  protected current(): OnboardingSlide {
    return this.slides[this.currentSlide()];
  }

  protected next(): void {
    if (this.currentSlide() < this.slides.length - 1) {
      this.currentSlide.update((i) => i + 1);
    } else {
      this.close();
    }
  }

  protected prev(): void {
    if (this.currentSlide() > 0) {
      this.currentSlide.update((i) => i - 1);
    }
  }
}
