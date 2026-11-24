import { Component, computed, inject } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { PzMemberDashboard } from './pz-member-dashboard';
import { PzStaffDashboard } from './pz-staff-dashboard';

/** Selecciona el panel según el rol del usuario autenticado. */
@Component({
  selector: 'pz-dashboard',
  imports: [PzMemberDashboard, PzStaffDashboard],
  templateUrl: './pz-dashboard.html',
  styleUrl: './pz-dashboard.scss',
})
export class PzDashboard {
  private readonly auth = inject(AuthService);

  protected readonly role = computed(() => this.auth.user()?.role ?? 'MEMBER');
}
