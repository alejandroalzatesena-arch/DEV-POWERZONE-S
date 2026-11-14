import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { RoutineDetail, RoutineSummary } from './routine.models';
import { RoutineService } from './routine.service';

const summary: RoutineSummary = {
  id: 100,
  name: 'Fuerza - 4 dias',
  goal: 'STRENGTH',
  experienceLevel: 'INTERMEDIATE',
  daysPerWeek: 4,
  status: 'ACTIVE',
  source: 'GENERATED',
  createdAt: '2026-11-09T15:00:00Z',
};

const detail: RoutineDetail = {
  ...summary,
  days: [
    {
      id: 200,
      dayNumber: 1,
      name: 'Tren superior A',
      focus: 'Empuje',
      exercises: [
        {
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
        },
      ],
    },
  ],
};

describe('RoutineService', () => {
  let service: RoutineService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RoutineService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista las rutinas del usuario autenticado', () => {
    let result: RoutineSummary[] | undefined;

    service.list().subscribe((routines) => (result = routines));

    const request = http.expectOne('/api/v1/routines');
    expect(request.request.method).toBe('GET');
    request.flush([summary]);

    expect(result).toHaveLength(1);
    expect(result?.[0].name).toBe('Fuerza - 4 dias');
  });

  it('agrega el usuario como parametro cuando el personal consulta a un miembro', () => {
    service.list(8).subscribe();

    const request = http.expectOne('/api/v1/routines?userId=8');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('genera una rutina enviando el usuario opcional', () => {
    let result: RoutineDetail | undefined;

    service.generate().subscribe((routine) => (result = routine));

    const request = http.expectOne('/api/v1/routines/generate');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ userId: null });
    request.flush(detail);

    expect(result?.id).toBe(100);
  });

  it('consulta el detalle de una rutina', () => {
    service.detail(100).subscribe();

    const request = http.expectOne('/api/v1/routines/100');
    expect(request.request.method).toBe('GET');
    request.flush(detail);
  });

  it('consulta la semana y un dia concreto', () => {
    service.week(100).subscribe();
    http.expectOne('/api/v1/routines/100/week').flush({ days: detail.days });

    service.day(100, 1).subscribe();
    http.expectOne('/api/v1/routines/100/days/1').flush(detail.days[0]);
  });

  it('ajusta la prescripcion de un ejercicio', () => {
    service
      .updatePrescription(100, 300, { sets: 5, reps: '6-8', restSeconds: 120, notes: null })
      .subscribe();

    const request = http.expectOne('/api/v1/routines/100/exercises/300');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.sets).toBe(5);
    request.flush(null);
  });

  it('archiva una rutina', () => {
    service.archive(100).subscribe();

    const request = http.expectOne('/api/v1/routines/100');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
