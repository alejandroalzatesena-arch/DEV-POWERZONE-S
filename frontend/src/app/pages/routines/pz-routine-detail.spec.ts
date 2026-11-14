import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ProgressSummary } from '../../core/progress/progress.models';
import { RoutineDetail, RoutineExercise } from '../../core/routines/routine.models';
import { PzRoutineDetail } from './pz-routine-detail';

const exercise: RoutineExercise = {
  id: 300,
  exerciseId: 10,
  name: 'Press banca',
  muscleGroup: 'CHEST',
  equipment: 'BARBELL',
  position: 1,
  sets: 4,
  reps: '4-6',
  restSeconds: 150,
  notes: null,
};

const detail: RoutineDetail = {
  id: 100,
  name: 'Fuerza - 2 dias',
  goal: 'STRENGTH',
  experienceLevel: 'INTERMEDIATE',
  daysPerWeek: 2,
  status: 'ACTIVE',
  source: 'GENERATED',
  createdAt: '2026-11-09T15:00:00Z',
  days: [
    { id: 200, dayNumber: 1, name: 'Tren superior A', focus: 'Empuje', exercises: [exercise] },
    { id: 201, dayNumber: 2, name: 'Tren inferior', focus: 'Pierna', exercises: [] },
  ],
};

const summary: ProgressSummary = {
  exerciseId: 10,
  exerciseName: 'Press banca',
  sessions: 3,
  maxWeightKg: 90,
  lastWeightKg: 85,
  lastPerformedOn: '2026-11-09',
};

describe('PzRoutineDetail', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PzRoutineDetail],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('carga la rutina y muestra la tabla del primer dia', () => {
    const fixture = TestBed.createComponent(PzRoutineDetail);
    fixture.componentRef.setInput('routineId', '100');

    fixture.detectChanges();
    http.expectOne('/api/v1/routines/100').flush(detail);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Fuerza - 2 dias');
    expect(text).toContain('Press banca');
    expect(text).toContain('4 x 4-6');
  });

  it('abre el panel de progreso con las series prescritas y consulta el historial', () => {
    const fixture = TestBed.createComponent(PzRoutineDetail);
    const component = fixture.componentInstance as unknown as {
      openProgress: (item: RoutineExercise) => void;
      loggingExercise: () => RoutineExercise | null;
      progressForm: { getRawValue: () => Record<string, string> };
      exerciseSummary: () => ProgressSummary | null;
    };

    component.openProgress(exercise);

    expect(component.loggingExercise()?.name).toBe('Press banca');
    expect(component.progressForm.getRawValue()['sets']).toBe('4');

    http.expectOne('/api/v1/progress/summary').flush([summary]);
    expect(component.exerciseSummary()?.sessions).toBe(3);
  });

  it('exige al menos una metrica antes de guardar', () => {
    const fixture = TestBed.createComponent(PzRoutineDetail);
    const component = fixture.componentInstance as unknown as {
      openProgress: (item: RoutineExercise) => void;
      saveProgress: () => void;
      progressForm: { patchValue: (value: Record<string, string>) => void };
      progressError: () => string;
    };

    component.openProgress(exercise);
    http.expectOne('/api/v1/progress/summary').flush([]);

    component.progressForm.patchValue({ weightKg: '', reps: '', sets: '' });
    component.saveProgress();

    expect(component.progressError()).toContain('al menos peso');
    http.expectNone('/api/v1/progress');
  });
});
