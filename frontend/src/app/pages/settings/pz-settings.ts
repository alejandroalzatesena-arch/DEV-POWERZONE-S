import { Component } from '@angular/core';
import { PzBadge } from '../../shared/ui/badge/pz-badge';
import { PzCard } from '../../shared/ui/card/pz-card';

@Component({
  selector: 'pz-settings',
  imports: [PzCard, PzBadge],
  template: `
    <div class="pz-page">
      <header class="pz-page__header">
        <div>
          <p class="pz-eyebrow">Configuracion</p>
          <h1>Ajustes</h1>
          <p>Recursos tecnicos del entorno y estado de la plataforma.</p>
        </div>
        <pz-badge tone="success">API operativa</pz-badge>
      </header>

      <section class="pz-grid">
        <pz-card title="Documentacion de API" subtitle="OpenAPI 3">
          <p class="pz-muted">
            Contrato interactivo de los endpoints disponibles.
          </p>
          <a class="pz-settings__link" href="/api/docs" target="_blank" rel="noopener">
            Abrir Swagger UI &rarr;
          </a>
        </pz-card>

        <pz-card title="Salud del servicio" subtitle="Spring Boot Actuator">
          <p class="pz-muted">
            Sondas de disponibilidad usadas por Docker y el pipeline de CI.
          </p>
          <a class="pz-settings__link" href="/api/v1/health" target="_blank" rel="noopener">
            Consultar /api/v1/health &rarr;
          </a>
        </pz-card>

        <pz-card title="Arquitectura" subtitle="Monolito modular">
          <p class="pz-muted">
            Decisiones tecnicas, limites de modulos y modelo multi-tenant.
          </p>
          <span class="pz-mono pz-muted">docs/ARCHITECTURE.md</span>
        </pz-card>
      </section>

      <pz-card title="Entorno" subtitle="Valores de referencia">
        <table class="pz-table">
          <tbody>
            <tr>
              <th scope="row">Frontend</th>
              <td class="pz-mono">Angular 22 &middot; zoneless &middot; standalone</td>
            </tr>
            <tr>
              <th scope="row">Backend</th>
              <td class="pz-mono">Spring Boot 3.5 &middot; Java 21</td>
            </tr>
            <tr>
              <th scope="row">Base de datos</th>
              <td class="pz-mono">PostgreSQL 16 &middot; Flyway</td>
            </tr>
            <tr>
              <th scope="row">Servicio de IA</th>
              <td class="pz-mono">FastAPI &middot; Python 3.12</td>
            </tr>
          </tbody>
        </table>
      </pz-card>
    </div>
  `,
  styles: [
    `
      .pz-settings__link {
        display: inline-block;
        margin-top: 0.85rem;
        font-size: 0.85rem;
        font-weight: 600;
      }
    `,
  ],
})
export class PzSettings {}
