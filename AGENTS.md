# HangPlan Backend - AI Agent Guide

## Purpose
- Spring Boot API for HangPlan.
- Handles auth, events, participants, expenses, and summaries.

## Tech Stack
- Java 25
- Spring Boot 3.5
- Spring Web, Data JPA, Security, OAuth2 Client, Validation
- PostgreSQL
- JWT (`jjwt`)

## Key Paths
- `src/main/java/com/hangplan/controller` - HTTP endpoints
- `src/main/java/com/hangplan/service` - business logic
- `src/main/java/com/hangplan/repository` - JPA repositories
- `src/main/java/com/hangplan/entity` - JPA entities
- `src/main/java/com/hangplan/dto` - request/response DTOs
- `src/main/resources/application.yml` - base config
- `src/main/resources/application-local.yml` - local overrides (gitignored)

## Local Run
- Build: `mvn clean install`
- Dev run: `SPRING_PROFILES_ACTIVE=local mvn spring-boot:run`
- Compile only: `mvn -q compile -DskipTests`

## Configuration Notes
- Default app port: `8080`
- Base DB: PostgreSQL (`spring.datasource.*`)
- Schema update mode is Hibernate `ddl-auto: update`
- Keep secrets and local credentials out of git.

## Subscription Handling
- `users.is_premium` is the current subscription feature flag.
- Default value is `FALSE` (free tier by default).
- Use this flag to gate real-time behavior only; avoid scattering tier checks.

## Real-time Architecture
- WebSocket endpoint: `/ws`
- Topic pattern: `/topic/events/{eventId}`
- Premium users are allowed to subscribe.
- Free users are rejected/ignored at subscription time.
- Event changes are published by `EventRealtimeService` after event mutations.

## Developer Notes
- Do not reintroduce polling on API endpoints for event detail updates.
- Keep WebSocket auth/subscription checks in dedicated real-time components.
- Preserve manual refresh fallback behavior for free users.
- This flag is temporary and will evolve into a fuller subscription system.

## Coding Guidelines for Agents
- Keep controllers thin; put logic in services.
- Reuse DTOs in `EventDtos` for API contracts.
- Preserve existing endpoint style and response semantics.
- Prefer explicit validation and clear HTTP errors (`ResponseStatusException`).
- Keep entity and repository changes minimal and intentional.
- Do not introduce destructive DB changes without explicit user request.

## Verification Before Finishing
- Compile backend after Java changes:
  - `mvn -q compile -DskipTests`
- If API contracts change, confirm frontend expectations are aligned.

