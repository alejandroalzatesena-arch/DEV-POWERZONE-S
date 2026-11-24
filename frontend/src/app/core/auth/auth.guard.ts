import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';
import { Role } from './auth.models';

/**
 * Protege las rutas privadas. Si el access token no esta en memoria intenta
 * recuperar la sesion con la cookie de refresh antes de redirigir.
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  return auth.ensureSession().pipe(
    map((authenticated) =>
      authenticated ? true : router.createUrlTree(['/auth/login']),
    ),
  );
};

/** Evita que un usuario autenticado vuelva a las pantallas de acceso. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return router.createUrlTree(['/app/dashboard']);
  }

  return auth.ensureSession().pipe(
    map((authenticated) =>
      authenticated ? router.createUrlTree(['/app/dashboard']) : true,
    ),
  );
};

/**
 * Restringe una ruta a los roles indicados. Se usa despues de `authGuard`,
 * por lo que el usuario ya esta resuelto cuando se evalua el rol.
 */
export function roleGuard(allowedRoles: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const role = auth.user()?.role;

    return role && allowedRoles.includes(role)
      ? true
      : router.createUrlTree(['/app/dashboard']);
  };
}
