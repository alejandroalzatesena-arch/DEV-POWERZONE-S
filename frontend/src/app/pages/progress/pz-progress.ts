import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/api/api-error';
import {
  ProgressEntry,
  ProgressSummary,
} from '../../core/progress/progress.models';
import { ProgressService } from '../../core/progress/progress.service';
import { PzBadge } from '../../shared/ui/badge/pz-badge';
import { PzCard } from '../../shared/ui/card/pz-card';
import { PzEmptyState } from '../../shared/ui/empty-state/pz-empty-state';

@Component({
  selector: 'pz-progress',
  imports: [ReactiveFormsModule, RouterLink, PzCard, PzBadge, PzEmptyState],
  templateUrl: './pz-progress.html',
  styleUrl: './pz-progress.scss',
})
export class PzProgress implements OnInit {
  private readonly progress = inject(ProgressService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly summary = signal<ProgressSummary[]>([]);
  protected readonly entries = signal<ProgressEntry[]>([]);
  protected readonly loadingSummary = signal(false);
  protected readonly loadingEntries = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly filterForm = this.fb.group({
    exerciseId: [''],
  });

  protected readonly totalSessions = computed(() =>
    this.summary().reduce((total, item) => total + item.sessions, 0),
  );

  protected readonly bestWeight = computed(() => {
    const weights = this.summary()
      .map((item) => item.maxWeightKg)
      .filter((value): value is number => typeof value === 'number');
    return weights.length === 0 ? null : Math.max(...weights);
  });

  ngOnInit(): void {
    this.loadSummary();
    this.loadEntries();
  }

  protected applyFilter(): void {
    this.loadEntries();
  }

  protected clearFilter(): void {
    this.filterForm.reset({ exerciseId: '' });
    this.loadEntries();
  }

  private loadSummary(): void {
    this.loadingSummary.set(true);
    this.progress
      .summary()
      .pipe(finalize(() => this.loadingSummary.set(false)))
      .subscribe({
        next: (summary) => this.summary.set(summary),
        error: (error: unknown) => this.errorMessage.set(ApiError.from(error).message),
      });
  }

  private loadEntries(): void {
    const raw = this.filterForm.getRawValue().exerciseId;
    const exerciseId = raw ? Number(raw) : undefined;

    this.loadingEntries.set(true);
    this.progress
      .list({ exerciseId })
      .pipe(finalize(() => this.loadingEntries.set(false)))
      .subscribe({
        next: (entries) => this.entries.set(entries),
        error: (error: unknown) => this.errorMessage.set(ApiError.from(error).message),
      });
  }
}
