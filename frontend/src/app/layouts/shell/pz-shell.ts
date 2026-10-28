import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { PzBadge } from '../../shared/ui/badge/pz-badge';

interface NavItem {
  label: string;
  path: string;
  exact: boolean;
  hint: string;
}

@Component({
  selector: 'pz-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, PzBadge],
  templateUrl: './pz-shell.html',
  styleUrl: './pz-shell.scss',
})
export class PzShell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = this.auth.user;
  protected readonly displayName = this.auth.displayName;
  protected readonly initials = computed(() => {
    const user = this.user();
    if (!user) {
      return 'PZ';
    }
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  });
  protected readonly roleLabel = computed(() => {
    switch (this.user()?.role) {
      case 'ADMIN':
        return 'Administrador';
      case 'TRAINER':
        return 'Entrenador';
      case 'SUPER_ADMIN':
        return 'Super administrador';
      default:
        return 'Miembro';
    }
  });

  protected readonly navItems: NavItem[] = [
    { label: 'Panel', path: '/app/dashboard', exact: true, hint: 'Resumen general' },
    { label: 'Rutinas', path: '/app/routines', exact: false, hint: 'Generacion y seguimiento' },
    { label: 'Miembros', path: '/app/members', exact: false, hint: 'Directorio del gimnasio' },
    { label: 'Ajustes', path: '/app/settings', exact: false, hint: 'Preferencias y API' },
  ];

  protected logout(): void {
    this.auth.logout();
    void this.router.navigate(['/auth/login']);
  }
}
