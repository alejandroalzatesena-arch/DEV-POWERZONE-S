export type Role = 'MEMBER' | 'TRAINER' | 'ADMIN' | 'SUPER_ADMIN';
export type Goal = 'STRENGTH' | 'HYPERTROPHY' | 'WEIGHT_LOSS' | 'ENDURANCE';
export type ExperienceLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

/** Usuario tal como lo expone la API (sin hash de contrasena). */
export interface User {
  id: number;
  gymId: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
  role: Role;
  active: boolean;
  birthDate?: string | null;
  heightCm?: number | null;
  weightKg?: number | null;
  goal?: Goal | null;
  experienceLevel?: ExperienceLevel | null;
  daysPerWeek?: number | null;
}

/** Respuesta de login y refresh; el refresh token viaja en cookie HttpOnly. */
export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  user: User;
}

export interface LoginPayload {
  email: string;
  password: string;
  rememberMe: boolean;
}

export interface RegisterPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
  birthDate?: string | null;
  heightCm?: number | null;
  weightKg?: number | null;
  goal?: Goal | null;
  experienceLevel?: ExperienceLevel | null;
  daysPerWeek?: number | null;
}

export const GOAL_OPTIONS: { value: Goal; label: string }[] = [
  { value: 'STRENGTH', label: 'Fuerza' },
  { value: 'HYPERTROPHY', label: 'Hipertrofia' },
  { value: 'WEIGHT_LOSS', label: 'Perdida de peso' },
  { value: 'ENDURANCE', label: 'Resistencia' },
];

export const LEVEL_OPTIONS: { value: ExperienceLevel; label: string }[] = [
  { value: 'BEGINNER', label: 'Principiante' },
  { value: 'INTERMEDIATE', label: 'Intermedio' },
  { value: 'ADVANCED', label: 'Avanzado' },
];
