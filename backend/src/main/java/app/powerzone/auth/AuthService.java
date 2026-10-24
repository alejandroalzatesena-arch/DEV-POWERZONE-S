package app.powerzone.auth;

import app.powerzone.auth.dto.LoginRequest;
import app.powerzone.auth.dto.RegisterRequest;
import app.powerzone.auth.mail.MailService;
import app.powerzone.common.ApiException;
import app.powerzone.gyms.GymRepository;
import app.powerzone.users.UserAccount;
import app.powerzone.users.UserRepository;
import app.powerzone.users.UserResponse;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    static final String DEFAULT_ROLE = "MEMBER";
    static final int MAX_ACTIVE_SESSIONS = 5;

    private final UserRepository userRepository;
    private final GymRepository gymRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final long rememberMeSeconds;

    public AuthService(UserRepository userRepository,
                       GymRepository gymRepository,
                       PasswordEncoder passwordEncoder,
                       MailService mailService,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       LoginAttemptService loginAttemptService,
                       @Value("${powerzone.jwt.remember-me-seconds:2592000}") long rememberMeSeconds) {
        this.userRepository = userRepository;
        this.gymRepository = gymRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
        this.rememberMeSeconds = rememberMeSeconds;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        long gymId = gymRepository.defaultGymId();
        String email = normalizeEmail(request.email());

        if (userRepository.existsByGymAndEmail(gymId, email)) {
            throw ApiException.conflict(
                    "EMAIL_TAKEN", "Ya existe una cuenta registrada con ese email.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        long userId = userRepository.insert(
                gymId,
                DEFAULT_ROLE,
                email,
                passwordHash,
                request.firstName().trim(),
                request.lastName().trim(),
                normalizeOptional(request.phone()),
                request.birthDate(),
                request.heightCm(),
                request.weightKg(),
                request.goal(),
                request.experienceLevel(),
                request.daysPerWeek());

        mailService.sendRegistrationConfirmation(email, request.firstName().trim());

        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> ApiException.notFound(
                        "USER_NOT_FOUND", "No se encontro el usuario recien creado."));
    }

    @Transactional(readOnly = true)
    public SessionResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        loginAttemptService.assertNotLocked(email);

        long gymId = gymRepository.defaultGymId();
        UserAccount user = userRepository.findByGymAndEmail(gymId, email).orElse(null);

        if (user == null
                || !user.active()
                || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            loginAttemptService.recordFailure(email);
            throw ApiException.unauthorized(
                    "INVALID_CREDENTIALS", "Email o contrasena incorrectos.");
        }

        loginAttemptService.clear(email);
        return createSession(user, request.rememberMe());
    }

    public SessionResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);

        UserAccount user = userRepository.findById(rotated.userId())
                .filter(UserAccount::active)
                .orElseThrow(() -> ApiException.unauthorized(
                        "INVALID_REFRESH_TOKEN", "La sesion no es valida o expiro."));

        JwtService.IssuedToken access = jwtService.issue(user);
        return new SessionResult(
                access.value(),
                access.expiresInSeconds(),
                rotated.value(),
                rotated.ttlSeconds(),
                UserResponse.from(user));
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    private SessionResult createSession(UserAccount user, boolean rememberMe) {
        JwtService.IssuedToken access = jwtService.issue(user);
        long ttlSeconds = rememberMe ? rememberMeSeconds : refreshTokenService.refreshSeconds();
        RefreshTokenService.IssuedRefreshToken refresh =
                refreshTokenService.issue(user.id(), java.time.Duration.ofSeconds(ttlSeconds));
        refreshTokenService.limitActiveSessions(user.id(), MAX_ACTIVE_SESSIONS);
        return new SessionResult(
                access.value(),
                access.expiresInSeconds(),
                refresh.value(),
                refresh.ttlSeconds(),
                UserResponse.from(user));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record SessionResult(
            String accessToken,
            long expiresIn,
            String refreshToken,
            long refreshTtlSeconds,
            UserResponse user) {
    }
}
