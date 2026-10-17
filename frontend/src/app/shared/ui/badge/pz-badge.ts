import { Component, input } from '@angular/core';

type BadgeTone = 'neutral' | 'success' | 'warning' | 'danger' | 'info';

/** Etiqueta compacta de estado. */
@Component({
  selector: 'pz-badge',
  template: '<ng-content />',
  styleUrl: './pz-badge.scss',
  host: {
    '[class.pz-badge--success]': "tone() === 'success'",
    '[class.pz-badge--warning]': "tone() === 'warning'",
    '[class.pz-badge--danger]': "tone() === 'danger'",
    '[class.pz-badge--info]': "tone() === 'info'",
  },
})
export class PzBadge {
  readonly tone = input<BadgeTone>('neutral');
}
