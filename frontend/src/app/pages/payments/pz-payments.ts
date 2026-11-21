import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/api/api-error';
import {
  PAYMENT_STATUS_LABELS,
  PAYMENT_STATUS_TONES,
  PageResult,
  Payment,
  PaymentStatus,
  formatDateTime,
  formatMoney,
} from '../../core/billing/billing.models';
import { BillingService } from '../../core/billing/billing.service';
import { ToastService } from '../../core/ui/toast.service';
import { saveBlob } from '../../core/ui/download';
import { PzBadge } from '../../shared/ui/badge/pz-badge';
import { PzButton } from '../../shared/ui/button/pz-button';
import { PzCard } from '../../shared/ui/card/pz-card';
import { PzEmptyState } from '../../shared/ui/empty-state/pz-empty-state';
import { PzMembershipCard } from '../../shared/ui/membership-card/pz-membership-card';

/** Historial de pagos con filtros, paginacion y descarga de comprobante (PPP-315/317). */
@Component({
  selector: 'pz-payments',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    PzCard,
    PzBadge,
    PzButton,
    PzEmptyState,
    PzMembershipCard,
  ],
  templateUrl: './pz-payments.html',
  styleUrl: './pz-payments.scss',
})
export class PzPayments implements OnInit {
  private readonly billing = inject(BillingService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly pageData = signal<PageResult<Payment> | null>(null);
  protected readonly pageIndex = signal(0);
  protected readonly loading = signal(true);
  protected readonly downloading = signal<number | null>(null);
  protected readonly errorMessage = signal('');

  protected readonly statusLabels = PAYMENT_STATUS_LABELS;
  protected readonly statusTones = PAYMENT_STATUS_TONES;
  protected readonly statusOptions = Object.keys(PAYMENT_STATUS_LABELS) as PaymentStatus[];
  protected readonly money = formatMoney;
  protected readonly dateTime = formatDateTime;

  protected readonly form = this.fb.group({
    from: [''],
    to: [''],
    status: [''],
  });

  protected readonly payments = computed(() => this.pageData()?.items ?? []);
  protected readonly total = computed(() => this.pageData()?.total ?? 0);
  protected readonly totalPages = computed(() => this.pageData()?.totalPages ?? 0);
  protected readonly hasPrevious = computed(() => this.pageIndex() > 0);
  protected readonly hasNext = computed(() => this.pageIndex() + 1 < this.totalPages());

  ngOnInit(): void {
    this.load();
  }

  protected applyFilter(): void {
    this.pageIndex.set(0);
    this.load();
  }

  protected clearFilter(): void {
    this.form.reset({ from: '', to: '', status: '' });
    this.pageIndex.set(0);
    this.load();
  }

  protected previousPage(): void {
    if (!this.hasPrevious()) {
      return;
    }
    this.pageIndex.update((page) => page - 1);
    this.load();
  }

  protected nextPage(): void {
    if (!this.hasNext()) {
      return;
    }
    this.pageIndex.update((page) => page + 1);
    this.load();
  }

  protected downloadReceipt(payment: Payment): void {
    if (this.downloading() !== null) {
      return;
    }
    this.downloading.set(payment.id);
    this.errorMessage.set('');
    this.billing
      .receipt(payment.id)
      .pipe(finalize(() => this.downloading.set(null)))
      .subscribe({
        next: (file) => {
          saveBlob(file.blob, file.filename ?? `comprobante-${payment.id}.pdf`);
          this.toast.success('Comprobante generado.');
        },
        error: (error: unknown) => this.errorMessage.set(ApiError.from(error).message),
      });
  }

  private load(): void {
    const value = this.form.getRawValue();
    this.loading.set(true);
    this.errorMessage.set('');
    this.billing
      .history({
        from: value.from || undefined,
        to: value.to || undefined,
        status: (value.status as PaymentStatus | '') || undefined,
        page: this.pageIndex(),
        size: 10,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.pageData.set(page),
        error: (error: unknown) => {
          this.errorMessage.set(ApiError.from(error).message);
          this.pageData.set(null);
        },
      });
  }
}
