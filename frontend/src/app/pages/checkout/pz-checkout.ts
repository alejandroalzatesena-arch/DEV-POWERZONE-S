import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/api/api-error';
import {
  PAYMENT_STATUS_LABELS,
  PAYMENT_STATUS_TONES,
  Payment,
  formatDateTime,
  formatMoney,
} from '../../core/billing/billing.models';
import { BillingService } from '../../core/billing/billing.service';
import { ToastService } from '../../core/ui/toast.service';
import { saveBlob } from '../../core/ui/download';
import { PzBadge } from '../../shared/ui/badge/pz-badge';
import { PzButton } from '../../shared/ui/button/pz-button';
import { PzCard } from '../../shared/ui/card/pz-card';

const FAILURE_CODES = [
  { value: 'CARD_DECLINED', label: 'Tarjeta rechazada por el banco' },
  { value: 'INSUFFICIENT_FUNDS', label: 'Fondos insuficientes' },
  { value: 'EXPIRED_CARD', label: 'Tarjeta vencida' },
  { value: 'TIMEOUT', label: 'La pasarela no respondio' },
];

/**
 * Confirmacion del pago (PPP-310). Con la pasarela simulada permite aprobar o
 * rechazar el cobro; con una pasarela real redirige al checkout alojado.
 */
@Component({
  selector: 'pz-checkout',
  imports: [RouterLink, PzCard, PzButton, PzBadge],
  templateUrl: './pz-checkout.html',
  styleUrl: './pz-checkout.scss',
})
export class PzCheckout implements OnInit {
  /** Parametro de ruta enlazado con `withComponentInputBinding`. */
  readonly paymentId = input.required<string>();
  /** Parametro de consulta `sandbox` que expone el checkout simulado. */
  readonly sandbox = input<string | boolean>('');

  private readonly billing = inject(BillingService);
  private readonly toast = inject(ToastService);

  protected readonly payment = signal<Payment | null>(null);
  protected readonly loading = signal(true);
  protected readonly working = signal(false);
  protected readonly downloading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly notice = signal('');
  protected readonly failureCode = signal(FAILURE_CODES[0].value);

  protected readonly failureCodes = FAILURE_CODES;
  protected readonly statusLabels = PAYMENT_STATUS_LABELS;
  protected readonly statusTones = PAYMENT_STATUS_TONES;
  protected readonly money = formatMoney;
  protected readonly dateTime = formatDateTime;

  protected readonly isSandbox = computed(() => {
    const value = this.sandbox();
    return value === true || value === '1' || value === 'true';
  });

  protected readonly isPending = computed(() => this.payment()?.status === 'PENDING');
  protected readonly isCompleted = computed(() => this.payment()?.status === 'COMPLETED');
  protected readonly isFailed = computed(() => {
    const status = this.payment()?.status;
    return status === 'FAILED' || status === 'EXPIRED';
  });

  protected readonly stepLabel = computed(() => {
    if (this.isCompleted()) {
      return 'Pago confirmado y membresia activada';
    }
    if (this.isFailed()) {
      return 'El pago no se completo';
    }
    return 'Confirmacion pendiente en la pasarela';
  });

  ngOnInit(): void {
    this.load();
  }

  protected pay(): void {
    const payment = this.payment();
    if (!payment || this.working()) {
      return;
    }
    this.working.set(true);
    this.errorMessage.set('');
    this.notice.set('');
    this.billing
      .sandboxPay(payment.id)
      .pipe(finalize(() => this.working.set(false)))
      .subscribe({
        next: (updated) => {
          this.payment.set(updated);
          this.notice.set('Pago aprobado. Tu membresia quedo activa automaticamente.');
          this.toast.success('Pago aprobado y membresia actualizada.');
        },
        error: (error: unknown) => this.errorMessage.set(ApiError.from(error).message),
      });
  }

  protected reject(): void {
    const payment = this.payment();
    if (!payment || this.working()) {
      return;
    }
    this.working.set(true);
    this.errorMessage.set('');
    this.notice.set('');
    this.billing
      .sandboxFail(payment.id, this.failureCode())
      .pipe(finalize(() => this.working.set(false)))
      .subscribe({
        next: (updated) => {
          this.payment.set(updated);
          this.toast.error(updated.failureMessage ?? 'El pago fue rechazado.');
        },
        error: (error: unknown) => this.errorMessage.set(ApiError.from(error).message),
      });
  }

  protected refresh(): void {
    this.load();
  }

  protected onFailureCode(event: Event): void {
    this.failureCode.set((event.target as HTMLSelectElement).value);
  }

  protected downloadReceipt(): void {
    const payment = this.payment();
    if (!payment || this.downloading()) {
      return;
    }
    this.downloading.set(true);
    this.errorMessage.set('');
    this.billing
      .receipt(payment.id)
      .pipe(finalize(() => this.downloading.set(false)))
      .subscribe({
        next: (file) => saveBlob(file.blob, file.filename ?? `comprobante-${payment.id}.pdf`),
        error: (error: unknown) => this.errorMessage.set(ApiError.from(error).message),
      });
  }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.billing
      .payment(Number(this.paymentId()))
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (payment) => this.payment.set(payment),
        error: (error: unknown) => this.errorMessage.set(ApiError.from(error).message),
      });
  }
}
