package app.powerzone.auth;

import app.powerzone.auth.dto.AuthResponse;
import app.powerzone.auth.dto.ForgotPasswordRequest;
import app.powerzone.auth.dto.LoginRequest;
import app.powerzone.auth.dto.RegisterRequest;
import app.powerzone.auth.dto.ResetPasswordRequest;
import app.powerzone.common.ApiException;
import app.powerzone.users.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "pz_refresh";

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final boolean secureCookies;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService,
                          @Value("${powerzone.cookies.secure:false}") boolean secureCookies) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.secureCookies = secureCookies;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity
                .created(URI.create("/api/v1/users/" + user.id()))
                .body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthService.SessionResult session = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE,
                sessionCookie(session.refreshToken(), session.refreshTtlSeconds()).toString());
        return ResponseEntity.ok(toResponse(session));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw ApiException.unauthorized(
                    "MISSING_SESSION", "No hay una sesion activa para renovar.");
        }

        AuthService.SessionResult session = authService.refresh(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE,
                sessionCookie(session.refreshToken(), session.refreshTtlSeconds()).toString());
        return ResponseEntity.ok(toResponse(session));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie().toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.request(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                              HttpServletResponse response) {
        passwordResetService.reset(request.token(), request.newPassword());
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie().toString());
        return ResponseEntity.noContent().build();
    }

    private AuthResponse toResponse(AuthService.SessionResult session) {
        return new AuthResponse(session.accessToken(), session.expiresIn(), session.user());
    }

    private ResponseCookie sessionCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    private ResponseCookie expiredCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
