import { Component, input } from '@angular/core';

type ButtonVariant = 'primary' | 'ghost' | 'danger';
type ButtonSize = 'md' | 'sm';

/**
 * Boton del design system. Uso: `<button pz-button variant="ghost">...</button>`.
 */
@Component({
  selector: 'button[pz-button]',
  template: '<ng-content />',
  styleUrl: './pz-button.scss',
  host: {
    '[class.pz-button--ghost]': "variant() === 'ghost'",
    '[class.pz-button--danger]': "variant() === 'danger'",
    '[class.pz-button--sm]': "size() === 'sm'",
    '[attr.type]': 'type()',
  },
})
export class PzButton {
  readonly variant = input<ButtonVariant>('primary');
  readonly size = input<ButtonSize>('md');
  readonly type = input<'button' | 'submit' | 'reset'>('button');
}
