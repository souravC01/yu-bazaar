# YU Bazaar Polished MVP Release Design

## Purpose

Prepare YU Bazaar as a reliable portfolio demonstration without expanding it into a larger marketplace platform. The release fixes contradictory product copy, listing-form defects, incomplete account verification, demo-mode presentation, seeded-image errors, profile functionality, and dead navigation.

## Product decisions

- Registration remains open to any valid email address.
- A verified `@yorku.ca` or `@my.yorku.ca` address receives York Verified status.
- Other verified addresses are presented as Public Sellers.
- Marketplace listings and their photographs remain publicly viewable.
- Authentication remains required for listing creation, seller inquiries, listing deletion, and profile access.
- The public recruiter account remains a read-only production demo account.
- The application remains one Spring Boot deployable. RabbitMQ and microservices are excluded.

## Listing creation

The server and listing form consume one canonical collection of supported wear conditions and meetup locations. Every location displayed in the form must pass server validation.

Listing limits are intentionally small and match the existing schema:

- Title: required, 120 characters maximum.
- Description: optional, 255 characters maximum.
- Price: CAD 0.00 through 100,000.00 inclusive.
- Inquiry message: required, 1,000 characters maximum.
- Image: required, at most 5 MB, JPG, PNG, GIF, or WebP.

The existing single-use submission token, authenticated seller identity, owner-only deletion, and image cleanup behavior remain intact.

## Account verification

Verification codes use a cryptographically secure six-digit value and expire ten minutes after issuance. A user can request a replacement code after a sixty-second cooldown; issuing a replacement invalidates the prior code. Attempt counters, CAPTCHA, and account lockouts are outside this release.

Verification failures distinguish invalid and expired codes on the verification page without exposing account information in unrelated password-reset flows. Local development continues to display the generated code. Production continues to deliver it by email.

The registration email must describe the account as awaiting verification. The sign-in page must support both York and public sellers.

## Demo and seeded content

The anonymous sign-in page displays a one-click control that fills the intentionally public demo credentials. Server-side `DemoAccountPolicy` remains authoritative for all write restrictions.

Seeded listings use an explicit branded placeholder instead of requesting an invalid media key. Existing UUID-backed uploaded images continue to use `/media/{key}`. A Flyway migration converts the legacy `default-listing.png` sentinel to `NULL`, allowing templates to choose the placeholder without a failing request.

## Profile

The authenticated profile displays listings owned by the current account, newest first. Each listing links to its public product page. Real owners can delete their listings through the existing protected deletion endpoint; the demo account sees no write control.

Editing listings, marking listings sold, saved listings, ratings, and analytics are excluded.

## Public interface and documentation

Public copy consistently explains the two verification tiers. The footer contains only working application or repository links; placeholder anchors for legal and safety pages are removed. The README describes public registration, public listing media, demo behavior, and the implemented profile accurately.

## Data changes

Two forward-only Flyway migrations are required:

1. Add nullable `otp_expires_at` and `otp_last_sent_at` timestamp-with-time-zone columns to `users`.
2. Replace the seeded `default-listing.png` sentinel with `NULL` in `item.image_path`.

Existing verified users and uploaded images are unaffected. Existing unverified users with no expiration can request a replacement code.

## Verification strategy

Automated coverage must prove:

- Every rendered listing option is accepted by the same server policy.
- Listing and inquiry limits are enforced.
- Codes expire, replacement codes supersede old codes, and cooldowns are enforced.
- Anonymous users can retrieve valid listing media.
- Demo credentials are visible while demo write operations remain forbidden.
- Seeded placeholders do not request `/media/default-listing.png`.
- Profiles display only the signed-in user's listings and hide demo write controls.
- Existing authentication, password-reset, ownership, storage, and health checks continue to pass.

Release verification includes the complete Maven suite, a production package build, and manual guest, public-seller, York-seller, owner, and demo journeys at desktop and mobile widths.

## Explicit exclusions

- Payments, checkout, chat, ratings, moderation, and administration.
- Multiple listing images, saved listings, and category management.
- Full legal or help-center pages.
- General-purpose rate limiting, CAPTCHA, and account lockouts.
- RabbitMQ, microservices, Kubernetes, service discovery, and event sourcing.
