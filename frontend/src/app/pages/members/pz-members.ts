import { Component } from '@angular/core';
import { PzCard } from '../../shared/ui/card/pz-card';
import { PzEmptyState } from '../../shared/ui/empty-state/pz-empty-state';

@Component({
  selector: 'pz-members',
  imports: [PzCard, PzEmptyState],
  templateUrl: './pz-members.html',
  styleUrl: './pz-members.scss',
})
export class PzMembers {}
