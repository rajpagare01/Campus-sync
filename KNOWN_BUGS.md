# Known Bugs

Updated: 28 April 2026

This file lists confirmed bugs or production-impacting issues currently present in the project.

## 1. Redis connectivity breaks auth/session flows

Severity: Critical

Symptoms:
- Login sometimes fails with `Redis command timed out`
- Refresh token rotation fails
- Logout and immediate token invalidation become unreliable
- OTP and password-reset flows can fail

Evidence:
- Runtime logs showed `QueryTimeoutException: Redis command timed out`
- Runtime logs showed `UnknownHostException` for `redis-16068.crce206.ap-south-1-1.ec2.cloud.redislabs.com`

Relevant files:
- [.env.example](/c:/Users/asus/Downloads/backend/backend/.env.example:1)
- [application.properties](/c:/Users/asus/Downloads/backend/backend/src/main/resources/application.properties:1)
- [UserService.java](/c:/Users/asus/Downloads/backend/backend/src/main/java/com/campussync/backend/Service/UserService.java:1)
- [JwtUtil.java](/c:/Users/asus/Downloads/backend/backend/src/main/java/com/campussync/backend/Config/JwtUtil.java:1)

Why this is a bug:
- Auth depends on Redis for refresh tokens, OTPs, and JWT blacklist entries
- If Redis DNS or network is broken, core auth operations degrade or fail

Suggested fix:
- Replace the broken Redis host with a reachable one
- Add environment-specific health checks and fail-fast startup validation for Redis
- Consider graceful fallback or clearer 503 responses when Redis is unavailable

## 2. Hardcoded secrets are present in committed config

Severity: Critical

Symptoms:
- Real-looking credentials and secrets are present in repository-tracked files
- Local defaults can accidentally be used in production-like runs

Relevant files:
- [application.properties](/c:/Users/asus/Downloads/backend/backend/src/main/resources/application.properties:1)
- [.env.example](/c:/Users/asus/Downloads/backend/backend/.env.example:1)

Examples observed:
- Database password default
- Gemini API key default
- Mail password default
- Redis password default
- Razorpay key defaults

Why this is a bug:
- This is a security exposure
- It also makes debugging confusing because the app may silently use unsafe defaults instead of failing loudly

Suggested fix:
- Remove all real/default secrets from committed files
- Replace them with placeholders only
- Rotate any leaked credentials immediately

## 3. Razorpay webhook secret default contains extra spaces

Severity: High

Symptoms:
- Webhook signature verification may fail when relying on the default fallback value

Relevant file:
- [application.properties](/c:/Users/asus/Downloads/backend/backend/src/main/resources/application.properties:1)

Problem:
- `payment.razorpay.webhook-secret=${RAZORPAY_WEBHOOK_SECRET: campussync_razorpay_webhook_2026_secure_key }`
- The fallback value includes leading and trailing spaces

Why this is a bug:
- Signature verification depends on exact secret matching
- Any extra whitespace changes the secret and causes invalid signature errors

Suggested fix:
- Remove the spaces around the fallback value
- Prefer no fallback at all for webhook secrets, so missing config fails explicitly

## 4. `/registrations/user/{userId}` ignores the `userId` path parameter

Severity: High

Symptoms:
- API route suggests admin or caller can fetch registrations for any user ID
- Actual implementation always returns registrations for the currently authenticated user

Relevant file:
- [RegistrationController.java](/c:/Users/asus/Downloads/backend/backend/src/main/java/com/campussync/backend/Controller/RegistrationController.java:1)

Problem:
- The route is declared as `GET /registrations/user/{userId}`
- The controller method does not accept or use `@PathVariable Long userId`

Why this is a bug:
- API contract is misleading
- Frontend or API consumers may believe they are requesting another user's registrations when they are not
- Admin behavior is inconsistent with route naming

Suggested fix:
- Either remove `{userId}` from the route and make it clearly "current user only"
- Or implement actual `userId` support with proper authorization checks

## 5. Immediate logout depends on Redis availability

Severity: High

Symptoms:
- Logout now blacklists the current JWT, but blacklist storage uses Redis
- If Redis is down, old access tokens may remain usable until JWT expiry

Relevant files:
- [UserService.java](/c:/Users/asus/Downloads/backend/backend/src/main/java/com/campussync/backend/Service/UserService.java:1)
- [JwtUtil.java](/c:/Users/asus/Downloads/backend/backend/src/main/java/com/campussync/backend/Config/JwtUtil.java:1)
- [JwtFilter.java](/c:/Users/asus/Downloads/backend/backend/src/main/java/com/campussync/backend/Config/JwtFilter.java:1)

Why this is a bug:
- Logout behavior is security-sensitive
- When Redis is unavailable, token invalidation becomes best-effort rather than guaranteed

Suggested fix:
- Fix Redis first
- Consider a database-backed token version strategy if Redis availability is not guaranteed

## 6. CORS config ignores environment-driven allowed origins

Severity: Medium

Symptoms:
- `.env.example` advertises `CORS_ALLOWED_ORIGINS`
- Backend CORS still uses a hardcoded list

Relevant files:
- [.env.example](/c:/Users/asus/Downloads/backend/backend/.env.example:1)
- [CorsConfig.java](/c:/Users/asus/Downloads/backend/backend/src/main/java/com/campussync/backend/Config/CorsConfig.java:1)

Why this is a bug:
- Deployment config and runtime behavior can drift
- Adding a new frontend origin in env alone will not work

Suggested fix:
- Read allowed origins from configuration properties instead of hardcoding them in Java

## Notes

- The ngrok error shown when opening `/api/payments/webhook` in a browser is not a backend bug by itself. That endpoint is designed for webhook `POST` requests with JSON and a signature header.
- The old "logout keeps previous user" issue was addressed in code with JWT blacklisting, but it still depends on Redis being healthy.
