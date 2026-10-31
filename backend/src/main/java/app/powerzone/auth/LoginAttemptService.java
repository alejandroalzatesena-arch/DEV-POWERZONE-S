package app.powerzone.auth;

import app.powerzone.common.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Bloqueo temporal por intentos fallidos. Ventana deslizante en memoria por
 * email; si el servicio escala horizontalmente se reemplaza por Redis.
 */
@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public void assertNotLocked(String email) {
        Deque<Instant> attempts = failures.get(key(email));
        if (attempts == null) {
            return;
        }
        synchronized (attempts) {
            prune(attempts);
            if (attempts.size() >= MAX_ATTEMPTS) {
                throw ApiException.tooManyRequests(
                        "ACCOUNT_LOCKED",
                        "Demasiados intentos fallidos. Intenta de nuevo en 15 minutos.");
            }
        }
    }

    public void recordFailure(String email) {
        Deque<Instant> attempts = failures.computeIfAbsent(key(email), k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts);
            attempts.addLast(Instant.now());
        }
    }

    public void clear(String email) {
        failures.remove(key(email));
    }

    /** Descarta las ventanas sin intentos recientes para liberar memoria. */
    public void purgeExpired() {
        failures.entrySet().removeIf(entry -> {
            Deque<Instant> attempts = entry.getValue();
            synchronized (attempts) {
                prune(attempts);
                return attempts.isEmpty();
            }
        });
    }

    private void prune(Deque<Instant> attempts) {
        Instant limit = Instant.now().minus(WINDOW);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(limit)) {
            attempts.pollFirst();
        }
    }

    private String key(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
