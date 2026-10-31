package app.powerzone.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.powerzone.auth.mail.MailService;
import app.powerzone.common.ApiException;
import app.powerzone.gyms.GymRepository;
import app.powerzone.users.UserAccount;
import app.powerzone.users.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private GymRepository gymRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MailService mailService;

    private PasswordResetService service;
    private UserAccount member;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                tokenRepository, userRepository, refreshTokenRepository, gymRepository,
                passwordEncoder, mailService, "http://localhost:4200");

        member = new UserAccount(
                5L, 1L, "ana@powerzone.app", "hash-bcrypt", "Ana", "Gomez", null,
                "MEMBER", true, LocalDate.of(1998, 3, 14),
                new BigDecimal("168.5"), new BigDecimal("62.0"),
                "STRENGTH", "INTERMEDIATE", 4);
    }

    @Test
    @DisplayName("genera un enlace con token hasheado para un email existente")
    void createsResetTokenForKnownEmail() {
        when(gymRepository.defaultGymId()).thenReturn(1L);
        when(userRepository.findByGymAndEmail(1L, "ana@powerzone.app")).thenReturn(Optional.of(member));

        service.request("  Ana@PowerZone.App ");

        verify(tokenRepository).invalidateForUser(5L);
        verify(tokenRepository).insert(eq(5L), anyString(), any(Instant.class));

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordReset(eq("ana@powerzone.app"), eq("Ana"), url.capture());
        assertThat(url.getValue()).startsWith("http://localhost:4200/auth/reset-password?token=");
    }

    @Test
    @DisplayName("no envia correo cuando el email no existe")
    void ignoresUnknownEmail() {
        when(gymRepository.defaultGymId()).thenReturn(1L);
        when(userRepository.findByGymAndEmail(1L, "desconocido@powerzone.app"))
                .thenReturn(Optional.empty());

        service.request("desconocido@powerzone.app");

        verifyNoInteractions(mailService);
        verify(tokenRepository, never()).insert(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("actualiza la contrasena y revoca todas las sesiones")
    void resetsPasswordAndRevokesSessions() {
        when(tokenRepository.findValidByHash(anyString()))
                .thenReturn(Optional.of(new PasswordResetTokenRepository.PasswordResetRow(
                        9L, 5L, Instant.now().plusSeconds(600))));
        when(userRepository.findById(5L)).thenReturn(Optional.of(member));
        when(passwordEncoder.encode("NuevaClave123")).thenReturn("new-hash");

        service.reset("raw-token", "NuevaClave123");

        verify(tokenRepository).markUsed(9L);
        verify(userRepository).updatePassword(5L, "new-hash");
        verify(refreshTokenRepository).revokeAllForUser(5L);
    }

    @Test
    @DisplayName("rechaza un token vencido o reutilizado")
    void rejectsInvalidToken() {
        when(tokenRepository.findValidByHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reset("raw-token", "NuevaClave123"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("INVALID_RESET_TOKEN"));

        verify(userRepository, never()).updatePassword(anyLong(), anyString());
    }
}
