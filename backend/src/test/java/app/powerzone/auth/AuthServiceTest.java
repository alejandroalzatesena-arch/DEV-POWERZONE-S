package app.powerzone.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.powerzone.auth.dto.LoginRequest;
import app.powerzone.auth.dto.RegisterRequest;
import app.powerzone.auth.mail.MailService;
import app.powerzone.common.ApiException;
import app.powerzone.gyms.GymRepository;
import app.powerzone.users.UserAccount;
import app.powerzone.users.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GymRepository gymRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MailService mailService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LoginAttemptService loginAttemptService;

    private AuthService authService;
    private UserAccount member;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, gymRepository, passwordEncoder, mailService,
                jwtService, refreshTokenService, loginAttemptService, 2_592_000L);

        member = new UserAccount(
                5L, 1L, "ana@powerzone.app", "hash-bcrypt", "Ana", "Gomez", "3001234567",
                "MEMBER", true, LocalDate.of(1998, 3, 14),
                new BigDecimal("168.5"), new BigDecimal("62.0"),
                "STRENGTH", "INTERMEDIATE", 4);
    }

    @Test
    @DisplayName("registra un miembro con email normalizado y rol MEMBER")
    void registersMember() {
        RegisterRequest request = new RegisterRequest(
                "  Ana@PowerZone.App ", "Secreta123", " Ana ", " Gomez ", "3001234567",
                LocalDate.of(1998, 3, 14), new BigDecimal("168.5"), new BigDecimal("62.0"),
                "STRENGTH", "INTERMEDIATE", 4);

        when(gymRepository.defaultGymId()).thenReturn(1L);
        when(userRepository.existsByGymAndEmail(1L, "ana@powerzone.app")).thenReturn(false);
        when(passwordEncoder.encode("Secreta123")).thenReturn("hash-bcrypt");
        when(userRepository.insert(
                eq(1L), eq("MEMBER"), eq("ana@powerzone.app"), eq("hash-bcrypt"),
                eq("Ana"), eq("Gomez"), eq("3001234567"), any(), any(), any(), any(), any(), any()))
                .thenReturn(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(member));

        var response = authService.register(request);

        assertThat(response.email()).isEqualTo("ana@powerzone.app");
        assertThat(response.role()).isEqualTo("MEMBER");
        verify(mailService).sendRegistrationConfirmation("ana@powerzone.app", "Ana");
    }

    @Test
    @DisplayName("rechaza el registro cuando el email ya existe en el gimnasio")
    void rejectsDuplicatedEmail() {
        RegisterRequest request = new RegisterRequest(
                "ana@powerzone.app", "Secreta123", "Ana", "Gomez", null,
                null, null, null, null, null, null);

        when(gymRepository.defaultGymId()).thenReturn(1L);
        when(userRepository.existsByGymAndEmail(1L, "ana@powerzone.app")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("EMAIL_TAKEN");
                    assertThat(api.status()).isEqualTo(HttpStatus.CONFLICT);
                });

        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("inicia sesion y emite access y refresh token")
    void logsInWithValidCredentials() {
        when(gymRepository.defaultGymId()).thenReturn(1L);
        when(userRepository.findByGymAndEmail(1L, "ana@powerzone.app")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("Secreta123", "hash-bcrypt")).thenReturn(true);
        when(jwtService.issue(member)).thenReturn(new JwtService.IssuedToken("access", 900));
        when(refreshTokenService.refreshSeconds()).thenReturn(604_800L);
        when(refreshTokenService.issue(5L, Duration.ofSeconds(604_800L)))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("refresh", 604_800));

        var session = authService.login(new LoginRequest("ana@powerzone.app", "Secreta123", false));

        assertThat(session.accessToken()).isEqualTo("access");
        assertThat(session.refreshToken()).isEqualTo("refresh");
        assertThat(session.user().email()).isEqualTo("ana@powerzone.app");
        verify(loginAttemptService).clear("ana@powerzone.app");
    }

    @Test
    @DisplayName("recordarme extiende el refresh token a 30 dias")
    void rememberMeUsesExtendedTtl() {
        when(gymRepository.defaultGymId()).thenReturn(1L);
        when(userRepository.findByGymAndEmail(1L, "ana@powerzone.app")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.issue(member)).thenReturn(new JwtService.IssuedToken("access", 900));
        when(refreshTokenService.issue(5L, Duration.ofSeconds(2_592_000L)))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("refresh", 2_592_000));

        authService.login(new LoginRequest("ana@powerzone.app", "Secreta123", true));

        verify(refreshTokenService).limitActiveSessions(5L, AuthService.MAX_ACTIVE_SESSIONS);
    }

    @Test
    @DisplayName("rechaza credenciales invalidas y registra el intento")
    void rejectsInvalidPassword() {
        when(gymRepository.defaultGymId()).thenReturn(1L);
        when(userRepository.findByGymAndEmail(1L, "ana@powerzone.app")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("incorrecta", "hash-bcrypt")).thenReturn(false);

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("ana@powerzone.app", "incorrecta", false)))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("INVALID_CREDENTIALS"));

        verify(loginAttemptService).recordFailure("ana@powerzone.app");
    }

    @Test
    @DisplayName("no consulta la base cuando la cuenta esta bloqueada")
    void rejectsLockedAccount() {
        doThrow(ApiException.tooManyRequests("ACCOUNT_LOCKED", "bloqueada"))
                .when(loginAttemptService).assertNotLocked("ana@powerzone.app");

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("ana@powerzone.app", "Secreta123", false)))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("ACCOUNT_LOCKED"));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("rota el refresh token y emite un nuevo access token")
    void refreshesSession() {
        when(refreshTokenService.rotate("old-refresh"))
                .thenReturn(new RefreshTokenService.RotatedRefreshToken("new-refresh", 600, 5L));
        when(userRepository.findById(5L)).thenReturn(Optional.of(member));
        when(jwtService.issue(member)).thenReturn(new JwtService.IssuedToken("new-access", 900));

        var session = authService.refresh("old-refresh");

        assertThat(session.accessToken()).isEqualTo("new-access");
        assertThat(session.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    @DisplayName("revoca el refresh token al cerrar sesion")
    void revokesTokenOnLogout() {
        authService.logout("raw-refresh");

        verify(refreshTokenService).revoke("raw-refresh");
    }
}
