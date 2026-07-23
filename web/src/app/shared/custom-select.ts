import { Component, ElementRef, HostListener, Input, forwardRef, inject, signal, computed } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface SelectOption {
  id: string;
  name: string;
}

@Component({
  selector: 'app-custom-select',
  imports: [FormsModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CustomSelect),
      multi: true
    }
  ],
  template: `
    <div class="custom-select-wrapper">
      <div class="select-trigger" (click)="toggleDropdown()" [class.open]="isOpen()" [class.disabled]="disabled">
        <span class="selected-text">
          @if (selectedLabel(); as label) {
            <span class="label-text">{{ label }}</span>
          } @else {
            <span class="placeholder">{{ placeholder }}</span>
          }
        </span>
        <span class="chevron">▼</span>
      </div>

      @if (isOpen()) {
        <div class="dropdown-popover">
          @if (showSearch()) {
            <div class="search-box">
              <span class="search-icon">🔍</span>
              <input
                type="text"
                [ngModel]="searchQuery()"
                (ngModelChange)="onSearchInput($event)"
                [placeholder]="allowCustomText ? 'Search or type custom value…' : 'Search…'"
                class="search-input"
                (click)="$event.stopPropagation()"
              />
            </div>
          }

          <div class="options-list">
            @for (opt of filteredOptions(); track opt.id) {
              <div
                class="option-item"
                [class.selected]="opt.id === value()"
                (click)="selectOption(opt)"
              >
                <span class="option-name">{{ opt.name }}</span>
              </div>
            } @empty {
              @if (allowCustomText && searchQuery().trim()) {
                <div class="option-item custom-option" (click)="selectCustomText(searchQuery().trim())">
                  <span>✨ Use custom: <strong>"{{ searchQuery().trim() }}"</strong></span>
                </div>
              } @else {
                <div class="no-results">No matching options</div>
              }
            }
          </div>
        </div>
      }

      @if (isOtherSelected() && allowOther) {
        <div class="other-specify-field">
          <label class="other-label">
            <span>Specify Detail *</span>
            <input
              type="text"
              [ngModel]="otherDetailValue()"
              (ngModelChange)="onOtherDetailChange($event)"
              placeholder="Please specify details…"
              class="other-input"
            />
          </label>
        </div>
      }
    </div>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
    }

    .custom-select-wrapper {
      position: relative;
      width: 100%;
    }

    .select-trigger {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 42px;
      padding: 10px 14px;
      background: #0D1322 !important;
      border: 1px solid rgba(255, 255, 255, 0.12) !important;
      border-radius: 10px;
      cursor: pointer;
      user-select: none;
      transition: all 0.2s ease;
      box-sizing: border-box;

      &:hover:not(.disabled) {
        border-color: rgba(99, 102, 241, 0.5) !important;
      }

      &.open {
        border-color: #6366F1 !important;
        box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.25) !important;
      }

      &.disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      .selected-text {
        display: flex;
        align-items: center;
        font-size: 13px;
        color: #F8FAFC;
        overflow: hidden;

        .label-text {
          font-weight: 500;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .placeholder {
          color: #64748B;
        }
      }

      .chevron {
        font-size: 10px;
        color: #94A3B8;
        transition: transform 0.2s ease;
        margin-left: 8px;
      }

      &.open .chevron {
        transform: rotate(180deg);
      }
    }

    .dropdown-popover {
      position: absolute;
      top: calc(100% + 6px);
      left: 0;
      right: 0;
      z-index: 1000;
      background: #161F33;
      border: 1px solid rgba(255, 255, 255, 0.12);
      border-radius: 12px;
      box-shadow: 0 16px 40px rgba(0, 0, 0, 0.6);
      padding: 8px;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .search-box {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 0 10px;
      background: #0D1322;
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 8px;

      .search-icon {
        font-size: 12px;
      }

      .search-input {
        width: 100%;
        border: none !important;
        background: transparent !important;
        padding: 8px 0 !important;
        font-size: 12px;
        box-shadow: none !important;
        color: #F8FAFC !important;
      }
    }

    .options-list {
      max-height: 220px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .option-item {
      display: flex;
      align-items: center;
      padding: 8px 12px;
      border-radius: 8px;
      cursor: pointer;
      transition: background 0.15s ease;
      font-size: 13px;
      color: #CBD5E1;

      &:hover {
        background: rgba(99, 102, 241, 0.15);
        color: #F8FAFC;
      }

      &.selected {
        background: rgba(99, 102, 241, 0.25);
        color: #818CF8;
        font-weight: 700;
      }

      &.custom-option {
        color: #818CF8;
        background: rgba(99, 102, 241, 0.1);
        border: 1px dashed rgba(99, 102, 241, 0.4);
      }
    }

    .no-results {
      padding: 14px;
      text-align: center;
      color: #64748B;
      font-size: 12px;
    }

    .other-specify-field {
      margin-top: 8px;
      padding: 12px;
      background: rgba(99, 102, 241, 0.08);
      border: 1px solid rgba(99, 102, 241, 0.3);
      border-radius: 10px;

      .other-label {
        display: flex;
        flex-direction: column;
        gap: 6px;

        span {
          font-size: 11px;
          font-weight: 700;
          color: #818CF8;
        }

        .other-input {
          width: 100%;
        }
      }
    }
  `]
})
export class CustomSelect implements ControlValueAccessor {
  @Input() options: (SelectOption | string)[] = [];
  @Input() placeholder = 'Select…';
  @Input() allowCustomText = false;
  @Input() allowOther = false;
  @Input() showSearch = () => true;

  private readonly elementRef = inject(ElementRef);

  protected readonly isOpen = signal(false);
  protected readonly value = signal<string>('');
  protected readonly searchQuery = signal<string>('');
  protected readonly otherDetailValue = signal<string>('');
  disabled = false;

  onChange: (val: string) => void = () => {};
  onTouched: () => void = () => {};

  protected readonly normalizedOptions = computed<SelectOption[]>(() => {
    return (this.options || []).map((opt) => {
      if (typeof opt === 'string') {
        return { id: opt, name: opt };
      }
      return opt;
    });
  });

  protected readonly selectedLabel = computed(() => {
    const val = this.value();
    if (!val) return '';
    const found = this.normalizedOptions().find((o) => o.id === val);
    if (found) return found.name;
    return val; // Allow custom text display
  });

  protected readonly isOtherSelected = computed(() => {
    const label = this.selectedLabel().toLowerCase();
    return label.includes('other') || label.includes('outro');
  });

  protected readonly filteredOptions = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const list = this.normalizedOptions();
    if (!q) return list;
    return list.filter((o) => o.name.toLowerCase().includes(q) || o.id.toLowerCase().includes(q));
  });

  writeValue(obj: any): void {
    this.value.set(obj ? String(obj) : '');
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  protected toggleDropdown(): void {
    if (this.disabled) return;
    this.isOpen.set(!this.isOpen());
    if (!this.isOpen()) {
      this.onTouched();
    } else {
      this.searchQuery.set('');
    }
  }

  protected selectOption(opt: SelectOption): void {
    this.value.set(opt.id);
    this.onChange(opt.id);
    this.onTouched();
    this.isOpen.set(false);
  }

  protected selectCustomText(text: string): void {
    this.value.set(text);
    this.onChange(text);
    this.onTouched();
    this.isOpen.set(false);
  }

  protected onSearchInput(val: string): void {
    this.searchQuery.set(val || '');
    if (this.allowCustomText && val) {
      // In combobox mode, live typing updates the form value directly
      this.value.set(val);
      this.onChange(val);
    }
  }

  protected onOtherDetailChange(detail: string): void {
    this.otherDetailValue.set(detail);
    // Trigger onChange if needed
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      if (this.isOpen()) {
        this.isOpen.set(false);
        this.onTouched();
      }
    }
  }
}
