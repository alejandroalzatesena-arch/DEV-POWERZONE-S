import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../../core/api/api-error';
import {
  BillingTone,
  MembershipStatus,
  MEMBERSHIP_STATE_LABELS,
  MEMBERSHIP_STATE_TONES,
  formatDate,
} from '../../../core/billing/billing.models';
import { BillingService } from '../../../core/billing/billing.service';
import { PzBadge } from '../badge/pz-badge';
import { PzButton } from '../button/pz-button';
import { PzCard } from '../card/pz-card';

/**
 * Tarjeta de estado de la membresia (PPP-316). Consulta el endpoint de estado
 * y resume plan, vigencia y dias restantes. El estado se comunica con texto,
 * icono y color, nunca solo con color.
 */
@Component({
  selector: 'pz-membership-card',
  imports: [RouterLink, PzCard, PzBadge, PzButton],
  templateUrl: './pz-membership-card.html',
  styleUrl: './pz-membership-card.scss',
})
export class PzMembershipCard implements OnInit {
  private readonly billing = inject(BillingService);

  /** Permite ocultar el acceso directo a renovar en contextos de solo lectura. */
  readonly showRenew = input(true);

  protected readonly membership = signal<MembershipStatus | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');

  protected readonly tone = computed<BillingTone>(() => {
    const membership = this.membership();
    return membership ? MEMBERSHIP_STATE_TONES[membership.state] : 'neutral';
  });

  protected readonly stateLabel = computed(() => {
    const membership = this.membership();
    return membership ? MEMBERSHIP_STATE_LABELS[membership.state] : 'Sin membresia';
  });

  protected readonly endDate = computed(() => formatDate(this.membership()?.endsAt));

  protected readonly daysLabel = computed(() => {
    const membership = this.membership();
    if (!membership || membership.state === 'NONE') {
      return '';
    }
    if (membership.state === 'EXPIRED') {
      return 'Sin dias restantes';
    }
    const days = membership.daysRemaining;
    return days === 1 ? 'Queda 1 dia' : `Quedan ${days} dias`;
  });

  protected readonly needsAttention = computed(() => {
    const state = this.membership()?.state;
    return state === 'NONE' || state === 'EXPIRING' || state === 'EXPIRED';
  });

  protected readonly attentionMessage = computed(() => {
    switch (this.membership()?.state) {
      case 'EXPIRING':
        return 'Tu membresia esta por vencer. Renueva ahora para no perder el acceso al gimnasio.';
      case 'EXPIRED':
        return 'Tu membresia vencio. Renueva tu plan para volver a entrenar con acceso completo.';
      case 'NONE':
        return 'Aun no tienes una membresia activa. Elige un plan para comenzar a entrenar.';
      default:
        return '';
    }
  });

  protected readonly formatDate = formatDate;

  ngOnInit(): void {
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.billing
      .membership()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (membership) => this.membership.set(membership),
        error: (error: unknown) => this.errorMessage.set(ApiError.from(error).message),
      });
  }
}
