package com.example.demo;

import com.example.demo.model.Item;
import com.example.demo.model.User;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileWorkflowTests {
    private static final String DEMO_EMAIL = "demo@yubazaar.app";

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
    void profileShowsOnlyTheAuthenticatedOwnersListings() throws Exception {
        String ownerEmail = "owner@gmail.com";
        String otherEmail = "other@gmail.com";
        createUser(ownerEmail, "Owner");
        createUser(otherEmail, "Other User");
        createItem("Owner Desk Lamp", ownerEmail);
        createItem("Other User Book", otherEmail);

        mockMvc.perform(get("/profile").with(user(ownerEmail).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile_view"))
                .andExpect(model().attribute("items", hasSize(1)))
                .andExpect(content().string(containsString("Owner Desk Lamp")))
                .andExpect(content().string(not(containsString("Other User Book"))))
                .andExpect(content().string(containsString("Delete listing")));
    }

    @Test
    void demoProfileShowsItsListingsWithoutWriteControls() throws Exception {
        createUser(DEMO_EMAIL, "YU Bazaar Demo");
        createItem("Demo Listing", DEMO_EMAIL);

        mockMvc.perform(get("/profile").with(user(DEMO_EMAIL).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("items", hasSize(1)))
                .andExpect(content().string(containsString("Demo Listing")))
                .andExpect(content().string(not(containsString("Delete listing"))));
    }

    @Test
    void emptyDemoProfileDoesNotOfferListingCreation() throws Exception {
        createUser(DEMO_EMAIL, "YU Bazaar Demo");

        mockMvc.perform(get("/profile").with(user(DEMO_EMAIL).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("items", hasSize(0)))
                .andExpect(content().string(containsString("read-only demo")))
                .andExpect(content().string(not(containsString("Post your first item"))));
    }

    private User createUser(String email, String name) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setAge(22);
        user.setGender("Prefer not to say");
        user.setDob("2004-01-01");
        user.setVerified(true);
        return userRepository.save(user);
    }

    private Item createItem(String title, String sellerEmail) {
        Item item = new Item();
        item.setTitle(title);
        item.setPrice(15.0);
        item.setWear("used");
        item.setLocation("Scott Library");
        item.setDescription("Working condition");
        item.setSellerEmail(sellerEmail);
        item.setImagePath(null);
        return itemRepository.save(item);
    }
}
