import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { MembershipStatus } from '../../../core/billing/billing.models';
import { PzMembershipCard } from './pz-membership-card';

const active: MembershipStatus = {
  state: 'ACTIVE',
  planName: 'Mensual',
  startsAt: '2026-11-01',
  endsAt: '2026-12-01',
  daysRemaining: 11,
  expiringThresholdDays: 7,
};

describe('PzMembershipCard', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PzMembershipCard],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function create() {
    const fixture = TestBed.createComponent(PzMembershipCard);
    fixture.detectChanges();
    return fixture;
  }

  it('muestra el plan, el estado y los dias restantes', () => {
    const fixture = create();
    http.expectOne('/api/v1/billing/membership').flush(active);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Mensual');
    expect(text).toContain('Activa');
    expect(text).toContain('Quedan 11 dias');
    expect(text).toContain('Renovar');
  });

  it('avisa cuando la membresia esta por vencer', () => {
    const fixture = create();
    http
      .expectOne('/api/v1/billing/membership')
      .flush({ ...active, state: 'EXPIRING', daysRemaining: 3 });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Por vencer');
    expect(text).toContain('esta por vencer');
    expect(text).toContain('Renovar ahora');
  });

  it('invita a comprar un plan cuando no hay membresia', () => {
    const fixture = create();
    http.expectOne('/api/v1/billing/membership').flush({
      state: 'NONE',
      planName: null,
      startsAt: null,
      endsAt: null,
      daysRemaining: 0,
      expiringThresholdDays: 7,
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Sin membresia');
    expect(text).toContain('Aun no tienes una membresia activa');
  });

  it('permite reintentar cuando falla la consulta', () => {
    const fixture = create();
    http.expectOne('/api/v1/billing/membership').flush(
      { code: 'INTERNAL_ERROR', detail: 'No se pudo consultar la membresia.' },
      { status: 500, statusText: 'Server Error' },
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No se pudo consultar la membresia.');

    const button = fixture.nativeElement.querySelector(
      'button[pz-button]',
    ) as HTMLButtonElement;
    button.click();
    fixture.detectChanges();

    http.expectOne('/api/v1/billing/membership').flush(active);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Quedan 11 dias');
  });

  it('oculta el acceso a renovar cuando se desactiva', () => {
    const fixture = TestBed.createComponent(PzMembershipCard);
    fixture.componentRef.setInput('showRenew', false);
    fixture.detectChanges();
    http.expectOne('/api/v1/billing/membership').flush(active);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Renovar');
  });
});
