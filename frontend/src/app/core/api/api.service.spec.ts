import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ApiError } from './api-error';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('antepone el prefijo de la API y serializa parametros', () => {
    service.get('billing/payments', { page: 0, status: 'COMPLETED', skip: undefined }).subscribe();

    const request = http.expectOne('/api/v1/billing/payments?page=0&status=COMPLETED');
    expect(request.request.method).toBe('GET');
    request.flush({});
  });

  it('normaliza los errores HTTP a ApiError', () => {
    let error: ApiError | undefined;
    service.get('billing/membership').subscribe({ error: (e: ApiError) => (error = e) });

    http.expectOne('/api/v1/billing/membership').flush(
      { title: 'No autorizado', detail: 'Sesion expirada', code: 'UNAUTHORIZED' },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(error).toBeInstanceOf(ApiError);
    expect(error?.status).toBe(401);
    expect(error?.code).toBe('UNAUTHORIZED');
  });

  it('descarga un blob leyendo el nombre del Content-Disposition', () => {
    const blob = new Blob(['pdf'], { type: 'application/pdf' });
    let result: { blob: Blob; filename: string | null } | undefined;

    service.download('billing/payments/42/receipt').subscribe((file) => (result = file));

    const request = http.expectOne('/api/v1/billing/payments/42/receipt');
    expect(request.request.responseType).toBe('blob');
    request.flush(blob, {
      headers: {
        'Content-Disposition': "attachment; filename*=UTF-8''comprobante%2042.pdf",
      },
    });

    expect(result?.filename).toBe('comprobante 42.pdf');
    expect(result?.blob.size).toBe(blob.size);
  });

  it('devuelve un nombre nulo cuando no hay cabecera', () => {
    let result: { blob: Blob; filename: string | null } | undefined;
    service.download('billing/payments/42/receipt').subscribe((file) => (result = file));

    http
      .expectOne('/api/v1/billing/payments/42/receipt')
      .flush(new Blob(['pdf'], { type: 'application/pdf' }));

    expect(result?.filename).toBeNull();
  });
});
