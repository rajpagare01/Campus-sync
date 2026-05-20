# CampusSync Frontend Backend Integration Guide

This document is based on the current backend code in this repository.

Use it as the source of truth for frontend integration.

## 1. Backend Overview

CampusSync currently provides these frontend-facing feature areas:

- OTP-based authentication and JWT sessions
- User profiles and activity history
- Posts
- Events
- Comments and threaded replies
- Likes
- Follow system
- Event registrations
- Home feed
- Notifications via REST and WebSocket
- Search via Elasticsearch
- Analytics dashboards
- Admin dashboard
- Local file uploads
- AI text generation through Ollama

Backend stack used by the project:

- Spring Boot
- MySQL
- Redis
- Elasticsearch
- STOMP WebSocket with SockJS
- Local file storage under `uploads/`
- Ollama for AI generation

## 2. Frontend URLs

Default local backend base:

```text
http://localhost:8080
```

Static uploaded files:

```text
http://localhost:8080/uploads/{fileName}
```

WebSocket endpoint:

```text
http://localhost:8080/ws/notifications
```

## 3. Very Important Security Rule

Frontend must not embed backend secrets.

The following backend environment keys are server-side only and must never be exposed in the frontend bundle:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_USERNAME`
- `REDIS_PASSWORD`
- `REDIS_SSL`
- `REDIS_TIMEOUT`
- `ELASTICSEARCH_URIS`
- `ELASTICSEARCH_USERNAME`
- `ELASTICSEARCH_PASSWORD`
- `ELASTICSEARCH_SSL_INSECURE`
- `ELASTICSEARCH_CA_FINGERPRINT`
- `ELASTICSEARCH_CONNECT_TIMEOUT`
- `ELASTICSEARCH_SOCKET_TIMEOUT`
- `OLLAMA_API_KEY`
- `OLLAMA_API_URL`
- `OLLAMA_MODEL`

Frontend only needs:

- backend base URL
- WebSocket endpoint URL
- uploaded-file base URL
- access token
- refresh token

## 4. Auth Model

### 4.1 Roles

Available roles:

- `STUDENT`
- `SOCIETY`
- `DEPARTMENT`
- `ADMIN`

### 4.2 Tokens

`AuthResponse`:

```json
{
  "user": {
    "id": "1",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "STUDENT",
    "profilePicture": null,
    "bio": null,
    "createdAt": "2026-04-20T00:00:00"
  },
  "accessToken": "jwt-token",
  "refreshToken": "uuid-token",
  "message": null
}
```

How auth works:

- access token is a JWT
- JWT contains subject = user email
- JWT contains a `role` claim
- access token expiry = 1 day
- refresh token expiry = 7 days
- refresh tokens are stored in Redis

Send authenticated HTTP requests with:

```http
Authorization: Bearer <accessToken>
```

### 4.3 OTP Flow

Register flow:

1. `POST /auth/register`
2. user receives OTP in email
3. `POST /auth/verify`
4. backend creates account and returns tokens

Login flow:

1. `POST /auth/login`
2. backend returns `AuthResponse`

Refresh flow:

1. `POST /auth/refresh-token`
2. send old refresh token
3. backend returns new access token and new refresh token

Logout behavior:

- `POST /auth/logout` only returns `"Logged out"`
- it does not revoke tokens
- frontend should clear local auth state itself

## 5. Real Route Access Rules

Actual security comes from `SecurityConfig`, not controller comments.

### Public routes

- `POST /auth/**`
- `GET /events`
- `GET /events/search`
- `GET /events/{single-segment}`

That means these event GET routes are public:

- `GET /events`
- `GET /events/all`
- `GET /events/{id}`

### Authenticated routes

Everything else requires a Bearer token, including:

- `GET /api/posts`
- `GET /users/{id}/profile`
- all `/api/search/**`
- all `/api/analytics/**`
- all `/feed/**`
- all `/api/notifications/**`
- all `/api/follow/**`
- all `/files/**`
- `GET /events/creator/{creatorId}`
- `GET /events/search/legacy`

### Role-restricted routes

- admin only: `GET /dashboard`
- student only: `POST /registrations?eventId=...`
- student or admin: `GET /registrations/user/{userId}`, `PUT /registrations/cancel/{id}`
- society or department or admin: `GET /registrations/event/{eventId}`

### Important note

Students can now create:

- posts
- events

## 6. Common Response Shapes

### 6.1 Custom paginated response

Used by posts, events, followers, following, recommendations, creator events:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true,
  "empty": true
}
```

### 6.2 Spring `Page` response

Used by `/api/search/**`.

Frontend should expect normal Spring Data page JSON:

- `content`
- `pageable`
- `totalPages`
- `totalElements`
- `size`
- `number`
- `first`
- `last`
- `numberOfElements`
- `empty`

### 6.3 Error response

Global error format usually looks like:

```json
{
  "timestamp": "2026-04-20T00:00:00",
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "email": "Invalid email format"
  }
}
```

Important backend behavior:

- validation errors usually return `400`
- auth failures can return `401`
- permission failures can return `403`
- many business errors are thrown as plain `RuntimeException` and currently return `500` with a useful `message`

Frontend should always read the error body `message` instead of depending only on status code semantics.

## 7. Enums and Frontend Constants

### Role

- `STUDENT`
- `DEPARTMENT`
- `ADMIN`
- `SOCIETY`

### EventStatus

- `DRAFT`
- `PUBLISHED`
- `CANCELLED`

### EventType

- `SOCIETY`
- `DEPARTMENT`
- `PLACEMENT`

### NotificationType

- `LIKE`
- `COMMENT`
- `FOLLOW`
- `EVENT_UPDATE`
- `POST_MENTION`
- `REPLY`

### Registration status

- `REGISTERED`
- `CANCELLED`

### Feed item type

- `POST`
- `EVENT`

### Activity type

- `POST`
- `COMMENT`
- `LIKE`
- `REGISTRATION`

## 8. Feature-by-Feature API Reference

## 8.1 Auth

### POST `/auth/register`

Auth: public

Request:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "secret123",
  "role": "STUDENT"
}
```

Validation:

- name: 2 to 50 chars
- email: valid email
- password: 6 to 100 chars
- role required
- backend rejects `ADMIN` registration

Response:

```json
{
  "message": "OTP sent to email",
  "email": "john@example.com"
}
```

### POST `/auth/verify`

Auth: public

Request:

```json
{
  "email": "john@example.com",
  "code": "123456"
}
```

Response: `AuthResponse`

### POST `/auth/login`

Auth: public

Request:

```json
{
  "email": "john@example.com",
  "password": "secret123"
}
```

Response: `AuthResponse`

### POST `/auth/refresh-token`

Auth: public

Request:

```json
{
  "refreshToken": "uuid-token"
}
```

Response: `AuthResponse`

### POST `/auth/logout`

Auth: public

Response:

```text
Logged out
```

### POST `/auth/resend-otp?email=...`

Auth: public

Response: plain string

Notes:

- resend cooldown is 60 seconds

### POST `/auth/forgot-password?email=...`

Auth: public

Response: plain string

### POST `/auth/reset-password?email=...&otp=...&newPassword=...`

Auth: public

Response: plain string

## 8.2 Users and Profiles

### GET `/users/profile`

Auth: required

Response: `UserProfileResponse`

```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "STUDENT",
  "bio": "Hello",
  "profilePictureUrl": "avatar.jpg",
  "verified": true,
  "createdAt": "2026-04-20T00:00:00",
  "updatedAt": "2026-04-20T00:00:00",
  "postCount": 5,
  "eventCount": 2,
  "likeCount": 20,
  "commentCount": 8,
  "followersCount": 0,
  "followingCount": 0,
  "following": false,
  "followedBy": false,
  "mutual": false
}
```

### GET `/users/{userId}/profile`

Auth: required in current backend

Response: `UserProfileResponse`

### PUT `/users/profile`

Auth: required

Request:

```json
{
  "name": "John Doe",
  "bio": "Updated bio",
  "profilePictureUrl": "avatar.jpg"
}
```

### PATCH `/users/profile/picture?pictureUrl=...`

Auth: required

Response: updated `UserProfileResponse`

### GET `/users/activity`

Auth: required

Response: `UserActivityResponse[]`

### GET `/users/{userId}/activity`

Auth: required in current backend

Response: `UserActivityResponse[]`

### GET `/users/{userId}/stats`

Auth: required in current backend

Response:

```json
{
  "userId": 1,
  "userName": "John Doe",
  "postCount": 5,
  "eventRegistrations": 2,
  "likeCount": 20,
  "commentCount": 8,
  "joinedDate": "2026-04-20T00:00:00"
}
```

### GET `/users/stats/my-stats`

Auth: required

Response: same map shape as above

## 8.3 Posts

### POST `/api/posts`

Auth: required

Allowed roles:

- `STUDENT`
- `SOCIETY`
- `DEPARTMENT`
- `ADMIN`

Request:

```json
{
  "content": "Student announcement",
  "mediaUrl": "my-image.jpg",
  "eventId": 12
}
```

Validation:

- content: 1 to 2000 chars
- mediaUrl max 1000 chars
- eventId optional

Response: `PostResponse`

```json
{
  "id": 1,
  "content": "Student announcement",
  "mediaUrl": "my-image.jpg",
  "eventId": 12,
  "eventTitle": "Hackathon",
  "authorId": 5,
  "authorName": "John Doe",
  "createdAt": "2026-04-20T00:00:00",
  "updatedAt": "2026-04-20T00:00:00",
  "likeCount": 0,
  "commentCount": 0,
  "likedByCurrentUser": false
}
```

### GET `/api/posts`

Auth: required

Query:

- `page` default `0`
- `size` default `20`, max `50`

Response: `PaginatedResponse<PostResponse>`

### GET `/api/posts/all`

Auth: required

Response: `PostResponse[]`

### GET `/api/posts/{id}`

Auth: required

Response: `PostResponse`

### PUT `/api/posts/{id}`

Auth: required

Update rule:

- author can update own post
- admin can update any post

Request: same shape and validation as `POST /api/posts`

Behavior:

- updates `content` and `mediaUrl`
- if `eventId` is provided, links the post to that event
- if `mediaUrl` is omitted or `null`, the existing media is left unchanged
- if `eventId` is omitted or `null`, the existing event link is left unchanged

Response: `PostResponse`

### GET `/api/posts/author/{authorId}`

Auth: required

Response: `PaginatedResponse<PostResponse>`

### GET `/api/posts/author/{authorId}/all`

Auth: required

Response: `PostResponse[]`

### GET `/api/posts/media`

Auth: required

Response: `PaginatedResponse<PostResponse>`

### GET `/api/posts/event/{eventId}`

Auth: required

Response: `PaginatedResponse<PostResponse>`

### DELETE `/api/posts/{id}`

Auth: required

Delete rule:

- author can delete own post
- admin can delete any post

Response:

```text
Post deleted successfully
```

## 8.4 Comments

All comment routes are under `/posts/**` and require authentication because of global security.

### POST `/posts/{postId}/comments`

Request:

```json
{
  "content": "Nice post"
}
```

Response: `CommentResponse`

### GET `/posts/{postId}/comments`

Response: threaded `CommentResponse[]`

`CommentResponse`:

```json
{
  "id": 1,
  "content": "Nice post",
  "authorId": 2,
  "authorName": "Jane",
  "postId": 10,
  "createdAt": "2026-04-20T00:00:00",
  "updatedAt": "2026-04-20T00:00:00",
  "parentCommentId": null,
  "replies": [],
  "replyCount": 0
}
```

### PUT `/posts/comments/{commentId}`

Auth: required

Rule:

- only comment owner can edit

### DELETE `/posts/comments/{commentId}`

Auth: required

Rule:

- owner or admin can delete
- backend blocks deletion if comment has replies

### POST `/posts/comments/{commentId}/replies`

Auth: required

Same request shape as comment create.

### GET `/posts/comments/{commentId}/replies`

Auth: required

Returns flat reply list.

## 8.5 Likes

All like routes are under `/posts/**` and require authentication.

### POST `/posts/{postId}/like`

Behavior:

- acts as toggle
- if user has liked, it unlikes
- if user has not liked, it likes

Response:

```json
{
  "liked": true,
  "likeCount": 4,
  "like": {
    "id": 11,
    "userId": 5,
    "userName": "John Doe",
    "postId": 10,
    "createdAt": "2026-04-20T00:00:00"
  }
}
```

### GET `/posts/{postId}/likes`

Response: `LikeResponse[]`

### GET `/posts/likes/user`

Response: likes created by current user

## 8.6 Follow System

All follow routes require authentication.

### POST `/api/follow/follow/{userId}`

Rule:

- cannot follow yourself
- cannot follow same user twice

Response:

```json
{
  "id": 1,
  "followerId": 5,
  "followerName": "John",
  "followingId": 6,
  "followingName": "Jane",
  "createdAt": "2026-04-20T00:00:00",
  "mutual": false
}
```

### DELETE `/api/follow/unfollow/{userId}`

Response:

```text
Successfully unfollowed user
```

### GET `/api/follow/is-following/{userId}`

Response:

```json
true
```

### GET `/api/follow/stats/{userId}`

Response:

```json
{
  "followersCount": 10,
  "followingCount": 4,
  "following": true,
  "followedBy": false,
  "mutual": false
}
```

### GET `/api/follow/followers/{userId}`

Response: `PaginatedResponse<FollowResponse>`

### GET `/api/follow/following/{userId}`

Response: `PaginatedResponse<FollowResponse>`

### GET `/api/follow/recommendations`

Response: `PaginatedResponse<User>`

Important:

- this returns the raw `User` entity, not a DTO
- `email` and `password` are JSON-hidden
- use it defensively in frontend

### GET `/api/follow/mutual`

Response: `User[]`

### GET `/api/follow/followers/users/{userId}`

Response: `PaginatedResponse<User>`

### GET `/api/follow/following/users/{userId}`

Response: `PaginatedResponse<User>`

## 8.7 Events

### POST `/events`

Auth: required

Allowed roles:

- `STUDENT`
- `SOCIETY`
- `DEPARTMENT`
- `ADMIN`

Request body is the `Event` entity.

Recommended request payload:

```json
{
  "title": "Hackathon",
  "description": "24-hour coding event",
  "venue": "Main Hall",
  "date": "2026-04-22T10:00:00",
  "type": "SOCIETY",
  "status": "DRAFT",
  "paid": false,
  "price": 0,
  "imageUrl": "poster.jpg"
}
```

Backend behavior:

- `createdBy` is set from current user
- `status` defaults to `DRAFT` if omitted

Response: `Event`

### GET `/events`

Auth: public

Returns only `PUBLISHED` events, paginated.

### GET `/events/all`

Auth: public

Returns published events only, non-paginated.

### GET `/events/{id}`

Auth: public

Behavior:

- increments `viewsCount` every time it is called

### PUT `/events/{id}`

Auth: required

Rule:

- creator or admin only

### DELETE `/events/{id}`

Auth: required

Rule:

- creator or admin only

### PATCH `/events/{id}/status?status=PUBLISHED`

Auth: required

Rule:

- creator or admin only

### GET `/events/search`

Auth: public

Query params:

- `keyword`
- `type`
- `status`
- `page`
- `size`

Response: `PaginatedResponse<Event>`

### GET `/events/search/legacy`

Auth: required in current backend

### GET `/events/{id}/analytics`

Auth: required

Rule:

- creator or admin only

Response:

```json
{
  "eventId": 10,
  "title": "Hackathon",
  "status": "PUBLISHED",
  "views": 150,
  "totalRegistrations": 90,
  "activeRegistrations": 80,
  "cancelledRegistrations": 10,
  "registrationConversionRate": 53.33,
  "engagementScore": 310.0
}
```

### GET `/events/creator/{creatorId}`

Auth: required in current backend

Response: `PaginatedResponse<Event>`

## 8.8 Event Registrations

### POST `/registrations?eventId={eventId}`

Auth: required

Allowed role:

- `STUDENT`

Response:

```json
{
  "id": 1,
  "userName": "John Doe",
  "eventTitle": "Hackathon",
  "status": "REGISTERED"
}
```

### GET `/registrations/user/{userId}`

Auth: required

Allowed roles:

- `STUDENT`
- `ADMIN`

Important:

- backend ignores the `userId` path value
- it always returns registrations for the current logged-in user

Response:

```json
[
  {
    "registrationId": 1,
    "eventId": 12,
    "eventTitle": "Hackathon",
    "eventVenue": "Main Hall",
    "status": "REGISTERED"
  }
]
```

### GET `/registrations/event/{eventId}`

Auth: required

Allowed roles:

- `SOCIETY`
- `DEPARTMENT`
- `ADMIN`

Response:

```json
[
  {
    "registrationId": 1,
    "userName": "John Doe",
    "status": "REGISTERED"
  }
]
```

### PUT `/registrations/cancel/{id}`

Auth: required

Allowed roles:

- `STUDENT`
- `ADMIN`

Rule:

- owner or admin only

Response:

```text
Registration cancelled
```

## 8.9 Feed

All feed routes require authentication.

### GET `/feed?page=0&size=20&filter=all&sort=date`

Query:

- `filter`: `all`, `posts`, `events`
- `sort`: `date`, `engagement`

Response: `FeedItem[]`

`FeedItem` combines posts and events:

```json
{
  "type": "POST",
  "id": 1,
  "createdAt": "2026-04-20T00:00:00",
  "title": "Post by John Doe",
  "content": "Post body",
  "authorName": "John Doe",
  "authorId": 5,
  "mediaUrl": "image.jpg",
  "likeCount": 4,
  "commentCount": 0,
  "likedByCurrentUser": true,
  "venue": null,
  "eventDate": null,
  "eventType": null,
  "paid": null,
  "price": null,
  "imageUrl": null,
  "engagementScore": 12.0
}
```

### GET `/feed/stats`

Response:

```json
{
  "totalPosts": 100,
  "totalEvents": 25,
  "paidEvents": 5,
  "timestamp": "2026-04-20T00:00:00"
}
```

### GET `/feed/posts`

Same as `/feed` with `filter=posts`.

### GET `/feed/events`

Same as `/feed` with `filter=events`.

## 8.10 Search

All search routes require authentication.

Search results use Elasticsearch document DTOs, not the main entity DTOs.

### GET `/api/search/posts?q=...`

Response element fields:

- `id`
- `content`
- `authorId`
- `authorName`
- `tags`
- `createdAt`
- `likeCount`
- `commentCount`
- `isPublished`

### GET `/api/search/events?q=...`

Response element fields:

- `id`
- `title`
- `description`
- `location`
- `eventDate`
- `createdAt`
- `organizerId`
- `organizerName`
- `attendeeCount`
- `category`

### GET `/api/search/users?q=...`

Response element fields:

- `id`
- `name`
- `email`
- `bio`
- `department`
- `followersCount`
- `isVerified`

### GET `/api/search/comments?q=...`

Response element fields:

- `id`
- `content`
- `authorId`
- `authorName`
- `postId`
- `createdAt`
- `likeCount`

Additional search endpoints:

- `GET /api/search/posts/by-author/{authorId}`
- `GET /api/search/events/by-location?location=...`
- `GET /api/search/events/by-category?category=...`
- `GET /api/search/users/by-department?department=...`
- `GET /api/search/posts/published`
- `GET /api/search/users/verified`
- `GET /api/search/posts/{postId}/comments`
- `GET /api/search/users/top`

## 8.11 Notifications

All notification REST routes require authentication.

### GET `/api/notifications`

Response: `NotificationDTO[]`

```json
{
  "id": 1,
  "userId": 5,
  "actorId": 7,
  "type": "LIKE",
  "relatedId": 10,
  "message": "Jane liked your post",
  "isRead": false,
  "createdAt": "2026-04-20T00:00:00",
  "readAt": null
}
```

### GET `/api/notifications/unread`

Response:

```json
{
  "count": 3,
  "notifications": []
}
```

### POST `/api/notifications/{id}/read`

Response: empty `200 OK`

### POST `/api/notifications/read-all`

Response:

```json
{
  "markedAsRead": 3
}
```

### DELETE `/api/notifications/{id}`

Response: empty `200 OK`

### Notification triggers

Notifications are created when:

- a user likes a post
- a user comments on a post
- a user follows another user

There is also code for event update notifications, but event update endpoints do not currently call it.

## 8.12 WebSocket Notifications

Protocol:

- STOMP over SockJS

Handshake endpoint:

```text
/ws/notifications
```

Broker config:

- application prefix: `/app`
- simple broker prefixes: `/topic`, `/queue`
- user prefix: `/user`

Useful frontend subscriptions:

- `/user/queue/notifications`
- `/topic/notifications/{currentUserId}`

Client send message to subscribe:

Destination:

```text
/app/notifications/subscribe
```

Message body:

```json
{
  "action": "SUBSCRIBE"
}
```

Ack read message:

Destination:

```text
/app/notifications/ack
```

Message body:

```json
{
  "notificationId": 123
}
```

Notes:

- backend broadcasts each notification both to `/user/queue/notifications` and `/topic/notifications/{userId}`
- because WebSocket auth wiring is minimal, subscribing to `/topic/notifications/{currentUserId}` is the safer frontend fallback

## 8.13 Files

All file upload routes require authentication.

### POST `/files/events/upload`

Content type:

- `multipart/form-data`

Field:

- `file`

Response:

```text
generated-file-name.ext
```

### POST `/files/posts/upload`

Same behavior as event upload.

Upload rules:

- image and video only
- max 50 MB
- stored locally in backend `uploads/`
- response is filename only, not full URL

Frontend should convert returned filename to:

```text
http://localhost:8080/uploads/{fileName}
```

## 8.14 Analytics

All analytics routes require authentication.

Available routes:

- `GET /api/analytics/users/engagement?days=30`
- `GET /api/analytics/users/daily-stats?days=30`
- `GET /api/analytics/users/retention?cohortDate=2026-04-01T00:00:00`
- `GET /api/analytics/users/growth?days=30`
- `GET /api/analytics/events/trending?limit=10`
- `GET /api/analytics/events/{id}/stats`
- `GET /api/analytics/events/category-stats`
- `GET /api/analytics/posts/{id}/performance`
- `GET /api/analytics/content/trends?days=30&limit=10`
- `GET /api/analytics/content/trending-posts?days=30&limit=10`
- `GET /api/analytics/platform/stats`
- `GET /api/analytics/platform/daily?days=30`
- `GET /api/analytics/platform/growth`
- `GET /api/analytics/platform/summary`

Key response DTOs:

- `UserEngagementMetrics`
- `DailyUserStats`
- `UserRetentionMetrics`
- `EventAnalytics`
- `PostPerformanceMetrics`
- `ContentTrend`
- `PlatformStats`

Many analytics DTOs serialize in `snake_case` because of `@JsonProperty`.

## 8.15 Dashboard

### GET `/dashboard`

Auth: required

Role: `ADMIN`

Response:

```json
{
  "totalUsers": 100,
  "totalEvents": 30,
  "totalRegistrations": 200,
  "paidEvents": 5,
  "publishedEvents": 18,
  "cancelledEvents": 2,
  "totalEventViews": 900,
  "averageRegistrationsPerEvent": 6.66,
  "eventEngagementRate": 22.22
}
```

## 8.16 AI

### POST `/api/ai/chat`

Auth: required

Request:

```json
{
  "conversationId": 1,
  "message": "Find upcoming events for me"
}
```

`conversationId` is optional. Omit it for a new chat.

Response:

```json
{
  "conversationId": 1,
  "answer": "Here are upcoming events I found...",
  "fallback": false,
  "createdAt": "2026-04-21T17:45:00",
  "sources": [
    {
      "type": "EVENT",
      "id": 10,
      "title": "Campus Hackathon",
      "subtitle": "SOCIETY",
      "venue": "Innovation Lab",
      "date": "2026-04-25T10:00:00",
      "registered": true
    }
  ]
}
```

Backend behavior:

- stores conversation and message history in MySQL
- uses current authenticated user role and email as prompt context
- includes upcoming published events as discovery context
- includes current user's active event registrations as context
- returns event source cards that the frontend can link to `/events/{id}`
- calls Ollama using `OLLAMA_API_URL` and `OLLAMA_MODEL`
- returns `fallback: true` with a useful local answer if Ollama is unavailable

### GET `/ai/generate?prompt=...`

Auth: required in current backend

Response: plain string

Backend behavior:

- sends prompt to local Ollama endpoint
- uses `OLLAMA_MODEL`, default `llama3`
- response body is plain text, not JSON

## 9. Rate Limiting

Current rate limit interceptor paths:

- `/auth/**`
- `/events/**`
- `/posts/**`
- `/users/**`
- `/feed/**`
- `/registrations/**`

Rules:

- `/auth/**`: 5 requests per minute per IP
- other configured paths: 100 requests per minute per IP

Important:

- `/api/posts/**` is not covered by the interceptor because the config uses `/posts/**`
- `/api/follow/**`, `/api/search/**`, `/api/analytics/**`, `/api/notifications/**`, `/dashboard`, and `/ai/**` are also not covered

When rate limited, backend returns:

- status `429`
- header `X-RateLimit-Retry-After-Seconds: 60`

## 10. Frontend State Recommendations

Recommended frontend auth store:

- `accessToken`
- `refreshToken`
- `user.id`
- `user.role`
- `user.firstName`
- `user.lastName`
- `user.profilePicture`

Recommended cache/query keys:

- `me`
- `user-profile-{id}`
- `user-stats-{id}`
- `posts-page-{page}-{size}`
- `events-page-{page}-{size}`
- `event-{id}`
- `post-{id}`
- `notifications`
- `notification-unread`
- `follow-stats-{userId}`
- `feed-{page}-{size}-{filter}-{sort}`

## 11. Known Backend Behaviors and Frontend Cautions

These are important for frontend implementation.

### 11.1 Security comments in controllers are misleading

Some controllers say an endpoint is public, but the actual security config still requires authentication.

Examples:

- `GET /api/posts`
- `GET /users/{id}/profile`
- all `/api/search/**`

Use the access rules in this document, not controller comments.

### 11.2 Search indexing is not automatically wired

The backend has Elasticsearch search services and repositories, but the indexing methods are not currently called by create/update/delete flows.

Frontend implication:

- search can be stale or empty if Elasticsearch documents were not populated separately

### 11.3 Profile follow stats are populated in `UserProfileResponse`

`UserProfileResponse` contains:

- `followersCount`
- `followingCount`
- `following`
- `followedBy`
- `mutual`

The profile service now populates those values for the current authenticated viewer.

Frontend can still use:

- `GET /api/follow/stats/{userId}`

when it needs to refresh only the follow relationship state.

### 11.4 Feed post comment counts are accurate

Home feed uses the comment service count for post `commentCount`.

Frontend implication:

- feed cards can show comment counts directly
- post detail can still be fetched when the user opens the full comment thread

### 11.5 Business errors are not always semantic HTTP errors

Examples like:

- already registered
- invalid OTP
- unauthorized owner checks in services

may come back as `500` with a readable message.

Frontend should display `message` from error body when present.

### 11.6 Event analytics and management are owner/admin protected

Even though students can create events, only:

- creator
- admin

can update, delete, change status, or read event analytics for that event.

### 11.7 File upload returns only a filename

Backend does not return a JSON object or absolute URL.

Frontend must construct the public URL itself.

### 11.8 `GET /events/{id}` mutates state

It increments `viewsCount`.

Frontend should avoid calling event detail unnecessarily.

## 12. Suggested Frontend Build Order

Recommended implementation order:

1. auth pages and token storage
2. current-user bootstrap
3. events list and event detail
4. post list and post create
5. comments and likes
6. profile pages
7. follow system
8. registrations
9. feed
10. notifications REST
11. WebSocket notifications
12. search
13. analytics
14. admin dashboard
15. AI helper

## 13. Minimum Frontend Environment Example

Suggested frontend `.env`:

```text
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_BASE_URL=http://localhost:8080/ws/notifications
VITE_UPLOADS_BASE_URL=http://localhost:8080/uploads
```

Do not put backend secrets into the frontend `.env`.
