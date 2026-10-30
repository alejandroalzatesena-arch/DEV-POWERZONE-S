import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { PzRegister } from './pz-register';

describe('PzRegister', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PzRegister],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('empieza invalido porque los campos obligatorios estan vacios', () => {
    const fixture = TestBed.createComponent(PzRegister);
    const component = fixture.componentInstance as unknown as { form: { invalid: boolean } };

    expect(component.form.invalid).toBe(true);
  });

  it('rechaza contrasenas distintas', () => {
    const fixture = TestBed.createComponent(PzRegister);
    const component = fixture.componentInstance as unknown as {
      form: {
        setValue: (value: Record<string, string>) => void;
        hasError: (error: string) => boolean;
      };
    };

    component.form.setValue({
      firstName: 'Ana',
      lastName: 'Gomez',
      email: 'ana@powerzone.app',
      password: 'Secreta123',
      confirmPassword: 'OtraClave123',
      phone: '',
      birthDate: '',
      heightCm: '',
      weightKg: '',
      goal: '',
      experienceLevel: '',
      daysPerWeek: '',
    });

    expect(component.form.hasError('passwordsMismatch')).toBe(true);
  });

  it('acepta un formulario completo y coherente', () => {
    const fixture = TestBed.createComponent(PzRegister);
    const component = fixture.componentInstance as unknown as {
      form: {
        setValue: (value: Record<string, string>) => void;
        valid: boolean;
      };
    };

    component.form.setValue({
      firstName: 'Ana',
      lastName: 'Gomez',
      email: 'ana@powerzone.app',
      password: 'Secreta123',
      confirmPassword: 'Secreta123',
      phone: '3001234567',
      birthDate: '1998-03-14',
      heightCm: '168',
      weightKg: '62',
      goal: 'STRENGTH',
      experienceLevel: 'INTERMEDIATE',
      daysPerWeek: '4',
    });

    expect(component.form.valid).toBe(true);
  });
});
