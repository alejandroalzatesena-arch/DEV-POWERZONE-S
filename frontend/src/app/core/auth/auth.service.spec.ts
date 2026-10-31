import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AuthResponse, User } from './auth.models';
import { AuthService } from './auth.service';
import { TokenService } from './token.service';

const member: User = {
  id: 5,
  gymId: 1,
  email: 'ana@powerzone.app',
  firstName: 'Ana',
  lastName: 'Gomez',
  role: 'MEMBER',
  active: true,
};

describe('AuthService', () => {
  let service: AuthService;
  let tokens: TokenService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    tokens = TestBed.inject(TokenService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('guarda el access token y el usuario al iniciar sesion', () => {
    const response: AuthResponse = { accessToken: 'jwt-access', expiresIn: 900, user: member };
    let result: User | undefined;

    service
      .login({ email: member.email, password: 'Secreta123', rememberMe: false })
      .subscribe((user) => (result = user));

    const request = http.expectOne('/api/v1/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.rememberMe).toBe(false);
    request.flush(response);

    expect(result?.email).toBe(member.email);
    expect(tokens.accessToken()).toBe('jwt-access');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('marca la sesion como anonima cuando el refresh falla', () => {
    let authenticated = true;

    service.ensureSession().subscribe((value) => (authenticated = value));

    const request = http.expectOne('/api/v1/auth/refresh');
    request.flush(
      { code: 'MISSING_SESSION', detail: 'Sin sesion' },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(authenticated).toBe(false);
    expect(service.isAuthenticated()).toBe(false);
  });

  it('recupera la sesion y el usuario con la cookie de refresh', () => {
    const response: AuthResponse = { accessToken: 'renewed', expiresIn: 900, user: member };
    let authenticated = false;

    service.ensureSession().subscribe((value) => (authenticated = value));

    http.expectOne('/api/v1/auth/refresh').flush(response);

    expect(authenticated).toBe(true);
    expect(tokens.accessToken()).toBe('renewed');
    expect(service.user()?.firstName).toBe('Ana');
  });

  it('limpia el estado al cerrar sesion', () => {
    service.clearSession();

    expect(tokens.accessToken()).toBeNull();
    expect(service.user()).toBeNull();
    expect(service.status()).toBe('anonymous');
  });
});
