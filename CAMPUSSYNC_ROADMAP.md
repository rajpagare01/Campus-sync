# CampusSync Feature Roadmap

Updated: 20 April 2026

This roadmap reflects the current backend state and the intended product direction.

## Project Vision

CampusSync is a campus social and event platform where:

- students interact socially through posts, likes, comments, follows, and event participation
- societies and departments publish updates, posts, and events
- events can be discovered directly from event pages, feed cards, and linked posts
- registrations, engagement, notifications, search, and analytics create a complete campus activity loop
- an AI chatbot helps users discover events, understand platform features, and get campus assistance

## Role Model

### Student

Target capabilities:

- view feed
- create posts
- create events if allowed by product policy
- like and comment on posts
- register for events
- cancel own registrations
- follow users
- manage profile
- receive notifications and registration emails
- use AI chatbot

Current backend status:

- mostly built

### Department

Target capabilities:

- post announcements
- create official events
- view participants
- view analytics for owned events
- engage with students
- use AI chatbot

Current backend status:

- partially built

### Society

Target capabilities:

- create events
- post updates
- engage with students
- track registrations
- view analytics for owned events
- use AI chatbot

Current backend status:

- partially built

### Admin

Target capabilities:

- manage users
- moderate content
- view platform analytics
- control roles and permissions
- manage reported content
- configure platform-level settings

Current backend status:

- partially built

Admin analytics and dashboard exist. User management, moderation, reports, and role-management APIs still need to be built.

## Feature Status Summary

| Feature Area | Status | Notes |
|---|---:|---|
| Authentication | Fully built | Register, verify OTP, login, refresh token, password reset, JWT, roles |
| User profiles | Mostly built | View/edit/activity/stats exist; follow stats are now populated for the current viewer |
| Posts | Mostly built | Create/read/update/delete, media URL, linked event support; share/report missing |
| Comments | Mostly built | Threaded comments/replies, edit/delete; reply notifications missing |
| Likes | Fully built | Toggle like, counts, user likes, like notifications |
| Events | Mostly built | Create/read/update/delete/status/search/analytics; no attendance check-in |
| Post-event integration | Mostly built | Posts can link to events; frontend event-card rendering still needed |
| Registrations | Fully built | Register, cancel, list, participants, duplicate prevention, confirmation email |
| Follow system | Mostly built | Follow/unfollow/stats/lists/recommendations; not used for personalized feed yet |
| Feed | Partially built | Mixed post/event feed exists with accurate post comment counts; not truly personalized |
| Notifications | Partially built | REST + WebSocket, like/comment/follow/event-update triggers; registration/reply notifications missing |
| Search | Partially built | Elasticsearch endpoints exist; indexing is not automatically wired |
| Analytics | Mostly built | User/event/post/platform analytics exist; needs refinement and frontend dashboards |
| Admin dashboard | Partially built | Metrics exist; admin management/moderation missing |
| File upload | Fully built | Images/videos up to 50 MB, local storage, static `/uploads/**` serving |
| Email system | Mostly built | OTP, password reset, registration confirmation; templating/HTML missing |
| AI chatbot | MVP built | Authenticated chat endpoint, stored conversations/messages, event and registration context, role-aware prompts, fallback handling, frontend drawer |
| Payments | Not built | Paid flag exists on events; payment gateway not implemented |
| Real-time engagement | Partially built | Notification WebSocket exists; live likes/comments/events not built |
| Testing | Partially built | Unit/security/config tests exist; full integration coverage missing |
| Performance | Partially built | Some pagination/caching/rate limiting; several gaps remain |

## Fully Built Or Production-Usable Backend Features

These features are already implemented enough for frontend integration, with normal UI/error-handling work still required.

### Authentication And Security

Implemented:

- email/password registration
- email OTP verification using Redis
- login with JWT access token
- refresh-token flow using Redis
- password reset with OTP
- BCrypt password encryption
- role-based access control
- JWT filter that maps `role` claim to `ROLE_*`
- validation on auth DTOs

Known limitations:

- logout does not revoke tokens; frontend clears session locally
- business-rule errors sometimes return `500` instead of semantic `400`

### Event Registration

Implemented:

- students register for events
- duplicate registration prevention
- cancel registration
- user registration list
- event participant list for society/department/admin
- confirmation email after successful registration

Confirmation email includes:

- registration ID
- event title
- event date/time
- venue
- registration status

### Likes

Implemented:

- toggle like/unlike
- post likes list
- current user liked posts
- like counts
- like notification to post owner

### File Uploads

Implemented:

- post media upload
- event image upload
- image/video validation
- 50 MB max file size
- local storage under `uploads/`
- static access through `/uploads/{fileName}`

## Mostly Built Features

These features are usable but still have important gaps.

### Posts

Built:

- authenticated post creation
- students, societies, departments, and admins can create posts
- paginated post list
- author post list
- media post list
- event-linked post list
- post delete by owner/admin
- event linking through `eventId`

Missing:

- post report/moderation
- post share feature
- hashtag extraction/indexing
- automatic Elasticsearch indexing
- frontend event-card rendering inside posts

### Comments

Built:

- add comment
- threaded comments
- add reply
- get replies
- edit own comment
- delete own comment or admin delete
- block deleting comments that still have replies
- comment notification for post owner

Missing:

- reply notification
- comment likes
- moderation/report flow
- soft-delete mode for comments with replies

### Events

Built:

- create event
- list published events
- view event detail
- update event by creator/admin
- delete event by creator/admin
- change event status by creator/admin
- search/filter events
- get events by creator
- event analytics for creator/admin
- event view count
- paid/free fields

Missing:

- attendance/check-in system
- capacity limits
- event waitlist
- event reminders
- payment integration for paid events
- richer event DTOs to avoid returning raw entity graphs

### Follow System

Built:

- follow user
- unfollow user
- follow stats
- followers/following lists
- recommended users
- mutual follows
- follow notification

Missing:

- personalized feed based on followed users
- privacy controls
- block/mute users
- personalized feed based on follow graph

### Analytics

Built:

- user engagement metrics
- daily stats
- retention metrics
- event analytics
- trending events
- post performance
- content trends
- platform stats
- platform summary
- admin dashboard metrics

Missing:

- frontend dashboards
- stronger metric definitions
- export reports
- role-specific analytics views
- analytics caching strategy
- permission checks for `/api/analytics/**`

## Partially Built Features

These features exist, but should not be considered complete.

### User Profiles

Built:

- current user profile
- public profile lookup
- update profile
- update profile picture URL
- user activity feed
- user stats summary

Missing:

- profile picture upload endpoint directly tied to profile update
- profile privacy settings
- public profile routes still require auth because of security config

### Feed System

Built:

- combined post/event feed
- filter by all/posts/events
- sort by date or engagement
- feed stats
- manual pagination

Missing:

- true personalized feed
- cursor pagination/infinite scroll API
- followed-user prioritization
- trending/ranking logic
- feed item DTO cleanup

### Notifications

Built:

- REST notification list
- unread count
- mark one read
- mark all read
- delete notification
- WebSocket endpoint
- user queue and topic broadcast
- like/comment/follow notification triggers

Missing:

- registration confirmation notification
- reply notification
- admin notification preferences
- push notification integration
- read receipts over WebSocket

### Search

Built:

- search posts
- search events
- search users
- search comments
- filtered search routes
- Elasticsearch repositories/documents

Missing:

- automatic indexing from create/update/delete flows
- reindex endpoint/job
- relevance tuning
- typo tolerance
- frontend search experience
- fallback database search if Elasticsearch is down

### AI Chatbot

Built:

- `GET /ai/generate?prompt=...`
- `POST /api/ai/chat`
- calls local Ollama `/api/generate`
- stores conversations and messages
- builds context from current user role, upcoming events, and active registrations
- returns source cards for event links
- handles unavailable Ollama with local fallback answers
- frontend navbar chat drawer with quick prompts, retry, and event source cards

Missing:

- conversation history management screen
- delete conversation endpoint
- regenerate answer endpoint
- optional streaming responses
- rate limiting for `/api/ai/**`
- deeper post/search context

## Not Built Yet

### Payment System

Target:

- paid event registration
- Razorpay or Stripe checkout
- order creation
- payment webhook verification
- receipt generation
- refunds
- payment status in registration

Current:

- event has `paid` and `price` fields only
- no payment/order/refund APIs exist

### Admin Moderation

Target:

- manage users
- change user roles
- deactivate users
- moderate posts/comments/events
- review reports
- remove content
- audit logs UI

Current:

- admin dashboard metrics exist
- full moderation system is not built

### Advanced Real-Time Features

Target:

- live likes
- live comments
- live feed updates
- live event updates
- online presence

Current:

- only notification WebSocket is built

## AI Chatbot Plan

The chatbot should become a first-class feature, not just a wrapper around `/ai/generate`.

### Product Goal

The CampusSync chatbot should help users:

- find relevant events
- ask what is happening on campus
- understand how to register for events
- get recommendations based on role and interests
- summarize event details
- help societies/departments draft posts and event descriptions
- help admins understand platform activity

### Chatbot User Modes

Student chatbot:

- recommend upcoming events
- answer registration questions
- explain event venue/date/price
- summarize feed activity
- suggest users or societies to follow
- help write posts

Society/department chatbot:

- draft event announcements
- generate post captions
- summarize participant interest
- suggest event promotion copy
- answer event analytics questions

Admin chatbot:

- summarize platform health
- summarize trending events/posts
- identify unusual activity patterns
- answer analytics questions

### Chatbot Backend MVP

Build these first:

- `POST /api/ai/chat`
- request body: `message`, optional `conversationId`
- response body: `conversationId`, `message`, `sources`, `createdAt`
- authenticated access
- use current user role/email as context
- store chat messages in database
- keep Ollama as the first LLM provider
- move Ollama URL/model out of hardcoded service into properties

Suggested DTOs:

```json
{
  "conversationId": "uuid-or-null",
  "message": "Find upcoming placement events"
}
```

```json
{
  "conversationId": "uuid",
  "message": "There are 2 upcoming placement events...",
  "sources": [
    {
      "type": "EVENT",
      "id": 12,
      "title": "Placement Prep Workshop"
    }
  ],
  "createdAt": "2026-04-20T12:00:00"
}
```

### Chatbot Backend Phase 2

Add tool-like context retrieval:

- retrieve upcoming events from `EventRepository`
- retrieve posts from `PostRepository`
- retrieve user registrations from `RegistrationRepository`
- retrieve analytics for admin users
- retrieve Elasticsearch search results when available

The chatbot should not hallucinate campus data. It should answer from backend data and include source references when possible.

### Chatbot Backend Phase 3

Add advanced behavior:

- conversation history screen
- delete conversation
- regenerate answer
- rate answer helpful/not helpful
- admin prompt templates
- safety filters
- rate limiting for `/api/ai/**`
- optional streaming responses

### Chatbot Frontend MVP

Build:

- floating chat button
- chat drawer
- message list
- input box
- loading state
- error state
- event/source cards in answers
- quick prompts:
  - "Show upcoming events"
  - "What can I register for?"
  - "Help me write a post"
  - "Summarize my registrations"

### Chatbot Acceptance Criteria

MVP is complete when:

- authenticated users can open chatbot from any page
- user sends a message and receives AI response
- conversation ID persists through the session
- chatbot can answer at least event discovery questions from live backend data
- chatbot handles LLM downtime gracefully
- chatbot does not expose secrets or raw internal errors

## Recommended Build Roadmap

## Phase 1: Stabilize Current Core

Goal:

- make existing backend reliable for frontend development

Tasks:

- normalize error responses and status codes
- fix security route mismatch for profile/public endpoints if desired
- finish automatic search indexing or mark search as manual
- done: wire event update notifications
- done: fix feed comment count
- done: populate follow stats in profile response
- done: add post update endpoint

## Phase 2: Frontend MVP

Goal:

- build usable student/social/event experience

Frontend tasks:

- auth screens
- OTP verification
- current user bootstrap
- event list/detail
- post list/create
- media upload
- comments/replies
- likes
- registration and cancellation
- profile edit
- follow/unfollow
- notification center

Completed Phase 2 slice:

- feed event cards link directly to `/events/{eventId}`
- event detail registration state can match by `eventId`
- user registration responses include `eventId`
- post text edits no longer clear existing media when `mediaUrl` is omitted

## Phase 3: AI Chatbot MVP

Goal:

- convert simple AI generation into real CampusSync chatbot

Backend tasks:

- done: add chat request/response DTOs
- done: add conversation/message entities
- done: add `POST /api/ai/chat`
- done: add event discovery context
- done: add user registration context
- done: add role-aware prompt builder
- done: add tests for chat service

Frontend tasks:

- done: chat drawer
- done: chat messages
- done: event source cards
- done: quick prompts
- done: retry/error handling

Remaining enhancements:

- conversation history screen
- delete conversation endpoint
- regenerate answer endpoint
- streaming response support
- rate limiting for `/api/ai/**`

## Phase 4: Search And Discovery (✅ COMPLETED)

Goal:

- make discovery reliable and useful

Tasks:

- ✅ ~~automatic Elasticsearch indexing on post/event/user/comment changes~~ (OBSOLETE: Removed Elasticsearch dependency entirely in favor of native JPA search for architectural simplicity)
- ✅ ~~reindex admin endpoint~~ (OBSOLETE: Native JPA is always up-to-date)
- ✅ search UI with tabs (Frontend Task)
- ✅ suggested users UI (Backend `GET /api/follow/recommendations` endpoint implemented to prioritize high-follower users not yet followed)
- ✅ trending posts/events (Backend `GET /feed/trending` endpoint implemented with engagement scoring)
- ✅ personalized feed from follows (Backend `FeedService` updated to heavily boost content authored by followed users)

## Phase 5: Admin And Moderation

Goal:

- give admin real platform control

Tasks:

- user management
- role changes
- deactivate users
- report post/comment/event
- moderation queue
- content removal
- audit log UI

## Phase 6: Payments

Goal:

- support paid event registrations

Tasks:

- payment provider selection
- order entity
- payment transaction entity
- create checkout endpoint
- webhook verification
- payment status in registration
- receipt email
- refund workflow

## Phase 7: Advanced Real-Time

Goal:

- make CampusSync feel live

Tasks:

- live post likes
- live comments
- live event updates
- feed refresh events
- online presence
- push notifications

## Current MVP Definition

Backend MVP is complete when:

- users can register/login/verify
- students can create posts and events
- users can view feed
- users can like/comment/follow
- students can register for events and receive email confirmation
- organizers can view participants
- users receive notifications
- profiles are usable
- uploads work

Frontend MVP is complete when:

- all above flows are usable from UI
- auth/session refresh works
- role-based buttons are hidden/shown correctly
- errors are displayed clearly
- mobile layout works

AI Chatbot MVP is complete when:

- chatbot has real chat UI
- backend stores conversations
- chatbot can answer event discovery and registration questions using backend data
- chatbot can help draft posts/events
- chatbot handles unavailable Ollama cleanly
