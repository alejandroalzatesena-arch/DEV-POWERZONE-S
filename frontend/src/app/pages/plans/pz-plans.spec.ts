import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { BillingPlan, PaymentMethodOption } from '../../core/billing/billing.models';
import { PzPlans } from './pz-plans';

const plans: BillingPlan[] = [
  {
    id: 3,
    code: 'MONTHLY',
    name: 'Mensual',
    description: 'Acceso por 30 dias',
    priceCents: 8_990_000,
    currency: 'COP',
    durationDays: 30,
  },
  {
    id: 4,
    code: 'QUARTERLY',
    name: 'Trimestral',
    description: 'Acceso por 90 dias',
    priceCents: 23_990_000,
    currency: 'COP',
    durationDays: 90,
  },
];

const methods: PaymentMethodOption[] = [
  { code: 'CARD', label: 'Tarjeta', kind: 'CARD', immediate: true },
  { code: 'PSE', label: 'PSE', kind: 'TRANSFER', immediate: false },
];

describe('PzPlans', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PzPlans],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  function load(componentFixture: ComponentFixture<PzPlans>) {
    http.expectOne('/api/v1/billing/plans').flush(plans);
    http.expectOne('/api/v1/billing/payment-methods').flush(methods);
    componentFixture.detectChanges();
  }

  function create() {
    const fixture = TestBed.createComponent(PzPlans);
    fixture.detectChanges();
    load(fixture);
    return fixture;
  }

  it('carga planes y metodos y preselecciona el primero', () => {
    const fixture = create();
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Mensual');
    expect(text).toContain('Trimestral');
    expect(text).toContain('Tarjeta');
    expect(text).toContain('89.900');
    expect(text).toContain('Total a pagar');

    const selected = fixture.nativeElement.querySelectorAll('.pz-plan-option.is-selected');
    expect(selected).toHaveLength(1);
  });

  it('cambia el resumen al elegir otro plan y otro metodo', () => {
    const fixture = create();

    const planButtons = fixture.nativeElement.querySelectorAll(
      '.pz-plan-option',
    ) as NodeListOf<HTMLButtonElement>;
    planButtons[1].click();
    fixture.detectChanges();

    const radios = fixture.nativeElement.querySelectorAll(
      '.pz-method-option input',
    ) as NodeListOf<HTMLInputElement>;
    radios[1].click();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('239.900');
    expect(text).toContain('PSE');
  });

  it('abre el checkout alojado y navega al detalle del pago', () => {
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = create();

    const button = fixture.nativeElement.querySelector(
      '.pz-plans__actions button',
    ) as HTMLButtonElement;
    button.click();

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

    expect(navigate).toHaveBeenCalledWith('/app/checkout/42?sandbox=1');
  });

  it('navega al detalle aunque la pasarela no devuelva URL', () => {
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = create();

    (fixture.nativeElement.querySelector('.pz-plans__actions button') as HTMLButtonElement).click();

    http.expectOne('/api/v1/billing/checkout').flush({
      paymentId: 42,
      reference: 'pz_abc123',
      status: 'PENDING',
      checkoutUrl: null,
      amountCents: 8_990_000,
      currency: 'COP',
      planName: 'Mensual',
      expiresAt: '2026-11-20T15:30:00Z',
    });

    expect(navigate).toHaveBeenCalledWith(['/app/checkout', 42]);
  });

  it('muestra un mensaje cuando no hay planes publicados', () => {
    const fixture = TestBed.createComponent(PzPlans);
    fixture.detectChanges();

    http.expectOne('/api/v1/billing/plans').flush([]);
    http.expectOne('/api/v1/billing/payment-methods').flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Sin planes disponibles');
  });

  it('muestra el error de carga de planes', () => {
    const fixture = TestBed.createComponent(PzPlans);
    fixture.detectChanges();

    http.expectOne('/api/v1/billing/payment-methods').flush(methods);
    http.expectOne('/api/v1/billing/plans').flush(
      { detail: 'No se pudieron cargar los planes.' },
      { status: 500, statusText: 'Server Error' },
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No se pudieron cargar los planes.');
  });

  it('muestra el error del checkout sin perder la seleccion', () => {
    const fixture = create();

    (fixture.nativeElement.querySelector('.pz-plans__actions button') as HTMLButtonElement).click();

    http.expectOne('/api/v1/billing/checkout').flush(
      { detail: 'Alcanzaste el limite de intentos.' },
      { status: 429, statusText: 'Too Many Requests' },
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Alcanzaste el limite de intentos.');
    expect(fixture.nativeElement.textContent).toContain('Mensual');
  });
});
