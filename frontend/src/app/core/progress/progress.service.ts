import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../api/api.service';
import {
  ProgressEntry,
  ProgressQuery,
  ProgressSummary,
  RegisterProgressPayload,
} from './progress.models';

/** Historial y resumen de cargas del usuario autenticado. */
@Injectable({ providedIn: 'root' })
export class ProgressService {
  private readonly api = inject(ApiService);

  register(payload: RegisterProgressPayload): Observable<ProgressEntry> {
    return this.api.post<ProgressEntry>('progress', payload);
  }

  list(query: ProgressQuery = {}): Observable<ProgressEntry[]> {
    return this.api.get<ProgressEntry[]>('progress', {
      exerciseId: query.exerciseId,
      from: query.from,
      to: query.to,
    });
  }

  summary(): Observable<ProgressSummary[]> {
    return this.api.get<ProgressSummary[]>('progress/summary');
  }
}
