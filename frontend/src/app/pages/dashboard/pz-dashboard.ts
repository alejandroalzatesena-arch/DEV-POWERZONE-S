import { Component } from '@angular/core';
import { PzBadge } from '../../shared/ui/badge/pz-badge';
import { PzCard } from '../../shared/ui/card/pz-card';
import { PzEmptyState } from '../../shared/ui/empty-state/pz-empty-state';

interface Stat {
  label: string;
  value: string;
  detail: string;
  tone: 'success' | 'info' | 'warning' | 'neutral';
}

interface Milestone {
  title: string;
  detail: string;
  done: boolean;
}

@Component({
  selector: 'pz-dashboard',
  imports: [PzCard, PzBadge, PzEmptyState],
  template: `
    <div class="pz-page">
      <header class="pz-page__header">
        <div>
          <p class="pz-eyebrow">Resumen</p>
          <h1>Bienvenido a PowerZone</h1>
          <p>
            Base del panel administrativo. Los indicadores se conectan a la API a medida que
            avanzan los sprints.
          </p>
        </div>
        <pz-badge tone="warning">Datos de ejemplo</pz-badge>
      </header>

      <section class="pz-grid" aria-label="Indicadores del gimnasio">
        @for (stat of stats; track stat.label) {
          <pz-card>
            <div class="pz-stat">
              <span class="pz-muted">{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <pz-badge [tone]="stat.tone">{{ stat.detail }}</pz-badge>
            </div>
          </pz-card>
        }
      </section>

      <section class="pz-dashboard__columns">
        <pz-card title="Puesta en marcha" subtitle="Estado del Sprint 0">
          <ul class="pz-milestones">
            @for (item of milestones; track item.title) {
              <li [class.is-done]="item.done">
                <span class="pz-milestones__mark" aria-hidden="true">
                  {{ item.done ? 'OK' : '--' }}
                </span>
                <span>
                  <strong>{{ item.title }}</strong>
                  <small>{{ item.detail }}</small>
                </span>
              </li>
            }
          </ul>
        </pz-card>

        <pz-card title="Actividad reciente">
          <pz-empty-state
            heading="Sin actividad todavia"
            description="Cuando existan miembros y rutinas registradas, aqui aparecera el historial del gimnasio."
            mark="i"
          />
        </pz-card>
      </section>
    </div>
  `,
  styleUrl: './pz-dashboard.scss',
})
export class PzDashboard {
  protected readonly stats: Stat[] = [
    { label: 'Miembros activos', value: '0', detail: 'Sprint 1', tone: 'info' },
    { label: 'Rutinas generadas', value: '0', detail: 'Sprint 2', tone: 'warning' },
    { label: 'Sesiones esta semana', value: '0', detail: 'Sprint 6', tone: 'neutral' },
    { label: 'Pagos pendientes', value: '0', detail: 'Sprint 3', tone: 'neutral' },
  ];

  protected readonly milestones: Milestone[] = [
    { title: 'Monorepo y Docker Compose', detail: 'Backend, frontend y servicio de IA', done: true },
    { title: 'API base con errores RFC 7807', detail: 'OpenAPI y endpoint de salud', done: true },
    { title: 'Esquema multi-tenant con Flyway', detail: 'Gimnasios, roles y usuarios', done: true },
    { title: 'Autenticacion y RBAC', detail: 'Sprint 1', done: false },
    { title: 'Motor de rutinas', detail: 'Sprint 2', done: false },
  ];
}
