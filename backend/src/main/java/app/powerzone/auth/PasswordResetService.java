package app.powerzone.auth;

import app.powerzone.auth.mail.MailService;
import app.powerzone.common.ApiException;
import app.powerzone.gyms.GymRepository;
import app.powerzone.users.UserAccount;
import app.powerzone.users.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperacion de contrasena con token de un solo uso. La respuesta al
 * solicitante es siempre la misma para no revelar si el email existe.
 */
@Service
public class PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GymRepository gymRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final String webUrl;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                GymRepository gymRepository,
                                PasswordEncoder passwordEncoder,
                                MailService mailService,
                                @Value("${powerzone.web-url:http://localhost:4200}") String webUrl) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.gymRepository = gymRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.webUrl = webUrl;
    }

    @Transactional
    public void request(String rawEmail) {
        long gymId = gymRepository.defaultGymId();
        String email = rawEmail.trim().toLowerCase(java.util.Locale.ROOT);

        userRepository.findByGymAndEmail(gymId, email).ifPresent(user -> {
            if (!user.active()) {
                return;
            }

            tokenRepository.invalidateForUser(user.id());

            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            tokenRepository.insert(user.id(), hash(rawToken), Instant.now().plus(TOKEN_TTL));

            String resetUrl = webUrl.replaceAll("/+$", "")
                    + "/auth/reset-password?token=" + rawToken;
            mailService.sendPasswordReset(user.email(), user.firstName(), resetUrl);
        });
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        PasswordResetTokenRepository.PasswordResetRow row = tokenRepository
                .findValidByHash(hash(rawToken))
                .orElseThrow(() -> ApiException.badRequest(
                        "INVALID_RESET_TOKEN", "El enlace de recuperacion no es valido o expiro."));

        UserAccount user = userRepository.findById(row.userId())
                .filter(UserAccount::active)
                .orElseThrow(() -> ApiException.badRequest(
                        "INVALID_RESET_TOKEN", "El enlace de recuperacion no es valido o expiro."));

        tokenRepository.markUsed(row.id());
        userRepository.updatePassword(user.id(), passwordEncoder.encode(newPassword));
        refreshTokenRepository.revokeAllForUser(user.id());
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
