import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../../core/api/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { PzButton } from '../../../shared/ui/button/pz-button';

@Component({
  selector: 'pz-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, PzButton],
  template: `
    <section class="pz-auth-card">
      <p class="pz-eyebrow">Recuperacion</p>
      <h1>Restablece tu contrasena</h1>
      <p>
        Escribe el email de tu cuenta y te enviaremos un enlace para crear una nueva
        contrasena.
      </p>

      @if (sent()) {
        <p class="pz-alert pz-alert--success" role="status">
          Si el email existe, enviamos las instrucciones. Revisa tu bandeja y la carpeta de
          spam.
        </p>
      } @else {
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label class="pz-field">
            <span>Email</span>
            <input
              type="email"
              formControlName="email"
              autocomplete="email"
              placeholder="tu@correo.com"
              [attr.aria-invalid]="invalid()"
            />
            @if (invalid()) {
              <small class="pz-field__error">Ingresa un email valido.</small>
            }
          </label>

          @if (errorMessage()) {
            <p class="pz-alert" role="alert">{{ errorMessage() }}</p>
          }

          <button pz-button type="submit" [disabled]="loading()">
            {{ loading() ? 'Enviando...' : 'Enviar enlace' }}
          </button>
        </form>
      }

      <p class="pz-auth-card__alt">
        <a routerLink="/auth/login">Volver al inicio de sesion</a>
      </p>
    </section>
  `,
  styleUrl: '../login/pz-auth-card.scss',
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
