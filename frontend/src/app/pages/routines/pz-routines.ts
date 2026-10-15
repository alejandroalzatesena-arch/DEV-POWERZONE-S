import { Component } from '@angular/core';
import { PzButton } from '../../shared/ui/button/pz-button';
import { PzCard } from '../../shared/ui/card/pz-card';
import { PzEmptyState } from '../../shared/ui/empty-state/pz-empty-state';

@Component({
  selector: 'pz-routines',
  imports: [PzCard, PzButton, PzEmptyState],
  template: `
    <div class="pz-page">
      <header class="pz-page__header">
        <div>
          <p class="pz-eyebrow">Entrenamiento</p>
          <h1>Rutinas</h1>
          <p>
            Generacion de rutinas segun objetivo, nivel y dias disponibles, con seguimiento
            de progreso por ejercicio.
          </p>
        </div>
        <button pz-button type="button" disabled>Generar rutina</button>
      </header>

      <pz-card title="Motor de rutinas" subtitle="Disponible en el Sprint 2">
        <pz-empty-state
          heading="Aun no hay rutinas"
          description="El modelo de datos, las reglas de generacion y la API de rutinas se incorporan en el Sprint 2 (02 al 14 de noviembre)."
          mark="R"
        />
      </pz-card>

      <section class="pz-grid">
        <pz-card title="Perfil de entrenamiento">
          <p class="pz-muted">
            Objetivo, experiencia y disponibilidad semanal definen la seleccion de ejercicios.
          </p>
        </pz-card>
        <pz-card title="Progreso por ejercicio">
          <p class="pz-muted">
            Registro de peso, repeticiones y notas para cada sesion completada.
          </p>
        </pz-card>
        <pz-card title="Vista semanal">
          <p class="pz-muted">
            Distribucion de los dias de entrenamiento con el detalle de cada bloque.
          </p>
        </pz-card>
      </section>
    </div>
  `,
})
export class PzRoutines {}
