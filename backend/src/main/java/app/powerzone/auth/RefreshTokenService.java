package app.powerzone.auth;

import app.powerzone.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Emite refresh tokens opacos y los persiste como hash SHA-256. El valor en
 * claro solo viaja al cliente en una cookie HttpOnly y nunca se almacena.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final long refreshSeconds;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${powerzone.jwt.refresh-seconds:604800}") long refreshSeconds) {
        this.repository = repository;
        this.refreshSeconds = refreshSeconds;
    }

    public IssuedRefreshToken issue(long userId) {
        return issue(userId, Duration.ofSeconds(refreshSeconds));
    }

    public IssuedRefreshToken issue(long userId, Duration ttl) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.insert(userId, hash(raw), Instant.now().plus(ttl));
        return new IssuedRefreshToken(raw, ttl.toSeconds());
    }

    /** Valida y rota el token: el anterior queda revocado en la misma operacion. */
    public RotatedRefreshToken rotate(String rawToken) {
        RefreshTokenRepository.RefreshTokenRow row = repository.findValidByHash(hash(rawToken))
                .orElseThrow(() -> ApiException.unauthorized(
                        "INVALID_REFRESH_TOKEN", "La sesion no es valida o expiro."));

        repository.revoke(row.id());

        Duration remaining = Duration.between(Instant.now(), row.expiresAt());
        if (remaining.isNegative() || remaining.isZero()) {
            remaining = Duration.ofMinutes(1);
        }
        IssuedRefreshToken next = issue(row.userId(), remaining);
        return new RotatedRefreshToken(next.value(), next.ttlSeconds(), row.userId());
    }

    public void revoke(String rawToken) {
        repository.findByHash(hash(rawToken)).ifPresent(row -> repository.revoke(row.id()));
    }

    public void revokeAllForUser(long userId) {
        repository.revokeAllForUser(userId);
    }

    public long refreshSeconds() {
        return refreshSeconds;
    }

    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    public record IssuedRefreshToken(String value, long ttlSeconds) {
    }

    public record RotatedRefreshToken(String value, long ttlSeconds, long userId) {
    }
}
