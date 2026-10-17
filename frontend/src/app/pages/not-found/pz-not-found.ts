import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PzButton } from '../../shared/ui/button/pz-button';

@Component({
  selector: 'pz-not-found',
  imports: [RouterLink, PzButton],
  template: `
    <main class="pz-not-found">
      <p class="pz-eyebrow">Error 404</p>
      <h1>Esta pagina no existe</h1>
      <p class="pz-muted">
        La ruta solicitada no forma parte de PowerZone o fue movida durante un sprint.
      </p>
      <a routerLink="/app/dashboard">
        <button pz-button type="button">Volver al panel</button>
      </a>
    </main>
  `,
  styles: [
    `
      .pz-not-found {
        display: grid;
        place-content: center;
        justify-items: center;
        gap: 0.85rem;
        min-height: 100vh;
        padding: 2rem;
        text-align: center;
      }

      p {
        max-width: 44ch;
        line-height: 1.55;
      }

      a {
        margin-top: 0.5rem;
      }
    `,
  ],
})
export class PzNotFound {}
