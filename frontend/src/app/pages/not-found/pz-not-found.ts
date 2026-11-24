import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PzButton } from '../../shared/ui/button/pz-button';

@Component({
  selector: 'pz-not-found',
  imports: [RouterLink, PzButton],
  templateUrl: './pz-not-found.html',
  styleUrl: './pz-not-found.scss',
})
export class PzNotFound {}
