import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../api/api.service';
import {
  RoutineDay,
  RoutineDetail,
  RoutineSummary,
  UpdatePrescriptionPayload,
} from './routine.models';

/** Acceso a la API de rutinas del usuario autenticado. */
@Injectable({ providedIn: 'root' })
export class RoutineService {
  private readonly api = inject(ApiService);

  list(userId?: number): Observable<RoutineSummary[]> {
    return this.api.get<RoutineSummary[]>('routines', { userId });
  }

  generate(userId?: number): Observable<RoutineDetail> {
    return this.api.post<RoutineDetail>('routines/generate', { userId: userId ?? null });
  }

  detail(routineId: number): Observable<RoutineDetail> {
    return this.api.get<RoutineDetail>(`routines/${routineId}`);
  }

  week(routineId: number): Observable<{ days: RoutineDay[] }> {
    return this.api.get<{ days: RoutineDay[] }>(`routines/${routineId}/week`);
  }

  day(routineId: number, dayNumber: number): Observable<RoutineDay> {
    return this.api.get<RoutineDay>(`routines/${routineId}/days/${dayNumber}`);
  }

  updatePrescription(
    routineId: number,
    routineExerciseId: number,
    payload: UpdatePrescriptionPayload,
  ): Observable<void> {
    return this.api.put<void>(
      `routines/${routineId}/exercises/${routineExerciseId}`,
      payload,
    );
  }

  archive(routineId: number): Observable<void> {
    return this.api.delete<void>(`routines/${routineId}`);
  }
}
