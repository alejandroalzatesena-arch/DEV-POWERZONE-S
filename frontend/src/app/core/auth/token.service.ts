import { Injectable, computed, signal } from '@angular/core';

/**
 * Access token en memoria. El refresh token vive en una cookie HttpOnly y se
 * conecta en el Sprint 1; nunca se persiste el access token en localStorage.
 */
@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly token = signal<string | null>(null);

  readonly accessToken = this.token.asReadonly();
  readonly isAuthenticated = computed(() => this.token() !== null);

  set(token: string): void {
    this.token.set(token);
  }

  clear(): void {
    this.token.set(null);
  }
}
