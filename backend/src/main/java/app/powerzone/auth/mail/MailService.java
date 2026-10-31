package app.powerzone.auth.mail;

/** Puerto de envio de correo. La implementacion por defecto escribe en el log. */
public interface MailService {

    void sendRegistrationConfirmation(String to, String firstName);

    void sendPasswordReset(String to, String firstName, String resetUrl);
}
