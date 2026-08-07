package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.DemoAccountPolicy;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserAdvice {
    private final UserRepository userRepository;
    private final DemoAccountPolicy demoAccountPolicy;

    public CurrentUserAdvice(UserRepository userRepository, DemoAccountPolicy demoAccountPolicy) {
        this.userRepository = userRepository;
        this.demoAccountPolicy = demoAccountPolicy;
    }

    @ModelAttribute
    public void addCurrentUser(Authentication authentication, Model model) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }

        userRepository.findByEmailIgnoreCase(authentication.getName())
                .map(User::getName)
                .ifPresent(name -> model.addAttribute("userName", name));
        model.addAttribute("currentUserEmail", authentication.getName());
        model.addAttribute("isDemo", demoAccountPolicy.isDemo(authentication));
    }
}
