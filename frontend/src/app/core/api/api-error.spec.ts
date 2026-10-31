import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { ApiError } from './api-error';

describe('ApiError', () => {
  it('traduce un ProblemDetail del backend', () => {
    const response = new HttpErrorResponse({
      status: 409,
      error: {
        code: 'EMAIL_TAKEN',
        detail: 'Ya existe una cuenta registrada con ese email.',
      },
    });

    const error = ApiError.from(response);

    expect(error.status).toBe(409);
    expect(error.code).toBe('EMAIL_TAKEN');
    expect(error.message).toContain('Ya existe');
  });

  it('expone los errores por campo de la validacion', () => {
    const response = new HttpErrorResponse({
      status: 400,
      error: {
        code: 'VALIDATION_ERROR',
        detail: 'La solicitud contiene datos invalidos',
        errors: { email: 'El email no tiene un formato valido' },
      },
    });

    const error = ApiError.from(response);

    expect(error.fieldErrors['email']).toContain('formato valido');
  });

  it('marca los fallos de red con estado 0', () => {
    const error = ApiError.from(new HttpErrorResponse({ status: 0 }));

    expect(error.isNetworkError).toBe(true);
    expect(error.code).toBe('HTTP_0');
  });
});
