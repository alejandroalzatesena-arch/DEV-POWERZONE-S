import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, firstValueFrom, of } from 'rxjs';
import { describe, expect, it } from 'vitest';
import { AuthService } from './auth.service';
import { authGuard, guestGuard } from './auth.guard';

function configure(auth: Record<string, unknown>): void {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
  });
}

function run(guard: typeof authGuard): unknown {
  return TestBed.runInInjectionContext(() => guard({} as never, {} as never));
}

describe('authGuard', () => {
  it('permite el acceso cuando hay sesion', () => {
    configure({ isAuthenticated: () => true });

    expect(run(authGuard)).toBe(true);
  });

  it('redirige al login cuando no hay sesion', async () => {
    configure({ isAuthenticated: () => false, ensureSession: () => of(false) });

    const value = await firstValueFrom(run(authGuard) as Observable<unknown>);

    expect(String(value)).toContain('/auth/login');
  });
});

describe('guestGuard', () => {
  it('desvia al panel a un usuario autenticado', () => {
    configure({ isAuthenticated: () => true });

    expect(String(run(guestGuard))).toContain('/app/dashboard');
  });

  it('permite ver el login a un visitante', async () => {
    configure({ isAuthenticated: () => false, ensureSession: () => of(false) });

    const value = await firstValueFrom(run(guestGuard) as Observable<unknown>);

    expect(value).toBe(true);
  });
});
