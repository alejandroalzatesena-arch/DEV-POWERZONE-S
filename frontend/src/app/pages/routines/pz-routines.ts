import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, switchMap } from 'rxjs';
import { ApiError } from '../../core/api/api-error';
import { ExperienceLevel, GOAL_OPTIONS, Goal, LEVEL_OPTIONS } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import {
  EXPERIENCE_LABELS,
  GOAL_LABELS,
  RoutineSummary,
} from '../../core/routines/routine.models';
import { RoutineService } from '../../core/routines/routine.service';
import { ToastService } from '../../core/ui/toast.service';
import { PzBadge } from '../../shared/ui/badge/pz-badge';
import { PzButton } from '../../shared/ui/button/pz-button';
import { PzCard } from '../../shared/ui/card/pz-card';
import { PzEmptyState } from '../../shared/ui/empty-state/pz-empty-state';

@Component({
  selector: 'pz-routines',
  imports: [ReactiveFormsModule, RouterLink, PzCard, PzButton, PzBadge, PzEmptyState],
  templateUrl: './pz-routines.html',
  styleUrl: './pz-routines.scss',
})
export class PzRoutines implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly routines = inject(RoutineService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly goalOptions = GOAL_OPTIONS;
  protected readonly levelOptions = LEVEL_OPTIONS;
  protected readonly goalLabels = GOAL_LABELS;
  protected readonly experienceLabels = EXPERIENCE_LABELS;

  protected readonly routinesList = signal<RoutineSummary[]>([]);
  protected readonly loading = signal(false);
  protected readonly generating = signal(false);
  protected readonly submitted = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly form = this.fb.group({
    goal: ['', [Validators.required]],
    experienceLevel: ['', [Validators.required]],
    daysPerWeek: [4, [Validators.required, Validators.min(2), Validators.max(7)]],
  });

  ngOnInit(): void {
    const user = this.auth.user();
    this.form.patchValue({
      goal: user?.goal ?? '',
      experienceLevel: user?.experienceLevel ?? '',
      daysPerWeek: user?.daysPerWeek ?? 4,
    });
    this.load();
  }

  protected invalid(control: 'goal' | 'experienceLevel' | 'daysPerWeek'): boolean {
    const field = this.form.controls[control];
    return field.invalid && (field.dirty || field.touched || this.submitted());
  }

  protected generate(): void {
    this.submitted.set(true);
    this.errorMessage.set('');

    if (this.form.invalid || this.generating()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.generating.set(true);

    this.auth
      .updateProfile({
        goal: value.goal as Goal,
        experienceLevel: value.experienceLevel as ExperienceLevel,
        daysPerWeek: value.daysPerWeek,
      })
      .pipe(
        switchMap(() => this.routines.generate()),
        finalize(() => this.generating.set(false)),
      )
      .subscribe({
        next: (routine) => {
          this.toast.success('Rutina generada con tu perfil actualizado.');
          void this.router.navigate(['/app/routines', routine.id]);
        },
        error: (error: unknown) => {
          const apiError = ApiError.from(error);
          this.errorMessage.set(
            apiError.code === 'PROFILE_INCOMPLETE'
              ? 'Completa tu objetivo, experiencia y dias disponibles para generar la rutina.'
              : apiError.message,
          );
        },
      });
  }

  private load(): void {
    this.loading.set(true);
    this.routines
      .list()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (routines) => this.routinesList.set(routines),
        error: (error: unknown) => this.errorMessage.set(ApiError.from(error).message),
      });
  }
}
