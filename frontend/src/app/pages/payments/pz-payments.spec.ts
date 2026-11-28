import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MembershipStatus, PageResult, Payment } from '../../core/billing/billing.models';
import { ToastService } from '../../core/ui/toast.service';
import { PzPayments } from './pz-payments';

const membership: MembershipStatus = {
  state: 'ACTIVE',
  planName: 'Mensual',
  startsAt: '2026-11-01',
  endsAt: '2026-12-01',
  daysRemaining: 11,
  expiringThresholdDays: 7,
};

const paid: Payment = {
  id: 42,
  planId: 3,
  planName: 'Mensual',
  amountCents: 8_990_000,
  currency: 'COP',
  methodCode: 'CARD',
  methodLabel: 'Tarjeta',
  status: 'COMPLETED',
  reference: 'pz_abc123',
  checkoutUrl: '/app/checkout/42?sandbox=1',
  failureCode: null,
  failureMessage: null,
  attempt: 1,
  expiresAt: '2026-11-20T15:30:00Z',
  completedAt: '2026-11-20T15:05:00Z',
  createdAt: '2026-11-20T15:00:00Z',
};

const rejected: Payment = {
  ...paid,
  id: 43,
  planName: 'Trimestral',
  amountCents: 23_990_000,
  status: 'FAILED',
  reference: 'pz_def456',
  failureCode: 'CARD_DECLINED',
  failureMessage: 'La tarjeta fue rechazada por el banco.',
  completedAt: null,
};

function pageOf(items: Payment[], total = items.length): PageResult<Payment> {
  return {
    items,
    page: 0,
    size: 10,
    total,
    totalPages: Math.ceil(total / 10),
  };
}

describe('PzPayments', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PzPayments],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
    http.verify();
  });

  function create(data = pageOf([paid, rejected])): ComponentFixture<PzPayments> {
    const fixture = TestBed.createComponent(PzPayments);
    fixture.detectChanges();
    http.expectOne('/api/v1/billing/membership').flush(membership);
    http.expectOne('/api/v1/billing/payments?page=0&size=10').flush(data);
    fixture.detectChanges();
    return fixture;
  }

  it('muestra la membresia y el historial de pagos', () => {
    const fixture = create();
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Mensual');
    expect(text).toContain('Activa');
    expect(text).toContain('2 transaccion(es) registradas');
    expect(text).toContain('89.900');
    expect(text).toContain('239.900');
    expect(text).toContain('Pagado');
    expect(text).toContain('Rechazado');
    expect(text).toContain('La tarjeta fue rechazada por el banco.');
  });

  it('solo ofrece comprobante para pagos completados', () => {
    const fixture = create();
    const rows = fixture.nativeElement.querySelectorAll('tbody tr') as NodeListOf<HTMLTableRowElement>;

    expect(rows[0].textContent).toContain('Descargar PDF');
    expect(rows[1].textContent).not.toContain('Descargar PDF');
  });

  it('pagina al siguiente bloque de resultados', () => {
    const fixture = create(pageOf([paid], 12));

    const next = Array.from(
      fixture.nativeElement.querySelectorAll('.pz-payments__pager button') as NodeListOf<HTMLButtonElement>,
    ).find((item) => item.textContent?.includes('Siguiente')) as HTMLButtonElement;
    next.click();

    http
      .expectOne('/api/v1/billing/payments?page=1&size=10')
      .flush({ ...pageOf([rejected], 12), page: 1 });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Pagina 2 de 2');
  });

  it('aplica los filtros y vuelve a la primera pagina', () => {
    const fixture = create(pageOf([paid], 12));

    const component = fixture.componentInstance as unknown as {
      form: { patchValue: (value: Record<string, string>) => void };
      nextPage: () => void;
      applyFilter: () => void;
    };
    component.nextPage();
    http.expectOne('/api/v1/billing/payments?page=1&size=10').flush({
      ...pageOf([rejected], 12),
      page: 1,
    });

    component.form.patchValue({
      from: '2026-11-01',
      to: '2026-11-30',
      status: 'COMPLETED',
    });
    component.applyFilter();

    http
      .expectOne(
        '/api/v1/billing/payments?from=2026-11-01&to=2026-11-30&status=COMPLETED&page=0&size=10',
      )
      .flush(pageOf([paid]));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('1 transaccion(es) registradas');
  });

  it('limpia los filtros y recarga el historial', () => {
    const fixture = create();
    const component = fixture.componentInstance as unknown as {
      form: { patchValue: (value: Record<string, string>) => void };
      clearFilter: () => void;
    };
    component.form.patchValue({ status: 'FAILED' });

    component.clearFilter();

    http.expectOne('/api/v1/billing/payments?page=0&size=10').flush(pageOf([paid]));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Pagado');
  });

  it('muestra el estado vacio cuando no hay pagos', () => {
    const fixture = create(pageOf([]));

    expect(fixture.nativeElement.textContent).toContain('Aun no tienes pagos');
    expect(fixture.nativeElement.textContent).toContain('Ver planes');
  });

  it('descarga el comprobante y notifica al usuario', () => {
    const toast = TestBed.inject(ToastService);
    const success = vi.spyOn(toast, 'success');
    const fixture = create(pageOf([paid]));

    const button = Array.from(
      fixture.nativeElement.querySelectorAll('tbody button') as NodeListOf<HTMLButtonElement>,
    ).find((item) => item.textContent?.includes('Descargar PDF')) as HTMLButtonElement;
    button.click();

    const request = http.expectOne('/api/v1/billing/payments/42/receipt');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['%PDF-1.4'], { type: 'application/pdf' }), {
      headers: { 'Content-Disposition': 'attachment; filename="comprobante-42.pdf"' },
    });
    fixture.detectChanges();

    expect(success).toHaveBeenCalledWith('Comprobante generado.');
  });

  it('muestra el error del historial y permite reintentar con filtros', () => {
    const fixture = TestBed.createComponent(PzPayments);
    fixture.detectChanges();
    http.expectOne('/api/v1/billing/membership').flush(membership);
    http.expectOne('/api/v1/billing/payments?page=0&size=10').flush(
      { detail: 'No se pudo cargar el historial.' },
      { status: 500, statusText: 'Server Error' },
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No se pudo cargar el historial.');

    const component = fixture.componentInstance as unknown as { applyFilter: () => void };
    component.applyFilter();
    http.expectOne('/api/v1/billing/payments?page=0&size=10').flush(pageOf([paid]));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Pagado');
  });
});
