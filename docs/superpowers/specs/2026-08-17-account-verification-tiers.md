# Specification: Account Verification Tiers (York Verified Student vs. Public Seller)

## 1. Overview & Objectives
Currently, YU Bazaar restricts account registration exclusively to `@yorku.ca` and `@my.yorku.ca` email addresses. This specification defines the architecture and implementation to open registration to all valid email domains while segregating and highlighting accounts based on institutional verification:

1. **York Verified Student**: Users registering with a `@yorku.ca` or `@my.yorku.ca` address.
2. **Public Seller**: Users registering with standard email providers (e.g. `@gmail.com`, `@outlook.com`).

Both user tiers undergo mandatory 6-digit email OTP verification to ensure genuine email ownership and prevent spam. York students receive special visual distinction, and marketplace shoppers are provided a **"✓ York Verified Only"** filter to easily browse items sold exclusively by verified students.

---

## 2. Domain & Backend Specifications

### 2.1 Model Updates
- **`User.java`**:
  - Remove the strict `@Pattern(regexp = "^[a-zA-Z0-9._-]+@(yorku\\.ca|my\\.yorku\\.ca)$")` constraint on `email`.
  - Maintain `@NotBlank` and `@Email`.
  - Add helper method:
    ```java
    public boolean isYorkVerified() {
        if (email == null) return false;
        String lower = email.toLowerCase();
        return lower.endsWith("@yorku.ca") || lower.endsWith("@my.yorku.ca");
    }
    ```
- **`Item.java`**:
  - Add helper method:
    ```java
    public boolean isSellerYorkVerified() {
        if (sellerEmail == null) return false;
        String lower = sellerEmail.toLowerCase();
        return lower.endsWith("@yorku.ca") || lower.endsWith("@my.yorku.ca");
    }
    ```

### 2.2 Controller & Search Filter Updates
- **`TemplateController.java`**:
  - Update `handleRegister` to validate standard email syntax without restricting domain.
  - Send 6-digit OTP code to the provided email regardless of domain.
- **`ItemController.java`**:
  - In `showHomePage` (`/home`) and `searchItems` (`/search`), support an optional boolean request parameter `yorkOnly=false`:
    - When `yorkOnly=true`, filter items where `item.isSellerYorkVerified()` is true.
    - Pass `yorkOnly` boolean flag to the Thymeleaf model so active filter chips can be highlighted.

---

## 3. UI & Styling Specifications

### 3.1 Badge Tokens & Components (`components.css`)
- **`.badge-verified`** (Mint Aloe `#c1fbd4`, border `#bbf7d0`, text `#166534`):
  - Displays: `✓ York Verified Student` (on product/profile) or `✓ York Verified` (on listing cards).
- **`.badge-public`** (Slate Neutral `#f4f4f5`, border `#e4e4e7`, text `#52525b`):
  - Displays: `Public Seller` (on listing cards/product page) or `Public Member` (on profile).

### 3.2 Marketplace Home (`home_page.html`)
- **Filter Chips**:
  - Add a **"✓ York Verified Only"** pill toggle in the hero category pills and above the marketplace listing grid.
- **Listing Cards**:
  - Display the appropriate badge (`.badge-verified` vs `.badge-public`) beside the item location.

### 3.3 Product Details (`product_page.html`)
- **Seller Profile Card**:
  - If York Verified: Show `York University Student` title with green checkmark badge.
  - If Public Seller: Show `Public Seller` title with slate neutral verified email badge.

### 3.4 Registration & Auth (`register_page.html`)
- Client-side validation updated to allow any standard email format.
- Helper banner: *"York University students (@my.yorku.ca) automatically receive a York Verified badge. Other email providers will register as a Public Seller."*

### 3.5 User Profile (`profile_view.html`)
- Display badge corresponding to the user's email domain.

---

## 4. Verification & Testing Plan
1. **Unit & Workflow Tests**:
   - Update `SecurityWorkflowTests.java` to test registration, OTP verification, and login for a public email (e.g. `jane.public@gmail.com`).
   - Verify `isYorkVerified()` returns `true` for `student@my.yorku.ca` and `false` for `jane.public@gmail.com`.
   - Verify item filtering with `yorkOnly=true` returns only items posted by York accounts.
2. **End-to-End Test Suite**:
   - Run `.\mvnw test` to ensure 100% test pass rate across all suites.
