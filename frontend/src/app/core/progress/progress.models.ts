export interface ProgressEntry {
  id: number;
  exerciseId: number;
  exerciseName: string;
  routineExerciseId?: number | null;
  performedOn: string;
  weightKg?: number | null;
  reps?: number | null;
  sets?: number | null;
  notes?: string | null;
  createdAt: string;
}

export interface ProgressSummary {
  exerciseId: number;
  exerciseName: string;
  sessions: number;
  maxWeightKg?: number | null;
  lastWeightKg?: number | null;
  lastPerformedOn: string;
}

export interface RegisterProgressPayload {
  exerciseId: number;
  routineExerciseId?: number | null;
  performedOn?: string | null;
  weightKg?: number | null;
  reps?: number | null;
  sets?: number | null;
  notes?: string | null;
}

export interface ProgressQuery {
  exerciseId?: number;
  from?: string;
  to?: string;
}
