import { ExperienceLevel, Goal } from '../auth/auth.models';

export type RoutineStatus = 'ACTIVE' | 'ARCHIVED';
export type RoutineSource = 'GENERATED' | 'MANUAL';

export interface RoutineSummary {
  id: number;
  name: string;
  goal: Goal;
  experienceLevel: ExperienceLevel;
  daysPerWeek: number;
  status: RoutineStatus;
  source: RoutineSource;
  createdAt: string;
}

export interface RoutineExercise {
  id: number;
  exerciseId: number;
  name: string;
  muscleGroup: string;
  equipment: string;
  position: number;
  sets: number;
  reps: string;
  restSeconds: number;
  notes?: string | null;
}

export interface RoutineDay {
  id: number;
  dayNumber: number;
  name: string;
  focus: string;
  exercises: RoutineExercise[];
}

export interface RoutineDetail {
  id: number;
  name: string;
  goal: Goal;
  experienceLevel: ExperienceLevel;
  daysPerWeek: number;
  status: RoutineStatus;
  source: RoutineSource;
  createdAt: string;
  days: RoutineDay[];
}

export interface UpdatePrescriptionPayload {
  sets: number;
  reps: string;
  restSeconds: number;
  notes?: string | null;
}

export const GOAL_LABELS: Record<Goal, string> = {
  STRENGTH: 'Fuerza',
  HYPERTROPHY: 'Hipertrofia',
  WEIGHT_LOSS: 'Perdida de peso',
  ENDURANCE: 'Resistencia',
};

export const EXPERIENCE_LABELS: Record<ExperienceLevel, string> = {
  BEGINNER: 'Principiante',
  INTERMEDIATE: 'Intermedio',
  ADVANCED: 'Avanzado',
};

export const MUSCLE_LABELS: Record<string, string> = {
  CHEST: 'Pecho',
  BACK: 'Espalda',
  LEGS: 'Pierna',
  SHOULDERS: 'Hombro',
  ARMS: 'Brazo',
  CORE: 'Core',
  FULL_BODY: 'Cuerpo completo',
  CARDIO: 'Cardio',
};

export const EQUIPMENT_LABELS: Record<string, string> = {
  BARBELL: 'Barra',
  DUMBBELL: 'Mancuernas',
  CABLE: 'Polea',
  MACHINE: 'Maquina',
  BODYWEIGHT: 'Peso corporal',
  KETTLEBELL: 'Kettlebell',
};

export function muscleLabel(value: string): string {
  return MUSCLE_LABELS[value] ?? value;
}

export function equipmentLabel(value: string): string {
  return EQUIPMENT_LABELS[value] ?? value;
}
