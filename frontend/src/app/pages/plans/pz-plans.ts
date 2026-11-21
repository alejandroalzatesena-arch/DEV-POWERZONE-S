import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { ApiError } from '../../core/api/api-error';
import {
  BillingPlan,
  CheckoutResult,
  PAYMENT_METHOD_KIND_LABELS,
  PaymentMethodOption,
  formatMoney,
  planDurationLabel,
} from '../../core/billing/billing.models';
import { BillingService } from '../../core/billing/billing.service';
import { ToastService } from '../../core/ui/toast.service';
import { PzBadge } from '../../shared/ui/badge/pz-badge';
import { PzButton } from '../../shared/ui/button/pz-button';
import { PzCard } from '../../shared/ui/card/pz-card';
import { PzEmptyState } from '../../shared/ui/empty-state/pz-empty-state';

interface PlansData {
  plans: BillingPlan[];
  methods: PaymentMethodOption[];
}

/** Seleccion de plan y apertura del checkout alojado (PPP-310). */
@Component({
  selector: 'pz-plans',
  imports: [ReactiveFormsModule, RouterLink, PzCard, PzButton, PzBadge, PzEmptyState],
  templateUrl: './pz-plans.html',
  styleUrl: './pz-plans.scss',
})
export class PzPlans implements OnInit {
  private readonly billing = inject(BillingService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly plans = signal<BillingPlan[]>([]);
  protected readonly methods = signal<PaymentMethodOption[]>([]);
  protected readonly selectedPlanId = signal<number | null>(null);
  protected readonly selectedMethodCode = signal('');
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly kindLabels = PAYMENT_METHOD_KIND_LABELS;
  protected readonly money = formatMoney;
  protected readonly durationLabel = planDurationLabel;

  protected readonly form = this.fb.group({
    planId: [0, [Validators.required, Validators.min(1)]],
    methodCode: ['', [Validators.required]],
  });

  protected readonly selectedPlan = computed(
    () => this.plans().find((plan) => plan.id === this.selectedPlanId()) ?? null,
  );

  protected readonly selectedMethod = computed(
    () => this.methods().find((method) => method.code === this.selectedMethodCode()) ?? null,
  );

  protected readonly canPay = computed(
    () => this.selectedPlanId() !== null && this.selectedMethodCode() !== '',
  );

  ngOnInit(): void {
    this.load();
  }

  protected selectPlan(plan: BillingPlan): void {
    this.selectedPlanId.set(plan.id);
    this.form.controls.planId.setValue(plan.id);
  }

  protected selectMethod(code: string): void {
    this.selectedMethodCode.set(code);
    this.form.controls.methodCode.setValue(code);
  }

  protected checkout(): void {
    this.errorMessage.set('');
    if (this.form.invalid || !this.canPay() || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitting.set(true);
    this.billing
      .checkout({ planId: value.planId, methodCode: value.methodCode })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (result) => {
          this.toast.success('Pago iniciado. Completa la confirmacion en la pasarela.');
          this.open(result);
        },
        error: (error: unknown) => this.errorMessage.set(ApiError.from(error).message),
      });
  }

  private open(result: CheckoutResult): void {
    const fallback = ['/app/checkout', result.paymentId];
    if (!result.checkoutUrl) {
      void this.router.navigate(fallback);
      return;
    }

    const target = new URL(result.checkoutUrl, window.location.origin);
    if (result.checkoutUrl.startsWith('/') || target.origin === window.location.origin) {
      void this.router.navigateByUrl(`${target.pathname}${target.search}`);
      return;
    }
    window.location.assign(target.toString());
  }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    forkJoin({
      plans: this.billing.plans(),
      methods: this.billing.paymentMethods(),
    })
      .pipe(
        catchError((error: unknown) => {
          this.errorMessage.set(ApiError.from(error).message);
          return of<PlansData>({ plans: [], methods: [] });
        }),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((data) => {
        this.plans.set(data.plans);
        this.methods.set(data.methods);

        const firstPlan = data.plans[0];
        if (firstPlan) {
          this.selectPlan(firstPlan);
        }
        const method = data.methods.find((item) => item.immediate) ?? data.methods[0];
        if (method) {
          this.selectMethod(method.code);
        }
      });
  }
}
