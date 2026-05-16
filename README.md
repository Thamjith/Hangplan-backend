# HangPlan Backend

Spring Boot API for HangPlan.

## Requirements

- Java 21 (set `JAVA_HOME` to JDK 21)
- Maven 3.9+
- PostgreSQL

## Database setup

1. Create a PostgreSQL database named `hangplan` (or your preferred name).
2. Create a local override file `src/main/resources/application-local.yml` (gitignored) and put local DB credentials there:

```yaml
spring:
  datasource:
    username: your_local_db_user
    password: your_local_db_password
```

3. For Google sign-in, set:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`
4. Optional for production: set a strong `JWT_SECRET`.

Google OAuth redirect URLs:

- Local default (when `OAUTH2_REDIRECT_URI` is unset): `http://localhost:8080/login/oauth2/code/google`
- Production (API subdomain): `https://api.hangplan.in/login/oauth2/code/google`

Register both in Google Cloud Console and set **`OAUTH2_REDIRECT_URI`** on the server when Spring sits behind nginx but still resolves requests as `localhost:8080` (otherwise Google gets the wrong `redirect_uri`).

Schema migrations run automatically via **Flyway** on startup (`src/main/resources/db/migration`). Hibernate uses `ddl-auto: update` for additive drift in development; authoritative relational changes belong in Flyway scripts.

## Run in development

```bash
mvn clean install
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

API base URL: `http://localhost:8080`

## Real-time updates & subscription model

HangPlan supports two update modes on the client:

- **Free (`FREE` plan):** manual refresh only; WebSocket subscriptions are rejected server-side.
- **Paid (`PAID_1Y`, etc.):** WebSocket subscription to `/topic/events/{eventId}` while `subscription_end` is in the future.

### Subscription model

- Table **`subscription_plans`** defines catalog plans (`name`, optional `duration_days`, `description`).
- Seeded plans:
  - **`FREE`** — default; `duration_days` is null (no expiry window).
  - **`PAID_1Y`** — `duration_days = 365`; access lasts until **`users.subscription_end`**.
- **`users`** references a plan via **`subscription_plan_id`** and stores **`subscription_start`** / **`subscription_end`** for paid windows.

Validity for premium (real-time) features is **not** a boolean on the user row: always evaluate **`subscription_end`** against now for non-`FREE` plans (see **`User.isActivePaidUser()`**).

### Assigning plans in application code

- New local and Google signups call **`SubscriptionService.assignFreePlan`** so every user starts on **`FREE`**.
- To activate a paid window programmatically (e.g. future billing hooks):

```java
subscriptionService.activatePaidPlan(user, "PAID_1Y");
```

That sets **`subscription_start`** to now and **`subscription_end`** to now plus the plan’s **`duration_days`**.

### Upgrading users manually (SQL)

Example: grant one year from now:

```sql
UPDATE users u
SET subscription_plan_id = (SELECT id FROM subscription_plans WHERE name = 'PAID_1Y'),
    subscription_start = NOW(),
    subscription_end = NOW() + INTERVAL '365 days'
WHERE u.email = 'you@example.com';
```

Adjust the `WHERE` clause as needed (by `id`, etc.).

### WebSocket flow (high-level)

- Endpoint: `/ws`
- Topic pattern: `/topic/events/{eventId}`
- Event mutations publish to this topic.
- **`WebSocketAuthChannelInterceptor`** allows `/topic/events/*` subscriptions only when **`user.isActivePaidUser()`** is true.

### Where logic lives

- Plans and assignment: **`SubscriptionPlan`** entity, **`SubscriptionPlanRepository`**, **`SubscriptionService`**
- User mapping and auth payloads: **`User`**, **`AuthService.toUserDto`**, **`AuthDtos.UserDto`**
- WebSocket gating: **`realtime/WebSocketAuthChannelInterceptor`**
- Broker config: **`config/WebSocketConfig`**
- Broadcasts: **`realtime/EventRealtimeService`**, **`EventController`**

### Manual test checklist

1. Sign up or log in as a **`FREE`** user and open an event page.
2. Confirm no WebSocket subscription for live updates; use **Refresh** to load changes.
3. Set that user to **`PAID_1Y`** with a future **`subscription_end`** (SQL above or **`SubscriptionService.activatePaidPlan`**), then log in again.
4. Open the same event in two sessions; perform join / decline / add expense in one and confirm the other updates immediately.
