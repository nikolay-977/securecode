package ru.skillfactory.securecode.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailSender implements OtpSender {
    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

    private final Session session;
    private final String fromEmail;
    private final String username;
    private final String password;
    private final boolean enabled;

    public EmailSender() {
        logger.debug("Initializing EmailSender");
        Properties config = loadConfig();
        this.username = config.getProperty("email.username");
        this.password = config.getProperty("email.password");
        this.fromEmail = config.getProperty("email.from");
        this.enabled = Boolean.parseBoolean(config.getProperty("enabled"));

        logger.info("EmailSender initialized with fromEmail: {}, enabled: {}", fromEmail, enabled);

        this.session = Session.getInstance(config, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private Properties loadConfig() {
        try {
            logger.debug("Loading email configuration from 'email.properties'");
            Properties props = new Properties();
            props.load(EmailSender.class.getClassLoader().getResourceAsStream("email.properties"));
            logger.info("Email configuration loaded successfully");
            return props;
        } catch (Exception e) {
            logger.error("Failed to load email configuration", e);
            throw new RuntimeException("Failed to load email configuration", e);
        }
    }

    @Override
    public void sendOtp(String toEmail, String code) {
        if (!enabled) {
            logger.warn("Email sending is disabled. Skipping sending OTP to {}", toEmail);
            return;
        }

        try {
            logger.debug("Preparing OTP email to {}", toEmail);
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Your OTP Code");
            message.setText("Your verification code is: " + code);

            Transport.send(message);
            logger.info("OTP email sent successfully to {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send OTP email to {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
