# HangPlan Backend

Spring Boot API for HangPlan.

## Requirements

- Java 25 (set `JAVA_HOME` to JDK 25)
- Maven 3.9+
- PostgreSQL

## Database setup

1. Create a PostgreSQL database named `hangplan` (or your preferred name).
2. Update datasource values in `src/main/resources/application.yml` if needed:
   - `spring.datasource.url`
   - `spring.datasource.username`
   - `spring.datasource.password`
3. For Google sign-in, set:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`
4. Optional for production: set a strong `JWT_SECRET`.

Google OAuth redirect URL for local development:
`http://localhost:8080/login/oauth2/code/google`

## Run in development

```bash
mvn clean install
mvn spring-boot:run
```

API base URL: `http://localhost:8080`
