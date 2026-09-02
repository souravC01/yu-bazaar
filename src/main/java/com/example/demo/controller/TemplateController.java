package com.example.demo.controller;

import com.example.demo.Email.EmailSender;
import com.example.demo.Email.EmailTemplate;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AccountVerificationService;
import com.example.demo.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

@Controller
public class TemplateController {
    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final AccountVerificationService accountVerificationService;
    private final boolean showLocalCodes;

    public TemplateController(UserRepository userRepository,
                              EmailSender emailSender,
                              PasswordEncoder passwordEncoder,
                              PasswordResetService passwordResetService,
                              AccountVerificationService accountVerificationService,
                              @Value("${app.mail.show-local-codes}") boolean showLocalCodes) {
        this.userRepository = userRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
        this.accountVerificationService = accountVerificationService;
        this.showLocalCodes = showLocalCodes;
    }

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid credentials or the account has not been verified.");
        }
        return "login_page";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        addRegistrationConstraints(model);
        return "register_page";
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam String name,
                                 @RequestParam String email,
                                 @RequestParam String gender,
                                 @RequestParam(required = false) String dob,
                                 @RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        String normalizedEmail = normalizeEmail(email);

        if (!normalizedEmail.matches(EMAIL_PATTERN)) {
            return registrationError(model, "Use a valid email address.");
        }
        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existingUser.isPresent()) {
            if (!existingUser.get().isVerified()) {
                redirectAttributes.addAttribute("email", normalizedEmail);
                redirectAttributes.addFlashAttribute(
                        "success",
                        "Your account is waiting for email verification. Enter your code or request a new one."
                );
                return "redirect:/verify";
            }
            redirectAttributes.addFlashAttribute("error", "Account already verified. Sign in to continue.");
            return "redirect:/login";
        }
        if (!password.equals(confirmPassword)) {
            return registrationError(model, "Passwords do not match.");
        }
        if (name == null || name.isBlank()) {
            return registrationError(model, "Name is required.");
        }
        if (dob == null || dob.isBlank()) {
            return registrationError(model, "Date of birth is required.");
        }
        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(dob);
        } catch (DateTimeParseException exception) {
            return registrationError(model, "Enter a valid date of birth.");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            return registrationError(model, "Date of birth cannot be in the future.");
        }
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age > 120) {
            return registrationError(model, "Enter a realistic date of birth.");
        }
        if (age < 16) {
            return registrationError(model, "You must be at least 16 years old to register.");
        }
        if (password.length() < 8) {
            return registrationError(model, "Password must be at least 8 characters.");
        }
        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setAge(age);
        user.setGender(gender);
        user.setDob(dob);
        user.setPassword(passwordEncoder.encode(password));
        user.setVerified(false);
        userRepository.save(user);

        AccountVerificationService.IssueResult issueResult = accountVerificationService.issueInitialCode(user);
        EmailTemplate template = EmailTemplate.REGISTRATION_SUCCESS;
        emailSender.sendEmail(normalizedEmail, template.getSubject(), template.getBody(user.getName()));

        redirectAttributes.addAttribute("email", normalizedEmail);
        if (showLocalCodes) {
            redirectAttributes.addFlashAttribute("localOtp", issueResult.code());
            redirectAttributes.addFlashAttribute("success", "Registration complete. Use the local verification code below.");
        } else if (issueResult.delivered()) {
            redirectAttributes.addFlashAttribute("success", "Registration complete. Check your email for the verification code.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Your account was created, but the verification email could not be sent.");
        }
        return "redirect:/verify";
    }

    @GetMapping({"/forgot-password", "/forgot_password"})
    public String forgotPassword() {
        return "forgot_password";
    }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(@RequestParam String email,
                                       RedirectAttributes redirectAttributes) {
        passwordResetService.requestReset(email).ifPresent(result -> {
            if (showLocalCodes) {
                redirectAttributes.addFlashAttribute("localPasswordResetUrl", result.resetUrl());
            }
        });

        redirectAttributes.addFlashAttribute(
                "success",
                "If that email belongs to an account, a password-reset link has been prepared."
        );
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPassword(@RequestParam(required = false) String token, Model model) {
        if (!passwordResetService.isTokenValid(token)) {
            model.addAttribute("error", "This password-reset link is invalid or has expired.");
        } else {
            model.addAttribute("token", token);
        }
        return "reset_password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String newPassword,
                                @RequestParam String confirmNewPassword,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmNewPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("token", token);
            return "reset_password";
        }
        if (newPassword.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters.");
            model.addAttribute("token", token);
            return "reset_password";
        }

        if (!passwordResetService.resetPassword(token, newPassword)) {
            model.addAttribute("error", "This password-reset link is invalid or has expired.");
            return "reset_password";
        }

        redirectAttributes.addFlashAttribute("success", "Password updated. You can now sign in.");
        return "redirect:/login";
    }

    @PostMapping("/verify")
    public String verifyOtp(@RequestParam String email,
                            @RequestParam String otp,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        String normalizedEmail = normalizeEmail(email);
        AccountVerificationService.VerificationResult result = accountVerificationService.verify(normalizedEmail, otp);

        if (result == AccountVerificationService.VerificationResult.VERIFIED) {
            redirectAttributes.addFlashAttribute("success", "Account verified. You can now sign in.");
            return "redirect:/login";
        }

        String error = switch (result) {
            case USER_NOT_FOUND -> "User not found.";
            case INVALID_CODE -> "Invalid verification code.";
            case EXPIRED_CODE -> "This verification code has expired. Request a new code.";
            case VERIFIED -> throw new IllegalStateException("Verified result should redirect");
        };
        model.addAttribute("error", error);
        model.addAttribute("email", normalizedEmail);
        return "verify_otp";
    }

    @PostMapping("/verify/resend")
    public String resendVerificationCode(@RequestParam String email,
                                         RedirectAttributes redirectAttributes) {
        String normalizedEmail = normalizeEmail(email);
        AccountVerificationService.ResendResult result = accountVerificationService.resend(normalizedEmail);

        redirectAttributes.addAttribute("email", normalizedEmail);
        switch (result.status()) {
            case SENT -> {
                redirectAttributes.addFlashAttribute("success", "A new verification code has been prepared.");
                if (showLocalCodes) {
                    redirectAttributes.addFlashAttribute("localOtp", result.localCode());
                }
            }
            case COOLDOWN -> redirectAttributes.addFlashAttribute(
                    "error",
                    "Wait 60 seconds before requesting another code."
            );
            case NOT_AVAILABLE -> redirectAttributes.addFlashAttribute(
                    "success",
                    "If this account still needs verification, a new code has been prepared."
            );
        }
        return "redirect:/verify";
    }

    @GetMapping("/verify")
    public String showVerifyPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", normalizeEmail(email));
        return "verify_otp";
    }

    private String registrationError(Model model, String message) {
        model.addAttribute("error", message);
        addRegistrationConstraints(model);
        return "register_page";
    }

    private void addRegistrationConstraints(Model model) {
        model.addAttribute("latestEligibleDob", LocalDate.now().minusYears(16));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
