export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'EXPIRED' | 'REFUNDED';
export type PaymentMethodKind = 'CARD' | 'WALLET' | 'TRANSFER';
export type MembershipState = 'NONE' | 'ACTIVE' | 'EXPIRING' | 'EXPIRED';
export type BillingTone = 'neutral' | 'success' | 'warning' | 'danger' | 'info';

/** Plan de membresia que vende el gimnasio. Los montos viajan en centavos. */
export interface BillingPlan {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  priceCents: number;
  currency: string;
  durationDays: number;
}

/** Metodo de pago habilitado para el gimnasio del usuario. */
export interface PaymentMethodOption {
  code: string;
  label: string;
  kind: PaymentMethodKind;
  immediate: boolean;
}

export interface CheckoutPayload {
  planId: number;
  methodCode: string;
}

/** Respuesta al abrir una sesion de pago alojada. */
export interface CheckoutResult {
  paymentId: number;
  reference: string;
  status: PaymentStatus;
  checkoutUrl?: string | null;
  amountCents: number;
  currency: string;
  planName: string;
  expiresAt: string;
}

export interface Payment {
  id: number;
  planId: number;
  planName: string;
  amountCents: number;
  currency: string;
  methodCode: string;
  methodLabel: string;
  status: PaymentStatus;
  reference: string;
  checkoutUrl?: string | null;
  failureCode?: string | null;
  failureMessage?: string | null;
  attempt: number;
  expiresAt: string;
  completedAt?: string | null;
  createdAt: string;
}

export interface PageResult<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface MembershipStatus {
  state: MembershipState;
  planName?: string | null;
  startsAt?: string | null;
  endsAt?: string | null;
  daysRemaining: number;
  expiringThresholdDays: number;
}

export interface PaymentQuery {
  from?: string;
  to?: string;
  status?: PaymentStatus | '';
  page?: number;
  size?: number;
}

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: 'Pendiente',
  COMPLETED: 'Pagado',
  FAILED: 'Rechazado',
  EXPIRED: 'Expirado',
  REFUNDED: 'Reembolsado',
};

export const PAYMENT_STATUS_TONES: Record<PaymentStatus, BillingTone> = {
  PENDING: 'warning',
  COMPLETED: 'success',
  FAILED: 'danger',
  EXPIRED: 'neutral',
  REFUNDED: 'info',
};

export const MEMBERSHIP_STATE_LABELS: Record<MembershipState, string> = {
  NONE: 'Sin membresia',
  ACTIVE: 'Activa',
  EXPIRING: 'Por vencer',
  EXPIRED: 'Vencida',
};

export const MEMBERSHIP_STATE_TONES: Record<MembershipState, BillingTone> = {
  NONE: 'neutral',
  ACTIVE: 'success',
  EXPIRING: 'warning',
  EXPIRED: 'danger',
};

export const PAYMENT_METHOD_KIND_LABELS: Record<PaymentMethodKind, string> = {
  CARD: 'Tarjeta',
  WALLET: 'Billetera digital',
  TRANSFER: 'Transferencia',
};

/** Traduce los centavos de la API al formato monetario colombiano. */
export function formatMoney(amountCents: number, currency: string): string {
  const amount = amountCents / 100;
  try {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency,
      maximumFractionDigits: 0,
    }).format(amount);
  } catch {
    return `${amount.toFixed(2)} ${currency}`;
  }
}

/** Formatea una fecha ISO o `YYYY-MM-DD` sin depender de la zona del servidor. */
export function formatDate(value?: string | null): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value.length <= 10 ? `${value}T00:00:00` : value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('es-CO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date);
}

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('es-CO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function planDurationLabel(days: number): string {
  if (days >= 365) {
    return days % 365 === 0 ? `${days / 365} ano(s)` : `${days} dias`;
  }
  if (days % 30 === 0) {
    return `${days / 30} mes(es)`;
  }
  return `${days} dias`;
}
