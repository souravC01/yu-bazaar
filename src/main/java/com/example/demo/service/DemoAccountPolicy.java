package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DemoAccountPolicy {
    private final String demoEmail;

    public DemoAccountPolicy(@Value("${app.demo.email}") String demoEmail) {
        this.demoEmail = demoEmail;
    }

    public boolean isDemo(String email) {
        return email != null && demoEmail.equalsIgnoreCase(email.trim());
    }

    public boolean isDemo(Authentication authentication) {
        return authentication != null && isDemo(authentication.getName());
    }

    public void requireWritable(Authentication authentication) {
        if (isDemo(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The public demo account is read-only.");
        }
    }
}
