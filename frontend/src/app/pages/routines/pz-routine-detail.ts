import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/api/api-error';
import {
  EXPERIENCE_LABELS,
  GOAL_LABELS,
  RoutineDetail,
  equipmentLabel,
  muscleLabel,
} from '../../core/routines/routine.models';
import { RoutineService } from '../../core/routines/routine.service';
import { PzBadge } from '../../shared/ui/badge/pz-badge';
import { PzCard } from '../../shared/ui/card/pz-card';
import { PzEmptyState } from '../../shared/ui/empty-state/pz-empty-state';

@Component({
  selector: 'pz-routine-detail',
  imports: [RouterLink, PzCard, PzBadge, PzEmptyState],
  templateUrl: './pz-routine-detail.html',
  styleUrl: './pz-routine-detail.scss',
})
export class PzRoutineDetail implements OnInit {
  /** Parametro de ruta enlazado con `withComponentInputBinding`. */
  readonly routineId = input.required<string>();

  private readonly routines = inject(RoutineService);

  protected readonly detail = signal<RoutineDetail | null>(null);
  protected readonly selectedDay = signal(1);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly goalLabels = GOAL_LABELS;
  protected readonly experienceLabels = EXPERIENCE_LABELS;
  protected readonly muscleLabel = muscleLabel;
  protected readonly equipmentLabel = equipmentLabel;

  protected readonly activeDay = computed(() => {
    const routine = this.detail();
    if (!routine) {
      return null;
    }
    return (
      routine.days.find((day) => day.dayNumber === this.selectedDay()) ??
      routine.days[0] ??
      null
    );
  });

  ngOnInit(): void {
    this.load();
  }

  protected selectDay(dayNumber: number): void {
    this.selectedDay.set(dayNumber);
  }

  private load(): void {
    this.loading.set(true);
    this.routines
      .detail(Number(this.routineId()))
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (routine) => {
          this.detail.set(routine);
          this.selectedDay.set(routine.days[0]?.dayNumber ?? 1);
        },
        error: (error: unknown) => this.errorMessage.set(ApiError.from(error).message),
      });
  }
}
