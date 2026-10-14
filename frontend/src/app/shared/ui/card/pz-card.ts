import { Component, input } from '@angular/core';

/** Contenedor con cabecera opcional. Uso: `<pz-card title="Resumen">...</pz-card>`. */
@Component({
  selector: 'pz-card',
  template: `
    @if (title()) {
      <header class="pz-card__header">
        <div>
          <h3>{{ title() }}</h3>
          @if (subtitle()) {
            <p>{{ subtitle() }}</p>
          }
        </div>
        <ng-content select="[card-action]" />
      </header>
    }
    <div class="pz-card__body">
      <ng-content />
    </div>
  `,
  styleUrl: './pz-card.scss',
})
export class PzCard {
  readonly title = input<string>('');
  readonly subtitle = input<string>('');
}
