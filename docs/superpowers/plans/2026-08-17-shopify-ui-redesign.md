# YU Bazaar UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the YU Bazaar web application into a modern, cinematic, dual-track commerce platform based on `Shopify_DESIGN.md` and the approved Stitch design specifications.

**Architecture:** Implement a modular CSS design system (`theme.css`, `components.css`, `marketplace.css`, `product.css`, `auth.css`) in Spring Boot's static resources, and update all Thymeleaf templates (`home_page.html`, `product_page.html`, `login_page.html`, `register_page.html`, `verify_otp.html`, `profile_view.html`, `forgot_password.html`, `reset_password.html`) to render the dual-track (Cinematic Night Hero + Warm Cream Transactional surfaces) with pill geometry, OpenType `ss03` typography, and Level 3 paper-halo shadows.

**Tech Stack:** Java 17, Spring Boot, Spring Security, Thymeleaf, Vanilla CSS3 (Custom Properties, Flexbox, Grid, Glassmorphism), Inter Variable & Inter Display typography with OpenType `ss03`.

**Spec:** [docs/superpowers/specs/2026-08-17-shopify-design-ui-redesign.md](file:///d:/Grind/Projects/yu-bazaar/docs/superpowers/specs/2026-08-17-shopify-design-ui-redesign.md)

## Global Constraints
- `rounded-pill: 9999px` must be used for all buttons, chips, tags, and search bars. No rounded rectangles for buttons.
- Display typography must use weight 330 (thin cut) for hero titles and large headings.
- OpenType `ss03` stylistic set must be enabled globally (`font-feature-settings: "ss03"`).
- Dual canvas polarity: Pure black `#000000` for hero & dark footers; Warm cream `#fbfbf5` and pure white `#ffffff` for transactional marketplace surfaces.
- Aloe mint (`#c1fbd4`) and pistachio (`#d4f9e0`) are reserved for the light track (verified badges, safety banners, chips).
- All existing Thymeleaf backend model bindings, CSRF tokens, forms, and JavaScript APIs (search suggestions, image uploads, demo restrictions) must be strictly preserved.

---

### Task 1: Core Design System Foundation (`theme.css` & `components.css`)

**Files:**
- Create: `src/main/resources/static/css/theme.css`
- Create: `src/main/resources/static/css/components.css`

**Interfaces:**
- Produces: CSS custom properties (`--canvas-night`, `--canvas-cream`, `--canvas-light`, `--aloe-10`, `--pistachio-10`, `--shadow-level-3`, `--radius-pill`), global font rules with `ss03`, button classes (`.btn-primary-pill`, `.btn-outline-dark`, `.btn-outline-light`, `.btn-aloe-pill`), input styles (`.input-field`), chip/badge classes (`.pill-chip-mint`, `.badge-verified`), and modal backdrop styles.

- [ ] **Step 1: Create `src/main/resources/static/css/theme.css`**
  Define Google Fonts imports (`Inter`, `Inter Tight`), CSS variables, reset, and base typography with `font-feature-settings: "ss03"`.

```css
@import url('https://fonts.googleapis.com/css2?family=Inter:ital,opsz,wght@0,14..32,100..900;1,14..32,100..900&display=swap');

:root {
  --canvas-night: #000000;
  --canvas-night-elevated: #0a0a0a;
  --canvas-cream: #fbfbf5;
  --canvas-light: #ffffff;
  --surface-elevated-dark: #1e2c31;
  --hairline-light: #e4e4e7;
  --hairline-dark: #1e2c31;

  --primary-ink: #000000;
  --on-primary: #ffffff;
  --on-dark: #ffffff;
  --aloe-10: #c1fbd4;
  --pistachio-10: #d4f9e0;

  --shade-30: #d4d4d8;
  --shade-40: #a1a1aa;
  --shade-50: #71717a;
  --shade-60: #52525b;
  --shade-70: #3f3f46;

  --radius-xs: 4px;
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 20px;
  --radius-pill: 9999px;

  --shadow-level-1: 0 1px 2px rgba(255,255,255,0.05), inset 0 1px 0 rgba(255,255,255,0.04);
  --shadow-level-3: 0 8px 8px rgba(0,0,0,0.03), 0 4px 4px rgba(0,0,0,0.03), 0 2px 2px rgba(0,0,0,0.03), 0 0 0 1px rgba(0,0,0,0.06);
  --shadow-level-4: 0 25px 50px -12px rgba(0,0,0,0.25);

  --font-family-display: "NeueHaasGrotesk Display", "Inter Display", "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  --font-family-body: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: var(--font-family-body);
  color: var(--primary-ink);
  background-color: var(--canvas-cream);
  font-feature-settings: "ss03" 1;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  line-height: 1.5;
}
```

- [ ] **Step 2: Create `src/main/resources/static/css/components.css`**
  Implement reusable pill buttons, input fields, badges, cards, flash alerts, and dialog modals.

- [ ] **Step 3: Verify static files exist and compile cleanly**
  Run: `./mvnw test-compile`
  Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**
```bash
git add src/main/resources/static/css/theme.css src/main/resources/static/css/components.css
git commit -m "feat(ui): add core design tokens and reusable component styling"
```

---

### Task 2: Marketplace Home Redesign (`marketplace.css` & `home_page.html`)

**Files:**
- Create: `src/main/resources/static/css/marketplace.css`
- Modify: `src/main/resources/templates/home_page.html`

**Interfaces:**
- Consumes: `theme.css`, `components.css`, product model lists (`products`, `categories`, `demoMode`, `currentUser`).
- Produces: Cinematic dark hero with campus background, pill search bar with live suggestion drop, category pills, cream listing grid, item creation modal, and dark footer.

- [ ] **Step 1: Create `src/main/resources/static/css/marketplace.css`**
  Implement hero section styles, pill search bar, autocomplete suggestion styling, 4-up responsive product grid, card hover animations, and modal styles.

- [ ] **Step 2: Update `src/main/resources/templates/home_page.html`**
  Integrate the design:
  - Header: Dual-mode navigation with YU Bazaar logo, Sell button, Profile pill, Sign In/Out.
  - Hero: Full-bleed campus photo background, 96px weight 330 headline (*"Commerce for Campus. Buy and sell across York University with verified student trust."*), centered pill search bar, and category filter pills.
  - Transactional Grid: `#fbfbf5` canvas, product cards with Level 3 halo shadow, condition tag, mint category chip, verified student badge, and black pill `View Item` button.
  - Modern Item Creation Modal with drag-drop R2 image upload preview and pill buttons.
  - Preserve all existing JavaScript functions (`setupAutocomplete`, `openModal`, `closeModal`, `previewImage`, `filterByCategory`).

- [ ] **Step 3: Run automated security & controller tests**
  Run: `./mvnw test -Dtest=SecurityWorkflowTests,YuBazaarApplicationTests`
  Expected: Tests pass.

- [ ] **Step 4: Commit**
```bash
git add src/main/resources/static/css/marketplace.css src/main/resources/templates/home_page.html
git commit -m "feat(ui): redesign marketplace home page with cinematic hero and cream listing grid"
```

---

### Task 3: Product Details Page Redesign (`product.css` & `product_page.html`)

**Files:**
- Create: `src/main/resources/static/css/product.css`
- Modify: `src/main/resources/templates/product_page.html`

**Interfaces:**
- Consumes: `theme.css`, `components.css`, `product` model, `isOwner`, `demoMode`, inquiry endpoints.
- Produces: 60/40 editorial gallery layout, sticky verified seller inquiry card, suggestion pills, campus safety banner, and similar items recommendations.

- [ ] **Step 1: Create `src/main/resources/static/css/product.css`**
  Implement the two-column 60/40 grid layout, gallery image container, thumbnail selector, sticky right panel, quick response inquiry pills, and pistachio safety banner.

- [ ] **Step 2: Update `src/main/resources/templates/product_page.html`**
  - Left column: Large high-res product photo, thumbnail carousel, condition badge (`Like New`), category tag (`pill-tag-mint`), detailed description, and campus meetup zone.
  - Right column: Sticky seller card with avatar, student major snippet, response time indicator, quick inquiry pills (*"Is this still available?"*, *"Can meet at Scott Library?"*), message form, and black pill `Send Inquiry to Seller` CTA.
  - Owner Actions: Discreet `Mark Sold` and `Delete Listing` pills when logged in as seller.
  - Safety & Recommendations: Pistachio campus safety card and 3-card *Similar Listings from Verified Students* grid.
  - JavaScript: Handle quick-inquiry suggestion pills filling the message textarea on click.

- [ ] **Step 3: Run automated product & security tests**
  Run: `./mvnw test -Dtest=SecurityWorkflowTests`
  Expected: Tests pass.

- [ ] **Step 4: Commit**
```bash
git add src/main/resources/static/css/product.css src/main/resources/templates/product_page.html
git commit -m "feat(ui): redesign product details page with editorial gallery and sticky inquiry panel"
```

---

### Task 4: Student Auth & Verification Redesign (`auth.css` & Auth Templates)

**Files:**
- Create: `src/main/resources/static/css/auth.css`
- Modify: `src/main/resources/templates/login_page.html`
- Modify: `src/main/resources/templates/register_page.html`
- Modify: `src/main/resources/templates/verify_otp.html`
- Modify: `src/main/resources/templates/forgot_password.html`
- Modify: `src/main/resources/templates/reset_password.html`

**Interfaces:**
- Consumes: `theme.css`, `components.css`, Spring Security login/register/OTP endpoints, CSRF tokens.
- Produces: Centered Level 3 paper-halo cards on cream canvas, 3-step progress indicator, 6-digit OTP code inputs with mint aloe highlights, `@my.yorku.ca` trust badges, and demo login prefill.

- [ ] **Step 1: Create `src/main/resources/static/css/auth.css`**
  Implement centered card layout (`max-width: 480px`), step progress bar (`.step-indicator`), 6-digit OTP input grid (`.otp-inputs`), focus transitions, and aloe trust badge container.

- [ ] **Step 2: Update `login_page.html`**
  Redesign login with clean 1px hairline inputs, black pill `Sign In` button, one-click demo credentials prefill button, and links to registration and password recovery.

- [ ] **Step 3: Update `register_page.html` and `verify_otp.html`**
  - `register_page.html`: 3-step indicator (`1. Student Info`), `@my.yorku.ca` email validation helper, mint aloe trust guarantee card, and black pill submit button.
  - `verify_otp.html`: 3-step indicator (`2. OTP Verification`), 6 individual digit input boxes with auto-advance JavaScript, resend code timer, and black pill verify button.

- [ ] **Step 4: Update `forgot_password.html` and `reset_password.html`**
  Align password reset views with the same elevated white card styling and pill buttons.

- [ ] **Step 5: Run automated authentication and password reset tests**
  Run: `./mvnw test -Dtest=SecurityWorkflowTests,PasswordResetWorkflowTests`
  Expected: All authentication, registration, OTP, and password reset tests pass.

- [ ] **Step 6: Commit**
```bash
git add src/main/resources/static/css/auth.css src/main/resources/templates/login_page.html src/main/resources/templates/register_page.html src/main/resources/templates/verify_otp.html src/main/resources/templates/forgot_password.html src/main/resources/templates/reset_password.html
git commit -m "feat(ui): redesign auth and verification flows with elevated cream cards and OTP boxes"
```

---

### Task 5: User Profile Dashboard Redesign (`profile_view.html`)

**Files:**
- Modify: `src/main/resources/templates/profile_view.html`

**Interfaces:**
- Consumes: `theme.css`, `components.css`, `user` model, user listings, deletion actions.
- Produces: Modern student dashboard with profile summary, active listings grid, and owner management controls.

- [ ] **Step 1: Update `src/main/resources/templates/profile_view.html`**
  - Student verified profile header with avatar, verified badge, email, and member since info.
  - Active listings cards with quick status toggle and delete confirmation modal.
  - Empty state with mint pill CTA to post first item.

- [ ] **Step 2: Commit**
```bash
git add src/main/resources/templates/profile_view.html
git commit -m "feat(ui): redesign user profile dashboard"
```

---

### Task 6: Comprehensive Verification & Visual Polish

**Files:**
- Modify: All CSS & template files as needed for visual consistency across breakpoints.

- [ ] **Step 1: Execute full test suite**
  Run: `./mvnw test`
  Expected: 100% tests pass (all 5 test suites).

- [ ] **Step 2: Visual & Responsive Review**
  Verify responsive breakpoints (1440px desktop, 768px tablet, 390px mobile), touch target accessibility (min 44px), ss03 font styling, and Level 3 paper-halo shadows.

- [ ] **Step 3: Final Commit**
```bash
git commit -am "style(ui): polish typography, responsive breakpoints, and paper-halo shadows"
```
