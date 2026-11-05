import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/api/api-error';
import {
  ExperienceLevel,
  GOAL_OPTIONS,
  Goal,
  LEVEL_OPTIONS,
  Role,
  UpdateProfilePayload,
} from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/ui/toast.service';
import { PzBadge } from '../../shared/ui/badge/pz-badge';
import { PzButton } from '../../shared/ui/button/pz-button';
import { PzCard } from '../../shared/ui/card/pz-card';

const ROLE_LABELS: Record<Role, string> = {
  MEMBER: 'Miembro',
  TRAINER: 'Entrenador',
  ADMIN: 'Administrador',
  SUPER_ADMIN: 'Super administrador',
};

@Component({
  selector: 'pz-settings',
  imports: [ReactiveFormsModule, PzBadge, PzButton, PzCard],
  templateUrl: './pz-settings.html',
  styleUrl: './pz-settings.scss',
})
export class PzSettings implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly goalOptions = GOAL_OPTIONS;
  protected readonly levelOptions = LEVEL_OPTIONS;

  protected readonly user = this.auth.user;
  protected readonly roleLabel = computed(() => ROLE_LABELS[this.user()?.role ?? 'MEMBER']);
  protected readonly saving = signal(false);
  protected readonly sendingReset = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly submitted = signal(false);

  protected readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    phone: ['', [Validators.maxLength(30)]],
    birthDate: [''],
    heightCm: ['', [Validators.min(50), Validators.max(250)]],
    weightKg: ['', [Validators.min(20), Validators.max(300)]],
    goal: [''],
    experienceLevel: [''],
    daysPerWeek: ['', [Validators.min(2), Validators.max(7)]],
  });

  ngOnInit(): void {
    this.fillFromUser();
  }

  protected invalid(control: string): boolean {
    const field = this.form.get(control);
    return !!field && field.invalid && (field.dirty || field.touched || this.submitted());
  }

  protected discard(): void {
    this.errorMessage.set('');
    this.submitted.set(false);
    this.fillFromUser();
  }

  protected save(): void {
    this.submitted.set(true);
    this.errorMessage.set('');

    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const payload: UpdateProfilePayload = {
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      phone: value.phone.trim(),
      birthDate: value.birthDate || null,
      heightCm: toNumber(value.heightCm),
      weightKg: toNumber(value.weightKg),
      goal: (value.goal || null) as Goal | null,
      experienceLevel: (value.experienceLevel || null) as ExperienceLevel | null,
      daysPerWeek: toNumber(value.daysPerWeek),
    };

    this.saving.set(true);
    this.auth
      .updateProfile(payload)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.form.markAsPristine();
          this.toast.success('Tu perfil se actualizo correctamente.');
        },
        error: (error: unknown) => {
          this.errorMessage.set(describeError(error));
        },
      });
  }

  protected sendPasswordReset(): void {
    const email = this.user()?.email;
    if (!email || this.sendingReset()) {
      return;
    }

    this.sendingReset.set(true);
    this.auth
      .forgotPassword(email)
      .pipe(finalize(() => this.sendingReset.set(false)))
      .subscribe({
        next: () => this.toast.success(`Te enviamos un enlace a ${email}.`),
        error: (error: unknown) => this.toast.error(ApiError.from(error).message),
      });
  }

  private fillFromUser(): void {
    const user = this.user();
    if (!user) {
      return;
    }

    this.form.reset({
      firstName: user.firstName,
      lastName: user.lastName,
      phone: user.phone ?? '',
      birthDate: user.birthDate ?? '',
      heightCm: toInput(user.heightCm),
      weightKg: toInput(user.weightKg),
      goal: user.goal ?? '',
      experienceLevel: user.experienceLevel ?? '',
      daysPerWeek: toInput(user.daysPerWeek),
    });
  }
}

function toInput(value: number | null | undefined): string {
  return value === null || value === undefined ? '' : String(value);
}

function toNumber(value: string): number | null {
  if (`${value}`.trim() === '') {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function describeError(error: unknown): string {
  const apiError = ApiError.from(error);
  if (apiError.code === 'VALIDATION_ERROR') {
    const first = Object.values(apiError.fieldErrors)[0];
    return first ?? 'Revisa los datos del formulario.';
  }
  return apiError.message;
}
