import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { ApiError } from '../../../core/api/api-error';
import {
  ExperienceLevel,
  GOAL_OPTIONS,
  Goal,
  LEVEL_OPTIONS,
  RegisterPayload,
} from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/ui/toast.service';
import { PzButton } from '../../../shared/ui/button/pz-button';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordsMismatch: true } : null;
}

@Component({
  selector: 'pz-register',
  imports: [ReactiveFormsModule, RouterLink, PzButton],
  templateUrl: './pz-register.html',
  styleUrls: ['../login/pz-auth-card.scss', '../pz-auth-forms.scss'],
})
export class PzRegister {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly goalOptions = GOAL_OPTIONS;
  protected readonly levelOptions = LEVEL_OPTIONS;
  protected readonly loading = signal(false);
  protected readonly submitted = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly form = this.fb.group(
    {
      firstName: ['', [Validators.required, Validators.maxLength(80)]],
      lastName: ['', [Validators.required, Validators.maxLength(80)]],
      email: ['', [Validators.required, Validators.email]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(72),
          Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
      phone: ['', [Validators.maxLength(30)]],
      birthDate: [''],
      heightCm: [''],
      weightKg: [''],
      goal: [''],
      experienceLevel: [''],
      daysPerWeek: [''],
    },
    { validators: passwordsMatch },
  );

  protected invalid(control: string): boolean {
    const field = this.form.get(control);
    return !!field && field.invalid && (field.dirty || field.touched || this.submitted());
  }

  protected submit(): void {
    this.submitted.set(true);
    this.errorMessage.set('');

    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const payload: RegisterPayload = {
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      email: value.email.trim().toLowerCase(),
      password: value.password,
      phone: value.phone.trim() || null,
      birthDate: value.birthDate || null,
      heightCm: toNumber(value.heightCm),
      weightKg: toNumber(value.weightKg),
      goal: (value.goal || null) as Goal | null,
      experienceLevel: (value.experienceLevel || null) as ExperienceLevel | null,
      daysPerWeek: toNumber(value.daysPerWeek),
    };

    this.loading.set(true);
    this.auth
      .register(payload)
      .pipe(
        switchMap(() =>
          this.auth.login({
            email: payload.email,
            password: payload.password,
            rememberMe: false,
          }),
        ),
      )
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.toast.success('Tu cuenta esta lista. Bienvenido a PowerZone.');
          void this.router.navigate(['/app/dashboard']);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          const apiError = ApiError.from(error);
          this.errorMessage.set(this.describe(apiError));
        },
      });
  }

  private describe(error: ApiError): string {
    if (error.code === 'EMAIL_TAKEN') {
      return 'Ya existe una cuenta con ese email. Intenta iniciar sesion.';
    }
    if (error.code === 'VALIDATION_ERROR') {
      const first = Object.values(error.fieldErrors)[0];
      return first ?? 'Revisa los datos del formulario.';
    }
    return error.message;
  }
}

function toNumber(value: string): number | null {
  if (value === null || value === undefined || `${value}`.trim() === '') {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}
