import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { BillingPlan, CheckoutResult, MembershipStatus, Payment } from './billing.models';
import { BillingService } from './billing.service';

const plan: BillingPlan = {
  id: 3,
  code: 'MONTHLY',
  name: 'Mensual',
  description: 'Acceso mensual',
  priceCents: 8_990_000,
  currency: 'COP',
  durationDays: 30,
};

const payment: Payment = {
  id: 42,
  planId: 3,
  planName: 'Mensual',
  amountCents: 8_990_000,
  currency: 'COP',
  methodCode: 'CARD',
  methodLabel: 'Tarjeta',
  status: 'PENDING',
  reference: 'pz_abc123',
  checkoutUrl: '/app/checkout/42?sandbox=1',
  failureCode: null,
  failureMessage: null,
  attempt: 1,
  expiresAt: '2026-11-20T15:30:00Z',
  completedAt: null,
  createdAt: '2026-11-20T15:00:00Z',
};

describe('BillingService', () => {
  let service: BillingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BillingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista los planes del gimnasio', () => {
    let result: BillingPlan[] | undefined;
    service.plans().subscribe((plans) => (result = plans));

    const request = http.expectOne('/api/v1/billing/plans');
    expect(request.request.method).toBe('GET');
    request.flush([plan]);

    expect(result?.[0].name).toBe('Mensual');
  });

  it('lista los metodos de pago habilitados', () => {
    service.paymentMethods().subscribe();

    const request = http.expectOne('/api/v1/billing/payment-methods');
    expect(request.request.method).toBe('GET');
    request.flush([{ code: 'CARD', label: 'Tarjeta', kind: 'CARD', immediate: true }]);
  });

  it('abre el checkout con el plan y el metodo', () => {
    let result: CheckoutResult | undefined;
    service.checkout({ planId: 3, methodCode: 'CARD' }).subscribe((r) => (result = r));

    const request = http.expectOne('/api/v1/billing/checkout');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ planId: 3, methodCode: 'CARD' });
    request.flush({
      paymentId: 42,
      reference: 'pz_abc123',
      status: 'PENDING',
      checkoutUrl: '/app/checkout/42?sandbox=1',
      amountCents: 8_990_000,
      currency: 'COP',
      planName: 'Mensual',
      expiresAt: '2026-11-20T15:30:00Z',
    });

    expect(result?.paymentId).toBe(42);
  });

  it('consulta el historial con paginacion por defecto', () => {
    service.history().subscribe();

    const request = http.expectOne('/api/v1/billing/payments?page=0&size=10');
    expect(request.request.method).toBe('GET');
    request.flush({ items: [payment], page: 0, size: 10, total: 1, totalPages: 1 });
  });

  it('serializa los filtros del historial', () => {
    service
      .history({
        from: '2026-11-01',
        to: '2026-11-30',
        status: 'COMPLETED',
        page: 2,
        size: 5,
      })
      .subscribe();

    const request = http.expectOne(
      '/api/v1/billing/payments?from=2026-11-01&to=2026-11-30&status=COMPLETED&page=2&size=5',
    );
    request.flush({ items: [], page: 2, size: 5, total: 0, totalPages: 0 });
  });

  it('omite los filtros vacios', () => {
    service.history({ from: '', to: undefined, status: '', page: 0, size: 10 }).subscribe();

    const request = http.expectOne('/api/v1/billing/payments?page=0&size=10');
    request.flush({ items: [], page: 0, size: 10, total: 0, totalPages: 0 });
  });

  it('consulta el detalle de un pago', () => {
    service.payment(42).subscribe();

    const request = http.expectOne('/api/v1/billing/payments/42');
    expect(request.request.method).toBe('GET');
    request.flush(payment);
  });

  it('consulta el estado de la membresia', () => {
    const membership: MembershipStatus = {
      state: 'ACTIVE',
      planName: 'Mensual',
      startsAt: '2026-11-01',
      endsAt: '2026-12-01',
      daysRemaining: 11,
      expiringThresholdDays: 7,
    };
    let result: MembershipStatus | undefined;
    service.membership().subscribe((status) => (result = status));

    const request = http.expectOne('/api/v1/billing/membership');
    request.flush(membership);

    expect(result?.state).toBe('ACTIVE');
  });

  it('aprueba y rechaza pagos en el sandbox', () => {
    service.sandboxPay(42).subscribe();
    const approve = http.expectOne('/api/v1/billing/payments/42/sandbox/pay');
    expect(approve.request.method).toBe('POST');
    approve.flush({ ...payment, status: 'COMPLETED' });

    service.sandboxFail(42, 'CARD_DECLINED').subscribe();
    const reject = http.expectOne('/api/v1/billing/payments/42/sandbox/fail');
    expect(reject.request.method).toBe('POST');
    expect(reject.request.body).toEqual({ failureCode: 'CARD_DECLINED' });
    reject.flush({ ...payment, status: 'FAILED' });
  });

  it('descarga el comprobante como blob con su nombre', () => {
    const blob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
    let filename: string | null = null;

    service.receipt(42).subscribe((file) => (filename = file.filename));

    const request = http.expectOne('/api/v1/billing/payments/42/receipt');
    expect(request.request.responseType).toBe('blob');
    request.flush(blob, {
      headers: { 'Content-Disposition': 'attachment; filename="comprobante-42.pdf"' },
    });

    expect(filename).toBe('comprobante-42.pdf');
  });
});
