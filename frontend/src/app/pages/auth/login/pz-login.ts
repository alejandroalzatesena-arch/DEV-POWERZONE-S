import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PzButton } from '../../../shared/ui/button/pz-button';

@Component({
  selector: 'pz-login',
  imports: [PzButton, RouterLink],
  template: `
    <div class="pz-auth-card">
      <p class="pz-eyebrow">Acceso</p>
      <h1>Inicia sesion</h1>
      <p>
        El formulario de acceso, la sesion segura y la opcion "recordarme" se habilitan en
        el Sprint 1.
      </p>

      <button pz-button type="button" disabled>Entrar</button>

      <p class="pz-auth-card__alt">
        Nuevo en PowerZone?
        <a routerLink="/auth/register">Crear una cuenta</a>
      </p>
    </div>
  `,
  styleUrl: './pz-auth-card.scss',
})
export class PzLogin {}
