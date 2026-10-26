import { Component, inject, input, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../../../core/api/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/ui/toast.service';
import { PzButton } from '../../../shared/ui/button/pz-button';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const password = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordsMismatch: true } : null;
}

@Component({
  selector: 'pz-reset-password',
  imports: [ReactiveFormsModule, RouterLink, PzButton],
  template: `
    <section class="pz-auth-card">
      <p class="pz-eyebrow">Nueva contrasena</p>
      <h1>Crea una contrasena nueva</h1>
      <p>El enlace es de un solo uso y vence a los 30 minutos.</p>

      @if (!token()) {
        <p class="pz-alert" role="alert">
          El enlace no incluye un token valido. Solicita uno nuevo.
        </p>
        <p class="pz-auth-card__alt">
          <a routerLink="/auth/forgot-password">Solicitar otro enlace</a>
        </p>
      } @else {
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label class="pz-field">
            <span>Nueva contrasena</span>
            <input type="password" formControlName="newPassword" autocomplete="new-password" />
            @if (invalid('newPassword')) {
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

          @if (errorMessage()) {
            <p class="pz-alert" role="alert">{{ errorMessage() }}</p>
          }

          <button pz-button type="submit" [disabled]="loading()">
            {{ loading() ? 'Guardando...' : 'Guardar contrasena' }}
          </button>
        </form>

        <p class="pz-auth-card__alt">
          <a routerLink="/auth/login">Volver al inicio de sesion</a>
        </p>
      }
    </section>
  `,
  styleUrl: '../login/pz-auth-card.scss',
})
export class PzResetPassword {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  /** Token enviado por correo, enlazado desde el query param `token`. */
  readonly token = input<string>('');

  protected readonly loading = signal(false);
  protected readonly submitted = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly form = this.fb.group(
    {
      newPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(72),
          Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
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

    this.loading.set(true);
    this.auth.resetPassword(this.token(), this.form.controls.newPassword.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.toast.success('Contrasena actualizada. Inicia sesion con tu nueva clave.');
        void this.router.navigate(['/auth/login']);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        const apiError = ApiError.from(error);
        this.errorMessage.set(
          apiError.code === 'INVALID_RESET_TOKEN'
            ? 'El enlace vencio o ya fue usado. Solicita uno nuevo.'
            : apiError.message,
        );
      },
    });
  }
}
