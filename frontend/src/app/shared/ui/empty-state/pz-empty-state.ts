import { Component, input } from '@angular/core';

/** Estado vacio reutilizable para vistas sin datos. */
@Component({
  selector: 'pz-empty-state',
  template: `
    <div class="pz-empty">
      <span class="pz-empty__mark" aria-hidden="true">{{ mark() }}</span>
      <h3>{{ heading() }}</h3>
      <p>{{ description() }}</p>
      <ng-content />
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .pz-empty {
        display: grid;
        justify-items: center;
        gap: 0.5rem;
        padding: 2.5rem 1.5rem;
        text-align: center;
      }

      .pz-empty__mark {
        display: grid;
        place-items: center;
        width: 2.75rem;
        height: 2.75rem;
        border-radius: 50%;
        background: var(--pz-primary-soft);
        color: var(--pz-primary);
        font-weight: 700;
        margin-bottom: 0.25rem;
      }

      h3 {
        font-size: 0.95rem;
      }

      p {
        max-width: 46ch;
        color: var(--pz-muted);
        font-size: 0.88rem;
        line-height: 1.5;
      }
    `,
  ],
})
export class PzEmptyState {
  readonly heading = input.required<string>();
  readonly description = input<string>('');
  readonly mark = input<string>('+');
}
