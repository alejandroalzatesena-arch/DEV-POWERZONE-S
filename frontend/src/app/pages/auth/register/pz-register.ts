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
  template: `
    <section class="pz-auth-card pz-auth-card--wide">
      <p class="pz-eyebrow">Nueva cuenta</p>
      <h1>Crea tu perfil</h1>
      <p>
        Tu perfil alimenta la generacion de rutinas: objetivo, experiencia y dias disponibles
        por semana.
      </p>

      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <fieldset class="pz-form-section">
          <legend>Cuenta</legend>
          <div class="pz-form-grid">
            <label class="pz-field">
              <span>Nombre</span>
              <input type="text" formControlName="firstName" autocomplete="given-name" />
              @if (invalid('firstName')) {
                <small class="pz-field__error">El nombre es obligatorio.</small>
              }
            </label>
            <label class="pz-field">
              <span>Apellido</span>
              <input type="text" formControlName="lastName" autocomplete="family-name" />
              @if (invalid('lastName')) {
                <small class="pz-field__error">El apellido es obligatorio.</small>
              }
            </label>
          </div>

          <label class="pz-field">
            <span>Email</span>
            <input type="email" formControlName="email" autocomplete="email" />
            @if (invalid('email')) {
              <small class="pz-field__error">Ingresa un email valido.</small>
            }
          </label>

          <div class="pz-form-grid">
            <label class="pz-field">
              <span>Contrasena</span>
              <input type="password" formControlName="password" autocomplete="new-password" />
              @if (invalid('password')) {
                <small class="pz-field__error">Minimo 8 caracteres con letras y numeros.</small>
              }
            </label>
            <label class="pz-field">
              <span>Confirmar contrasena</span>
              <input type="password" formControlName="confirmPassword" autocomplete="new-password" />
              @if (form.hasError('passwordsMismatch') && submitted()) {
                <small class="pz-field__error">Las contrasenas no coinciden.</small>
              }
            </label>
          </div>
        </fieldset>

        <fieldset class="pz-form-section">
          <legend>Perfil de entrenamiento</legend>
          <div class="pz-form-grid">
            <label class="pz-field">
              <span>Telefono (opcional)</span>
              <input type="tel" formControlName="phone" autocomplete="tel" />
            </label>
            <label class="pz-field">
              <span>Fecha de nacimiento (opcional)</span>
              <input type="date" formControlName="birthDate" />
            </label>
            <label class="pz-field">
              <span>Estatura en cm (opcional)</span>
              <input type="number" formControlName="heightCm" min="50" max="250" />
            </label>
            <label class="pz-field">
              <span>Peso en kg (opcional)</span>
              <input type="number" formControlName="weightKg" min="20" max="300" step="0.1" />
            </label>
            <label class="pz-field">
              <span>Objetivo</span>
              <select formControlName="goal">
                <option value="">Selecciona un objetivo</option>
                @for (option of goalOptions; track option.value) {
                  <option [value]="option.value">{{ option.label }}</option>
                }
              </select>
            </label>
            <label class="pz-field">
              <span>Experiencia</span>
              <select formControlName="experienceLevel">
                <option value="">Selecciona tu nivel</option>
                @for (option of levelOptions; track option.value) {
                  <option [value]="option.value">{{ option.label }}</option>
                }
              </select>
            </label>
          </div>

          <label class="pz-field">
            <span>Dias de entrenamiento por semana</span>
            <input type="number" formControlName="daysPerWeek" min="2" max="7" />
            <small class="pz-field__hint">Entre 2 y 7 dias.</small>
          </label>
        </fieldset>

        @if (errorMessage()) {
          <p class="pz-alert" role="alert">{{ errorMessage() }}</p>
        }

        <button pz-button type="submit" [disabled]="loading()">
          {{ loading() ? 'Creando cuenta...' : 'Crear cuenta' }}
        </button>
      </form>

      <p class="pz-auth-card__alt">
        Ya tienes cuenta?
        <a routerLink="/auth/login">Iniciar sesion</a>
      </p>
    </section>
  `,
  styleUrl: '../login/pz-auth-card.scss',
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
