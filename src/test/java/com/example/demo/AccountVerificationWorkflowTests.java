package com.example.demo;

import com.example.demo.model.User;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = "app.mail.show-local-codes=true")
@AutoConfigureMockMvc
class AccountVerificationWorkflowTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void cleanDatabase() {
        itemRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void expiredVerificationCodeIsRejectedAndCleared() throws Exception {
        User user = createUnverifiedUser("expired@example.com", "123456");
        user.setOtpExpiresAt(Instant.now().minusSeconds(1));
        userRepository.save(user);

        mockMvc.perform(post("/verify")
                        .with(csrf())
                        .param("email", user.getEmail())
                        .param("otp", "123456"))
                .andExpect(status().isOk())
                .andExpect(view().name("verify_otp"))
                .andExpect(model().attribute(
                        "error",
                        "This verification code has expired. Request a new code."
                ));

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.isVerified()).isFalse();
        assertThat(reloaded.getOtp()).isNull();
        assertThat(reloaded.getOtpExpiresAt()).isNull();
    }

    @Test
    void unexpiredVerificationCodeActivatesAccountAndClearsCode() throws Exception {
        User user = createUnverifiedUser("valid@example.com", "123456");
        user.setOtpExpiresAt(Instant.now().plusSeconds(600));
        userRepository.save(user);

        mockMvc.perform(post("/verify")
                        .with(csrf())
                        .param("email", user.getEmail())
                        .param("otp", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("success", "Account verified. You can now sign in."));

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.isVerified()).isTrue();
        assertThat(reloaded.getOtp()).isNull();
        assertThat(reloaded.getOtpExpiresAt()).isNull();
    }

    @Test
    void resendReplacesTheOldCodeAndEnforcesCooldown() throws Exception {
        User user = createUnverifiedUser("resend@example.com", "111111");
        user.setOtpExpiresAt(Instant.now().plusSeconds(600));
        user.setOtpLastSentAt(Instant.now().minusSeconds(61));
        userRepository.save(user);

        MvcResult result = mockMvc.perform(post("/verify/resend")
                        .with(csrf())
                        .param("email", user.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verify?email=resend%40example.com"))
                .andExpect(flash().attributeExists("localOtp"))
                .andReturn();

        String replacement = (String) result.getFlashMap().get("localOtp");
        assertThat(replacement).hasSize(6).isNotEqualTo("111111");

        mockMvc.perform(post("/verify/resend")
                        .with(csrf())
                        .param("email", user.getEmail()))
                .andExpect(flash().attribute(
                        "error",
                        "Wait 60 seconds before requesting another code."
                ));
    }

    @Test
    void oldCodeFailsAfterResend() throws Exception {
        User user = createUnverifiedUser("old-code@example.com", "222222");
        user.setOtpExpiresAt(Instant.now().plusSeconds(600));
        user.setOtpLastSentAt(Instant.now().minusSeconds(61));
        userRepository.save(user);

        mockMvc.perform(post("/verify/resend")
                        .with(csrf())
                        .param("email", user.getEmail()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/verify")
                        .with(csrf())
                        .param("email", user.getEmail())
                        .param("otp", "222222"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Invalid verification code."));
    }

    @Test
    void verificationPageResendsUsingTheEmailTheUserEntered() throws Exception {
        mockMvc.perform(get("/verify"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "formaction=\"/verify/resend\""
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                        "type=\"hidden\" name=\"email\""
                ))));
    }

    private User createUnverifiedUser(String email, String otp) {
        User user = new User();
        user.setName("Verification User");
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setAge(22);
        user.setGender("Prefer not to say");
        user.setDob("2004-01-01");
        user.setOtp(otp);
        user.setVerified(false);
        return userRepository.save(user);
    }
}
