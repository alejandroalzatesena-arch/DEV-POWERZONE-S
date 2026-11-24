import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ROLE_LABELS } from '../../core/auth/auth.models';
import { PzIcon, PzIconName } from '../../shared/ui/icon/pz-icon';

interface NavItem {
  label: string;
  path: string;
  exact: boolean;
  hint: string;
  icon: PzIconName;
}

@Component({
  selector: 'pz-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, PzIcon],
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
    const role = this.user()?.role;
    return role ? ROLE_LABELS[role] : 'Miembro';
  });

  private readonly memberNavItems: NavItem[] = [
    { label: 'Panel', path: '/app/dashboard', exact: true, hint: 'Resumen general', icon: 'dashboard' },
    {
      label: 'Rutinas',
      path: '/app/routines',
      exact: false,
      hint: 'Generacion y seguimiento',
      icon: 'routines',
    },
    {
      label: 'Progreso',
      path: '/app/progress',
      exact: false,
      hint: 'Cargas y evolucion',
      icon: 'progress',
    },
    {
      label: 'Pagos',
      path: '/app/payments',
      exact: false,
      hint: 'Membresia y comprobantes',
      icon: 'payments',
    },
    {
      label: 'Ajustes',
      path: '/app/settings',
      exact: false,
      hint: 'Cuenta y perfil',
      icon: 'settings',
    },
  ];

  private readonly staffNavItems: NavItem[] = [
    ...this.memberNavItems.slice(0, 4),
    {
      label: 'Miembros',
      path: '/app/members',
      exact: false,
      hint: 'Directorio del gimnasio',
      icon: 'members',
    },
    ...this.memberNavItems.slice(4),
  ];

  protected readonly visibleNavItems = computed(() =>
    this.auth.isStaff() ? this.staffNavItems : this.memberNavItems,
  );

  protected logout(): void {
    this.auth.logout();
    void this.router.navigate(['/auth/login']);
  }
}
