package com.example.demo;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class YuBazaarApplicationTests {

	@Autowired
	private UserRepository userRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void persistsOtpLifecycleTimestamps() {
		User user = new User();
		user.setName("Verification Student");
		user.setEmail("verification@example.com");
		user.setPassword("encoded-password");
		user.setAge(22);
		user.setGender("Prefer not to say");
		user.setDob("2004-01-01");
		user.setOtp("123456");
		user.setOtpExpiresAt(Instant.parse("2026-09-01T18:10:00Z"));
		user.setOtpLastSentAt(Instant.parse("2026-09-01T18:00:00Z"));

		User saved = userRepository.saveAndFlush(user);
		User reloaded = userRepository.findById(saved.getId()).orElseThrow();

		assertThat(reloaded.getOtpExpiresAt()).isEqualTo(Instant.parse("2026-09-01T18:10:00Z"));
		assertThat(reloaded.getOtpLastSentAt()).isEqualTo(Instant.parse("2026-09-01T18:00:00Z"));
	}

}
