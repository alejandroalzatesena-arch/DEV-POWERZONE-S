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
  templateUrl: './pz-reset-password.html',
  styleUrls: ['../login/pz-auth-card.scss', '../pz-auth-forms.scss'],
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
