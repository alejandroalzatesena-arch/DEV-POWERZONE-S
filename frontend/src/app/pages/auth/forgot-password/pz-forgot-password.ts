import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../../core/api/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { PzButton } from '../../../shared/ui/button/pz-button';

@Component({
  selector: 'pz-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, PzButton],
  templateUrl: './pz-forgot-password.html',
  styleUrls: ['../login/pz-auth-card.scss', '../pz-auth-forms.scss'],
})
export class PzForgotPassword {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);

  protected readonly loading = signal(false);
  protected readonly sent = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  protected invalid(): boolean {
    const field = this.form.controls.email;
    return field.invalid && (field.dirty || field.touched);
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    this.errorMessage.set('');
    if (this.form.invalid || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.auth.forgotPassword(this.form.controls.email.value.trim().toLowerCase()).subscribe({
      next: () => {
        this.loading.set(false);
        this.sent.set(true);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.errorMessage.set(ApiError.from(error).message);
      },
    });
  }
}
