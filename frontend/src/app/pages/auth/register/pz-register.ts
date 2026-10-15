import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PzButton } from '../../../shared/ui/button/pz-button';

@Component({
  selector: 'pz-register',
  imports: [PzButton, RouterLink],
  template: `
    <div class="pz-auth-card">
      <p class="pz-eyebrow">Nueva cuenta</p>
      <h1>Crea tu perfil</h1>
      <p>
        El registro de miembros con validacion de datos y perfil de entrenamiento se
        habilita en el Sprint 1.
      </p>

      <button pz-button type="button" disabled>Crear cuenta</button>

      <p class="pz-auth-card__alt">
        Ya tienes cuenta?
        <a routerLink="/auth/login">Iniciar sesion</a>
      </p>
    </div>
  `,
  styleUrl: '../login/pz-auth-card.scss',
})
export class PzRegister {}
