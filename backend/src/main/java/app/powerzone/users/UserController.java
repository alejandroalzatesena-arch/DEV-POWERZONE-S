package app.powerzone.users;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.findById(subjectId(jwt));
    }

    @PutMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(subjectId(jwt), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public List<UserResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return userService.findAllByGym(gymId(jwt));
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeRole(@PathVariable long userId,
                                                   @RequestBody RoleChangeRequest request) {
        return ResponseEntity.ok(userService.changeRole(userId, request.role()));
    }

    static long subjectId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    static long gymId(Jwt jwt) {
        Object claim = jwt.getClaim("gymId");
        return claim instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(claim));
    }

    public record RoleChangeRequest(String role) {
    }
}
