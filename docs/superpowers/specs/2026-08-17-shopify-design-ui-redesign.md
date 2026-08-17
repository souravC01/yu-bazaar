# YU Bazaar UI Redesign Specification (Shopify-Inspired Dual-Track System)

## 1. Overview & Vision
YU Bazaar is a campus marketplace for York University students to buy, sell, and discover items securely. The goal of this redesign is to elevate YU Bazaar from a basic utility interface into a premium, state-of-the-art web commerce experience. 

The design is built upon the dual-track design language defined in [Shopify_DESIGN.md](file:///d:/Grind/Projects/yu-bazaar/Shopify_DESIGN.md):
1. **The Cinematic Narrative Track (Night Canvas `#000000`):** Used for marketing, hero discovery, brand storytelling, and full-bleed campus aesthetics. Characterized by high-contrast black backgrounds, ultra-thin display typography (weight 330), negative space, and white-stroked pill CTAs.
2. **The Transactional Track (Cream Canvas `#fbfbf5` & White `#ffffff`):** Used for marketplace browsing, product inspection, seller inquiries, and authentication/verification flows. Characterized by soft Level 3 paper-halo shadows, pastel mint aloe (`#c1fbd4`) and pistachio (`#d4f9e0`) trust chips, and solid black pill action buttons.

---

## 2. Design Tokens & Styling Foundation

### 2.1 Color Palette
- **Surfaces & Backgrounds:**
  - `--canvas-night`: `#000000` (Pure black for hero & dark footer)
  - `--canvas-night-elevated`: `#0a0a0a` / `#16171d` (Dark cards and interactive frames)
  - `--canvas-cream`: `#fbfbf5` (Warm editorial off-white base for listings & forms)
  - `--canvas-light`: `#ffffff` (Pure white for cards and input fields)
  - `--hairline-light`: `#e4e4e7` (1px subtle border on light cards/dividers)
  - `--hairline-dark`: `#1e2c31` (1px subtle border on dark elements)
- **Brand & Accents:**
  - `--primary-ink`: `#000000` (Primary text on light, button fills)
  - `--on-primary`: `#ffffff` (Text on black pills & dark canvas)
  - `--aloe-10`: `#c1fbd4` (Mint accent for verified student badges & active tags)
  - `--pistachio-10`: `#d4f9e0` (Soft pastel green for campus safety & informational callouts)
  - `--shade-30`: `#d4d4d8` (Neutral tag background on light canvas)
  - `--shade-50`: `#71717a` (Secondary helper text)
  - `--shade-70`: `#3f3f46` (Pill button pressed state)

### 2.2 Typography & Stylistic Sets
- **Display Typography:** `Neue Haas Grotesk Display`, `Inter Display`, or light `Helvetica/Arial` fallback rendered at weight **330** (thin editorial cut).
  - `display-xxl` (96px, line-height 1.0, tracking +2.4px): Hero headline.
  - `display-md` (48px, line-height 1.14): Product price tags and section titles.
  - `heading-xl` (28px, weight 500): Card headings and product titles.
- **UI & Body Typography:** `Inter Variable`, `Inter`, `sans-serif` (weights 400 to 550) for UI body, form labels, table cells, and buttons.
- **OpenType `ss03` Signature:** Enabled globally via `font-feature-settings: "ss03"` across all text elements for clean, geometric character styling.

### 2.3 Geometry & Elevation
- **Pill Shape Standard:** Every button, chip, search capsule, and category filter uses `--rounded-pill: 9999px`. Rounded rectangle buttons are prohibited.
- **Card Radius:** Standard cards use `--rounded-lg: 12px` to `--rounded-xl: 20px`.
- **Level 3 Elevation (Stacked Paper-Halo Shadow):**
  `box-shadow: 0 8px 8px rgba(0,0,0,0.04), 0 4px 4px rgba(0,0,0,0.04), 0 2px 2px rgba(0,0,0,0.04), 0 0 0 1px rgba(0,0,0,0.06);`
  Provides soft, premium depth on the light cream canvas without muddy dark shadows.

---

## 3. Screen-by-Screen Specifications

### 3.1 Global Header & Navigation
- **Dual Mode:**
  - On Dark Hero: Transparent or `#000000` with white wordmark (`YU Bazaar`), white pill outline buttons (`List an Item`, `Sign In`).
  - On Sticky/Transactional Pages: Frosted `#ffffff` (with backdrop-filter blur) or `#fbfbf5`, 1px hairline border, black wordmark, student profile avatar pill, and black pill action CTA.
- **Search Capsule:** Integrated pill input with autocomplete and live category filtering.

### 3.2 Marketplace Home (`home_page.html`)
- **Cinematic Dark Hero:**
  - Full-bleed architectural imagery with subtle dark overlay.
  - Headline: *"Commerce for Campus. Buy and sell across York University with verified student trust."* in 96px weight 330 display font.
  - Search pill with high-contrast search button.
  - Row of category filter pills in white outline style (*Textbooks, Tech & Electronics, Dorm Living, Course Notes, Apparel*).
  - White-stroked outline pill CTA: `Sell an Item` (triggers listing creation modal).
- **Transactional Listing Grid:**
  - Background: `--canvas-cream` (`#fbfbf5`).
  - 3-up to 4-up responsive grid of listing cards.
  - Each card: White background, Level 3 paper-halo shadow, 12px rounded corners, crisp product photography container with smooth hover zoom, bold price tag, item title, campus location badge (Keele / Glendon), mint aloe category chip (`pill-tag-mint`), verified student badge, and black pill `View Item` button.
- **Item Creation Modal:**
  - Modern dialog component with glassmorphism backdrop, clean 8px-radius form inputs, drag-and-drop Cloudflare R2 image upload preview, and black pill submit button.
- **Footer:** Full-width pure black (`#000000`) footer with links to York campus safety, student resources, terms, and copyright.

### 3.3 Product Details (`product_page.html`)
- **Canvas:** Editorial cream canvas (`#fbfbf5`) with 60/40 two-column asymmetric desktop layout.
- **Left Column (Gallery & Details):**
  - Large photography stage with 12px rounded container.
  - Thumbnail carousel switcher with pill-bordered active states.
  - Condition chip (`Like New`, `Good`, `Brand New`) and mint category chip.
  - Full item description, posted timestamp, and campus meetup zone (e.g. *Vari Hall*, *Steacie Library*, *Student Centre*).
- **Right Column (Sticky Student Seller Panel):**
  - White elevated card with Level 3 halo shadow.
  - Item title in `heading-xl` (28px).
  - Large price in `display-md` (48px) formatted in CAD.
  - Verified York Student Profile Card: Student avatar, faculty/major snippet, verified checkmark badge, average response time.
  - Quick Inquiry Suggestion Chips: Pill chips with preset questions (*"Is this still available?"*, *"Can meet at Scott Library?"*).
  - Message Textarea: Clean 1px border input with 8px radius.
  - Primary Action: Full-width `button-primary-pill` labeled `Send Inquiry to Seller`.
  - Owner Actions: Discreet owner management pills (`Mark as Sold`, `Delete Listing`).
- **Campus Safety Callout:** Full-width pistachio band (`card-pistachio-band`) with verified campus meetup safety tips.
- **Similar Student Listings:** 3-card grid of related recommendations.

### 3.4 Authentication & Verification Flows (`login_page.html`, `register_page.html`, `verify_otp.html`, `forgot_password.html`, `reset_password.html`)
- **Canvas:** Clean cream canvas (`#fbfbf5`) with centered, elevated white container card.
- **Step Navigation Indicator:** Visual 3-step progress bar (`1. Student Info` $\rightarrow$ `2. OTP Code` $\rightarrow$ `3. Ready`).
- **Input Design:** Crisp 1px hairline inputs with focused state rings, clear helper text indicating `@my.yorku.ca` requirements.
- **OTP Verification Stage:** 6 discrete digit inputs with automatic focus forwarding and mint aloe active highlight.
- **Trust & Verification Callout:** Mint aloe banner explaining student email verification, safety guarantees, and anti-spam protection.
- **Demo Account Switcher:** One-click prefill button for guest recruiters and reviewers.

### 3.5 User Profile (`profile_view.html`)
- **Header:** Clean student banner with verified status, email, and member since date.
- **Listings Management:** Tabbed view of *Active Listings* and *Inquiries Sent*, with quick delete and status update toggles.

---

## 4. Frontend Architecture & File Structure

We will structure the CSS assets cleanly in `src/main/resources/static/css/` to ensure reusability, maintainability, and zero bloat:

```
src/main/resources/
  static/
    css/
      theme.css        <-- CSS Variables, Design Tokens, Typography, ss03 styling
      components.css   <-- Pill buttons, cards, halo shadows, inputs, chips, modal
      marketplace.css  <-- Home page hero, product grid, search capsule
      product.css      <-- Product detail gallery, sticky seller panel, safety band
      auth.css         <-- Verification cards, OTP inputs, step indicators
  templates/
    home_page.html     <-- Updated with cinematic hero & cream listing grid
    product_page.html  <-- Updated with 60/40 layout & sticky inquiry panel
    login_page.html    <-- Updated with cream card & pill buttons
    register_page.html <-- Updated with student info flow & trust banner
    verify_otp.html    <-- Updated with 6-digit OTP boxes & mint highlights
    profile_view.html  <-- Updated student dashboard
    forgot_password.html
    reset_password.html
```

---

## 5. Verification Plan
1. **Visual Fidelity Verification:** Inspect rendered pages across desktop (1440px), tablet (1024px/768px), and mobile (390px) to verify responsive scaling, pill geometry, typography weights, and Level 3 paper-halo shadows.
2. **Interactive Functionality:**
   - Search & autocomplete suggestion dropdowns.
   - Category filtering pills.
   - Listing creation modal & image upload preview.
   - Student registration & OTP entry inputs.
   - Seller inquiry form submission.
   - Demo mode login & read-only restrictions.
3. **Automated Spring Boot & Thymeleaf Tests:** Run `./mvnw test` to ensure all existing security boundaries, authentication flows, and controller routes continue to pass seamlessly with the new templates.
