package com.example.demo.controller;

import com.example.demo.Email.EmailSender;
import com.example.demo.Email.EmailTemplate;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class TemplateController {
    private static final String YORK_EMAIL_PATTERN = "[a-zA-Z0-9._%+-]+@(yorku\\.ca|my\\.yorku\\.ca)";

    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final boolean showLocalCodes;

    public TemplateController(UserRepository userRepository,
                              EmailSender emailSender,
                              PasswordEncoder passwordEncoder,
                              PasswordResetService passwordResetService,
                              @Value("${app.mail.show-local-codes}") boolean showLocalCodes) {
        this.userRepository = userRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
        this.showLocalCodes = showLocalCodes;
    }

    @GetMapping({"/", "/login"})
    public String showLoginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid credentials or the account has not been verified.");
        }
        return "login_page";
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register_page";
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam String name,
                                 @RequestParam String email,
                                 @RequestParam int age,
                                 @RequestParam String gender,
                                 @RequestParam String dob,
                                 @RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        String normalizedEmail = normalizeEmail(email);

        if (!password.equals(confirmPassword)) {
            return registrationError(model, "Passwords do not match.");
        }
        if (name == null || name.isBlank()) {
            return registrationError(model, "Name is required.");
        }
        if (!normalizedEmail.matches(YORK_EMAIL_PATTERN)) {
            return registrationError(model, "Use a valid York University email address.");
        }
        if (age <= 0) {
            return registrationError(model, "Age must be a positive number.");
        }
        if (password.length() < 8) {
            return registrationError(model, "Password must be at least 8 characters.");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return registrationError(model, "Email is already registered.");
        }

        String otp = generateOtp();
        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setAge(age);
        user.setGender(gender);
        user.setDob(dob);
        user.setPassword(passwordEncoder.encode(password));
        user.setOtp(otp);
        user.setVerified(false);
        userRepository.save(user);

        boolean otpSent = emailSender.sendOtpEmail(normalizedEmail, otp);
        EmailTemplate template = EmailTemplate.REGISTRATION_SUCCESS;
        emailSender.sendEmail(normalizedEmail, template.getSubject(), template.getBody(user.getName()));

        redirectAttributes.addAttribute("email", normalizedEmail);
        if (showLocalCodes) {
            redirectAttributes.addFlashAttribute("localOtp", otp);
            redirectAttributes.addFlashAttribute("success", "Registration complete. Use the local verification code below.");
        } else if (otpSent) {
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
        return "redirect:/";
    }

    @PostMapping("/verify")
    public String verifyOtp(@RequestParam String email,
                            @RequestParam String otp,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);

        if (user == null) {
            model.addAttribute("error", "User not found.");
            model.addAttribute("email", normalizedEmail);
            return "verify_otp";
        }
        if (user.getOtp() == null || !user.getOtp().equals(otp.trim())) {
            model.addAttribute("error", "Invalid verification code.");
            model.addAttribute("email", normalizedEmail);
            return "verify_otp";
        }

        user.setVerified(true);
        user.setOtp(null);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Account verified. You can now sign in.");
        return "redirect:/";
    }

    @GetMapping("/verify")
    public String showVerifyPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", normalizeEmail(email));
        return "verify_otp";
    }

    private String registrationError(Model model, String message) {
        model.addAttribute("error", message);
        return "register_page";
    }

    private String generateOtp() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
