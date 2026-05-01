# HangPlan Backend

Spring Boot API for HangPlan.

## Requirements

- Java 25 (set `JAVA_HOME` to JDK 25)
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

## Run in development

```bash
mvn clean install
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

API base URL: `http://localhost:8080`

## Real-time Updates & Subscription Model

HangPlan now supports two update modes:

- Free users (`is_premium = false`) use manual refresh.
- Premium users (`is_premium = true`) can subscribe to real-time updates.

### Database flag

Default users stay on the free tier.

```sql
ALTER TABLE users ADD COLUMN is_premium BOOLEAN DEFAULT FALSE;
```

### WebSocket flow (high-level)

- Endpoint: `/ws`
- Topic pattern: `/topic/events/{eventId}`
- Event mutations (join/decline/add expense/create) publish to this topic.
- Subscription is gated server-side: only premium users can subscribe.

### Where logic lives

- Premium flag: `User` entity + auth DTOs (`AuthDtos.UserDto` / `AuthService`).
- WebSocket infra and premium gating:
  - `config/WebSocketConfig`
  - `realtime/WebSocketAuthChannelInterceptor`
  - `realtime/EventRealtimeService`
- Event broadcasts are triggered from `EventController`.

### Manual test checklist

1. Login with a free user (`is_premium = false`) and open an event page.
2. Confirm no real-time subscription is made; use manual refresh to see changes.
3. Mark another user as premium (`is_premium = true`) in DB and log in.
4. Open the same event on two premium sessions.
5. Perform join/decline/add expense in one session and verify the other session updates immediately.
