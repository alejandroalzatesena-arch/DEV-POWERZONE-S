import { Component } from '@angular/core';
import { PzCard } from '../../shared/ui/card/pz-card';
import { PzEmptyState } from '../../shared/ui/empty-state/pz-empty-state';

@Component({
  selector: 'pz-members',
  imports: [PzCard, PzEmptyState],
  template: `
    <div class="pz-page">
      <header class="pz-page__header">
        <div>
          <p class="pz-eyebrow">Administracion</p>
          <h1>Miembros</h1>
          <p>
            Directorio de miembros del gimnasio con su rol, estado de membresia y ultima
            actividad registrada.
          </p>
        </div>
      </header>

      <pz-card title="Directorio" subtitle="Gestion de usuarios en el Sprint 1">
        <pz-empty-state
          heading="Sin miembros registrados"
          description="El registro de usuarios y la administracion de perfiles llegan con el Sprint 1."
          mark="M"
        />
      </pz-card>
    </div>
  `,
})
export class PzMembers {}
