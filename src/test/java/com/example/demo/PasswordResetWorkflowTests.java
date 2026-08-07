package com.example.demo;

import com.example.demo.model.User;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetWorkflowTests {
    private static final String EMAIL = "reset.student@my.yorku.ca";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        itemRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void resetTokenIsHashedExpiringAndSingleUse() throws Exception {
        createUser();

        MvcResult requestResult = mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("email", " RESET.STUDENT@MY.YORKU.CA "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attributeExists("success", "localPasswordResetUrl"))
                .andReturn();

        String resetUrl = (String) requestResult.getFlashMap().get("localPasswordResetUrl");
        String rawToken = UriComponentsBuilder.fromUriString(resetUrl)
                .build()
                .getQueryParams()
                .getFirst("token");

        User requestedUser = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(rawToken).isNotBlank();
        assertThat(requestedUser.getPasswordResetTokenHash())
                .hasSize(64)
                .isNotEqualTo(rawToken);
        assertThat(requestedUser.getPasswordResetExpiresAt()).isAfter(Instant.now());

        mockMvc.perform(get("/reset-password").param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(view().name("reset_password"))
                .andExpect(model().attribute("token", rawToken));

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("token", rawToken)
                        .param("newPassword", "updated-pass")
                        .param("confirmNewPassword", "updated-pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        User resetUser = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(passwordEncoder.matches("updated-pass", resetUser.getPassword())).isTrue();
        assertThat(resetUser.getPasswordResetTokenHash()).isNull();
        assertThat(resetUser.getPasswordResetExpiresAt()).isNull();

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("token", rawToken)
                        .param("newPassword", "replayed-pass")
                        .param("confirmNewPassword", "replayed-pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset_password"))
                .andExpect(model().attributeExists("error"));

        User replayedUser = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(passwordEncoder.matches("updated-pass", replayedUser.getPassword())).isTrue();
    }

    @Test
    void expiredTokenIsRejectedAndCleared() throws Exception {
        createUser();
        String rawToken = requestResetLink(EMAIL);

        User user = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        user.setPasswordResetExpiresAt(Instant.now().minusSeconds(1));
        userRepository.save(user);

        mockMvc.perform(get("/reset-password").param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(view().name("reset_password"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeDoesNotExist("token"));

        User expiredUser = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(expiredUser.getPasswordResetTokenHash()).isNull();
        assertThat(expiredUser.getPasswordResetExpiresAt()).isNull();
    }

    @Test
    void unknownEmailReceivesTheSameGenericResponse() throws Exception {
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("email", "unknown@my.yorku.ca"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute(
                        "success",
                        "If that email belongs to an account, a password-reset link has been prepared."
                ))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    void publicDemoAccountCannotRequestAPasswordReset() throws Exception {
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("email", "demo@yubazaar.app"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute(
                        "success",
                        "If that email belongs to an account, a password-reset link has been prepared."
                ))
                .andExpect(flash().attributeCount(1));
    }

    private String requestResetLink(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("email", email))
                .andReturn();
        String resetUrl = (String) result.getFlashMap().get("localPasswordResetUrl");
        return UriComponentsBuilder.fromUriString(resetUrl)
                .build()
                .getQueryParams()
                .getFirst("token");
    }

    private User createUser() {
        User user = new User();
        user.setName("Reset Student");
        user.setEmail(EMAIL);
        user.setPassword(passwordEncoder.encode("original-pass"));
        user.setAge(22);
        user.setGender("Prefer not to say");
        user.setDob("2004-01-01");
        user.setVerified(true);
        return userRepository.save(user);
    }
}
