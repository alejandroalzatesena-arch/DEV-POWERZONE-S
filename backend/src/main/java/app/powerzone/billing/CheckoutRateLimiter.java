package app.powerzone.billing;

import app.powerzone.common.ApiException;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Limite de intentos de checkout por usuario. Complementa el rate limit
 * global por IP y reduce el abuso de la pasarela.
 */
@Component
public class CheckoutRateLimiter {

    private static final long WINDOW_MILLIS = 60_000;

    private final Map<Long, Deque<Long>> attempts = new ConcurrentHashMap<>();
    private final int limit;
    private final Clock clock;

    public CheckoutRateLimiter(@Value("${powerzone.payments.checkout-limit:5}") int limit,
                               Clock clock) {
        this.limit = limit;
        this.clock = clock;
    }

    public void check(long userId) {
        long now = clock.millis();
        Deque<Long> hits = attempts.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && now - hits.peekFirst() > WINDOW_MILLIS) {
                hits.pollFirst();
            }
            if (hits.size() >= limit) {
                throw ApiException.tooManyRequests(
                        "CHECKOUT_RATE_LIMITED",
                        "Demasiados intentos de pago seguidos. Espera un minuto e intenta de nuevo.");
            }
            hits.addLast(now);
        }
    }

    /** Solo para pruebas: limpia las ventanas en memoria. */
    public void reset() {
        attempts.clear();
    }
}
