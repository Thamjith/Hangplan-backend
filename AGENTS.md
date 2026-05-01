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
- Flyway (schema migrations under `src/main/resources/db/migration`)

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
- Hibernate `ddl-auto: update` for dev drift; relational migrations belong in Flyway
- Keep secrets and local credentials out of git.

## Subscription Architecture
- **`subscription_plans`** is the normalized catalog (`FREE`, `PAID_1Y`, …).
- **`users.subscription_plan_id`** is a foreign key to **`subscription_plans`**.
- **`users.subscription_start`** / **`users.subscription_end`** define the paid window when applicable.
- **Always** validate paid feature access with **`subscription_end`** (see **`User.isActivePaidUser()`**). Do **not** reintroduce boolean premium flags on **`users`**.
- Auth responses expose **`subscriptionPlan`** (plan **name**) and **`subscriptionEnd`** only—avoid leaking unrelated billing fields through DTOs.

## Real-time Architecture
- WebSocket endpoint: `/ws`
- Topic pattern: `/topic/events/{eventId}`
- WebSocket subscriptions require an **active paid** subscription (**`User.isActivePaidUser()`**).
- **`FREE`** or expired **`subscription_end`** users are denied at subscribe time.
- Event changes are published by **`EventRealtimeService`** after event mutations.

## Developer Notes
- **WebSocket / real-time updates are a paid-tier feature** (active non-**FREE** plan with **`subscription_end` > now).
- All server-side checks for that capability must use **`isActivePaidUser()`** (or equivalent logic), not removed legacy flags.
- **`SubscriptionService.assignFreePlan`** runs for new signups (local + Google).
- **`SubscriptionService.activatePaidPlan(user, planName)`** sets **`subscription_start`** / **`subscription_end`** from the plan’s **`durationDays`**—use this when integrating upgrades or billing webhooks later.

### Future plans (not implemented yet)
- Monthly subscriptions and trials
- Stripe or similar pricing integration
- Feature flags per plan row

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
