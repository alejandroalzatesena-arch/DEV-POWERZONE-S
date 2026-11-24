import { Component, input } from '@angular/core';

/** Estado vacio reutilizable para vistas sin datos. */
@Component({
  selector: 'pz-empty-state',
  templateUrl: './pz-empty-state.html',
  styleUrl: './pz-empty-state.scss',
})
export class PzEmptyState {
  readonly heading = input.required<string>();
  readonly description = input<string>('');
  readonly mark = input<string>('+');
}
