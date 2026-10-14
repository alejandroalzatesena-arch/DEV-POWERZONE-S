import { Component, inject } from '@angular/core';
import { ToastService } from '../../../core/ui/toast.service';

/** Region viva que muestra las notificaciones del `ToastService`. */
@Component({
  selector: 'pz-toast-host',
  template: `
    <div class="pz-toasts" aria-live="polite" aria-atomic="false">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="pz-toast pz-toast--{{ toast.tone }}" role="status">
          <span>{{ toast.message }}</span>
          <button
            type="button"
            class="pz-toast__close"
            aria-label="Cerrar notificacion"
            (click)="toastService.dismiss(toast.id)"
          >
            &times;
          </button>
        </div>
      }
    </div>
  `,
  styleUrl: './pz-toast-host.scss',
})
export class PzToastHost {
  protected readonly toastService = inject(ToastService);
}
