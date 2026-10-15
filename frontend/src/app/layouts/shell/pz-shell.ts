import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
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
  protected readonly navItems: NavItem[] = [
    { label: 'Panel', path: '/app/dashboard', exact: true, hint: 'Resumen general' },
    { label: 'Rutinas', path: '/app/routines', exact: false, hint: 'Generacion y seguimiento' },
    { label: 'Miembros', path: '/app/members', exact: false, hint: 'Directorio del gimnasio' },
    { label: 'Ajustes', path: '/app/settings', exact: false, hint: 'Preferencias y API' },
  ];
}
