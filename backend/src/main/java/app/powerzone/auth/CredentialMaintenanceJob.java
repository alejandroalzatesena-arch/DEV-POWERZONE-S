package app.powerzone.auth;

import app.powerzone.auth.PasswordResetTokenRepository;
import app.powerzone.auth.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limpieza periodica de credenciales vencidas. Evita que las tablas de sesion
 * y recuperacion crezcan sin limite.
 */
@Component
public class CredentialMaintenanceJob {

    private static final Logger log = LoggerFactory.getLogger(CredentialMaintenanceJob.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginAttemptService loginAttemptService;

    public CredentialMaintenanceJob(RefreshTokenRepository refreshTokenRepository,
                                    PasswordResetTokenRepository passwordResetTokenRepository,
                                    LoginAttemptService loginAttemptService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Scheduled(fixedDelayString = "${powerzone.maintenance.cleanup-interval-ms:3600000}")
    public void cleanup() {
        int refresh = refreshTokenRepository.deleteExpired();
        int reset = passwordResetTokenRepository.deleteExpired();
        loginAttemptService.purgeExpired();
        log.info("Limpieza de credenciales: {} refresh tokens, {} tokens de recuperacion", refresh, reset);
    }
}
