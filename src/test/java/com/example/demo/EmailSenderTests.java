package com.example.demo;

import com.example.demo.Email.EmailSender;
import com.example.demo.Email.EmailTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailSenderTests {

    @Test
    void sendsMailWithTheConfiguredBrandNameAndAddress() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailSender emailSender = new EmailSender(
                mailSender,
                true,
                "yubazaarsupport@gmail.com",
                "YU Bazaar"
        );

        assertThat(emailSender.sendEmail("student@my.yorku.ca", "Welcome", "Hello")).isTrue();

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("YU Bazaar <yubazaarsupport@gmail.com>");
        assertThat(message.getTo()).containsExactly("student@my.yorku.ca");
        assertThat(message.getSubject()).isEqualTo("Welcome");
        assertThat(message.getText()).isEqualTo("Hello");
    }

    @Test
    void verificationAndRegistrationMessagesDescribeTheActualLifecycle() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailSender emailSender = new EmailSender(
                mailSender,
                true,
                "yubazaarsupport@gmail.com",
                "YU Bazaar"
        );

        assertThat(emailSender.sendOtpEmail("seller@example.com", "123456")).isTrue();

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getText()).contains("expires in 10 minutes");

        String registrationBody = EmailTemplate.REGISTRATION_SUCCESS.getBody("Seller");
        assertThat(registrationBody)
                .contains("verify your email before signing in")
                .doesNotContain("account is now active");
    }
}
