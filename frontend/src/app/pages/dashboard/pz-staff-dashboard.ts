import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { ApiError } from '../../core/api/api-error';
import { ROLE_LABELS, User } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import {
  BillingService,
} from '../../core/billing/billing.service';
import {
  PageResult,
  PAYMENT_STATUS_LABELS,
  PAYMENT_STATUS_TONES,
  Payment,
  formatDateTime,
  formatMoney,
} from '../../core/billing/billing.models';
import { UsersService } from '../../core/users/users.service';
import { PzBadge } from '../../shared/ui/badge/pz-badge';
import { PzCard } from '../../shared/ui/card/pz-card';
import { PzEmptyState } from '../../shared/ui/empty-state/pz-empty-state';

interface StaffStats {
  label: string;
  value: string;
  detail: string;
}

@Component({
  selector: 'pz-staff-dashboard',
  imports: [RouterLink, PzCard, PzBadge, PzEmptyState],
  templateUrl: './pz-staff-dashboard.html',
  styleUrl: './pz-staff-dashboard.scss',
})
export class PzStaffDashboard implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly usersApi = inject(UsersService);
  private readonly billingApi = inject(BillingService);

  protected readonly roleLabels = ROLE_LABELS;
  protected readonly paymentLabels = PAYMENT_STATUS_LABELS;
  protected readonly paymentTones = PAYMENT_STATUS_TONES;
  protected readonly formatDateTime = formatDateTime;
  protected readonly formatMoney = formatMoney;

  protected readonly greetingName = computed(() => this.auth.user()?.firstName ?? 'equipo');
  protected readonly users = signal<User[]>([]);
  protected readonly payments = signal<Payment[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');

  protected readonly activeUsers = computed(() => this.users().filter((user) => user.active));

  protected readonly staffUsers = computed(() =>
    this.users().filter((user) => user.role !== 'MEMBER'),
  );

  protected readonly recentPayments = computed(() => this.payments().slice(0, 5));

  protected readonly lastPayment = computed(() => this.recentPayments()[0] ?? null);

  protected readonly stats = computed<StaffStats[]>(() => {
    const last = this.lastPayment();
    return [
      {
        label: 'Usuarios del gimnasio',
        value: String(this.users().length),
        detail: `${this.activeUsers().length} activos`,
      },
      {
        label: 'Personal',
        value: String(this.staffUsers().length),
        detail: 'Entrenadores y administradores',
      },
      {
        label: 'Pagos recientes',
        value: String(this.recentPayments().length),
        detail: 'Ultimos 5 registros',
      },
      {
        label: 'Ultimo pago',
        value: last ? this.formatMoney(last.amountCents, last.currency) : '—',
        detail: last ? this.paymentLabels[last.status] : 'Sin registros',
      },
    ];
  });

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    forkJoin({
      users: this.usersApi.list().pipe(
        catchError((error: unknown) => {
          this.errorMessage.set(ApiError.from(error).message);
          return of<User[]>([]);
        }),
      ),
      payments: this.billingApi.history({ size: 5 }).pipe(
        catchError(() =>
          of<PageResult<Payment>>({ items: [], page: 0, size: 5, total: 0, totalPages: 0 }),
        ),
      ),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((data) => {
        this.users.set(data.users);
        this.payments.set(data.payments.items);
      });
  }
}
