import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, tap, throwError } from 'rxjs';
import { ApiService } from '../api/api.service';
import {
  AuthResponse,
  LoginPayload,
  RegisterPayload,
  UpdateProfilePayload,
  User,
} from './auth.models';
import { TokenService } from './token.service';

export type SessionStatus = 'unknown' | 'anonymous' | 'authenticated';

/**
 * Estado de sesion de la aplicacion. El access token vive en memoria y se
 * renueva con la cookie HttpOnly `pz_refresh`.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly tokens = inject(TokenService);

  private readonly currentUser = signal<User | null>(null);
  private readonly statusSignal = signal<SessionStatus>('unknown');
  private readonly busy = signal(false);
  private refreshAttempted = false;

  readonly user = this.currentUser.asReadonly();
  readonly status = this.statusSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.statusSignal() === 'authenticated');
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');
  readonly displayName = computed(() => {
    const user = this.currentUser();
    return user ? `${user.firstName} ${user.lastName}`.trim() : '';
  });

  login(payload: LoginPayload): Observable<User> {
    this.busy.set(true);
    return this.api.post<AuthResponse>('auth/login', payload).pipe(
      tap((response) => this.applySession(response)),
      map((response) => response.user),
      finalize(() => this.busy.set(false)),
    );
  }

  register(payload: RegisterPayload): Observable<User> {
    this.busy.set(true);
    return this.api.post<User>('auth/register', payload).pipe(
      finalize(() => this.busy.set(false)),
    );
  }

  /** Renueva el access token usando la cookie de refresh. */
  refresh(): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('auth/refresh', {}).pipe(
      tap((response) => this.applySession(response)),
    );
  }

  forgotPassword(email: string): Observable<void> {
    return this.api.post<void>('auth/forgot-password', { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.api.post<void>('auth/reset-password', { token, newPassword });
  }

  /** Guarda cambios parciales del perfil y refresca el usuario en memoria. */
  updateProfile(payload: UpdateProfilePayload): Observable<User> {
    return this.api.put<User>('users/me', payload).pipe(
      tap((user) => this.currentUser.set(user)),
    );
  }

  logout(): void {
    this.api.post<void>('auth/logout', {})
      .pipe(catchError(() => []))
      .subscribe();
    this.clearSession();
  }

  clearSession(): void {
    this.tokens.clear();
    this.currentUser.set(null);
    this.statusSignal.set('anonymous');
  }

  /**
   * Resuelve el estado de sesion una sola vez por carga de pagina. Devuelve
   * `true` si hay sesion activa.
   */
  ensureSession(): Observable<boolean> {
    if (this.statusSignal() === 'authenticated') {
      return of(true);
    }
    if (this.refreshAttempted) {
      return of(false);
    }

    this.refreshAttempted = true;
    return this.refresh().pipe(
      map(() => true),
      catchError(() => {
        this.statusSignal.set('anonymous');
        return of(false);
      }),
    );
  }

  updateUser(user: User): void {
    this.currentUser.set(user);
  }

  /** Recupera el usuario actual cuando ya existe un access token valido. */
  loadProfile(): Observable<User> {
    return this.api.get<User>('users/me').pipe(
      tap((user) => {
        this.currentUser.set(user);
        this.statusSignal.set('authenticated');
      }),
      catchError((error: unknown) => {
        this.clearSession();
        return throwError(() => error);
      }),
    );
  }

  private applySession(response: AuthResponse): void {
    this.tokens.set(response.accessToken);
    this.currentUser.set(response.user);
    this.statusSignal.set('authenticated');
    this.refreshAttempted = true;
  }
}
