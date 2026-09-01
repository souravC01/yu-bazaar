# YU Bazaar Polished MVP Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a reliable portfolio-ready YU Bazaar MVP whose public-seller model, York verification tier, listing workflow, recruiter demo, profile, seeded content, and documentation behave consistently.

**Architecture:** Preserve the single Spring Boot deployable and deepen two existing seams: listing rules become one reusable policy consumed by the controller and template, and verification-code lifecycle moves from the MVC controller into a focused account-verification module. Flyway applies additive, production-safe schema changes; existing security, storage, mail, and repository adapters remain in place.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Spring MVC, Spring Security, Spring Data JPA, Thymeleaf, Flyway, PostgreSQL, H2, JUnit 5, MockMvc, Maven

**Spec:** `docs/superpowers/specs/2026-09-01-polished-mvp-release.md`

## Global Constraints

- Keep one Spring Boot deployable; do not add RabbitMQ or microservices.
- Preserve public browsing and public `/media/{key}` access.
- Preserve public registration with York Verified and Public Seller tiers.
- Preserve server-enforced read-only behavior for `demo@yubazaar.app`.
- Use forward-only Flyway migrations that retain existing users, listings, and UUID-backed images.
- Use test-driven development: add each regression test and observe the expected failure before changing production code.
- Do not add payments, chat, moderation, ratings, multiple images, saved listings, or a general rate-limiting subsystem.

---

### Task 1: Establish production-safe migrations

**Files:**
- Create: `src/main/resources/db/migration/V5__add_otp_lifecycle.sql`
- Create: `src/main/resources/db/migration/V6__normalize_seed_listing_images.sql`
- Modify: `src/main/java/com/example/demo/model/User.java`
- Test: `src/test/java/com/example/demo/YuBazaarApplicationTests.java`

**Interfaces:**
- Produces: nullable `User.getOtpExpiresAt()`, `User.setOtpExpiresAt(Instant)`, `User.getOtpLastSentAt()`, and `User.setOtpLastSentAt(Instant)` persistence fields.
- Produces: seeded items with `image_path IS NULL`; uploaded UUID image keys remain unchanged.

- [ ] **Step 1: Add a failing migration/entity mapping test**

Extend `YuBazaarApplicationTests` with repository assertions that the context can save and reload both OTP timestamps:

```java
@Autowired
private UserRepository userRepository;

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
```

- [ ] **Step 2: Run the focused test and confirm RED**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=YuBazaarApplicationTests test
```

Expected: test compilation fails because the OTP lifecycle accessors do not exist.

- [ ] **Step 3: Add the entity fields and migrations**

Add to `User`:

```java
@Column(name = "otp_expires_at")
private Instant otpExpiresAt;

@Column(name = "otp_last_sent_at")
private Instant otpLastSentAt;
```

Add conventional getters and setters for both fields.

Create `V5__add_otp_lifecycle.sql`:

```sql
ALTER TABLE users ADD COLUMN otp_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN otp_last_sent_at TIMESTAMP WITH TIME ZONE;
```

Create `V6__normalize_seed_listing_images.sql`:

```sql
UPDATE item
SET image_path = NULL
WHERE image_path = 'default-listing.png';
```

- [ ] **Step 4: Run the focused test and confirm GREEN**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=YuBazaarApplicationTests test
```

Expected: the context applies all six migrations and the timestamp persistence test passes.

- [ ] **Step 5: Commit the migration slice**

```powershell
git add src/main/resources/db/migration/V5__add_otp_lifecycle.sql src/main/resources/db/migration/V6__normalize_seed_listing_images.sql src/main/java/com/example/demo/model/User.java src/test/java/com/example/demo/YuBazaarApplicationTests.java
git commit -m "feat: add verification lifecycle fields"
```

---

### Task 2: Centralize listing rules and form options

**Files:**
- Create: `src/main/java/com/example/demo/service/ListingPolicy.java`
- Create: `src/test/java/com/example/demo/ListingPolicyTests.java`
- Modify: `src/main/java/com/example/demo/controller/ItemController.java`
- Modify: `src/main/resources/templates/home_page.html`
- Test: `src/test/java/com/example/demo/SecurityWorkflowTests.java`

**Interfaces:**
- Produces: `ListingPolicy.conditions()`, `ListingPolicy.locations()`, and `ListingPolicy.validate(String, double, String, String, String)`.
- Produces: `ListingPolicy.ConditionOption(String value, String label)` and `ListingPolicy.ValidationResult(boolean valid, String message)`.
- Produces model attributes `listingConditions` and `listingLocations` for `home_page.html`.

- [ ] **Step 1: Write failing policy tests**

Create `ListingPolicyTests` with these behaviors:

```java
class ListingPolicyTests {
    private final ListingPolicy policy = new ListingPolicy();

    @Test
    void acceptsEveryAdvertisedConditionAndLocation() {
        for (ListingPolicy.ConditionOption condition : policy.conditions()) {
            for (String location : policy.locations()) {
                assertThat(policy.validate("Desk lamp", 20.00, condition.value(), location, "Working"))
                        .isEqualTo(ListingPolicy.ValidationResult.valid());
            }
        }
    }

    @Test
    void rejectsOversizedAndOutOfRangeListingValues() {
        assertThat(policy.validate("x".repeat(121), 20, "used", "Scott Library", "Working").message())
                .isEqualTo("Title must be 120 characters or fewer.");
        assertThat(policy.validate("Lamp", 100000.01, "used", "Scott Library", "Working").message())
                .isEqualTo("Price must be between $0.00 and $100,000.00.");
        assertThat(policy.validate("Lamp", 20, "used", "Scott Library", "x".repeat(256)).message())
                .isEqualTo("Description must be 255 characters or fewer.");
    }
}
```

- [ ] **Step 2: Run the policy tests and confirm RED**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=ListingPolicyTests test
```

Expected: test compilation fails because `ListingPolicy` does not exist.

- [ ] **Step 3: Implement the focused listing policy**

Create a Spring `@Component` with immutable options and these limits:

```java
public static final int MAX_TITLE_LENGTH = 120;
public static final int MAX_DESCRIPTION_LENGTH = 255;
public static final double MAX_PRICE = 100_000.00;

private static final List<ConditionOption> CONDITIONS = List.of(
        new ConditionOption("new", "Brand New"),
        new ConditionOption("used (like new)", "Used (Like New)"),
        new ConditionOption("used", "Used (Good)"),
        new ConditionOption("poor", "Fair / Poor")
);

private static final List<String> LOCATIONS = List.of(
        "Vari Hall",
        "Scott Library",
        "Student Centre",
        "Bergeron Centre for Engineering Excellence",
        "Steacie Science and Engineering Library",
        "York Lanes",
        "Accolade Building East",
        "Accolade Building West",
        "Archives of Ontario",
        "Atkinson",
        "Bennett Centre for Student Services",
        "Behavioural Sciences Building",
        "Burton Auditorium",
        "Calumet College",
        "Chemistry Building",
        "Computer Methods Building",
        "Central Square",
        "Central Utilities Building",
        "Centre for Film and Theatre",
        "Curtis Lecture Halls",
        "Dahdaleh Building",
        "Executive Learning Centre",
        "Farquharson Life Sciences",
        "Founders College",
        "Founders Tennis Court",
        "Frost Library (Glendon campus)",
        "Glendon Hall (Glendon campus)",
        "Hart House (Osgoode Hall Law School)",
        "Health, Nursing and Environmental Studies Building",
        "Hilliard Residence (Glendon campus)",
        "Ignat Kaneff Building",
        "Kaneff Tower",
        "Kinsmen Building",
        "Lassonde Building",
        "LA&PS @ IBM Markham",
        "Life Sciences Building",
        "Lorna R. Marsden Honours Court & Welcome Centre",
        "Lumbers Building",
        "McLaughlin College",
        "Norman Bethune College",
        "Petrie Science and Engineering Building",
        "Physical Resources Building",
        "Rob and Cheryl McEwen Graduate Study Building",
        "Ross Building - North wing",
        "Ross Building - South wing",
        "Seneca @ York",
        "Seymour Schulich Building",
        "Sheridan College - Trafalgar Campus",
        "Sherman Health Science Research Centre",
        "Stedman Lecture Halls",
        "Stong College",
        "Tait McKenzie Centre",
        "Tait Tennis Courts",
        "Technology and Enhanced Learning Building",
        "Tennis Canada",
        "The Joan & Martin Goldfarb Centre for Fine Arts",
        "Track and Field Centre",
        "Vanier College",
        "West Office Building",
        "William Small Centre",
        "Winters College",
        "York Hall (Glendon campus)",
        "Off Campus"
);

public record ConditionOption(String value, String label) {}

public record ValidationResult(boolean valid, String message) {
    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String message) {
        return new ValidationResult(false, message);
    }
}
```

Return `CONDITIONS` from `conditions()` and `LOCATIONS` from `locations()`. Implement validation in this order: required title, title length, finite price/range, known condition, known location, description length.

- [ ] **Step 4: Replace controller duplication and render from the policy**

Inject `ListingPolicy` into `ItemController`, remove `VALID_WEAR_OPTIONS` and `VALID_LOCATIONS`, and use:

```java
ListingPolicy.ValidationResult validation = listingPolicy.validate(title, price, wear, location, description);
if (!validation.valid()) {
    return homeWithError(model, session, validation.message(), authentication);
}
```

In `prepareHomeModel`, add:

```java
model.addAttribute("listingConditions", listingPolicy.conditions());
model.addAttribute("listingLocations", listingPolicy.locations());
```

Replace hard-coded `<option>` elements in `home_page.html` with:

```html
<option value="">-- Condition --</option>
<option th:each="condition : ${listingConditions}"
        th:value="${condition.value}"
        th:text="${condition.label}"></option>
```

```html
<option value="">-- Select York Campus Location --</option>
<option th:each="location : ${listingLocations}"
        th:value="${location}"
        th:text="${location}"></option>
```

Add `maxlength="120"` to the title, `max="100000"` to the price, and `maxlength="255"` to the description.

- [ ] **Step 5: Add a controller regression test for a formerly rejected location**

In `SecurityWorkflowTests`, submit a listing using `Off Campus` and assert a redirect to `/home` plus a persisted item whose location is `Off Campus`. Reuse the existing authenticated user, session token, image, and CSRF setup from `newListingUsesAuthenticatedSellerIdentity`.

- [ ] **Step 6: Run listing tests and confirm GREEN**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=ListingPolicyTests,SecurityWorkflowTests test
```

Expected: all policy and security workflow tests pass.

- [ ] **Step 7: Commit the listing-policy slice**

```powershell
git add src/main/java/com/example/demo/service/ListingPolicy.java src/main/java/com/example/demo/controller/ItemController.java src/main/resources/templates/home_page.html src/test/java/com/example/demo/ListingPolicyTests.java src/test/java/com/example/demo/SecurityWorkflowTests.java
git commit -m "fix: align listing options and validation"
```

---

### Task 3: Validate seller inquiries

**Files:**
- Modify: `src/main/java/com/example/demo/controller/ItemController.java`
- Modify: `src/main/resources/templates/product_page.html`
- Test: `src/test/java/com/example/demo/SecurityWorkflowTests.java`

**Interfaces:**
- Produces: inquiry messages constrained to 1 through 1,000 non-whitespace characters.
- Preserves: authenticated, non-demo inquiry delivery through `EmailSender`.

- [ ] **Step 1: Add failing inquiry validation tests**

In `SecurityWorkflowTests`, create buyer, seller, and item records, then assert:

```java
mockMvc.perform(post("/send-inquiry")
                .param("itemId", item.getId().toString())
                .param("message", "   ")
                .with(user(buyerEmail).roles("USER"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("product_page"))
        .andExpect(model().attribute("error", "Enter a message for the seller."));

mockMvc.perform(post("/send-inquiry")
                .param("itemId", item.getId().toString())
                .param("message", "x".repeat(1001))
                .with(user(buyerEmail).roles("USER"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(model().attribute("error", "Message must be 1,000 characters or fewer."));
```

- [ ] **Step 2: Run the focused tests and confirm RED**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=SecurityWorkflowTests test
```

Expected: the blank and oversized inquiry assertions fail because the controller currently accepts both.

- [ ] **Step 3: Implement minimal inquiry validation**

In `sendInquiry`, load the item, populate `item` and `isOwner`, and return `product_page` with the exact errors above before loading the buyer or sending email. Trim the accepted message once and use the trimmed value in the email body.

In `product_page.html`, add `required maxlength="1000"` to the inquiry `<textarea>` and a short `1,000 characters maximum` hint.

- [ ] **Step 4: Run the focused tests and confirm GREEN**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=SecurityWorkflowTests test
```

Expected: inquiry validation and all existing security workflow tests pass.

- [ ] **Step 5: Commit the inquiry slice**

```powershell
git add src/main/java/com/example/demo/controller/ItemController.java src/main/resources/templates/product_page.html src/test/java/com/example/demo/SecurityWorkflowTests.java
git commit -m "fix: validate seller inquiry messages"
```

---

### Task 4: Encapsulate the verification-code lifecycle

**Files:**
- Create: `src/main/java/com/example/demo/service/AccountVerificationService.java`
- Create: `src/test/java/com/example/demo/AccountVerificationWorkflowTests.java`
- Modify: `src/main/java/com/example/demo/controller/TemplateController.java`
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/resources/templates/verify_otp.html`

**Interfaces:**
- Produces: `AccountVerificationService.issueInitialCode(User)` returning `IssueResult(String code, boolean delivered)`.
- Produces: `AccountVerificationService.verify(String email, String code)` returning `VerificationResult`.
- Produces: `AccountVerificationService.resend(String email)` returning `ResendResult(ResendStatus status, String localCode)`.
- Produces enums: `VerificationResult { VERIFIED, USER_NOT_FOUND, INVALID_CODE, EXPIRED_CODE }` and `ResendStatus { SENT, COOLDOWN, NOT_AVAILABLE }`.

- [ ] **Step 1: Write failing verification workflow tests**

Create `AccountVerificationWorkflowTests` as `@SpringBootTest` plus `@AutoConfigureMockMvc`. Cover these behaviors with persisted users:

```java
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
            .andExpect(model().attribute("error", "This verification code has expired. Request a new code."));

    User reloaded = userRepository.findById(user.getId()).orElseThrow();
    assertThat(reloaded.isVerified()).isFalse();
    assertThat(reloaded.getOtp()).isNull();
}
```

```java
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
            .andExpect(flash().attribute("error", "Wait 60 seconds before requesting another code."));
}
```

Also add a successful unexpired verification test and a test proving the old code fails after resend.

- [ ] **Step 2: Run the workflow tests and confirm RED**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=AccountVerificationWorkflowTests test
```

Expected: test compilation or route assertions fail because the verification module and resend endpoint do not exist.

- [ ] **Step 3: Implement `AccountVerificationService`**

Use `SecureRandom`, `UserRepository`, `EmailSender`, and these properties:

```properties
app.verification.ttl=PT10M
app.verification.resend-cooldown=PT60S
```

The module must:

```java
public enum VerificationResult {
    VERIFIED, USER_NOT_FOUND, INVALID_CODE, EXPIRED_CODE
}

public enum ResendStatus {
    SENT, COOLDOWN, NOT_AVAILABLE
}

public record IssueResult(String code, boolean delivered) {}

public record ResendResult(ResendStatus status, String localCode) {}

private String generateCode() {
    return String.format("%06d", secureRandom.nextInt(1_000_000));
}
```

- Set `otp`, `otpExpiresAt = now + ttl`, and `otpLastSentAt = now` before sending.
- Treat a missing expiration as expired.
- Clear `otp` and `otpExpiresAt` after success or expiry.
- Reject resend when `otpLastSentAt + cooldown` is after the current time.
- Refuse resend for missing or already verified accounts using `NOT_AVAILABLE`.
- Return the code in `IssueResult`/`ResendResult` so the controller can expose it only when `app.mail.show-local-codes=true`.

- [ ] **Step 4: Move controller behavior behind the module**

Inject `AccountVerificationService` into `TemplateController`. Remove `generateOtp()` and direct OTP comparison.

During registration, save the new user, call `issueInitialCode(user)`, and retain the returned delivery/code values for the current redirect behavior.

Map `VerificationResult` to:

- `VERIFIED`: redirect to `/login` with success.
- `USER_NOT_FOUND`: `User not found.`
- `INVALID_CODE`: `Invalid verification code.`
- `EXPIRED_CODE`: `This verification code has expired. Request a new code.`

Add:

```java
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
```

Use the exact messages above in the workflow tests. The `NOT_AVAILABLE` response does not expose whether an unrelated email address exists.

- [ ] **Step 5: Add the resend form to `verify_otp.html`**

Replace the current “try again” text with a POST form containing the email as a hidden field and a `Resend code` button. Preserve Thymeleaf CSRF integration and the existing local-code display.

- [ ] **Step 6: Run verification and security tests and confirm GREEN**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=AccountVerificationWorkflowTests,SecurityWorkflowTests test
```

Expected: verification lifecycle and existing registration/login tests pass.

- [ ] **Step 7: Commit the verification slice**

```powershell
git add src/main/java/com/example/demo/service/AccountVerificationService.java src/main/java/com/example/demo/controller/TemplateController.java src/main/resources/application.properties src/main/resources/templates/verify_otp.html src/test/java/com/example/demo/AccountVerificationWorkflowTests.java
git commit -m "feat: add expiring verification codes"
```

---

### Task 5: Correct email and public-tier copy

**Files:**
- Modify: `src/main/java/com/example/demo/Email/EmailSender.java`
- Modify: `src/main/java/com/example/demo/Email/EmailTemplate.java`
- Modify: `src/main/resources/templates/login_page.html`
- Modify: `src/main/resources/templates/home_page.html`
- Modify: `src/main/resources/templates/product_page.html`
- Modify: `src/main/resources/templates/profile_view.html`
- Test: `src/test/java/com/example/demo/EmailSenderTests.java`
- Test: `src/test/java/com/example/demo/SecurityWorkflowTests.java`

**Interfaces:**
- Produces: consistent “York Verified Student” and “Public Seller” language.
- Produces: verification email stating a ten-minute expiry and registration email stating verification is still required.

- [ ] **Step 1: Add failing copy assertions**

Extend `EmailSenderTests` to capture `sendOtpEmail` and assert the body contains `expires in 10 minutes` rather than claiming loosely that the OTP is valid. Add an `EmailTemplate.REGISTRATION_SUCCESS` assertion that the body contains `verify your email before signing in` and does not contain `account is now active`.

In `SecurityWorkflowTests`, assert anonymous `/login` contains `York Verified Student or Public Seller` and does not contain `Must be an active @my.yorku.ca or @yorku.ca email`.

- [ ] **Step 2: Run the copy tests and confirm RED**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=EmailSenderTests,SecurityWorkflowTests test
```

Expected: assertions fail against the current email and sign-in language.

- [ ] **Step 3: Update copy without changing workflows**

Use these canonical statements throughout affected files:

- `Open to York community members and public sellers.`
- `Verify any valid email to sell. York email addresses receive York Verified Student status.`
- `Sign in as a York Verified Student or Public Seller.`

Update the OTP email to say the code expires in ten minutes. Update the registration-success template to say registration was received and verification is required before sign-in. Remove footer and hero claims that the marketplace is restricted exclusively to York addresses.

- [ ] **Step 4: Run the copy tests and confirm GREEN**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=EmailSenderTests,SecurityWorkflowTests test
```

Expected: copy assertions and existing workflows pass.

- [ ] **Step 5: Commit the copy slice**

```powershell
git add src/main/java/com/example/demo/Email/EmailSender.java src/main/java/com/example/demo/Email/EmailTemplate.java src/main/resources/templates/login_page.html src/main/resources/templates/home_page.html src/main/resources/templates/product_page.html src/main/resources/templates/profile_view.html src/test/java/com/example/demo/EmailSenderTests.java src/test/java/com/example/demo/SecurityWorkflowTests.java
git commit -m "fix: align verification tier messaging"
```

---

### Task 6: Polish public demo access and public media behavior

**Files:**
- Modify: `src/main/resources/templates/login_page.html`
- Modify: `src/main/resources/templates/home_page.html`
- Modify: `src/main/resources/templates/product_page.html`
- Test: `src/test/java/com/example/demo/SecurityWorkflowTests.java`

**Interfaces:**
- Produces: an anonymous-visible `Try the read-only demo` control that fills the public credentials.
- Produces: explicit static placeholder rendering when `item.imagePath == null`.
- Preserves: anonymous access to valid UUID-backed `/media/{key}` resources.

- [ ] **Step 1: Add failing demo, media, and placeholder tests**

In `SecurityWorkflowTests`, assert anonymous `/login` contains `Try the read-only demo` and the public demo email.

Extend the uploaded-media test to perform a second request without `.with(user(...))`:

```java
mockMvc.perform(get("/media/" + item.getImagePath()))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(content().bytes(new byte[]{1, 2, 3}));
```

Persist an item with `imagePath = null`, request its home and product pages, and assert each contains `/images/YUB_Logo.jpg` while neither contains `/media/default-listing.png`.

- [ ] **Step 2: Run the focused tests and confirm RED**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=SecurityWorkflowTests test
```

Expected: demo and explicit-placeholder assertions fail; anonymous media should already pass and documents the intended policy.

- [ ] **Step 3: Make demo access and placeholders explicit**

Remove the authenticated `isDemo` condition around the login-page quick-fill block. Label the button `Try the read-only demo` and retain the intentionally public credentials already present in its JavaScript.

In both listing-card and product-page images, render:

```html
<img th:if="${item.imagePath != null}"
     th:src="@{/media/{filename}(filename=${item.imagePath})}"
     onerror="this.onerror=null; this.src='/images/YUB_Logo.jpg';"
     alt="Listing photo">
<img th:unless="${item.imagePath != null}"
     th:src="@{/images/YUB_Logo.jpg}"
     alt="YU Bazaar listing placeholder">
```

Keep the existing page-specific CSS classes and lazy-loading attributes on the appropriate tags.

- [ ] **Step 4: Run the focused tests and confirm GREEN**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=SecurityWorkflowTests test
```

Expected: demo, placeholder, public-media, and existing demo-write restriction tests pass.

- [ ] **Step 5: Commit the demo/media slice**

```powershell
git add src/main/resources/templates/login_page.html src/main/resources/templates/home_page.html src/main/resources/templates/product_page.html src/test/java/com/example/demo/SecurityWorkflowTests.java
git commit -m "fix: polish demo and listing placeholders"
```

---

### Task 7: Show owned listings on the profile

**Files:**
- Modify: `src/main/java/com/example/demo/repository/ItemRepository.java`
- Modify: `src/main/java/com/example/demo/controller/ProfileController.java`
- Modify: `src/main/resources/templates/profile_view.html`
- Create: `src/test/java/com/example/demo/ProfileWorkflowTests.java`

**Interfaces:**
- Produces: `ItemRepository.findBySellerEmailIgnoreCaseOrderByIdDesc(String email)`.
- Produces: profile model attribute `items` containing only the authenticated user's listings.
- Consumes: global `isDemo` attribute from `CurrentUserAdvice` and existing `/delete-item` owner protection.

- [ ] **Step 1: Write failing profile workflow tests**

Create `ProfileWorkflowTests` using `@SpringBootTest` and `@AutoConfigureMockMvc`. Persist an owner, a second user, one item for each, then assert:

```java
mockMvc.perform(get("/profile").with(user(ownerEmail).roles("USER")))
        .andExpect(status().isOk())
        .andExpect(view().name("profile_view"))
        .andExpect(model().attribute("items", hasSize(1)))
        .andExpect(content().string(containsString("Owner Desk Lamp")))
        .andExpect(content().string(not(containsString("Other User Book"))))
        .andExpect(content().string(containsString("Delete listing")));
```

Add a demo-authenticated request asserting the profile contains its listing but does not contain `Delete listing`.

- [ ] **Step 2: Run profile tests and confirm RED**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=ProfileWorkflowTests test
```

Expected: the `items` model assertion fails because the profile currently loads only the user.

- [ ] **Step 3: Add the repository query and controller model**

Add to `ItemRepository`:

```java
List<Item> findBySellerEmailIgnoreCaseOrderByIdDesc(String sellerEmail);
```

Inject `ItemRepository` into `ProfileController` and add:

```java
model.addAttribute(
        "items",
        itemRepository.findBySellerEmailIgnoreCaseOrderByIdDesc(authentication.getName())
);
```

- [ ] **Step 4: Render the owned-listings section**

In `profile_view.html`, add an `Your Listings` section after account details:

- Empty state linking to `/home` when `items.isEmpty()`.
- One card per item with title, price, location, and link to `/product/{id}`.
- A CSRF-protected POST form to `/delete-item` with hidden `id` only when `!isDemo`.
- Static placeholder for null image paths and `/media/{key}` for UUID-backed paths.

Reuse existing button, card, badge, and alert classes; add only small profile-specific rules inside the template if no existing class fits.

- [ ] **Step 5: Run profile and ownership tests and confirm GREEN**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=ProfileWorkflowTests,SecurityWorkflowTests test
```

Expected: profile filtering, demo read-only rendering, and owner-only deletion tests pass.

- [ ] **Step 6: Commit the profile slice**

```powershell
git add src/main/java/com/example/demo/repository/ItemRepository.java src/main/java/com/example/demo/controller/ProfileController.java src/main/resources/templates/profile_view.html src/test/java/com/example/demo/ProfileWorkflowTests.java
git commit -m "feat: show owned listings on profiles"
```

---

### Task 8: Remove dead navigation and finalize documentation

**Files:**
- Modify: `src/main/resources/templates/home_page.html`
- Modify: `README.md`
- Test: `src/test/java/com/example/demo/SecurityWorkflowTests.java`

**Interfaces:**
- Produces: a footer containing only functioning local routes and the repository URL.
- Produces: README claims that match tested behavior.

- [ ] **Step 1: Add a failing footer regression test**

In `SecurityWorkflowTests`, request `/` anonymously and assert the rendered HTML does not contain `href="#terms"`, `href="#privacy"`, `href="#security"`, or `href="#vari-hall"`. Assert it contains `https://github.com/souravC01/yu-bazaar`.

- [ ] **Step 2: Run the footer test and confirm RED**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=SecurityWorkflowTests test
```

Expected: the dead-anchor assertions fail.

- [ ] **Step 3: Replace placeholder footer navigation**

Keep three compact footer groups:

- Marketplace: `/`, `/home?yorkOnly=true`, and `/search?keyword=Textbook`.
- Account: `/login`, `/register`, and authenticated `/profile`.
- Project: the GitHub repository and the existing on-page campus-safety message without a fake link.

Do not create empty legal, terms, privacy, or help pages.

- [ ] **Step 4: Update README behavior and architecture statements**

Document:

- Public Seller registration and York Verified Student status.
- Public listing and photograph access.
- Ten-minute verification codes and sixty-second resend cooldown.
- Read-only demo quick access and restrictions.
- Profile-owned listings.
- Intentional placeholders for seeded listings.

Remove the claim that `/media/{key}` requires sign-in. Keep the single-deployable Render/Neon/R2/Brevo architecture and do not mention RabbitMQ or microservices as implemented work.

- [ ] **Step 5: Run the footer test and confirm GREEN**

Run:

```powershell
.\mvnw.cmd --batch-mode -Dtest=SecurityWorkflowTests test
```

Expected: no dead anchors remain and the GitHub project link is rendered.

- [ ] **Step 6: Commit documentation and navigation**

```powershell
git add src/main/resources/templates/home_page.html README.md src/test/java/com/example/demo/SecurityWorkflowTests.java
git commit -m "docs: align portfolio release behavior"
```

---

### Task 9: Complete release verification

**Files:**
- Modify only if verification exposes a release-blocking defect in a file already covered by Tasks 1–8.
- Verify: `target/surefire-reports/`
- Verify: `target/yu-bazaar-0.0.1-SNAPSHOT.jar`

**Interfaces:**
- Consumes: all production and test behavior from Tasks 1–8.
- Produces: a green Maven test suite, production package, clean working tree except planned commits, and a recorded manual release checklist.

- [ ] **Step 1: Run the complete automated suite**

Run:

```powershell
.\mvnw.cmd --batch-mode test
```

Expected: every test passes with zero failures and zero errors.

- [ ] **Step 2: Build the production artifact**

Run:

```powershell
.\mvnw.cmd --batch-mode clean package
```

Expected: `BUILD SUCCESS` and `target/yu-bazaar-0.0.1-SNAPSHOT.jar` exists.

- [ ] **Step 3: Exercise local user journeys**

Run the application with the default development profile and verify:

1. Guest: browse, search, filter, product page, and listing image.
2. Public Seller: register, see local code, verify, sign in, create listing, view profile listing, inquire, and delete own listing.
3. York Seller: repeat registration and confirm York Verified Student presentation.
4. Demo: use quick-fill, browse/profile, and confirm create/inquiry/delete controls are absent and direct POSTs remain forbidden.
5. Verification: expire a code in H2 through the test suite, request a replacement, confirm the old code fails and new code succeeds.

- [ ] **Step 4: Check responsive presentation**

Inspect home, product, login, verification, and profile pages at approximately 1440 px, 768 px, and 390 px widths. Confirm no horizontal overflow, hidden controls, overlapping cards, or unusable form actions.

- [ ] **Step 5: Review Git and migrations**

Run:

```powershell
git status --short
git diff main...HEAD --stat
git log --oneline --decorate -10
```

Confirm migrations V5 and V6 are forward-only, no secrets or generated build artifacts are tracked, and every release change belongs to the approved scope.

- [ ] **Step 6: Create the final release commit only if verification required tracked fixes**

Stage only the exact verified fixes, rerun the affected focused test followed by the full suite, then commit:

```powershell
git commit -m "fix: complete polished MVP verification"
```

If verification requires no tracked fixes, do not create an empty commit.
