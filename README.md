# YU Bazaar

YU Bazaar is a marketplace application created for York University students to list, discover, and inquire about items available within the campus community.

This repository is an independently maintained portfolio edition of a four-person course project. It starts from a sanitized snapshot of the original application and documents the modernization work separately from the instructor-hosted repository.

## Current Status

The portfolio edition is under active modernization. PostgreSQL migration, secure configuration, session-backed authentication, password hashing, listing ownership checks, safer image uploads, and initial workflow tests are now in place. Password recovery, persistent media storage, broader validation, and deployment remain active work.

## Technology

- Java 17
- Spring Boot and Spring MVC
- Spring Data JPA
- Spring Security
- Thymeleaf
- PostgreSQL in production
- H2 for local development and isolated tests
- Flyway database migrations
- Maven

## Original Project

The original YU Bazaar application was developed by a four-person student team and is hosted in the [course repository](https://github.com/hvpham-yorku/YuBazaar).

My verified contributions to the original project included:

- Registration and recovery email notifications
- Password recovery using generated recovery codes
- York University email OTP verification
- Item search and live search suggestions
- Fixes to the item-listing and home-page workflow

All original contributors retain credit for the team project. This repository preserves the project's MIT license and clearly separates the original coursework from subsequent portfolio improvements.

## Local Development

The default `dev` profile uses an in-memory H2 database and disables external email delivery, so no cloud credentials are required:

```powershell
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8080` after the application starts.

Register with a `yorku.ca` or `my.yorku.ca` address. In local development, the verification and recovery codes appear on the verification page instead of being emailed. Local data is reset whenever the application restarts.

## Production Configuration

Production uses environment variables rather than committed credentials. Copy `.env.example` into your deployment platform's secret manager and provide the Neon PostgreSQL and mail settings there. Flyway baselines the existing Neon schema at version 1 on its first production connection and manages later migrations normally. Do not commit a populated `.env` file.

## Testing

```powershell
.\mvnw.cmd test
```

The current suite covers route protection, password hashing, verified login, authenticated listing ownership, owner-only deletion, and uploaded-image delivery.

## License

Licensed under the MIT License. See [LICENSE](LICENSE).
