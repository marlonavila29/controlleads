import { Component, ElementRef, HostListener, Input, forwardRef, inject, signal, computed } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CatalogService, Country } from '../core/api/catalog.service';

@Component({
  selector: 'app-country-select',
  imports: [FormsModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CountrySelect),
      multi: true
    }
  ],
  template: `
    <div class="country-select-container">
      <div class="select-trigger" (click)="toggleDropdown()" [class.open]="isOpen()" [class.disabled]="disabled">
        <span class="selected-text">
          @if (selectedCountry(); as c) {
            <span class="flag-icon">{{ getFlagEmoji(c.code) }}</span>
            <span class="country-name">{{ c.name }}</span>
            <span class="country-code">({{ c.code }})</span>
          } @else {
            <span class="placeholder">Select a country…</span>
          }
        </span>
        <span class="chevron">▼</span>
      </div>

      @if (isOpen()) {
        <div class="dropdown-popover">
          <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
              type="text"
              [ngModel]="searchQuery()"
              (ngModelChange)="searchQuery.set($event || '')"
              placeholder="Search country or ISO code…"
              class="search-input"
              #searchInput
              (click)="$event.stopPropagation()"
            />
          </div>

          <div class="options-list">
            @for (c of filteredCountries(); track c.code) {
              <div
                class="option-item"
                [class.selected]="c.code === value()"
                (click)="selectCountry(c)"
              >
                <span class="flag-icon">{{ getFlagEmoji(c.code) }}</span>
                <span class="country-name">{{ c.name }}</span>
                <span class="country-code-badge">{{ c.code }}</span>
              </div>
            } @empty {
              <div class="no-results">No countries match "{{ searchQuery() }}"</div>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
    }

    .country-select-container {
      position: relative;
      width: 100%;
    }

    .select-trigger {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      padding: 10px 14px;
      background: #0D1322 !important;
      border: 1px solid rgba(255, 255, 255, 0.12) !important;
      border-radius: 10px;
      cursor: pointer;
      user-select: none;
      transition: all 0.2s ease;

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
        gap: 8px;
        font-size: 13px;
        color: #F8FAFC;
        overflow: hidden;

        .flag-icon {
          font-size: 16px;
        }

        .country-name {
          font-weight: 500;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .country-code {
          color: #818CF8;
          font-weight: 700;
          font-size: 11px;
        }

        .placeholder {
          color: #64748B;
        }
      }

      .chevron {
        font-size: 10px;
        color: #94A3B8;
        transition: transform 0.2s ease;
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
      gap: 10px;
      padding: 8px 12px;
      border-radius: 8px;
      cursor: pointer;
      transition: background 0.15s ease;

      &:hover {
        background: rgba(99, 102, 241, 0.15);
      }

      &.selected {
        background: rgba(99, 102, 241, 0.25);
        border: 1px solid rgba(99, 102, 241, 0.4);
      }

      .flag-icon {
        font-size: 16px;
      }

      .country-name {
        flex: 1;
        font-size: 13px;
        color: #F8FAFC;
        font-weight: 500;
      }

      .country-code-badge {
        font-size: 11px;
        font-weight: 700;
        color: #818CF8;
        background: rgba(99, 102, 241, 0.15);
        padding: 2px 6px;
        border-radius: 4px;
      }
    }

    .no-results {
      padding: 16px;
      text-align: center;
      color: #64748B;
      font-size: 12px;
    }
  `]
})
export class CountrySelect implements ControlValueAccessor {
  protected readonly catalogs = inject(CatalogService);
  private readonly elementRef = inject(ElementRef);

  protected readonly isOpen = signal(false);
  protected readonly value = signal<string>('');
  protected readonly searchQuery = signal<string>('');
  disabled = false;

  onChange: (val: string) => void = () => {};
  onTouched: () => void = () => {};

  protected readonly selectedCountry = computed(() => {
    const val = this.value();
    if (!val) return null;
    return this.catalogs.countries().find((c) => c.code.toUpperCase() === val.toUpperCase()) ?? null;
  });

  protected readonly filteredCountries = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const list = this.catalogs.countries();
    if (!q) return list;
    return list.filter(
      (c) => c.name.toLowerCase().includes(q) || c.code.toLowerCase().includes(q)
    );
  });

  constructor() {
    this.catalogs.loadCountries();
  }

  writeValue(obj: any): void {
    this.value.set(obj ? String(obj).toUpperCase() : '');
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

  protected selectCountry(c: Country): void {
    this.value.set(c.code);
    this.onChange(c.code);
    this.onTouched();
    this.isOpen.set(false);
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

  protected getFlagEmoji(countryCode: string): string {
    if (!countryCode || countryCode.length !== 2) return '🌐';
    const codePoints = countryCode
      .toUpperCase()
      .split('')
      .map((char) => 127397 + char.charCodeAt(0));
    return String.fromCodePoint(...codePoints);
  }
}
