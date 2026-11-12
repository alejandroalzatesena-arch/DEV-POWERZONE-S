import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ProgressEntry, ProgressSummary } from './progress.models';
import { ProgressService } from './progress.service';

const entry: ProgressEntry = {
  id: 77,
  exerciseId: 10,
  exerciseName: 'Press banca',
  routineExerciseId: 300,
  performedOn: '2026-11-09',
  weightKg: 82,
  reps: 6,
  sets: 4,
  notes: 'Buena tecnica',
  createdAt: '2026-11-09T15:30:00Z',
};

describe('ProgressService', () => {
  let service: ProgressService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProgressService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('registra una entrada de progreso', () => {
    let result: ProgressEntry | undefined;

    service
      .register({
        exerciseId: 10,
        routineExerciseId: 300,
        weightKg: 82,
        reps: 6,
        sets: 4,
      })
      .subscribe((created) => (result = created));

    const request = http.expectOne('/api/v1/progress');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.exerciseId).toBe(10);
    request.flush(entry);

    expect(result?.id).toBe(77);
  });

  it('consulta el historial sin filtros', () => {
    service.list().subscribe();

    const request = http.expectOne('/api/v1/progress');
    expect(request.request.method).toBe('GET');
    request.flush([entry]);
  });

  it('serializa los filtros del historial', () => {
    service.list({ exerciseId: 10, from: '2026-11-01', to: '2026-11-30' }).subscribe();

    const request = http.expectOne(
      '/api/v1/progress?exerciseId=10&from=2026-11-01&to=2026-11-30',
    );
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('consulta el resumen agregado', () => {
    const summary: ProgressSummary = {
      exerciseId: 10,
      exerciseName: 'Press banca',
      sessions: 4,
      maxWeightKg: 90,
      lastWeightKg: 85,
      lastPerformedOn: '2026-11-09',
    };
    let result: ProgressSummary[] | undefined;

    service.summary().subscribe((items) => (result = items));

    const request = http.expectOne('/api/v1/progress/summary');
    expect(request.request.method).toBe('GET');
    request.flush([summary]);

    expect(result?.[0].maxWeightKg).toBe(90);
  });
});
