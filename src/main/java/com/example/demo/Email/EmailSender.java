package com.example.demo.Email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String fromAddress;
    private final String fromName;

    public EmailSender(JavaMailSender mailSender,
                       @Value("${app.mail.enabled}") boolean enabled,
                       @Value("${app.mail.from}") String fromAddress,
                       @Value("${app.mail.from-name}") String fromName) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    public boolean sendEmail(String toEmail, String subject, String body) {
        if (!enabled) {
            LOGGER.info("Email delivery is disabled; skipped message '{}' to {}", subject, toEmail);
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(String.format("%s <%s>", fromName, fromAddress));
        message.setTo(toEmail);
        message.setText(body);
        message.setSubject(subject);

        try {
            mailSender.send(message);
            return true;
        } catch (MailException exception) {
            LOGGER.error("Unable to send email '{}' to {}", subject, toEmail, exception);
            return false;
        }
    }

    public boolean sendOtpEmail(String toEmail, String otp) {
        String subject = "Your OTP for YU Bazaar Registration";
        String body = "Dear User,\n\n"
                + "Thank you for registering at YU Bazaar. Please use the following OTP to verify your account:\n\n"
                + otp + "\n\n"
                + "This verification code expires in 10 minutes.\n\n"
                + "Regards,\n"
                + "YU Bazaar Team";

        return sendEmail(toEmail, subject, body);
    }
}
