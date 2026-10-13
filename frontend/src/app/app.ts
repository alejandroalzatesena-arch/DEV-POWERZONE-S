import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { PzToastHost } from './shared/ui/toast-host/pz-toast-host';

@Component({
  selector: 'pz-root',
  imports: [RouterOutlet, PzToastHost],
  template: `
    <router-outlet />
    <pz-toast-host />
  `,
})
export class App {}
