import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'pz-root',
  imports: [RouterOutlet],
  template: `
    <main class="pz-bootstrap">
      <h1>PowerZone</h1>
      <p>Estructura base del frontend. Las vistas se incorporan por sprint.</p>
      <router-outlet />
    </main>
  `,
  styles: [
    `
      .pz-bootstrap {
        display: grid;
        place-content: center;
        min-height: 100vh;
        gap: 0.5rem;
        text-align: center;
        font-family: system-ui, sans-serif;
        background: #0b0f14;
        color: #e8eef7;
      }
      h1 {
        font-size: 2.4rem;
        letter-spacing: 0.04em;
        margin: 0;
      }
      p {
        color: #8fa3bd;
        margin: 0;
      }
    `,
  ],
})
export class App {}
