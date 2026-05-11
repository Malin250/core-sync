# Core Sync Service

A Spring Boot 4 REST API providing authentication, AI chat, user profiles, and state sync for the Core Sync mobile app.

---

## Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- An OpenAI-compatible API key
- An AWS S3 bucket (or compatible object store)

### 1 — Configure environment

```bash
cp .env.example .env
# Edit .env and fill in every ACTION REQUIRED field
nano .env
```

Key values you **must** set:

| Variable | Description |
|---|---|
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET_KEY` | `openssl rand -hex 32` |
| `AI_API_KEY` | OpenAI (or compatible) key |
| `FIREBASE_SERVICE_ACCOUNT_BASE64` | base64-encoded Firebase service-account JSON |
| `FIREBASE_STORAGE_BUCKET` | Firebase Storage bucket, e.g. `your-project-id.appspot.com` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated front-end origins |

### 2 — Run with Docker Compose

```bash
docker compose up -d
```

The API is available at `http://localhost:8080`.  
Swagger UI: `http://localhost:8080/swagger-ui.html` (disable in production via `SWAGGER_ENABLED=false`).

### 3 — Run locally (without Docker)

```bash
# Export required env vars first, then:
./gradlew bootRun
```

---

## API Overview

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Create account |
| POST | `/api/v1/auth/authenticate` | Public | Login → tokens |
| POST | `/api/v1/auth/logout` | Bearer | Invalidate token |
| POST | `/api/v1/auth/password-reset/request` | Public | Request reset email |
| POST | `/api/v1/auth/password-reset/confirm` | Public | Apply new password |
| GET/PUT | `/api/v1/profile` | Bearer | Read / update profile |
| POST | `/api/v1/profile/picture` | Bearer | Upload profile photo |
| GET/PUT | `/api/v1/state` | Bearer | Read / update user state |
| POST | `/api/v1/ai/chat` | Bearer | Send AI message |
| GET | `/api/v1/ai/history` | Bearer | Retrieve chat history |

---

## Security Notes

- Tokens are short-lived (24 h access / 7 d refresh). Logout blacklists the access token in memory.
  **ACTION REQUIRED (multi-node):** Replace `TokenBlacklistService` with Redis.
- Account lockout triggers after 5 consecutive failed logins. Unlock via password-reset.
- CORS: set `CORS_ALLOWED_ORIGINS` to your exact front-end URL(s). An empty value disables cross-origin requests.
- Swagger is **disabled by default** in production (`SWAGGER_ENABLED=false`).
- Profile pictures are stored in **Firebase Storage** under `profile-pictures/`. Uploads are limited to 5 MB and must be JPEG, PNG, WebP, or GIF. Objects are granted public-read ACL on upload so the mobile app can display them directly.
- Password-reset tokens expire in 15 minutes and are single-use.
- **ACTION REQUIRED:** Wire a real email service in `PasswordResetService` before going live.

---

## Project Structure

```
src/main/java/com/example/coresyncservice/
├── config/
│   ├── annotations/     RateLimited annotation
│   ├── aspect/          Rate-limiting AOP advice
│   ├── ApplicationConfig.java
│   ├── JwtAuthenticationFilter.java
│   ├── OpenApiConfig.java
│   ├── SecurityConfiguration.java
│   └── StartupLogger.java
├── controller/          REST endpoints
├── dto/                 Request / response bodies
├── exception/           Custom exceptions + global handler
├── model/               JPA entities
├── repository/          Spring Data interfaces
└── service/             Business logic
```
