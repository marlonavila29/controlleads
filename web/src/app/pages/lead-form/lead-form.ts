import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CatalogService } from '../../core/api/catalog.service';
import { Duplicate, LeadsService } from '../../core/api/leads.service';
import { Topbar } from '../../shared/topbar';

@Component({
  selector: 'app-lead-form',
  imports: [ReactiveFormsModule, RouterLink, Topbar],
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
    utmSource: [''],
    utmMedium: [''],
    utmCampaign: ['']
  });

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

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const value = this.form.getRawValue();
    this.leadsService
      .create({ ...value, countryCode: value.countryCode.toUpperCase() })
      .subscribe({
        next: (lead) => this.router.navigate(['/leads', lead.id]),
        error: (err) => {
          this.submitting.set(false);
          this.error.set(err.error?.title ?? 'Could not create the lead.');
        }
      });
  }
}
