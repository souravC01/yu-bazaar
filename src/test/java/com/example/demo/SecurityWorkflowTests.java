package com.example.demo;

import com.example.demo.model.Item;
import com.example.demo.model.User;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.upload.directory=target/test-uploads")
@AutoConfigureMockMvc
class SecurityWorkflowTests {
    private static final Path TEST_UPLOAD_DIRECTORY = Path.of("target", "test-uploads");
    private static final String DEMO_EMAIL = "demo@yubazaar.app";
    private static final String DEMO_PASSWORD = "Demo@YuBazaar2026";
    private static final String DEMO_PASSWORD_HASH = "$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        itemRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUploads() throws Exception {
        FileSystemUtils.deleteRecursively(TEST_UPLOAD_DIRECTORY);
    }

    @Test
    void anonymousUsersCanBrowseMarketplaceHomeAndProductDetails() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign In")));

        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign In")));
    }

    @Test
    void anonymousUsersAreRedirectedToLoginForProtectedRoutes() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void healthCheckIsPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}"));
    }

    @Test
    void registrationNormalizesEmailAndHashesPassword() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Test Student")
                        .param("email", " STUDENT@MY.YORKU.CA ")
                        .param("age", "22")
                        .param("gender", "Prefer not to say")
                        .param("dob", "2004-01-01")
                        .param("password", "secure-pass")
                        .param("confirmPassword", "secure-pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verify?email=student%40my.yorku.ca"));

        User registeredUser = userRepository.findByEmailIgnoreCase("student@my.yorku.ca").orElseThrow();
        assertThat(registeredUser.getPassword()).isNotEqualTo("secure-pass");
        assertThat(passwordEncoder.matches("secure-pass", registeredUser.getPassword())).isTrue();
        assertThat(registeredUser.isVerified()).isFalse();
    }

    @Test
    void publicEmailCanRegisterAndVerifyAsPublicUser() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Public Seller")
                        .param("email", "public.user@gmail.com")
                        .param("age", "25")
                        .param("gender", "Female")
                        .param("dob", "1999-05-15")
                        .param("password", "secure-pass")
                        .param("confirmPassword", "secure-pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verify?email=public.user%40gmail.com"));

        User publicUser = userRepository.findByEmailIgnoreCase("public.user@gmail.com").orElseThrow();
        assertThat(publicUser.isVerified()).isFalse();
        assertThat(publicUser.isYorkVerified()).isFalse();

        // Verify OTP
        mockMvc.perform(post("/verify")
                        .with(csrf())
                        .param("email", "public.user@gmail.com")
                        .param("otp", publicUser.getOtp()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User verifiedPublicUser = userRepository.findByEmailIgnoreCase("public.user@gmail.com").orElseThrow();
        assertThat(verifiedPublicUser.isVerified()).isTrue();
        assertThat(verifiedPublicUser.isYorkVerified()).isFalse();

        // Can log in
        mockMvc.perform(formLogin("/login")
                        .userParameter("email")
                        .user("public.user@gmail.com")
                        .password("secure-pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(authenticated().withUsername("public.user@gmail.com"));
    }

    @Test
    void yorkEmailReceivesYorkVerifiedStatus() {
        User yorkStudent = new User();
        yorkStudent.setEmail("student@my.yorku.ca");
        yorkStudent.setVerified(true);
        assertThat(yorkStudent.isYorkVerified()).isTrue();

        Item yorkItem = new Item();
        yorkItem.setSellerEmail("student@my.yorku.ca");
        assertThat(yorkItem.isSellerYorkVerified()).isTrue();

        Item publicItem = new Item();
        publicItem.setSellerEmail("seller@gmail.com");
        assertThat(publicItem.isSellerYorkVerified()).isFalse();
    }

    @Test
    void verifiedAccountCanLogIn() throws Exception {
        createUser("student@my.yorku.ca", true);

        mockMvc.perform(formLogin("/login")
                        .userParameter("email")
                        .user("student@my.yorku.ca")
                        .password("secure-pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(authenticated().withUsername("student@my.yorku.ca"));
    }

    @Test
    void publicDemoAccountCanLogInAndOnlySeesReadOnlyControls() throws Exception {
        createDemoUser();

        mockMvc.perform(formLogin("/login")
                        .userParameter("email")
                        .user(DEMO_EMAIL)
                        .password(DEMO_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(authenticated().withUsername(DEMO_EMAIL));

        mockMvc.perform(get("/home").with(user(DEMO_EMAIL).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Public demo mode is read-only")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("id=\"addItemButton\"")
                )));
    }

    @Test
    void newListingUsesAuthenticatedSellerIdentity() throws Exception {
        String sellerEmail = "seller@my.yorku.ca";
        createUser(sellerEmail, true);
        MockHttpSession session = listingSession("new-listing-token");
        MockMultipartFile image = new MockMultipartFile(
                "image", "listing.png", "image/png", new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/add-item")
                        .file(image)
                        .param("title", "Course Textbook")
                        .param("price", "25.00")
                        .param("wear", "used")
                        .param("location", "Scott Library")
                        .param("description", "Clean copy")
                        .param("submissionToken", "new-listing-token")
                        .session(session)
                        .with(user(sellerEmail).roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        Item item = itemRepository.findAll().get(0);
        assertThat(item.getSellerEmail()).isEqualTo(sellerEmail);
        assertThat(item.getImagePath()).endsWith(".png");

        mockMvc.perform(get("/media/" + item.getImagePath())
                        .with(user(sellerEmail).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void repeatedListingSubmissionTokenCreatesOnlyOneItem() throws Exception {
        String sellerEmail = "seller@my.yorku.ca";
        createUser(sellerEmail, true);
        MockHttpSession session = listingSession("single-use-token");

        mockMvc.perform(listingRequest(sellerEmail, session, "single-use-token", "first.png"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        mockMvc.perform(listingRequest(sellerEmail, session, "single-use-token", "second.png"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        assertThat(itemRepository.findAll()).hasSize(1);
    }

    @Test
    void publicDemoAccountCannotCreateListings() throws Exception {
        MockHttpSession session = listingSession("demo-token");

        mockMvc.perform(listingRequest(DEMO_EMAIL, session, "demo-token", "demo.png"))
                .andExpect(status().isForbidden());

        assertThat(itemRepository.count()).isZero();
    }

    @Test
    void publicDemoAccountCannotSendInquiriesOrDeleteListings() throws Exception {
        mockMvc.perform(post("/send-inquiry")
                        .param("itemId", "1")
                        .param("message", "Is this available?")
                        .with(user(DEMO_EMAIL).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/delete-item")
                        .param("id", "1")
                        .with(user(DEMO_EMAIL).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyListingOwnerCanDeleteListing() throws Exception {
        String ownerEmail = "owner@my.yorku.ca";
        String otherEmail = "other@my.yorku.ca";
        createUser(ownerEmail, true);
        createUser(otherEmail, true);

        Item item = new Item();
        item.setTitle("Desk Lamp");
        item.setPrice(15.0);
        item.setWear("used");
        item.setLocation("York Lanes");
        item.setDescription("Working condition");
        item.setSellerEmail(ownerEmail);
        item.setImagePath("00000000-0000-0000-0000-000000000000.png");
        item = itemRepository.save(item);

        mockMvc.perform(post("/delete-item")
                        .param("id", item.getId().toString())
                        .with(user(otherEmail).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(itemRepository.existsById(item.getId())).isTrue();

        mockMvc.perform(post("/delete-item")
                        .param("id", item.getId().toString())
                        .with(user(ownerEmail).roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
        assertThat(itemRepository.existsById(item.getId())).isFalse();
    }

    @Test
    void yorkOnlyFilterReturnsOnlyYorkListings() throws Exception {
        String yorkSeller = "student@my.yorku.ca";
        String publicSeller = "seller@gmail.com";
        createUser(yorkSeller, true);
        createUser(publicSeller, true);

        Item yorkItem = new Item();
        yorkItem.setTitle("EECS Notes");
        yorkItem.setPrice(10.0);
        yorkItem.setWear("new");
        yorkItem.setLocation("Vari Hall");
        yorkItem.setSellerEmail(yorkSeller);
        yorkItem.setImagePath("york.png");
        itemRepository.save(yorkItem);

        Item publicItem = new Item();
        publicItem.setTitle("Desk Chair");
        publicItem.setPrice(40.0);
        publicItem.setWear("used");
        publicItem.setLocation("Off Campus");
        publicItem.setSellerEmail(publicSeller);
        publicItem.setImagePath("public.png");
        itemRepository.save(publicItem);

        // Standard /home returns both
        mockMvc.perform(get("/home").with(user(yorkSeller).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EECS Notes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Desk Chair")));

        // yorkOnly=true returns only EECS Notes
        mockMvc.perform(get("/home").param("yorkOnly", "true").with(user(yorkSeller).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EECS Notes")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Desk Chair"))));
    }

    @Test
    void productDetailsShowsCorrectBadgeForYorkAndPublicSellers() throws Exception {
        String yorkSeller = "student@my.yorku.ca";
        String publicSeller = "seller@gmail.com";
        createUser(yorkSeller, true);
        createUser(publicSeller, true);

        Item yorkItem = new Item();
        yorkItem.setTitle("York Textbook");
        yorkItem.setPrice(30.0);
        yorkItem.setWear("used");
        yorkItem.setLocation("Scott Library");
        yorkItem.setSellerEmail(yorkSeller);
        yorkItem.setImagePath("york.png");
        yorkItem = itemRepository.save(yorkItem);

        Item publicItem = new Item();
        publicItem.setTitle("Public Keyboard");
        publicItem.setPrice(20.0);
        publicItem.setWear("new");
        publicItem.setLocation("Keele");
        publicItem.setSellerEmail(publicSeller);
        publicItem.setImagePath("public.png");
        publicItem = itemRepository.save(publicItem);

        // York item details should show "York Verified Student" and NOT "Public Seller"
        mockMvc.perform(get("/product/" + yorkItem.getId()).with(user(yorkSeller).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("York Verified Student")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Public Seller"))));

        // Public item details should show "Public Seller" and NOT "York Verified Student"
        mockMvc.perform(get("/product/" + publicItem.getId()).with(user(publicSeller).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Public Seller")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("York Verified Student"))));
    }

    private User createUser(String email, boolean verified) {
        User user = new User();
        user.setName("Test Student");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("secure-pass"));
        user.setAge(22);
        user.setGender("Prefer not to say");
        user.setDob("2004-01-01");
        user.setVerified(verified);
        return userRepository.save(user);
    }

    private void createDemoUser() {
        jdbcTemplate.update(
                "INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "YU Bazaar Demo", DEMO_EMAIL, DEMO_PASSWORD_HASH, 21, "Prefer not to say", "2005-01-01", null, true
        );
    }

    private MockHttpSession listingSession(String token) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("listingSubmissionToken", token);
        return session;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder listingRequest(
            String sellerEmail,
            MockHttpSession session,
            String token,
            String imageName) {
        MockMultipartFile image = new MockMultipartFile(
                "image", imageName, "image/png", new byte[]{1, 2, 3}
        );
        return multipart("/add-item")
                .file(image)
                .param("title", "Course Textbook")
                .param("price", "25.00")
                .param("wear", "used")
                .param("location", "Scott Library")
                .param("description", "Clean copy")
                .param("submissionToken", token)
                .session(session)
                .with(user(sellerEmail).roles("USER"))
                .with(csrf());
    }
}
