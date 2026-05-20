# CampusSync Backend Handbook

## 1. What This Backend Is

`CampusSync` is a Spring Boot 3 backend for a campus platform that combines identity, events, payments, social networking, realtime notifications, AI assistance, moderation, analytics, and creator tools in a single monolith.

This backend currently supports:

- JWT authentication with refresh-token rotation
- email OTP verification and password reset
- Redis-backed production token/state flows with local in-memory development fallbacks
- event creation, publishing, registration, payment, attendance, certificate, and feedback
- social content: posts, comments, likes, follows, feed
- realtime notifications over STOMP + SockJS
- AI chat and streaming responses
- moderation, reporting, dashboard, and analytics
- cloud-ready storage via local or S3-backed file storage

It is still a monolith, but several abstractions already exist that make future service extraction possible.

## 2. Technology Stack

Core runtime:

- Java `21`
- Spring Boot `3.5.13`
- Maven

Spring modules:

- Spring Web
- Spring Validation
- Spring Security
- Spring Data JPA
- Spring Mail
- Spring WebSocket
- Spring Actuator
- Spring WebFlux

Infrastructure:

- MySQL as primary database
- Redis for production token/state/cache behavior
- Micrometer + Prometheus endpoint
- Swagger / Springdoc OpenAPI
- Bucket4j rate limiting

Third-party integrations:

- Razorpay for paid event checkout
- Gemini API for AI responses
- AWS S3 compatible storage for object storage

## 3. Architectural Style

The backend follows a conventional layered architecture:

- `Controller`: HTTP request/response boundaries
- `Service`: business rules, orchestration, side effects
- `Repository`: database access
- `Model`: JPA entities and enums
- `Dto`: request/response contracts
- `Config`: infrastructure wiring and application policy
- `Exception`: central API error mapping

The main execution path is:

`Controller -> Service -> Repository -> Database`

Cross-cutting concerns are handled through:

- security filter chain and JWT filter
- global exception handling with `ProblemDetail`
- async task executors
- rate limiting
- logging correlation ID
- websocket message broadcasting

## 4. Package Map

Top-level Java packages under `src/main/java/com/campussync/backend`:

- `Controller`
- `Service`
- `Repository`
- `Model`
- `Dto`
- `Config`
- `Exception`

Important controller classes:

- `AuthController`
- `UserProfileController`
- `EventController`
- `EventCreatorController`
- `RegistrationController`
- `PaymentController`
- `PostController`
- `CommentController`
- `LikeController`
- `FollowController`
- `FeedController`
- `NotificationController`
- `SearchController`
- `AIController`
- `AnalyticsController`
- `DashboardController`
- `AdminModerationController`
- `ReportController`
- `RealtimeController`
- `FileController`
- `SocietyController`

Important service classes:

- `UserService`
- `EventService`
- `RegistrationService`
- `PaymentService`
- `EventParticipantService`
- `DynamicRegistrationFieldService`
- `FeedbackService`
- `NotificationService`
- `SocietyMembershipService`
- `NotificationDispatchService`
- `RealtimeService`
- `PostService`
- `CommentService`
- `LikeService`
- `FollowService`
- `FeedService`
- `SearchService`
- `SearchIndexService`
- `AIService`
- `CertificateService`
- `QrCodeService`
- `EmailService`
- `AnalyticsService`
- `DashboardService`
- `AdminModerationService`
- `AuditService`
- `FileService`
- `LocalFileStorageService`
- `S3FileStorageService`

Important config classes:

- `SecurityConfig`
- `JwtFilter`
- `JwtUtil`
- `WebSocketConfig`
- `CorsConfig`
- `WebConfig`
- `RateLimitConfig`
- `ObservabilityConfig`
- `AsyncConfig`
- `RuntimeProfileValidator`
- `RedisConfig`
- `CacheConfig`
- `LocalCacheConfig`
- `StorageProperties`
- `PropertiesConfig`
- `PaymentSchemaMigration`
- `NotificationSchemaMigration`
- `SwaggerConfig`

## 5. Runtime Profiles and Environments

### Local / Default / Dev

Local runtime is intentionally more forgiving:

- default Spring profile is `dev`
- storage provider defaults to `local`
- Redis is not required for normal local startup
- transient OTP/reset/verification state is stored in memory
- access-token blocklist and refresh-token storage can use in-memory stores
- cache falls back to local in-memory cache

This makes the backend developer-friendly without forcing Redis or S3 during local development.

### Production

Production expectations:

- run with `prod` profile
- use Redis-backed token, blocklist, transient-state, and cache behavior
- use external object storage such as S3
- externalize secrets through environment variables
- avoid local filesystem dependence
- avoid in-memory token-state fallbacks unless explicitly intended

### Important Runtime Guardrails

`RuntimeProfileValidator` and property-based behavior are used to avoid accidental production misconfiguration, especially around token storage and Redis requirements.

## 6. Configuration Model

Main application config file:

- `src/main/resources/application.properties`

Current important properties:

- `spring.profiles.default=dev`
- datasource host/port/db/username/password driven by env variables
- `spring.jpa.hibernate.ddl-auto=update`
- `spring.jpa.show-sql=true`
- multipart upload limits configured
- Gemini API config
- mail config
- JWT secret config
- Redis config
- actuator exposure config
- payment provider config
- storage provider config
- token runtime behavior config
- correlation ID logging pattern

### Important Configuration Reality

The current `application.properties` still contains development defaults for several sensitive values such as database password, mail credentials, Razorpay keys, Redis password, and Gemini key.

That means:

- the code is environment-variable aware
- but the checked-in defaults are not production-safe
- real production deployment should remove or neutralize sensitive defaults
- secrets should come only from env vars or secret managers

This is one of the most important operational caveats in the current backend.

## 7. Module-by-Module Functional Breakdown

### 7.1 Auth and User Lifecycle

Handled primarily by:

- `AuthController`
- `UserService`
- `JwtUtil`
- `JwtFilter`
- `RefreshTokenStore` implementations
- `AccessTokenBlocklistStore` implementations
- `TransientStateStore` implementations

Capabilities:

- register with email OTP
- verify account
- login
- refresh access token
- logout current session
- logout all sessions
- forgot password
- reset password
- resend OTP
- token version invalidation
- account activation/deactivation support

### 7.2 Event Management

Handled primarily by:

- `EventController`
- `EventService`
- `SearchService`
- `AnalyticsService`

Capabilities:

- create event
- edit event
- delete event
- publish/cancel status changes
- public listing
- creator-specific listing
- search and filtering
- analytics per event

### 7.3 Registration and Attendance

Handled primarily by:

- `RegistrationController`
- `RegistrationService`
- `EventParticipantService`
- `QrCodeService`

Capabilities:

- free registration
- paid registration
- registration status lookup
- cancellation
- user registration history
- participant list
- participant export
- QR verification check-in
- attendance tracking

### 7.4 Payments

Handled primarily by:

- `PaymentController`
- `PaymentService`
- `PaymentGateway`
- `RazorpayPaymentGateway`
- `PaymentOrderRepository`

Capabilities:

- checkout session creation
- order persistence
- webhook processing
- payment lookup
- refund initiation
- retry-safe pending-payment flow

### 7.5 Social

Handled primarily by:

- `PostController`
- `CommentController`
- `LikeController`
- `FollowController`
- `FeedController`
- `PostService`
- `CommentService`
- `LikeService`
- `FollowService`
- `FeedService`

Capabilities:

- create/edit/delete posts
- comment and reply
- like/unlike
- follow/unfollow
- follower/following stats
- recommendations
- social feed

### 7.6 Notifications and Realtime

Handled primarily by:

- `NotificationController`
- `NotificationService`
- `NotificationDispatchService`
- `RealtimeController`
- `RealtimeService`
- `WebSocketConfig`
- `PresenceTrackingListener`

Capabilities:

- store notification rows
- unread and read state APIs
- push notifications over websocket
- user-specific queue delivery
- topic fallback delivery
- realtime event, social, and feed updates

### 7.7 AI

Handled primarily by:

- `AIController`
- `AIService`
- `AiConversation`
- `AiMessage`

Capabilities:

- chat
- one-off generate endpoint
- conversation list/detail
- conversation delete
- regenerate message
- streaming chat output

### 7.8 Moderation / Reporting / Analytics / Dashboard

Handled primarily by:

- `AnalyticsController`
- `DashboardController`
- `AdminModerationController`
- `ReportController`
- `AnalyticsService`
- `DashboardService`
- `AdminModerationService`
- `AuditService`

Capabilities:

- reports against content
- report resolution
- user role/status changes
- audit logging
- platform metrics
- dashboard summaries

### 7.9 File Storage

Handled primarily by:

- `FileController`
- `FileService`
- `FileStorageService`
- `LocalFileStorageService`
- `S3FileStorageService`

Capabilities:

- event image uploads
- post media uploads
- local or cloud-backed storage abstraction

## 8. Domain Model

### 8.1 User

Main purpose:

- identity, role, profile, verification, and lifecycle state

Important fields:

- `id`
- `name`
- `email`
- `password`
- `role`
- `isVerified`
- `active`
- `bio`
- `profilePictureUrl`
- `tokenVersion`
- `createdAt`
- `updatedAt`
- `deactivatedAt`
- `deactivationReason`
- `verificationCode`

Behavioral notes:

- `tokenVersion` allows forced invalidation of older JWTs
- `isVerified` gates email-verification lifecycle
- `active=false` effectively disables account access

### 8.2 Event

Main purpose:

- organizer-created event with registration and payment behavior

Important fields:

- `id`
- `title`
- `description`
- `venue`
- `date`
- `type`
- `status`
- `paid`
- `price`
- `imageUrl`
- `viewsCount`
- `createdBy`
- `createdAt`

Behavioral notes:

- event can be public, draft, or cancelled through status logic
- `paid` plus `price` determine whether checkout is required

### 8.3 Registration

Main purpose:

- link user to event and track registration/payment/attendance state

Important fields:

- `id`
- `user`
- `event`
- `status`
- `paymentRequired`
- `paymentStatus`
- `paymentOrderId`
- `attended`
- `checkedInAt`
- `qrCode`
- `createdAt`

Current practical state model:

- `REGISTERED`
- `PAYMENT_PENDING`
- `CANCELLED`

Current payment-status usage:

- `NOT_REQUIRED`
- `PENDING`
- `PAID`
- `FAILED`
- `REFUNDED`
- `CANCELLED`

Behavioral notes:

- `attended` is only true after organizer/admin check-in
- `checkedInAt` is set at verification time
- `qrCode` is a verification token, not a payment token
- QR is generated only when the participant is actually confirmed

### 8.4 PaymentOrder

Main purpose:

- persist checkout and payment lifecycle for an event registration

Important fields:

- `id`
- `provider`
- `status`
- `user`
- `event`
- `registration`
- `amountInMinorUnits`
- `currency`
- `providerOrderId`
- `providerPaymentId`
- `checkoutPublicKey`
- `paidAt`
- `refundedAt`
- `failureReason`
- `createdAt`
- `updatedAt`

Behavioral notes:

- one registration is expected to map cleanly to one active payment-order record in the retry-safe flow
- provider/status columns were hardened to `VARCHAR` storage to avoid enum truncation problems

### 8.5 PaymentTransaction

Main purpose:

- store transaction-level history separate from the main order row

Behavioral notes:

- transaction status column was also hardened to `VARCHAR`

### 8.6 Notification

Main purpose:

- persistent inbox-style notification record

Important fields:

- `id`
- `userId`
- `actorId`
- `type`
- `relatedId`
- `message`
- `isRead`
- `createdAt`
- `readAt`

Known notification types:

- `LIKE`
- `COMMENT`
- `FOLLOW`
- `EVENT_UPDATE`
- `EVENT_BROADCAST`
- `POST_MENTION`
- `REPLY`

Behavioral notes:

- notification type storage was migrated away from fragile MySQL enum assumptions
- broadcast notifications are both stored and pushed live

### 8.7 Feedback

Main purpose:

- attendee rating and comment for an event

Important fields:

- `id`
- `user`
- `event`
- `rating`
- `comment`
- `createdAt`
- `updatedAt`

Behavioral notes:

- only attended users can submit feedback
- one user/event feedback constraint is expected logically

### 8.8 Social Entities

`Post`:

- author content, optionally tied to event/media

`Comment`:

- comment or reply associated with a post

`Like`:

- user-to-post reaction

`Follow`:

- follower/following relationship between users

### 8.9 AI Entities

`AiConversation`:

- stores conversation metadata and owner

`AiMessage`:

- stores prompt/response history inside a conversation

### 8.10 Report and Audit

`Report`:

- moderation report with target type and resolution status

`AuditLogEntry`:

- admin or system audit trail

### 8.11 SocietyMembership

Main purpose:

- manages user requests to join an event creator's society or organizing group
- status: PENDING, ACCEPTED, REJECTED

### 8.12 EventRegistrationField

Main purpose:

- defines dynamic, custom registration fields for an event
- supports validation types like EMAIL, NUMBER, SELECT, MULTI_SELECT

### 8.13 EventRegistrationAnswer

Main purpose:

- stores user answers to dynamic registration fields
- includes label snapshot for export stability

## 9. Enums and State Vocabulary

Important enums visible in the model package:

- `Role`
- `EventStatus`
- `EventType`
- `PaymentProvider`
- `PaymentOrderStatus`
- `PaymentTransactionStatus`
- `ReportStatus`
- `ReportTargetType`

Current important business vocabulary:

- user roles: `STUDENT`, `SOCIETY`, `DEPARTMENT`, `ADMIN`
- registration lifecycle: `REGISTERED`, `PAYMENT_PENDING`, `CANCELLED`
- payment lifecycle: `PENDING`, `PAID`, `FAILED`, `REFUNDED`
- attendance lifecycle: not attended -> checked in -> eligible for certificate and feedback

## 10. Data and Relationship Summary

Key relationships:

- one `User` can create many `Event`
- one `User` can have many `Registration`
- one `Event` can have many `Registration`
- one `Registration` belongs to one `User` and one `Event`
- one `Registration` may link to one payment order flow
- one `User` can create many `Post`
- one `Post` can have many `Comment`
- one `User` can follow many users and be followed by many users
- one `User` can submit many `Feedback`, but practically only one per event should exist
- one `Notification` belongs to a target `userId` and may have an `actorId`

## 11. Detailed Business Flows

### 11.1 Registration Flow: Free Event

1. user hits `POST /registrations?eventId=...`
2. service validates user, event, event status, and duplicate-registration conditions
3. registration row is created as confirmed
4. `status=REGISTERED`
5. `paymentRequired=false`
6. `paymentStatus=NOT_REQUIRED`
7. QR verification token is generated immediately
8. email/notification side effects may be triggered
9. response returns registration summary

### 11.2 Registration Flow: Paid Event

1. user hits `POST /registrations?eventId=...`
2. backend verifies event is paid
3. registration is created or reused in pending state
4. payment order is created or reused
5. Razorpay order/session metadata is returned
6. UI redirects/open checkout
7. webhook or verification callback finalizes payment outcome

Success outcome:

- payment order becomes `PAID`
- registration becomes `REGISTERED`
- payment status becomes `PAID`
- QR verification token is generated

Failure/abandon outcome:

- order may remain pending or transition to failed
- registration must not be treated as completed
- user can retry checkout without duplicate order-row corruption

### 11.3 Payment Retry Safety

This is an important corrected behavior in the current backend.

Current expected behavior:

- if a paid registration already exists in pending form, the backend reuses it
- if a payment order already exists for that registration, it is reused instead of inserting another row
- the UI should not treat `PAYMENT_PENDING` as “already successfully registered”

This prevents:

- duplicate key insert errors on `payment_order`
- fake “registered” UI state after checkout was closed or failed

### 11.4 QR Generation and Check-in

QR purpose:

- verify a confirmed participant at the event

QR does not represent:

- payment authorization
- checkout link
- payment collection token

QR generation rules:

- free registration: generate at successful registration
- paid registration: generate only after payment confirmation

Check-in flow:

1. organizer/admin scans QR
2. `POST /events/check-in`
3. request body contains `qrCode`
4. backend validates signature/payload
5. backend extracts registration and user identity
6. backend ensures registration is confirmed
7. backend ensures caller is event creator or admin
8. duplicate check-in is rejected
9. registration is updated with:
   - `attended=true`
   - `checkedInAt=<timestamp>`

### 11.5 Participant Management

Organizer tools include:

- paginated participant list
- filter by paid state
- filter by attendance state
- CSV export

Use cases:

- attendance desk
- paid participant validation
- post-event record keeping
- creator operations

### 11.6 Broadcast Messaging

Flow:

1. organizer calls `POST /events/{eventId}/broadcast`
2. service resolves event ownership
3. service loads only `REGISTERED` participants
4. recipients are deduplicated by user
5. notification rows are saved
6. websocket push is triggered

Important current behavior:

- broadcasts do not target cancelled or unpaid-pending registrations
- current manager/organizer is used as sender context
- notifications should still dispatch even if some optional lookup paths fail

### 11.7 Certificate Generation

Flow:

1. request `GET /events/{eventId}/certificate/{userId}`
2. backend validates event and user relationship
3. backend validates attended status
4. PDF is generated dynamically
5. response returns PDF bytes

Important current rules:

- certificate only for attended participants
- attendee may access own certificate
- organizer/admin can access participant certificate according to current authorization logic
- certificate generation no longer depends on `lowagie` / OpenPDF imports in the implementation

### 11.8 Feedback Flow

Flow:

1. attendee submits `POST /events/{eventId}/feedback`
2. backend validates rating range and payload
3. backend validates attendance
4. backend persists feedback
5. reads happen through paginated listing

Rules:

- only attended users can submit
- rating is expected in range `1..5`

### 11.9 Notification Flow

Notifications can be created from:

- likes
- comments
- follows
- event updates
- event broadcasts
- replies

Delivery model:

- persistent DB record
- websocket push for active clients
- unread/read REST retrieval

### 11.10 AI Flow

Capabilities include:

- single generate endpoint
- authenticated multi-message conversation flow
- conversation history retrieval
- conversation deletion
- regenerate endpoint
- streaming endpoint for chunked response delivery

Provider model:

- Gemini API configuration is externalized by properties
- fail-fast behavior is configurable

### 11.11 Society Membership Flow

Flow:

1. user submits `POST /societies/{societyId}/join-requests`
2. backend persists pending request
3. organizer/admin reviews list via `GET /societies/{societyId}/join-requests`
4. organizer accepts or rejects with a reason
5. notifications are pushed to requester on outcome

### 11.12 Dynamic Registration Fields Flow

Flow:

1. organizer defines fields via `PUT /events/{eventId}/registration-fields`
2. backend persists fields and generates unique `fieldKey`s
3. attendee calls `GET /events/{eventId}/registration-fields` to render form
4. attendee submits answers in `POST /registrations` payload
5. answers are validated (regex, option lists) and saved with label snapshots
6. answers appear in participant export CSVs

## 12. Security Model

### 12.1 Authentication

Authentication is based on:

- access JWT for request auth
- refresh token for session renewal

Main pieces:

- `JwtUtil`
- `JwtFilter`
- `SecurityConfig`
- refresh token store abstraction
- access token blocklist store abstraction

### 12.2 Token Storage Abstractions

Refresh token abstraction:

- `RefreshTokenStore`
- `RedisRefreshTokenStore`
- `InMemoryRefreshTokenStore`

Access-token blocklist abstraction:

- `AccessTokenBlocklistStore`
- `RedisAccessTokenBlocklistStore`
- `InMemoryAccessTokenBlocklistStore`

Transient auth-state abstraction:

- `TransientStateStore`
- `RedisTransientStateStore`
- `InMemoryTransientStateStore`

This split is important because it lets production require Redis while local environments can stay runnable without external services.

### 12.3 Authorization

Authorization is enforced through:

- request matcher rules in security config
- authenticated-only routes
- role checks
- service-level creator/admin ownership checks

Typical protected groups:

- auth-public routes are open
- user profile routes require auth
- event creator tools require organizer/admin access
- admin routes require elevated roles

### 12.4 Rate Limiting

Rate limiting exists and should especially protect:

- auth endpoints
- AI endpoints

### 12.5 Password and OTP Flows

User lifecycle security includes:

- OTP-based signup verification
- forgot-password OTP
- reset password flow
- resend OTP cooldown behavior

### 12.6 Security Caveats

Important current caveat:

- the config file still contains unsafe development defaults for secrets
- this must be cleaned for true production readiness

## 13. Caching, Redis, and Transient State

### Local runtime

- local/default/dev can operate without Redis
- in-memory cache and state storage are used

### Production runtime

- Redis-backed state and cache are expected

### Why this matters

Without this abstraction split, auth flows such as OTP verification or logout can fail locally if Redis is unavailable. That problem was explicitly corrected in the current backend.

## 14. File Storage Design

Storage abstraction:

- `FileStorageService`

Implementations:

- `LocalFileStorageService`
- `S3FileStorageService`

Current behavior:

- `storage.provider=local` by default
- cloud deployment should set `STORAGE_PROVIDER=s3`

Expected S3-related env vars:

- `S3_BUCKET`
- `AWS_REGION`
- `S3_ENDPOINT`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `S3_PATH_STYLE_ACCESS`

Upload endpoints:

- `POST /files/events/upload`
- `POST /files/posts/upload`

## 15. Search

The current runtime path does not require a locally running Elasticsearch node.

Search-related behavior is served through:

- `SearchController`
- `SearchService`
- `SearchIndexService`

Current endpoint families cover:

- posts
- events
- users
- comments
- discovery
- author-filtered content
- event/category/location variations
- department/verified/top-user views

Important note:

- if managed search is reintroduced or expanded later, it should remain fully externalized and never assume localhost

## 16. Async and Background Work

`AsyncConfig` exists to offload heavier tasks.

Typical async-worthy work in this backend:

- email sending
- notification dispatch
- indexing or search side effects
- PDF generation support paths

Current system is still monolithic and in-process. It does not yet fully use a broker like Kafka or RabbitMQ for durable asynchronous workflows.

## 17. WebSocket and Realtime

Configuration:

- `WebSocketConfig`

Current endpoint:

- `/ws/notifications`

STOMP destinations:

- app destination: `/app/notifications/subscribe`
- user queue: `/user/queue/notifications`
- topic fallback: `/topic/notifications/{userId}`

Realtime uses:

- live notification delivery
- live social/event updates
- presence tracking

Operational note:

- if frontend websocket base URL is wrong, notifications may appear stored in DB but not show live

## 18. Observability and Operational Concerns

### Logging

Logging includes correlation ID support:

- `logging.pattern.level=%5p [corr:%X{correlationId:-na}]`

This helps trace requests across logs.

### Actuator

Exposed endpoints include:

- `health`
- `info`
- `metrics`
- `prometheus`

### Micrometer / Prometheus

Micrometer is present and metrics are available for Prometheus scraping.

### Health Checks

Mail and Redis health probes are disabled by default unless explicitly enabled, which avoids noisy local failures.

## 19. Schema Hardening and Runtime Migrations

The backend currently uses runtime patch migrations for certain unsafe schema cases instead of a full migration framework.

Important migration helpers:

- `NotificationSchemaMigration`
- `PaymentSchemaMigration`

Why they exist:

- earlier MySQL enum/column sizing caused truncation for values like `EVENT_BROADCAST` and provider names like `RAZORPAY`
- startup migrations convert fragile enum/short columns to safer `VARCHAR` columns

Important trade-off:

- this is helpful as a rescue mechanism
- but long term Flyway or Liquibase should replace runtime patching

## 20. Error Handling

Central handler:

- `GlobalExceptionHandler`

Current error style:

- `ProblemDetail`
- validation error mapping
- explicit status-oriented exceptions

Important exception types:

- `BadRequestException`
- `ConflictException`
- `ForbiddenOperationException`
- `NotFoundException`

Typical examples:

- duplicate check-in -> conflict
- certificate without attendance -> forbidden or bad request depending on service rule
- invalid feedback payload -> bad request

## 21. Controller and Endpoint Reference

This section is grouped by controller family. Most major APIs expose both legacy and `/api/v1/...` aliases.

### 21.1 AuthController

Base paths:

- `/auth`
- `/api/v1/auth`

Endpoints:

- `POST /register`
- `POST /verify`
- `POST /login`
- `POST /refresh-token`
- `POST /logout`
- `POST /resend-otp`
- `POST /forgot-password`
- `POST /reset-password`

Purpose:

- full user onboarding and session lifecycle

### 21.2 UserProfileController

Base paths:

- `/users`
- `/api/v1/users`

Endpoints:

- `GET /profile`
- `GET /{userId}/profile`
- `PUT /profile`
- `PATCH /profile/picture`
- `GET /activity`
- `GET /{userId}/activity`
- `GET /{userId}/stats`
- `GET /stats/my-stats`

Purpose:

- current-user profile and public/user stats views

### 21.3 EventController

Base paths:

- `/events`
- `/api/v1/events`

Endpoints:

- `POST /`
- `GET /`
- `GET /all`
- `GET /{id}`
- `PUT /{id}`
- `DELETE /{id}`
- `PATCH /{id}/status`
- `GET /search`
- `GET /search/legacy`
- `GET /{id}/analytics`
- `GET /creator/{creatorId}`

Purpose:

- core event CRUD, listing, search, analytics

### 21.4 EventCreatorController

Base paths:

- `/events`
- `/api/v1/events`

Endpoints:

- `GET /{eventId}/participants`
- `GET /{eventId}/participants/export`
- `POST /check-in`
- `POST /{eventId}/broadcast`
- `GET /{eventId}/certificate/me`
- `GET /{eventId}/certificate/{userId}`
- `POST /{eventId}/feedback`
- `GET /{eventId}/feedback`
- `GET /{eventId}/registration-fields`
- `PUT /{eventId}/registration-fields`

Purpose:

- event-creator operational tools and attendee experience endpoints

### 21.5 RegistrationController

Base paths:

- `/registrations`
- `/api/v1/registrations`

Endpoints:

- `POST /`
- `GET /event/{eventId}/my-status`
- `GET /event/{eventId}/status`
- `GET /status/{eventId}`
- `GET /me`
- `GET /user/{userId}`
- `GET /event/{eventId}`
- `PUT /cancel/{id}`

Purpose:

- create/cancel registration, list user registrations, resolve button state for frontend

Important integration note:

- frontend button state should use the explicit status endpoints, not the generic event listing path

### 21.6 PaymentController

Base paths:

- `/api/payments`
- `/api/v1/payments`

Endpoints:

- `POST /checkout`
- `POST /webhook`
- `GET /me`
- `GET /orders/{orderId}`
- `GET /{orderId}`
- `POST /orders/{orderId}/refund`

Purpose:

- paid event checkout, payment lookup, webhook processing, refund

### 21.7 PostController

Base paths:

- `/api/posts`
- `/api/v1/posts`

Endpoints:

- `POST /`
- `GET /`
- `GET /all`
- `GET /{id}`
- `PUT /{id}`
- `DELETE /{id}`
- `GET /author/{authorId}`
- `GET /author/{authorId}/all`
- `GET /media`
- `GET /event/{eventId}`

Purpose:

- post CRUD and event-related post views

### 21.8 CommentController / LikeController

Comment-style endpoints under posts:

- `POST /{postId}/comments`
- `GET /{postId}/comments`
- `PUT /comments/{commentId}`
- `DELETE /comments/{commentId}`
- `POST /comments/{commentId}/replies`
- `GET /comments/{commentId}/replies`

Like-style endpoints:

- `POST /{postId}/like`
- `GET /{postId}/likes`
- `GET /likes/user`

### 21.9 FollowController

Base paths:

- `/api/follow`
- `/api/v1/follow`

Endpoints:

- `POST /follow/{userId}`
- `DELETE /unfollow/{userId}`
- `GET /is-following/{userId}`
- `GET /stats/{userId}`
- `GET /followers/{userId}`
- `GET /following/{userId}`
- `GET /recommendations`
- `GET /mutual`
- `GET /followers/users/{userId}`
- `GET /following/users/{userId}`

### 21.10 FeedController

Base paths:

- `/feed`
- `/api/v1/feed`

Endpoints:

- `GET /`
- `GET /stats`
- `GET /posts`
- `GET /events`
- `GET /trending`

### 21.11 SearchController

Base paths:

- `/api/search`
- `/api/v1/search`

Endpoints:

- `GET /posts`
- `GET /events`
- `GET /users`
- `GET /comments`
- `GET /discover`
- `GET /posts/by-author/{authorId}`
- `GET /events/by-location`
- `GET /events/by-category`
- `GET /users/by-department`
- `GET /posts/published`
- `GET /users/verified`
- `GET /posts/{postId}/comments`
- `GET /users/top`

### 21.12 NotificationController

Base paths:

- `/api/notifications`
- `/api/v1/notifications`

Endpoints:

- `GET /`
- `GET /unread`
- `POST /{id}/read`
- `POST /read-all`
- `DELETE /{id}`

### 21.13 AIController

Endpoints:

- `GET /ai/generate`
- `POST /api/ai/chat`
- `GET /api/ai/conversations`
- `GET /api/ai/conversations/{conversationId}`
- `DELETE /api/ai/conversations/{conversationId}`
- `POST /api/ai/conversations/{conversationId}/regenerate`
- `POST /api/ai/chat/stream`

### 21.14 Analytics / Dashboard / Admin / Reports

Analytics base:

- `/api/analytics`
- `/api/v1/analytics`

Admin base:

- `/admin`
- `/api/v1/admin`

Important admin endpoints include:

- `GET /users`
- `PATCH /users/{userId}/role`
- `PATCH /users/{userId}/status`
- `GET /reports`
- `PATCH /reports/{reportId}`
- `GET /audit-logs`

### 21.15 FileController

Main usage:

- event uploads
- post uploads

### 21.16 SocietyController

Base paths:

- `/api/societies`
- `/api/v1/societies`

Endpoints:

- `POST /{societyId}/join-requests`
- `DELETE /{societyId}/join-requests/me`
- `GET /{societyId}/membership-status`
- `GET /{societyId}/join-requests`
- `POST /{societyId}/join-requests/{requestId}/accept`
- `POST /{societyId}/join-requests/{requestId}/reject`
- `GET /{societyId}/members`

Purpose:

- managing society membership lifecycle and requests

## 22. DTO and API Contract Notes

The backend uses DTOs rather than exposing JPA entities directly for public API operations.

Important response families include:

- auth responses
- registration responses
- payment checkout/order responses
- participant responses
- check-in responses
- feedback responses
- notification responses
- paginated wrapper responses

Important current contract behaviors:

- registration-status endpoints are meant for frontend button state
- pending paid registration should not be rendered like a fully completed registration
- certificate endpoint uses `userId`, not `registrationId`

## 23. Current Integration Rules the Frontend Must Respect

These are important because they were sources of actual bugs.

- use explicit registration status endpoints for event action buttons
- do not treat `PAYMENT_PENDING` as confirmed registration
- do not generate another checkout blindly if a pending order already exists
- certificate download identifies participant by `userId`
- QR code is for post-registration verification only
- broadcast only reaches confirmed participants
- live notifications depend on correct websocket base URL configuration

## 24. Testing Landscape

Existing test focus areas include:

- JWT logic
- logout and token invalidation
- payment flow
- participant management
- feedback rules
- creator-controller `ProblemDetail` behavior

High-value areas for continued test investment:

- webhook idempotency
- payment retry races
- broadcast delivery behavior
- websocket auth flows
- transient-state expiry
- certificate permission matrix
- refund edge cases

## 25. Known Strengths

The backend currently does several things well:

- layered architecture is clear
- local runtime is now less fragile
- creator tools have improved significantly
- payment retry behavior is safer
- schema hardening exists for previous enum issues
- DTO and `ProblemDetail` usage improve API quality
- storage abstraction supports cloud migration

## 26. Known Weaknesses and Risks

These are important for any serious production-readiness review.

- `application.properties` still contains unsafe development defaults for secrets
- `spring.jpa.hibernate.ddl-auto=update` is convenient but risky in production
- runtime patch migrations are not a substitute for Flyway/Liquibase
- payment status and registration status still rely on string-style conventions in parts of the flow
- async work is still in-process rather than queue-backed
- websocket delivery is best-effort, not a durable message system
- local filesystem mode still exists and must not be mistaken for production storage

## 27. Recommended Next Improvements

Priority improvements:

- remove all checked-in secret defaults
- introduce Flyway or Liquibase
- formalize registration/payment state machines with enums end-to-end
- add idempotency keys for checkout creation and webhook processing
- move notification/email jobs to a queue
- tighten audit coverage for admin actions
- add contract/integration tests for frontend-critical endpoints

Strategic evolution path:

- split into `identity`, `events`, `payments`, `notifications`, `social`, `analytics`, and `ai` services when operational maturity justifies it

## 28. Final Summary

`CampusSync` is a broad Spring Boot campus-platform backend that now supports:

- secure-ish local development without mandatory Redis
- event registration for free and paid events
- retry-safe payment behavior
- QR-based attendee verification
- participant operations for event creators
- broadcast messaging
- certificate generation
- feedback and ratings
- social and realtime collaboration features
- AI chat
- moderation, reporting, and analytics

The most important current realities to remember are:

- paid registration is not complete until payment confirmation
- QR is only for verified registered participants
- attendance unlocks certificates and feedback
- Redis is required for proper production runtime, not local development
- checked-in defaults in config still need production cleanup
- this handbook reflects the backend as it exists now, including the recent fixes around payment retries, broadcast delivery, certificate flow, Redis-free local runtime, and schema hardening
