import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

/** Layout de dos columnas para las pantallas publicas de autenticacion. */
@Component({
  selector: 'pz-auth-layout',
  imports: [RouterOutlet, RouterLink],
  template: `
    <div class="pz-auth">
      <section class="pz-auth__panel">
        <a class="pz-brand" routerLink="/">
          <img
            class="pz-brand__logo"
            src="brand/powerzone-logo.webp"
            alt="PowerZone"
            width="720"
            height="657"
          />
        </a>

        <div class="pz-auth__pitch">
          <p class="pz-eyebrow">Entrena con datos</p>
          <h1>Tu gimnasio, tus rutinas y tu progreso en un solo lugar.</h1>
          <p class="pz-muted">
            PowerZone conecta a miembros y entrenadores con rutinas generadas a partir del
            perfil, el objetivo y la disponibilidad semanal.
          </p>

          <ul class="pz-auth__bullets">
            <li>Rutinas personalizadas por objetivo</li>
            <li>Seguimiento de progreso por ejercicio</li>
            <li>Panel administrativo multi-sede</li>
          </ul>
        </div>

        <p class="pz-muted pz-auth__legal">Proyecto academico SENA ADSO &middot; 2026</p>
      </section>

      <section class="pz-auth__form">
        <router-outlet />
      </section>
    </div>
  `,
  styleUrl: './pz-auth-layout.scss',
})
export class PzAuthLayout {}
