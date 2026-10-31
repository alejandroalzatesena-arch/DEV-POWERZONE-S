package app.powerzone.users;

import app.powerzone.common.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse findById(long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> ApiException.notFound(
                        "USER_NOT_FOUND", "No se encontro el usuario solicitado."));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAllByGym(long gymId) {
        return userRepository.findAllByGym(gymId).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse updateProfile(long userId, UpdateProfileRequest request) {
        UserAccount current = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(
                        "USER_NOT_FOUND", "No se encontro el usuario solicitado."));

        String firstName = request.firstName() == null || request.firstName().isBlank()
                ? current.firstName()
                : request.firstName().trim();
        String lastName = request.lastName() == null || request.lastName().isBlank()
                ? current.lastName()
                : request.lastName().trim();

        userRepository.updateProfile(
                userId,
                firstName,
                lastName,
                request.phone() == null ? current.phone() : blankToNull(request.phone()),
                request.birthDate() == null ? current.birthDate() : request.birthDate(),
                request.heightCm() == null ? current.heightCm() : request.heightCm(),
                request.weightKg() == null ? current.weightKg() : request.weightKg(),
                request.goal() == null ? current.goal() : request.goal(),
                request.experienceLevel() == null ? current.experienceLevel() : request.experienceLevel(),
                request.daysPerWeek() == null ? current.daysPerWeek() : request.daysPerWeek());

        return findById(userId);
    }

    @Transactional
    public UserResponse changeRole(long userId, String roleCode) {
        userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(
                        "USER_NOT_FOUND", "No se encontro el usuario solicitado."));
        userRepository.updateRole(userId, roleCode);
        return findById(userId);
    }

    private static String blankToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
