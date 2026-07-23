import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-add-button',
  imports: [RouterLink],
  template: `
    @if (routerLink()) {
      <a [routerLink]="routerLink()" class="add-btn">
        <svg class="plus-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        <span class="btn-text">{{ label() }}</span>
      </a>
    } @else {
      <button [type]="type()" class="add-btn" [disabled]="disabled()" (click)="btnClick.emit($event)">
        <svg class="plus-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        <span class="btn-text">{{ label() }}</span>
      </button>
    }
  `,
  styles: [`
    :host {
      display: inline-block;
    }

    .add-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: var(--cl-btn-padding-y, 10px) var(--cl-btn-padding-x, 18px);
      background: var(--cl-btn-primary-bg, linear-gradient(135deg, #6366F1 0%, #4F46E5 100%));
      color: var(--cl-btn-primary-text, #FFFFFF);
      font-family: var(--cl-font-family-display);
      font-weight: 700;
      font-size: var(--cl-btn-font-size, 13px);
      border: var(--cl-btn-border, none);
      border-radius: var(--cl-btn-radius, 10px);
      box-shadow: var(--cl-btn-shadow, 0 4px 14px rgba(99, 102, 241, 0.35));
      cursor: pointer;
      text-decoration: none;
      transition: all 0.2s ease;
      box-sizing: border-box;

      .plus-icon {
        flex-shrink: 0;
        transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      }

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: var(--cl-btn-shadow-hover, 0 6px 20px rgba(99, 102, 241, 0.5));
        background: var(--cl-btn-primary-hover-bg, linear-gradient(135deg, #4F46E5 0%, #4338CA 100%));

        .plus-icon {
          transform: rotate(90deg);
        }
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
        box-shadow: none;
      }
    }
  `]
})
export class AddButton {
  readonly label = input.required<string>();
  readonly routerLink = input<string | any[] | null>(null);
  readonly type = input<'button' | 'submit'>('button');
  readonly disabled = input<boolean>(false);
  readonly btnClick = output<MouseEvent>();
}
