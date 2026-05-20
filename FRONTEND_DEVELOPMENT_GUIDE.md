# Frontend Development Guide - CampusSync

This guide matches the current backend implementation.

For exact API contracts, request payloads, response shapes, route permissions, enums, and backend quirks, use:

- [FRONTEND_BACKEND_INTEGRATION_GUIDE.md](</C:/Users/asus/Downloads/backend/backend/FRONTEND_BACKEND_INTEGRATION_GUIDE.md>)

This file is the frontend build guide.
The integration guide is the API contract guide.

## 1. Source Of Truth

Use these files in this order:

1. `FRONTEND_BACKEND_INTEGRATION_GUIDE.md`
2. `FRONTEND_DEVELOPMENT_GUIDE.md`
3. `V0DEV_FRONTEND_PROMPT.md`
4. `V0DEV_QUICK_PROMPT.md`

Important:

- older prompt files are still useful for layout generation and UI scaffolding
- they contain outdated backend assumptions
- whenever a prompt conflicts with the integration guide, the integration guide wins

## 2. What The Backend Actually Supports

Frontend should be built around these real backend capabilities:

- OTP registration and JWT login
- refresh-token session flow
- user profiles and activity history
- posts
- comments and replies
- likes
- follow system
- events
- event registrations
- personalized feed
- notifications via REST and WebSocket
- Elasticsearch-based search
- analytics and admin dashboard
- local file uploads
- AI chatbot through `POST /api/ai/chat`, backed by Ollama with local fallback behavior

Frontend should not assume these features exist right now:

- payment gateway flows
- order history
- receipts
- refunds
- cookie-based auth
- Socket.IO

## 3. Non-Negotiable Backend Reality

These points must shape the frontend:

- backend base URL is `http://localhost:8080`
- authenticated requests use `Authorization: Bearer <accessToken>`
- WebSocket uses STOMP over SockJS, not Socket.IO
- uploads return a filename only, not a full URL
- many business-rule failures come back as `500` with a readable `message`
- some controller comments say "public" even when backend security still requires auth
- `POST /auth/logout` only returns `"Logged out"` and does not revoke tokens
- students can create posts and events
- event update, delete, status change, and analytics are owner/admin only
- `GET /events/{id}` increments event views
- search indexing is not automatically wired into all create/update/delete flows

## 4. Development Environment

### Backend assumptions

Before starting frontend work, make sure:

- backend runs on `http://localhost:8080`
- MySQL is connected
- Redis is connected
- Elasticsearch is running if search is needed

### Frontend env

Recommended frontend `.env`:

```text
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_BASE_URL=http://localhost:8080/ws/notifications
VITE_UPLOADS_BASE_URL=http://localhost:8080/uploads
```

Do not place backend secrets in the frontend environment.

### Dev server port

Current backend CORS allows:

- `http://localhost:3000`
- `http://localhost:3001`
- `http://127.0.0.1:3000`
- `http://127.0.0.1:3001`

If you use Vite, run it on `3000` or `3001`.

Do not assume default Vite port `5173` will work unless backend CORS is changed.

## 5. Recommended Frontend Stack

Pragmatic stack for this backend:

- React 18+
- TypeScript
- Vite
- React Router
- Axios
- TanStack Query for server state
- React Hook Form + Zod
- Tailwind CSS
- `@stomp/stompjs` + `sockjs-client` for notifications

Optional global client state:

- Redux Toolkit if you want a structured global store
- or Zustand for lighter auth/UI state

Recommendation:

- use TanStack Query for API data
- use Redux Toolkit or Zustand only for auth/session/UI state

## 6. Suggested Project Structure

```text
src/
  app/
    router/
    providers/
    store/
  lib/
    api/
    auth/
    websocket/
    utils/
  features/
    auth/
    users/
    posts/
    comments/
    likes/
    follow/
    events/
    registrations/
    feed/
    notifications/
    search/
    analytics/
    dashboard/
    ai/
  components/
    layout/
    ui/
    forms/
  hooks/
  types/
  pages/
```

## 7. Core Frontend Modules To Create First

### `lib/api/client.ts`

Responsibilities:

- create Axios instance
- set base URL from `VITE_API_BASE_URL`
- attach access token automatically
- centralize error normalization
- optionally intercept `401` and try refresh flow

### `lib/auth/session.ts`

Responsibilities:

- store `accessToken`
- store `refreshToken`
- store minimal user info
- clear session on logout

Store at minimum:

- `accessToken`
- `refreshToken`
- `user.id`
- `user.role`
- `user.firstName`
- `user.lastName`
- `user.profilePicture`

### `lib/websocket/notifications.ts`

Responsibilities:

- connect to `/ws/notifications`
- subscribe to `/user/queue/notifications`
- also support `/topic/notifications/{userId}` fallback
- send `/app/notifications/subscribe`
- send `/app/notifications/ack`

### `lib/utils/uploads.ts`

Responsibilities:

- convert returned filename into public URL

Example:

```ts
export function buildUploadUrl(fileName: string) {
  return `${import.meta.env.VITE_UPLOADS_BASE_URL}/${fileName}`;
}
```

## 8. Build Order

Use this order. It matches the backend's actual readiness.

1. auth pages and token handling
2. current-user bootstrap
3. public event listing and event detail
4. post list and create post
5. comments and likes
6. user profiles and follow stats
7. registrations
8. feed
9. notifications REST
10. notifications WebSocket
11. search
12. analytics
13. admin dashboard
14. AI helper

## 9. Feature Implementation Notes

## 9.1 Auth

Implement these frontend flows:

- register
- verify OTP
- login
- refresh token
- forgot password
- reset password

Important backend behavior:

- register is OTP-first, not instant account creation
- verify returns full auth session
- refresh token returns a new access token and a new refresh token
- logout is frontend-local cleanup

## 9.2 Profiles

Profile endpoints return activity counts and current-viewer follow fields.

Frontend rule:

- use `/users/{id}/profile` for profile content
- use `/api/follow/stats/{id}` only when you need to refresh follow state without reloading the full profile

## 9.3 Posts

Posts are under `/api/posts`, not `/posts`.

Important:

- create post requires auth
- student can create post
- update post uses `PUT /api/posts/{id}` and is author/admin only
- delete post is author/admin only

## 9.4 Comments And Likes

Comments and likes use `/posts/**` routes.

Frontend should not assume route consistency across modules.

Examples:

- posts listing: `/api/posts`
- post update: `PUT /api/posts/{id}`
- comments: `/posts/{postId}/comments`
- likes: `/posts/{postId}/like`

## 9.5 Events

Frontend should treat event reads and writes differently:

- listing and detail are public
- create requires auth
- student can create event
- update/delete/status/analytics are creator/admin only

Avoid unnecessary repeated event-detail calls because detail increments views.

## 9.6 Registrations

Important backend quirk:

- `GET /registrations/user/{userId}` ignores the path `userId`
- backend always returns registrations for the logged-in user

Frontend should call it with the current user id only for URL consistency, not because backend needs it.

Registration list items include `eventId`; use it to match event detail registration state instead of matching by title.

## 9.7 Feed

Feed returns mixed `POST` and `EVENT` items.

Important:

- feed post `commentCount` is populated from the comment service
- event feed cards should link to `/events/{id}` for direct registration

## 9.8 AI Chatbot

Use `POST /api/ai/chat` for the app chatbot.

Frontend behavior:

- keep `conversationId` from the first response and send it with later messages
- render `sources` as event cards linking to `/events/{id}`
- show a small fallback badge when `fallback` is `true`
- provide quick prompts for event discovery, registrations, and role-specific help
- keep the old `GET /ai/generate` endpoint out of the main UI

## 9.9 Search

Search endpoints return Elasticsearch document DTOs and Spring `Page` JSON, not the app's custom `PaginatedResponse`.

Frontend should implement a separate parser for:

- Spring `Page`
- custom `PaginatedResponse`

Do not use one shared pagination type for every endpoint.

## 9.10 Notifications

Frontend should support both:

- REST fetch for history and unread state
- WebSocket for real-time push

Recommended notification lifecycle:

1. fetch initial unread count on app load
2. connect WebSocket after auth bootstrap
3. prepend new notifications to local state
4. mark read using REST or WS ack

## 9.11 Files

File upload is a two-step frontend flow:

1. upload file to `/files/.../upload`
2. get filename string
3. build public URL with `/uploads/{fileName}`
4. store that URL or filename in the next API payload

## 9.11 Analytics

Analytics endpoints are read-only and authenticated.

Notes:

- many analytics DTOs use `snake_case`
- analytics responses are not consistent with the rest of the app's DTO naming style
- create dedicated TypeScript types for analytics

## 10. Error Handling Strategy

Frontend should normalize errors like this:

- prefer `response.data.message` when available
- if validation errors exist, surface field-level messages
- treat `403` as permission issue
- treat `401` as expired/missing auth
- treat `500` with readable business messages as valid user-facing errors

Examples of user-facing messages worth showing directly:

- `Invalid OTP`
- `Already registered for this event`
- `Unauthorized action`
- `Please wait before requesting another OTP`

## 11. Query Keys And Caching

Recommended query keys:

- `me`
- `user-profile-{id}`
- `user-activity-{id}`
- `user-stats-{id}`
- `follow-stats-{id}`
- `posts-page-{page}-{size}`
- `post-{id}`
- `events-page-{page}-{size}`
- `event-{id}`
- `feed-{page}-{size}-{filter}-{sort}`
- `notifications`
- `notification-unread`
- `registrations-my`
- `event-participants-{eventId}`
- `search-posts-{query}-{page}-{size}`
- `search-events-{query}-{page}-{size}`
- `search-users-{query}-{page}-{size}`

Invalidate after:

- login/logout/refresh: `me`, notifications, profile caches
- follow/unfollow: `follow-stats-*`, profile lists
- like/comment/post create: post detail, post lists, feed
- event update/status: event detail, event lists, analytics
- registration create/cancel: event detail, my registrations, event analytics

## 12. Frontend Checklist

Before calling the frontend "ready", verify:

- app runs on port `3000` or `3001`
- all authenticated requests send Bearer token
- refresh flow works
- logout clears local session
- uploads render correctly from `/uploads/{fileName}`
- post create works for `STUDENT`
- event create works for `STUDENT`
- registration create is visible only to `STUDENT`
- event management actions are hidden for non-owner non-admin users
- WebSocket notifications arrive live
- search pages handle Spring `Page` response shape
- analytics pages handle `snake_case` DTOs

## 13. How To Use v0dev Or AI Generators

If you use:

- `V0DEV_FRONTEND_PROMPT.md`
- `V0DEV_QUICK_PROMPT.md`
- Copilot
- ChatGPT
- Claude

always include this instruction:

```text
Use FRONTEND_BACKEND_INTEGRATION_GUIDE.md as the API contract source of truth.
Ignore any older assumptions about payments, Socket.IO, verify-email routes, or public post/profile routes.
```

That one line will prevent most incorrect generations.

## 14. Useful References

- [FRONTEND_BACKEND_INTEGRATION_GUIDE.md](</C:/Users/asus/Downloads/backend/backend/FRONTEND_BACKEND_INTEGRATION_GUIDE.md>)
- [V0DEV_FRONTEND_PROMPT.md](</C:/Users/asus/Downloads/backend/backend/V0DEV_FRONTEND_PROMPT.md>)
- [V0DEV_QUICK_PROMPT.md](</C:/Users/asus/Downloads/backend/backend/V0DEV_QUICK_PROMPT.md>)

If Springdoc is running, also check:

- `http://localhost:8080/swagger-ui/index.html`

## 15. Final Recommendation

Do not start by generating the whole frontend at once.

Build in slices:

1. auth
2. events
3. posts
4. comments and likes
5. profiles and follow
6. registrations
7. notifications
8. search
9. analytics

That order matches the backend and reduces rework.
