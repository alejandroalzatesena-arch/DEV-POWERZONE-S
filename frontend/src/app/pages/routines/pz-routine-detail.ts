import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/api/api-error';
import {
  EXPERIENCE_LABELS,
  GOAL_LABELS,
  RoutineDetail,
  RoutineExercise,
  equipmentLabel,
  muscleLabel,
} from '../../core/routines/routine.models';
import { RoutineService } from '../../core/routines/routine.service';
import { ProgressService } from '../../core/progress/progress.service';
import { ProgressSummary } from '../../core/progress/progress.models';
import { ToastService } from '../../core/ui/toast.service';
import { PzBadge } from '../../shared/ui/badge/pz-badge';
import { PzButton } from '../../shared/ui/button/pz-button';
import { PzCard } from '../../shared/ui/card/pz-card';
import { PzEmptyState } from '../../shared/ui/empty-state/pz-empty-state';

@Component({
  selector: 'pz-routine-detail',
  imports: [ReactiveFormsModule, RouterLink, PzCard, PzButton, PzBadge, PzEmptyState],
  templateUrl: './pz-routine-detail.html',
  styleUrl: './pz-routine-detail.scss',
})
export class PzRoutineDetail implements OnInit {
  /** Parametro de ruta enlazado con `withComponentInputBinding`. */
  readonly routineId = input.required<string>();

  private readonly routines = inject(RoutineService);
  private readonly progress = inject(ProgressService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly detail = signal<RoutineDetail | null>(null);
  protected readonly selectedDay = signal(1);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly goalLabels = GOAL_LABELS;
  protected readonly experienceLabels = EXPERIENCE_LABELS;
  protected readonly muscleLabel = muscleLabel;
  protected readonly equipmentLabel = equipmentLabel;

  protected readonly loggingExercise = signal<RoutineExercise | null>(null);
  protected readonly savingProgress = signal(false);
  protected readonly progressError = signal('');
  protected readonly exerciseSummary = signal<ProgressSummary | null>(null);

  protected readonly progressForm = this.fb.group({
    performedOn: [today(), [Validators.required]],
    weightKg: [''],
    reps: [''],
    sets: [''],
    notes: [''],
  });

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

  protected readonly weekVolume = computed(() => {
    const routine = this.detail();
    if (!routine) {
      return 0;
    }
    return routine.days.reduce((total, day) => total + day.exercises.length, 0);
  });

  ngOnInit(): void {
    this.load();
  }

  protected selectDay(dayNumber: number): void {
    this.selectedDay.set(dayNumber);
    this.closeProgress();
  }

  protected openProgress(exercise: RoutineExercise): void {
    this.loggingExercise.set(exercise);
    this.progressError.set('');
    this.exerciseSummary.set(null);
    this.progressForm.reset({
      performedOn: today(),
      weightKg: '',
      reps: '',
      sets: String(exercise.sets),
      notes: '',
    });
    this.loadSummary(exercise.exerciseId);
  }

  protected closeProgress(): void {
    this.loggingExercise.set(null);
    this.progressError.set('');
  }

  protected saveProgress(): void {
    const exercise = this.loggingExercise();
    if (!exercise || this.savingProgress()) {
      return;
    }

    const value = this.progressForm.getRawValue();
    const weightKg = toNumber(value.weightKg);
    const reps = toNumber(value.reps);
    const sets = toNumber(value.sets);

    if (weightKg === null && reps === null && sets === null) {
      this.progressError.set('Registra al menos peso, repeticiones o series.');
      return;
    }

    this.progressError.set('');
    this.savingProgress.set(true);

    this.progress
      .register({
        exerciseId: exercise.exerciseId,
        routineExerciseId: exercise.id,
        performedOn: value.performedOn || null,
        weightKg,
        reps,
        sets,
        notes: value.notes.trim() || null,
      })
      .pipe(finalize(() => this.savingProgress.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Progreso registrado.');
          this.closeProgress();
          this.loadSummary(exercise.exerciseId);
        },
        error: (error: unknown) => this.progressError.set(ApiError.from(error).message),
      });
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

  private loadSummary(exerciseId: number): void {
    this.progress.summary().subscribe({
      next: (summaries) => {
        this.exerciseSummary.set(
          summaries.find((summary) => summary.exerciseId === exerciseId) ?? null,
        );
      },
      error: () => this.exerciseSummary.set(null),
    });
  }
}

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

function toNumber(value: string): number | null {
  const trimmed = `${value ?? ''}`.trim();
  if (trimmed === '') {
    return null;
  }
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : null;
}
