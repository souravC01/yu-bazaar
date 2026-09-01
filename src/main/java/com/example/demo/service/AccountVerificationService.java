package com.example.demo.service;

import com.example.demo.Email.EmailSender;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class AccountVerificationService {
    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final Duration ttl;
    private final Duration resendCooldown;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountVerificationService(UserRepository userRepository,
                                      EmailSender emailSender,
                                      @Value("${app.verification.ttl}") Duration ttl,
                                      @Value("${app.verification.resend-cooldown}") Duration resendCooldown) {
        this.userRepository = userRepository;
        this.emailSender = emailSender;
        this.ttl = ttl;
        this.resendCooldown = resendCooldown;
    }

    @Transactional
    public IssueResult issueInitialCode(User user) {
        String code = generateCode();
        setNewCode(user, code, Instant.now());
        userRepository.save(user);
        return new IssueResult(code, emailSender.sendOtpEmail(user.getEmail(), code));
    }

    @Transactional
    public VerificationResult verify(String email, String code) {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return VerificationResult.USER_NOT_FOUND;
        }

        Instant now = Instant.now();
        if (user.getOtpExpiresAt() == null || !user.getOtpExpiresAt().isAfter(now)) {
            clearCode(user);
            userRepository.save(user);
            return VerificationResult.EXPIRED_CODE;
        }
        if (user.getOtp() == null || code == null || !user.getOtp().equals(code.trim())) {
            return VerificationResult.INVALID_CODE;
        }

        user.setVerified(true);
        clearCode(user);
        userRepository.save(user);
        return VerificationResult.VERIFIED;
    }

    @Transactional
    public ResendResult resend(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.isVerified()) {
            return new ResendResult(ResendStatus.NOT_AVAILABLE, null);
        }

        Instant now = Instant.now();
        if (user.getOtpLastSentAt() != null
                && user.getOtpLastSentAt().plus(resendCooldown).isAfter(now)) {
            return new ResendResult(ResendStatus.COOLDOWN, null);
        }

        String code = generateReplacementCode(user.getOtp());
        setNewCode(user, code, now);
        userRepository.save(user);
        emailSender.sendOtpEmail(user.getEmail(), code);
        return new ResendResult(ResendStatus.SENT, code);
    }

    private void setNewCode(User user, String code, Instant issuedAt) {
        user.setOtp(code);
        user.setOtpExpiresAt(issuedAt.plus(ttl));
        user.setOtpLastSentAt(issuedAt);
    }

    private void clearCode(User user) {
        user.setOtp(null);
        user.setOtpExpiresAt(null);
    }

    private String generateReplacementCode(String previousCode) {
        String code;
        do {
            code = generateCode();
        } while (code.equals(previousCode));
        return code;
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    public enum VerificationResult {
        VERIFIED,
        USER_NOT_FOUND,
        INVALID_CODE,
        EXPIRED_CODE
    }

    public enum ResendStatus {
        SENT,
        COOLDOWN,
        NOT_AVAILABLE
    }

    public record IssueResult(String code, boolean delivered) {
    }

    public record ResendResult(ResendStatus status, String localCode) {
    }
}
