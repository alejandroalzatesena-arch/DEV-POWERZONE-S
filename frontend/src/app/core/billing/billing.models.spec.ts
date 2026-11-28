import { describe, expect, it } from 'vitest';
import {
  MEMBERSHIP_STATE_LABELS,
  PAYMENT_STATUS_LABELS,
  PAYMENT_STATUS_TONES,
  formatDate,
  formatDateTime,
  formatMoney,
  planDurationLabel,
} from './billing.models';

describe('billing.models', () => {
  it('traduce los estados de pago y sus tonos', () => {
    expect(PAYMENT_STATUS_LABELS.COMPLETED).toBe('Pagado');
    expect(PAYMENT_STATUS_LABELS.FAILED).toBe('Rechazado');
    expect(PAYMENT_STATUS_TONES.COMPLETED).toBe('success');
    expect(PAYMENT_STATUS_TONES.FAILED).toBe('danger');
    expect(PAYMENT_STATUS_TONES.PENDING).toBe('warning');
  });

  it('traduce los estados de membresia', () => {
    expect(MEMBERSHIP_STATE_LABELS.NONE).toBe('Sin membresia');
    expect(MEMBERSHIP_STATE_LABELS.EXPIRING).toBe('Por vencer');
    expect(MEMBERSHIP_STATE_LABELS.EXPIRED).toBe('Vencida');
  });

  it('formatea centavos como moneda colombiana sin decimales', () => {
    const formatted = formatMoney(8_990_000, 'COP');

    expect(formatted).toContain('89.900');
    expect(formatted).not.toContain(',00');
  });

  it('cae en un formato simple cuando la moneda no es valida', () => {
    expect(formatMoney(1_250, 'INVALID')).toBe('12.50 INVALID');
  });

  it('formatea fechas cortas sin desfase de zona horaria', () => {
    expect(formatDate('2026-11-20')).toContain('2026');
    expect(formatDate(null)).toBe('—');
    expect(formatDate('')).toBe('—');
  });

  it('formatea fecha y hora', () => {
    const formatted = formatDateTime('2026-11-20T15:30:00Z');

    expect(formatted).toContain('2026');
    expect(formatDateTime(undefined)).toBe('—');
  });

  it('describe la duracion de un plan', () => {
    expect(planDurationLabel(30)).toBe('1 mes(es)');
    expect(planDurationLabel(90)).toBe('3 mes(es)');
    expect(planDurationLabel(365)).toBe('1 ano(s)');
    expect(planDurationLabel(45)).toBe('45 dias');
  });
});
