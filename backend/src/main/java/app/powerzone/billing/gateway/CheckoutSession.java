package app.powerzone.billing.gateway;

import java.time.Instant;

/** Sesion de pago creada por la pasarela. Solo expone la URL alojada. */
public record CheckoutSession(String checkoutUrl, Instant expiresAt) {
}
