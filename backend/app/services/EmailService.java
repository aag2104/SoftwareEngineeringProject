package services;

import play.Logger;
import play.Configuration;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Email service for sending interview reminders and notifications.
 * Can be extended to use SMTP, SendGrid, or other email providers.
 */
@Singleton
public class EmailService {

    private final Configuration config;
    private final boolean emailEnabled;
    private final String fromEmail;

    @Inject
    public EmailService(Configuration config) {
        this.config = config;
        this.emailEnabled = config.getBoolean("email.enabled", false);
        this.fromEmail = config.getString("email.from", "no-reply@rajob.edu");
    }

    /**
     * Send an email to the specified recipient
     */
    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            Logger.info("Email service disabled. Would send to: {} with subject: {}", to, subject);
            Logger.debug("Email body: {}", body);
            return;
        }

        try {
            // TODO: Implement actual email sending using Play's mail module or external service
            // For now, just log
            Logger.info("Sending email to: {} with subject: {}", to, subject);
            Logger.debug("Email body: {}", body);
        } catch (Exception e) {
            Logger.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send email to multiple recipients
     */
    public void sendEmailBatch(String[] recipients, String subject, String body) {
        for (String to : recipients) {
            sendEmail(to, subject, body);
        }
    }
}