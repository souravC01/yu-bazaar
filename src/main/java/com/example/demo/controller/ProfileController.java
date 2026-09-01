package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ProfileController {
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public ProfileController(UserRepository userRepository, ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping("/profile")
    public String viewProfile(Authentication authentication, Model model) {
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("user", user);
        model.addAttribute(
                "items",
                itemRepository.findBySellerEmailIgnoreCaseOrderByIdDesc(authentication.getName())
        );
        return "profile_view";
    }
}
