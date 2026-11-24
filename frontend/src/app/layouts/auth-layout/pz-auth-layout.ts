import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

/** Layout de dos columnas para las pantallas publicas de autenticacion. */
@Component({
  selector: 'pz-auth-layout',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './pz-auth-layout.html',
  styleUrl: './pz-auth-layout.scss',
})
export class PzAuthLayout {}
