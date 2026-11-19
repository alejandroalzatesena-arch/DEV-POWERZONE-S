import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ApiError } from './api-error';

export type QueryParams = Record<string, string | number | boolean | null | undefined>;

export interface DownloadedFile {
  blob: Blob;
  filename: string | null;
}

/**
 * Cliente HTTP de la aplicacion. Centraliza el prefijo `/api/v1`, la
 * serializacion de parametros y la traduccion de errores a `ApiError`.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1';

  get<T>(path: string, params?: QueryParams): Observable<T> {
    return this.http
      .get<T>(this.url(path), { params: toHttpParams(params) })
      .pipe(catchError((error: unknown) => throwError(() => ApiError.from(error))));
  }

  post<T>(path: string, body: unknown, params?: QueryParams): Observable<T> {
    return this.http
      .post<T>(this.url(path), body, { params: toHttpParams(params) })
      .pipe(catchError((error: unknown) => throwError(() => ApiError.from(error))));
  }

  put<T>(path: string, body: unknown): Observable<T> {
    return this.http
      .put<T>(this.url(path), body)
      .pipe(catchError((error: unknown) => throwError(() => ApiError.from(error))));
  }

  patch<T>(path: string, body: unknown): Observable<T> {
    return this.http
      .patch<T>(this.url(path), body)
      .pipe(catchError((error: unknown) => throwError(() => ApiError.from(error))));
  }

  delete<T>(path: string): Observable<T> {
    return this.http
      .delete<T>(this.url(path))
      .pipe(catchError((error: unknown) => throwError(() => ApiError.from(error))));
  }

  /**
   * Descarga un archivo binario y conserva el nombre sugerido por el servidor
   * en `Content-Disposition`.
   */
  download(path: string): Observable<DownloadedFile> {
    return this.http
      .get(this.url(path), { observe: 'response', responseType: 'blob' })
      .pipe(
        map((response) => ({
          blob: response.body ?? new Blob(),
          filename: filenameFrom(response.headers.get('Content-Disposition')),
        })),
        catchError((error: unknown) => throwError(() => ApiError.from(error))),
      );
  }

  private url(path: string): string {
    return `${this.baseUrl}/${path.replace(/^\/+/, '')}`;
  }
}

function filenameFrom(header: string | null): string | null {
  if (!header) {
    return null;
  }
  const encoded = /filename\*=(?:UTF-8'')?([^;]+)/i.exec(header);
  const plain = /filename="?([^";]+)"?/i.exec(header);
  const value = encoded?.[1] ?? plain?.[1];
  return value ? decodeURIComponent(value.trim().replace(/^"|"$/g, '')) : null;
}

function toHttpParams(params?: QueryParams): HttpParams {
  let result = new HttpParams();
  if (!params) {
    return result;
  }

  for (const [key, value] of Object.entries(params)) {
    if (value !== null && value !== undefined) {
      result = result.set(key, String(value));
    }
  }
  return result;
}
