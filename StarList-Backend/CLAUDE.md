# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Build
```bash
./mvnw clean install
```

### Run (dev profile, active by default)
```bash
./mvnw spring-boot:run -pl app
```

### Run tests
```bash
./mvnw test
```

### Run a single test class
```bash
./mvnw test -pl <module> -Dtest=ClassName
```

### Health check endpoint
```
GET /check/health
```

## Architecture

This is a **multi-module Maven project** using Spring Boot 4.0.3 and Java 25. The dependency chain flows:

```
app -> controller -> service -> repository -> model
```

### Modules

- **`model`** — Pure domain POJOs (`model.domain`) and enums (`model.enums`). No JPA or Spring annotations. Uses Lombok `@Data`, `@Builder`. All enums are stored as strings (`EnumType.STRING`) in the entity layer.
- **`repository`** — JPA entities (`repository.entity`) and Spring Data JPA repository interfaces (`repository.api`). Depends on `model`. Package is `repository.*`.
- **`service`** — Business logic layer (not yet implemented).
- **`controller`** — REST controllers. Scanned by `app` via `@SpringBootApplication(scanBasePackages = {"app", "controller"})`.
- **`app`** — Entry point (`StarListApp`), Spring config, and interceptors. Contains `WebConfig` (registers interceptors) and `RequestLoggingInterceptor` (logs all HTTP requests/responses with timing).

### Spring Profiles

| Profile | Database | DDL mode |
|---------|----------|----------|
| `dev` (default) | PostgreSQL `localhost:5432/starlist` | `update` |
| `test` | H2 in-memory (PostgreSQL compatibility mode) | `create-drop` |
| `prod` | PostgreSQL via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` env vars | `validate` |

All profiles use UTC timezone for JPA timestamps. Logs are written to `logs/request-trace.log`.

### Domain Model

The app is a gamified task/habit tracker with a galaxy cosmetics system. Core entities:

- **User** — central entity; owns all others via `CascadeType.ALL` + `orphanRemoval`. Tracks `totalCoins` (monthly reset) and `currentGalaxyCycle`.
- **Task** — to-do items with `DifficultyLevel`, `coinReward`/`coinPenalty`, soft-delete via `deletedAt`.
- **Habit** — recurring habits with streak tracking (`currentStreak`, `bestStreak`), soft-delete.
- **HabitCompletion** — one record per habit per day (unique constraint on `habit_id + completed_date`).
- **ItemCatalog** — master catalog of purchasable galaxy cosmetics.
- **GalaxyItem** — user's purchased items and placement.
- **CoinTransaction** — full audit log of all coin movements (positive = earned, negative = spent).
- **AiConversation** — chat history with AI; tasks created from a conversation are NOT cascade-deleted when the conversation ends.

Authentication is planned via AWS Cognito (`cognitoUserId` field on User). Password storage is not modeled.
