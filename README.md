# YU Bazaar

YU Bazaar is a marketplace application created for York University students to list, discover, and inquire about items available within the campus community.

This repository is an independently maintained portfolio edition of a four-person course project. It starts from a sanitized snapshot of the original application and documents the modernization work separately from the instructor-hosted repository.

## Current Status

The portfolio edition is under active modernization. The current work focuses on secure configuration, PostgreSQL migration, authentication, listing ownership, automated testing, and deployment readiness.

## Technology

- Java 17
- Spring Boot and Spring MVC
- Spring Data JPA
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

The default `dev` profile uses an in-memory H2 database, so no cloud credentials are required:

```powershell
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8080` after the application starts.

## Production Configuration

Production uses environment variables rather than committed credentials. Copy `.env.example` into your deployment platform's secret manager and provide the PostgreSQL and mail settings there. Do not commit a populated `.env` file.

## Testing

```powershell
.\mvnw.cmd test
```

## License

Licensed under the MIT License. See [LICENSE](LICENSE).
