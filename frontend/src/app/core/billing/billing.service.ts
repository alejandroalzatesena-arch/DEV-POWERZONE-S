import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../api/api.service';
import {
  BillingPlan,
  CheckoutPayload,
  CheckoutResult,
  MembershipStatus,
  PageResult,
  Payment,
  PaymentMethodOption,
  PaymentQuery,
} from './billing.models';

/**
 * Acceso a la API de pagos y membresias del usuario autenticado. La descarga
 * del comprobante se resuelve como blob para no exponer URLs firmadas.
 */
@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly api = inject(ApiService);

  plans(): Observable<BillingPlan[]> {
    return this.api.get<BillingPlan[]>('billing/plans');
  }

  paymentMethods(): Observable<PaymentMethodOption[]> {
    return this.api.get<PaymentMethodOption[]>('billing/payment-methods');
  }

  checkout(payload: CheckoutPayload): Observable<CheckoutResult> {
    return this.api.post<CheckoutResult>('billing/checkout', payload);
  }

  history(query: PaymentQuery = {}): Observable<PageResult<Payment>> {
    return this.api.get<PageResult<Payment>>('billing/payments', {
      from: query.from || undefined,
      to: query.to || undefined,
      status: query.status || undefined,
      page: query.page ?? 0,
      size: query.size ?? 10,
    });
  }

  payment(paymentId: number): Observable<Payment> {
    return this.api.get<Payment>(`billing/payments/${paymentId}`);
  }

  membership(): Observable<MembershipStatus> {
    return this.api.get<MembershipStatus>('billing/membership');
  }

  sandboxPay(paymentId: number): Observable<Payment> {
    return this.api.post<Payment>(`billing/payments/${paymentId}/sandbox/pay`, {});
  }

  sandboxFail(paymentId: number, failureCode?: string): Observable<Payment> {
    return this.api.post<Payment>(`billing/payments/${paymentId}/sandbox/fail`, {
      failureCode: failureCode || null,
    });
  }

  receipt(paymentId: number): Observable<{ blob: Blob; filename: string | null }> {
    return this.api.download(`billing/payments/${paymentId}/receipt`);
  }
}
