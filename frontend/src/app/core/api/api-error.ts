import { HttpErrorResponse } from '@angular/common/http';

/** Cuerpo de error RFC 7807 emitido por el backend. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  path?: string;
  errors?: Record<string, string>;
}

/**
 * Error normalizado de la API. Todos los servicios lanzan `ApiError`, nunca
 * un `HttpErrorResponse` crudo, para que la UI tenga un contrato estable.
 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message);
    this.name = 'ApiError';
  }

  get isNetworkError(): boolean {
    return this.status === 0;
  }

  static from(error: unknown): ApiError {
    if (error instanceof ApiError) {
      return error;
    }

    if (error instanceof HttpErrorResponse) {
      const problem = error.error as ProblemDetail | null;
      return new ApiError(
        error.status,
        problem?.code ?? `HTTP_${error.status}`,
        problem?.detail ?? defaultMessage(error.status),
        problem?.errors ?? {},
      );
    }

    return new ApiError(0, 'UNKNOWN', 'Ocurrio un error inesperado.');
  }
}

function defaultMessage(status: number): string {
  switch (status) {
    case 0:
      return 'No se pudo conectar con el servidor.';
    case 400:
      return 'La solicitud contiene datos invalidos.';
    case 401:
      return 'Tu sesion expiro. Inicia sesion de nuevo.';
    case 403:
      return 'No tienes permisos para esta operacion.';
    case 404:
      return 'El recurso solicitado no existe.';
    case 409:
      return 'La operacion entra en conflicto con el estado actual.';
    case 429:
      return 'Demasiadas solicitudes. Intenta de nuevo en unos segundos.';
    default:
      return 'Ocurrio un error inesperado.';
  }
}
