import { Component, input } from '@angular/core';

export type PzIconName =
  | 'dashboard'
  | 'routines'
  | 'progress'
  | 'payments'
  | 'members'
  | 'settings';

/** Iconos de linea del design system. Uso: `<pz-icon name="routines" />`. */
@Component({
  selector: 'pz-icon',
  template: `
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.7"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      @switch (name()) {
        @case ('dashboard') {
          <rect x="3" y="3" width="7" height="7" rx="2" />
          <rect x="14" y="3" width="7" height="7" rx="2" />
          <rect x="3" y="14" width="7" height="7" rx="2" />
          <rect x="14" y="14" width="7" height="7" rx="2" />
        }
        @case ('routines') {
          <path d="M4 9v6M7 7v10M17 7v10M20 9v6M7 12h10" />
        }
        @case ('progress') {
          <path d="M4 20V5M4 20h16" />
          <path d="M7 15l4-5 3 3 5-7" />
        }
        @case ('payments') {
          <rect x="2.5" y="5.5" width="19" height="13" rx="2.5" />
          <path d="M2.5 10h19" />
          <path d="M6 15h3" />
        }
        @case ('members') {
          <circle cx="9" cy="8" r="3.2" />
          <path d="M3.5 19a5.5 5.5 0 0 1 11 0" />
          <path d="M16 5.2a3.2 3.2 0 0 1 0 5.6M17.4 19a5.5 5.5 0 0 0-2.4-4.5" />
        }
        @case ('settings') {
          <circle cx="12" cy="12" r="3" />
          <path
            d="M19.4 15a1.7 1.7 0 0 0 .34 1.87l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06A1.7 1.7 0 0 0 15 19.4a1.7 1.7 0 0 0-1 1.55V21a2 2 0 1 1-4 0v-.09a1.7 1.7 0 0 0-1-1.55 1.7 1.7 0 0 0-1.87.34l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-1.55-1H3a2 2 0 1 1 0-4h.09a1.7 1.7 0 0 0 1.55-1 1.7 1.7 0 0 0-.34-1.87l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-1.55V3a2 2 0 1 1 4 0v.09a1.7 1.7 0 0 0 1 1.55 1.7 1.7 0 0 0 1.87-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.7 1.7 0 0 0 19.4 9v.09a1.7 1.7 0 0 0 1.55 1H21a2 2 0 1 1 0 4h-.09a1.7 1.7 0 0 0-1.51.91Z"
          />
        }
      }
    </svg>
  `,
  styles: [
    `
      :host {
        display: inline-flex;
        width: 1.05rem;
        height: 1.05rem;
        flex: none;
      }

      svg {
        width: 100%;
        height: 100%;
      }
    `,
  ],
})
export class PzIcon {
  readonly name = input.required<PzIconName>();
}
