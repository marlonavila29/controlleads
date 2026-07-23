import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CatalogService } from '../../core/api/catalog.service';
import { Duplicate, LeadsService } from '../../core/api/leads.service';
import { CountrySelect } from '../../shared/country-select';
import { CustomSelect } from '../../shared/custom-select';
import { Sidebar } from '../../shared/sidebar';

export const contactRequiredValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const email = control.get('email')?.value;
  const phone = control.get('phone')?.value;
  return (email && email.trim()) || (phone && phone.trim()) ? null : { contactMissing: true };
};

@Component({
  selector: 'app-lead-form',
  imports: [ReactiveFormsModule, RouterLink, Sidebar, CountrySelect, CustomSelect],
  templateUrl: './lead-form.html',
  styleUrl: './lead-form.scss'
})
export class LeadForm {
  protected readonly catalogs = inject(CatalogService);
  private readonly leadsService = inject(LeadsService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly duplicates = signal<Duplicate[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    fullName: ['', Validators.required],
    countryCode: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]],
    email: ['', Validators.email],
    phone: [''],
    courseId: ['', Validators.required],
    channelId: ['', Validators.required],
    otherChannelName: [''],
    utmSource: [''],
    utmMedium: [''],
    utmCampaign: ['']
  }, { validators: [contactRequiredValidator] });

  constructor() {
    this.catalogs.loadAll();
    // Duplicate check while typing contact data — warn, never block (RN-04).
    this.form.valueChanges.pipe(debounceTime(400), takeUntilDestroyed()).subscribe(({ email, phone }) => {
      if (!email && !phone) {
        this.duplicates.set([]);
        return;
      }
      this.leadsService.duplicates(email || null, phone || null).subscribe((d) => this.duplicates.set(d));
    });
  }

  protected isOtherChannel(): boolean {
    const channelId = this.form.getRawValue().channelId;
    if (!channelId) return false;
    const name = this.catalogs.nameOf('channels', channelId);
    return name.toLowerCase().includes('other') || name.toLowerCase().includes('outro');
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    if (this.isOtherChannel()) {
      const otherName = value.otherChannelName.trim();
      if (!otherName) {
        this.error.set('Please specify the channel name for "Other".');
        return;
      }
      if (!value.utmSource) {
        value.utmSource = otherName;
      } else {
        value.utmSource = `${otherName} (${value.utmSource})`;
      }
    }

    this.submitting.set(true);
    this.error.set(null);

    this.leadsService
      .create({
        fullName: value.fullName,
        countryCode: value.countryCode.toUpperCase(),
        email: value.email || undefined,
        phone: value.phone || undefined,
        courseId: value.courseId,
        channelId: value.channelId,
        utmSource: value.utmSource || undefined,
        utmMedium: value.utmMedium || undefined,
        utmCampaign: value.utmCampaign || undefined
      })
      .subscribe({
        next: (lead) => this.router.navigate(['/leads', lead.id]),
        error: (err) => {
          this.submitting.set(false);
          this.error.set(err.error?.title ?? 'Could not create the lead.');
        }
      });
  }
}
