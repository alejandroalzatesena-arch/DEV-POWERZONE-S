import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../../../core/api/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { PzButton } from '../../../shared/ui/button/pz-button';

@Component({
  selector: 'pz-login',
  imports: [ReactiveFormsModule, RouterLink, PzButton],
  templateUrl: './pz-login.html',
  styleUrl: './pz-auth-card.scss',
})
export class PzLogin {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly submitted = signal(false);

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    rememberMe: [false],
  });

  protected invalid(control: 'email' | 'password'): boolean {
    const field = this.form.controls[control];
    return field.invalid && (field.dirty || field.touched || this.submitted());
  }

  protected submit(): void {
    this.submitted.set(true);
    this.errorMessage.set('');

    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
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
    if (error.code === 'ACCOUNT_LOCKED') {
      return 'Demasiados intentos fallidos. Espera 15 minutos antes de reintentar.';
    }
    if (error.code === 'INVALID_CREDENTIALS') {
      return 'Email o contrasena incorrectos. Verifica tus datos.';
    }
    return error.message;
  }
}
