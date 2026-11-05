import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { ApiError } from '../../core/api/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { ProgressEntry, ProgressSummary } from '../../core/progress/progress.models';
import { ProgressService } from '../../core/progress/progress.service';
import {
  EXPERIENCE_LABELS,
  GOAL_LABELS,
  RoutineDetail,
  RoutineSummary,
} from '../../core/routines/routine.models';
import { RoutineService } from '../../core/routines/routine.service';
import { PzBadge } from '../../shared/ui/badge/pz-badge';
import { PzButton } from '../../shared/ui/button/pz-button';
import { PzCard } from '../../shared/ui/card/pz-card';
import { PzEmptyState } from '../../shared/ui/empty-state/pz-empty-state';

interface DashboardData {
  routines: RoutineSummary[];
  summary: ProgressSummary[];
  entries: ProgressEntry[];
}

interface DashboardStat {
  label: string;
  value: string;
  detail: string;
}

@Component({
  selector: 'pz-dashboard',
  imports: [RouterLink, PzCard, PzBadge, PzButton, PzEmptyState],
  templateUrl: './pz-dashboard.html',
  styleUrl: './pz-dashboard.scss',
})
export class PzDashboard implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly routinesApi = inject(RoutineService);
  private readonly progressApi = inject(ProgressService);
  private readonly router = inject(Router);

  protected readonly goalLabels = GOAL_LABELS;
  protected readonly experienceLabels = EXPERIENCE_LABELS;

  protected readonly user = this.auth.user;
  protected readonly routines = signal<RoutineSummary[]>([]);
  protected readonly summary = signal<ProgressSummary[]>([]);
  protected readonly entries = signal<ProgressEntry[]>([]);
  protected readonly routineDetail = signal<RoutineDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadingDetail = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly greetingName = computed(() => this.user()?.firstName ?? 'atleta');

  protected readonly activeRoutines = computed(() =>
    this.routines().filter((routine) => routine.status === 'ACTIVE'),
  );

  protected readonly activeRoutine = computed(() => this.activeRoutines()[0] ?? null);

  protected readonly nextDay = computed(() => this.routineDetail()?.days[0] ?? null);

  protected readonly totalSessions = computed(() =>
    this.summary().reduce((total, item) => total + item.sessions, 0),
  );

  protected readonly bestWeight = computed(() => {
    const weights = this.summary()
      .map((item) => item.maxWeightKg)
      .filter((value): value is number => typeof value === 'number');
    return weights.length === 0 ? null : Math.max(...weights);
  });

  protected readonly bestExercise = computed(() => {
    const best = this.bestWeight();
    if (best === null) {
      return null;
    }
    return this.summary().find((item) => item.maxWeightKg === best)?.exerciseName ?? null;
  });

  protected readonly recentEntries = computed(() =>
    [...this.entries()]
      .sort((a, b) => b.performedOn.localeCompare(a.performedOn) || b.id - a.id)
      .slice(0, 4),
  );

  protected readonly stats = computed<DashboardStat[]>(() => {
    const routine = this.activeRoutine();
    const profileDays = this.user()?.daysPerWeek;
    const best = this.bestWeight();

    return [
      {
        label: 'Rutinas activas',
        value: String(this.activeRoutines().length),
        detail: `${this.routines().length} en total`,
      },
      {
        label: 'Dias por semana',
        value: routine ? String(routine.daysPerWeek) : profileDays ? String(profileDays) : '—',
        detail: routine ? 'Segun tu plan activo' : 'Segun tu perfil',
      },
      {
        label: 'Sesiones registradas',
        value: String(this.totalSessions()),
        detail: `${this.summary().length} ejercicios con historial`,
      },
      {
        label: 'Mejor carga',
        value: best !== null ? `${best} kg` : '—',
        detail: this.bestExercise() ?? 'Sin registros todavia',
      },
    ];
  });

  ngOnInit(): void {
    this.load();
  }

  protected goToRoutines(): void {
    void this.router.navigate(['/app/routines']);
  }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    forkJoin({
      routines: this.routinesApi.list(),
      summary: this.progressApi.summary(),
      entries: this.progressApi.list(),
    })
      .pipe(
        catchError((error: unknown) => {
          this.errorMessage.set(ApiError.from(error).message);
          return of<DashboardData>({ routines: [], summary: [], entries: [] });
        }),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((data) => {
        this.routines.set(data.routines);
        this.summary.set(data.summary);
        this.entries.set(data.entries);

        const active = data.routines.find((routine) => routine.status === 'ACTIVE');
        if (active) {
          this.loadDetail(active.id);
        }
      });
  }

  private loadDetail(routineId: number): void {
    this.loadingDetail.set(true);
    this.routinesApi
      .detail(routineId)
      .pipe(finalize(() => this.loadingDetail.set(false)))
      .subscribe({
        next: (detail) => this.routineDetail.set(detail),
        error: () => this.routineDetail.set(null),
      });
  }
}
