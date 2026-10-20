package app.powerzone.auth;

import app.powerzone.auth.dto.RegisterRequest;
import app.powerzone.auth.mail.MailService;
import app.powerzone.common.ApiException;
import app.powerzone.gyms.GymRepository;
import app.powerzone.users.UserResponse;
import app.powerzone.users.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    static final String DEFAULT_ROLE = "MEMBER";

    private final UserRepository userRepository;
    private final GymRepository gymRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public AuthService(UserRepository userRepository,
                       GymRepository gymRepository,
                       PasswordEncoder passwordEncoder,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.gymRepository = gymRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
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
}
