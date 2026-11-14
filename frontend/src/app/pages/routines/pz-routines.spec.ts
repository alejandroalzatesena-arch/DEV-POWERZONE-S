import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { RoutineSummary } from '../../core/routines/routine.models';
import { PzRoutines } from './pz-routines';

const routine: RoutineSummary = {
  id: 100,
  name: 'Fuerza - 4 dias',
  goal: 'STRENGTH',
  experienceLevel: 'INTERMEDIATE',
  daysPerWeek: 4,
  status: 'ACTIVE',
  source: 'GENERATED',
  createdAt: '2026-11-09T15:00:00Z',
};

describe('PzRoutines', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PzRoutines],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('empieza invalido cuando el perfil no tiene objetivo ni experiencia', () => {
    const fixture = TestBed.createComponent(PzRoutines);
    const component = fixture.componentInstance as unknown as {
      form: { invalid: boolean };
    };

    expect(component.form.invalid).toBe(true);
  });

  it('acepta un formulario con objetivo, experiencia y dias validos', () => {
    const fixture = TestBed.createComponent(PzRoutines);
    const component = fixture.componentInstance as unknown as {
      form: { patchValue: (value: Record<string, string | number>) => void; valid: boolean };
    };

    component.form.patchValue({
      goal: 'HYPERTROPHY',
      experienceLevel: 'BEGINNER',
      daysPerWeek: 5,
    });

    expect(component.form.valid).toBe(true);
  });

  it('carga y muestra el listado de rutinas', () => {
    const fixture = TestBed.createComponent(PzRoutines);

    fixture.detectChanges();
    const request = http.expectOne('/api/v1/routines');
    expect(request.request.method).toBe('GET');
    request.flush([routine]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Fuerza - 4 dias');
    expect(fixture.nativeElement.textContent).toContain('4 dias');
  });

  it('muestra el estado vacio cuando no hay rutinas', () => {
    const fixture = TestBed.createComponent(PzRoutines);

    fixture.detectChanges();
    http.expectOne('/api/v1/routines').flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aun no tienes rutinas');
  });
});
