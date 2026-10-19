package app.powerzone.auth.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Adaptador de desarrollo: no envia correo real, registra el contenido para
 * poder verificar el flujo. En produccion se reemplaza por un adaptador SMTP
 * sin cambiar los casos de uso.
 */
@Service
public class LoggingMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailService.class);

    @Override
    public void sendRegistrationConfirmation(String to, String firstName) {
        log.info("Correo de confirmacion de registro para {} ({})", firstName, to);
    }

    @Override
    public void sendPasswordReset(String to, String firstName, String resetUrl) {
        log.info("Correo de recuperacion para {} ({}): {}", firstName, to, resetUrl);
    }
}
