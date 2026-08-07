package com.example.demo.service;

import com.example.demo.Email.EmailSender;
import com.example.demo.Email.EmailTemplate;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final Duration tokenTtl;
    private final String baseUrl;
    private final DemoAccountPolicy demoAccountPolicy;

    public PasswordResetService(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                EmailSender emailSender,
                                @Value("${app.password-reset.ttl}") Duration tokenTtl,
                                @Value("${app.base-url}") String baseUrl,
                                DemoAccountPolicy demoAccountPolicy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.tokenTtl = tokenTtl;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.demoAccountPolicy = demoAccountPolicy;
    }

    @Transactional
    public Optional<ResetRequestResult> requestReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (demoAccountPolicy.isDemo(normalizedEmail)) {
            return Optional.empty();
        }

        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(user -> {
                    String rawToken = generateToken();
                    String resetUrl = buildResetUrl(rawToken);

                    user.setPasswordResetTokenHash(hashToken(rawToken));
                    user.setPasswordResetExpiresAt(Instant.now().plus(tokenTtl));
                    userRepository.save(user);

                    EmailTemplate template = EmailTemplate.PASSWORD_RESET;
                    boolean delivered = emailSender.sendEmail(
                            user.getEmail(),
                            template.getSubject(),
                            template.getBody(user.getName(), resetUrl)
                    );
                    return new ResetRequestResult(resetUrl, delivered);
                });
    }

    @Transactional
    public boolean isTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        Optional<User> matchingUser = userRepository.findForPasswordReset(hashToken(rawToken));
        if (matchingUser.isEmpty()) {
            return false;
        }

        User user = matchingUser.get();
        if (demoAccountPolicy.isDemo(user.getEmail())) {
            clearResetToken(user);
            return false;
        }
        if (isExpired(user)) {
            clearResetToken(user);
            return false;
        }
        return true;
    }

    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        Optional<User> matchingUser = userRepository.findForPasswordReset(hashToken(rawToken));
        if (matchingUser.isEmpty()) {
            return false;
        }

        User user = matchingUser.get();
        if (demoAccountPolicy.isDemo(user.getEmail())) {
            clearResetToken(user);
            return false;
        }
        if (isExpired(user)) {
            clearResetToken(user);
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        clearResetToken(user);
        return true;
    }

    private boolean isExpired(User user) {
        return user.getPasswordResetExpiresAt() == null
                || !user.getPasswordResetExpiresAt().isAfter(Instant.now());
    }

    private void clearResetToken(User user) {
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String buildResetUrl(String rawToken) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/reset-password")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public record ResetRequestResult(String resetUrl, boolean delivered) {
    }
}
