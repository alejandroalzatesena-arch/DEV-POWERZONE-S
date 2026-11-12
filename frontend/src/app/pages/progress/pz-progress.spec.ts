import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ProgressEntry, ProgressSummary } from '../../core/progress/progress.models';
import { PzProgress } from './pz-progress';

const summary: ProgressSummary = {
  exerciseId: 10,
  exerciseName: 'Press banca',
  sessions: 4,
  maxWeightKg: 90,
  lastWeightKg: 85,
  lastPerformedOn: '2026-11-09',
};

const entry: ProgressEntry = {
  id: 77,
  exerciseId: 10,
  exerciseName: 'Press banca',
  routineExerciseId: 300,
  performedOn: '2026-11-09',
  weightKg: 85,
  reps: 6,
  sets: 4,
  notes: 'Buena tecnica',
  createdAt: '2026-11-09T15:30:00Z',
};

describe('PzProgress', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PzProgress],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('carga el resumen y el historial al iniciar', () => {
    const fixture = TestBed.createComponent(PzProgress);

    fixture.detectChanges();
    http.expectOne('/api/v1/progress/summary').flush([summary]);
    http.expectOne('/api/v1/progress').flush([entry]);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Press banca');
    expect(text).toContain('Buena tecnica');
    expect(text).toContain('90');
  });

  it('vuelve a consultar el historial al aplicar el filtro por ejercicio', () => {
    const fixture = TestBed.createComponent(PzProgress);
    const component = fixture.componentInstance as unknown as {
      filterForm: { patchValue: (value: Record<string, string>) => void };
      applyFilter: () => void;
      clearFilter: () => void;
    };

    fixture.detectChanges();
    http.expectOne('/api/v1/progress/summary').flush([summary]);
    http.expectOne('/api/v1/progress').flush([entry]);

    component.filterForm.patchValue({ exerciseId: '10' });
    component.applyFilter();
    http.expectOne('/api/v1/progress?exerciseId=10').flush([entry]);

    component.clearFilter();
    http.expectOne('/api/v1/progress').flush([entry]);
  });

  it('muestra el estado vacio cuando no hay registros', () => {
    const fixture = TestBed.createComponent(PzProgress);

    fixture.detectChanges();
    http.expectOne('/api/v1/progress/summary').flush([]);
    http.expectOne('/api/v1/progress').flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aun no hay progreso');
  });
});
