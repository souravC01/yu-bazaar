# Account Verification Tiers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Open registration to all email domains, introduce dual verification tiers ("York Verified Student" vs. "Public Seller") with distinct badges, and provide a "✓ York Verified Only" marketplace filter.

**Architecture:** Update `User` and `Item` entities with verification helper methods, adjust `TemplateController` registration validation, enhance `ItemController` with optional `yorkOnly` filtering, style the badges in `components.css`, and update the Thymeleaf templates.

**Tech Stack:** Java 17, Spring Boot, Spring Security, Thymeleaf, Vanilla CSS3.

**Spec:** [docs/superpowers/specs/2026-08-17-account-verification-tiers.md](file:///d:/Grind/Projects/yu-bazaar/docs/superpowers/specs/2026-08-17-account-verification-tiers.md)

## Global Constraints
- All users (both York students and Public Sellers) must undergo 6-digit OTP verification.
- `isYorkVerified()` returns `true` if and only if `email` ends with `@yorku.ca` or `@my.yorku.ca`.
- Visual styling must adhere to the design system in `Shopify_DESIGN.md` (pill geometry, mint aloe for verified badges, subtle slate for public seller badges).

---

### Task 1: Domain Models & Registration Validation Update

**Files:**
- Modify: `src/main/java/com/example/demo/model/User.java`
- Modify: `src/main/java/com/example/demo/model/Item.java`
- Modify: `src/main/java/com/example/demo/controller/TemplateController.java`
- Modify: `src/test/java/com/example/demo/SecurityWorkflowTests.java`

- [ ] **Step 1: Update `User.java`**
  Remove `@Pattern` restriction on email, add `isYorkVerified()` method.

- [ ] **Step 2: Update `Item.java`**
  Add `isSellerYorkVerified()` helper method.

- [ ] **Step 3: Update `TemplateController.java`**
  Remove `@yorku.ca` / `@my.yorku.ca` restriction in `handleRegister`, allowing all valid emails while generating OTP.

- [ ] **Step 4: Update `SecurityWorkflowTests.java`**
  Add test for registering and verifying a non-York email (e.g. `jane.public@gmail.com`).

- [ ] **Step 5: Run tests and commit**
  Run: `./mvnw test -Dtest=SecurityWorkflowTests`
  Commit: `feat(auth): enable public email registration with York verification detection`

---

### Task 2: Marketplace Filter & Controller Logic

**Files:**
- Modify: `src/main/java/com/example/demo/controller/ItemController.java`
- Modify: `src/test/java/com/example/demo/SecurityWorkflowTests.java`

- [ ] **Step 1: Update `ItemController.java`**
  Add `@RequestParam(required = false, defaultValue = "false") boolean yorkOnly` to `/home` and `/search`.
  When `yorkOnly == true`, filter the returned `items` list to include only items where `item.isSellerYorkVerified()` is true.
  Pass `yorkOnly` to the model.

- [ ] **Step 2: Add test in `SecurityWorkflowTests.java`**
  Verify `yorkOnly=true` filters out non-York listings.

- [ ] **Step 3: Run tests and commit**
  Run: `./mvnw test -Dtest=SecurityWorkflowTests`
  Commit: `feat(marketplace): add York Verified Only filter support in ItemController`

---

### Task 3: UI Badges & Template Updates

**Files:**
- Modify: `src/main/resources/static/css/components.css`
- Modify: `src/main/resources/templates/register_page.html`
- Modify: `src/main/resources/templates/home_page.html`
- Modify: `src/main/resources/templates/product_page.html`
- Modify: `src/main/resources/templates/profile_view.html`

- [ ] **Step 1: Add `.badge-public` to `components.css`**
  Clean slate pill badge style.

- [ ] **Step 2: Update `register_page.html`**
  Update JS email validator to standard email regex, update helper copy about York vs Public Seller tier.

- [ ] **Step 3: Update `home_page.html`**
  Add "✓ York Verified Only" filter chip in hero and section header; render `✓ York Verified` or `Public Seller` badge on each listing card.

- [ ] **Step 4: Update `product_page.html` and `profile_view.html`**
  Render York Verified badge vs Public Seller badge on seller profile card and user profile.

- [ ] **Step 5: Commit**
  Commit: `feat(ui): display York Verified Student and Public Seller badges and filters`

---

### Task 4: Full Test Suite Verification

- [ ] **Step 1: Execute all tests**
  Run: `./mvnw test`
  Expected: 100% tests passing.
