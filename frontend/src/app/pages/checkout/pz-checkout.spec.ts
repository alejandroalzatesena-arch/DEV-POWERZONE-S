import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { Payment } from '../../core/billing/billing.models';
import { PzCheckout } from './pz-checkout';

const pending: Payment = {
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

const completed: Payment = {
  ...pending,
  status: 'COMPLETED',
  completedAt: '2026-11-20T15:05:00Z',
};

const failed: Payment = {
  ...pending,
  status: 'FAILED',
  failureCode: 'INSUFFICIENT_FUNDS',
  failureMessage: 'Fondos insuficientes. Prueba con otro metodo de pago.',
  completedAt: '2026-11-20T15:05:00Z',
};

describe('PzCheckout', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PzCheckout],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
    http.verify();
  });

  function create(payment: Payment, sandbox = '1'): ComponentFixture<PzCheckout> {
    const fixture = TestBed.createComponent(PzCheckout);
    fixture.componentRef.setInput('paymentId', String(payment.id));
    fixture.componentRef.setInput('sandbox', sandbox);
    fixture.detectChanges();
    http.expectOne(`/api/v1/billing/payments/${payment.id}`).flush(payment);
    fixture.detectChanges();
    return fixture;
  }

  it('muestra el resumen del pago pendiente y el panel sandbox', () => {
    const fixture = create(pending);
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Mensual');
    expect(text).toContain('89.900');
    expect(text).toContain('Tarjeta');
    expect(text).toContain('Pendiente');
    expect(text).toContain('Entorno de pruebas');
  });

  it('oculta el panel sandbox con una pasarela real', () => {
    const fixture = create(pending, '');
    const text = fixture.nativeElement.textContent as string;

    expect(text).not.toContain('Entorno de pruebas');
    expect(text).toContain('Esperando confirmacion');
    expect(text).toContain('Abrir pasarela');
  });

  it('simula un pago aprobado y habilita el comprobante', () => {
    const fixture = create(pending);

    const button = Array.from(
      fixture.nativeElement.querySelectorAll('.pz-checkout__sandbox button') as NodeListOf<HTMLButtonElement>,
    ).find((item) => item.textContent?.includes('Simular pago aprobado')) as HTMLButtonElement;
    button.click();

    const request = http.expectOne('/api/v1/billing/payments/42/sandbox/pay');
    expect(request.request.method).toBe('POST');
    request.flush(completed);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Pago confirmado y membresia activada');
    expect(text).toContain('Descargar comprobante');
    expect(text).not.toContain('Entorno de pruebas');
  });

  it('simula un rechazo con el motivo elegido', () => {
    const fixture = create(pending);

    const select = fixture.nativeElement.querySelector(
      '.pz-checkout__sandbox select',
    ) as HTMLSelectElement;
    select.value = 'INSUFFICIENT_FUNDS';
    select.dispatchEvent(new Event('change'));

    const button = Array.from(
      fixture.nativeElement.querySelectorAll('.pz-checkout__sandbox button') as NodeListOf<HTMLButtonElement>,
    ).find((item) => item.textContent?.includes('Simular rechazo')) as HTMLButtonElement;
    button.click();

    const request = http.expectOne('/api/v1/billing/payments/42/sandbox/fail');
    expect(request.request.body).toEqual({ failureCode: 'INSUFFICIENT_FUNDS' });
    request.flush(failed);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('El pago no se completo');
    expect(text).toContain('Fondos insuficientes');
    expect(text).toContain('Intentar con otro pago');
  });

  it('descarga el comprobante de un pago completado', () => {
    const fixture = create(completed);

    const button = Array.from(
      fixture.nativeElement.querySelectorAll('button[pz-button]') as NodeListOf<HTMLButtonElement>,
    ).find((item) => item.textContent?.includes('Descargar comprobante')) as HTMLButtonElement;
    button.click();

    const request = http.expectOne('/api/v1/billing/payments/42/receipt');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['%PDF-1.4'], { type: 'application/pdf' }), {
      headers: { 'Content-Disposition': 'attachment; filename="comprobante-42.pdf"' },
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Descargar comprobante');
  });

  it('muestra el error cuando el pago no existe', () => {
    const fixture = TestBed.createComponent(PzCheckout);
    fixture.componentRef.setInput('paymentId', '99');
    fixture.detectChanges();

    http.expectOne('/api/v1/billing/payments/99').flush(
      { detail: 'No se encontro el pago solicitado.' },
      { status: 404, statusText: 'Not Found' },
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No se encontro el pago solicitado.');
  });

  it('permite refrescar el estado del pago pendiente', () => {
    const fixture = create(pending, '');

    const button = Array.from(
      fixture.nativeElement.querySelectorAll('button[pz-button]') as NodeListOf<HTMLButtonElement>,
    ).find((item) => item.textContent?.includes('actualizar estado')) as HTMLButtonElement;
    button.click();

    http.expectOne('/api/v1/billing/payments/42').flush(completed);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Pago confirmado');
  });
});
