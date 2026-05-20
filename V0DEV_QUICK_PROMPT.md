# CampusSync Frontend - Quick Prompt for v0dev

## One-Line Brief
Build a responsive React/TypeScript frontend for CampusSync, a social and event management platform for college campuses, with real-time notifications, payments, and analytics.

---

## Quick Context

### What is CampusSync?
A full-featured social networking platform for college campuses featuring:
- User authentication and profiles
- Social feed with posts, comments, likes
- Event discovery and registration
- Real-time notifications
- Payment processing (Stripe & Razorpay)
- Analytics dashboard
- Full-text search

### Tech Stack to Use
- **Framework:** React 18+ with TypeScript
- **Styling:** Tailwind CSS + shadcn/ui
- **State Management:** Redux Toolkit
- **HTTP:** Axios with interceptors
- **Real-time:** Socket.io-client
- **Forms:** React Hook Form + Zod
- **Routing:** React Router v6
- **Build:** Vite

### Backend API Location
```
Base URL: http://localhost:8080/api
```

All endpoints require JWT authentication (Bearer token in header).

---

## Phase 1: Core Build (Start Here)

### Pages to Create
1. **Login Page** - Email/password login with remember me
2. **Register Page** - Email verification flow
3. **Feed Page** - Timeline of posts with infinite scroll
4. **User Profile Page** - View/edit profile, follower info
5. **Discover Users Page** - Search and follow recommendations
6. **Header/Navigation** - Top bar with search, notifications, user menu
7. **Sidebar** - Main navigation menu

### Key Components
```
Auth:
- LoginForm component with validation
- RegisterForm component with email verification
- Protected route wrapper for authenticated pages

Feed:
- PostCard component (with like, comment buttons)
- PostCreationModal (text + image upload)
- CommentsSection component
- Feed component with infinite scroll pagination

User:
- UserProfile component (view/edit profile)
- UserCard component (follower cards)
- UserSearch component
- FollowButton component

Layout:
- Header component (search bar, notifications, user menu)
- Sidebar component (navigation)
- MainLayout wrapper component
```

### State Management (Redux)
```
Store structure:
- auth: { user, token, isAuthenticated, loading }
- feed: { posts, loading, hasMore, currentPage }
- ui: { sidebarOpen, theme, notifications }
- notifications: { list, unreadCount }
```

### Key API Endpoints to Integrate
```
Authentication:
POST   /auth/register
POST   /auth/login
POST   /auth/verify-email
POST   /auth/refresh-token

Users:
GET    /users/{userId}
PUT    /users/{userId}
GET    /users/search
GET    /users/{userId}/followers
GET    /users/{userId}/following

Feed:
POST   /posts
GET    /posts (paginated)
GET    /posts/{postId}
PUT    /posts/{postId}
DELETE /posts/{postId}
POST   /posts/{postId}/like
DELETE /posts/{postId}/like
POST   /posts/{postId}/comments
GET    /posts/{postId}/comments

Follow:
POST   /follows
DELETE /follows/{followId}
GET    /follows/recommended

Notifications:
GET    /notifications
PUT    /notifications/{id}/read
DELETE /notifications/{id}
```

---

## Phase 2: Events

### Pages
1. **Events Browser Page** - Browse all events with filters
2. **Event Details Page** - Full event info with register button
3. **Event Creation Modal** - Create new events
4. **My Events Page** - User's registered/created events

### Key API Endpoints
```
POST   /events
GET    /events (with filters/pagination)
GET    /events/{eventId}
PUT    /events/{eventId}
DELETE /events/{eventId}
POST   /events/{eventId}/register
DELETE /events/{eventId}/register
GET    /events/{eventId}/attendees
```

---

## Phase 3: Payments

### Pages
1. **Event Ticket Purchase Page** - Select quantity and payment method
2. **Payment Form** - Stripe/Razorpay checkout
3. **Order History Page** - View past orders and receipts

### Key API Endpoints
```
POST   /payments/create
GET    /orders
GET    /orders/{orderId}
POST   /orders/{orderId}/refund
GET    /receipts/{orderId}
```

---

## Phase 4: Real-time Notifications

### Features
1. Notification center dropdown
2. Notification bell with badge count
3. Toast notifications for real-time events
4. WebSocket connection for live updates

### Key Setup
```
WebSocket endpoint: ws://localhost:8080/ws/notifications

Events to listen for:
- like_post
- comment_post
- new_follower
- event_update
- payment_confirmation
```

---

## Phase 5: Analytics Dashboard

### Features
1. User engagement metrics
2. Post performance stats
3. Trending content
4. Platform statistics

### Key API Endpoints
```
GET /analytics/users/engagement
GET /analytics/posts/{postId}/performance
GET /analytics/events/trending
GET /analytics/platform/stats
```

---

## Design System

### Colors
```
Primary: #0066CC (Campus Blue)
Secondary: #00AA44 (Campus Green)
Accent: #FF9900 (Campus Orange)
Neutrals: #000, #333, #666, #999, #CCC, #EEE, #FFF
```

### Typography
- Headings: Bold, 24-32px
- Body: Regular, 14-16px
- Small: 12-13px

### Spacing
Base unit: 8px
Use multiples: 8px, 16px, 24px, 32px

### Responsive Breakpoints
- Mobile: < 640px
- Tablet: 640px - 1024px
- Desktop: > 1024px
- Max width: 1200px

---

## Project Structure

```
src/
├── components/
│   ├── auth/ (LoginForm, RegisterForm, etc.)
│   ├── feed/ (Feed, PostCard, PostCreation, etc.)
│   ├── events/ (EventBrowser, EventCard, etc.)
│   ├── user/ (UserProfile, UserCard, etc.)
│   ├── notifications/ (NotificationCenter, NotificationBell, etc.)
│   ├── payments/ (PaymentForm, OrderHistory, etc.)
│   ├── analytics/ (Dashboard, MetricCard, etc.)
│   ├── common/ (Header, Sidebar, Modal, Toast, etc.)
│   └── layout/ (MainLayout, AuthLayout, etc.)
├── pages/
│   ├── LoginPage.tsx
│   ├── FeedPage.tsx
│   ├── ProfilePage.tsx
│   ├── EventsPage.tsx
│   ├── PaymentPage.tsx
│   ├── NotificationsPage.tsx
│   └── ...
├── services/ (API calls using Axios)
│   ├── api.ts (Axios instance with interceptors)
│   ├── auth.service.ts
│   ├── user.service.ts
│   ├── post.service.ts
│   ├── event.service.ts
│   ├── payment.service.ts
│   └── ...
├── redux/
│   ├── store.ts
│   ├── slices/ (auth, feed, events, notifications, etc.)
│   └── hooks.ts
├── hooks/ (useWebSocket, useAuth, useInfiniteScroll, etc.)
├── types/ (TypeScript interfaces)
├── utils/ (validators, formatters, helpers)
├── styles/ (globals.css, Tailwind config)
└── App.tsx
```

---

## Key Implementation Details

### Authentication Flow
1. User registers/logs in
2. Backend returns JWT token
3. Store token in localStorage
4. Configure Axios to attach token to all requests
5. On 401, attempt refresh or redirect to login

### API Service Pattern
```typescript
// Example: api/post.service.ts
import api from './api';

export const postService = {
  createPost: (data) => api.post('/posts', data),
  getPosts: (page) => api.get('/posts', { params: { page } }),
  likePost: (postId) => api.post(`/posts/${postId}/like`),
  unlikePost: (postId) => api.delete(`/posts/${postId}/like`),
};
```

### Redux Slice Pattern
```typescript
// Example: redux/slices/feed.slice.ts
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { postService } from '../../services/post.service';

export const fetchPosts = createAsyncThunk('feed/fetchPosts', 
  (page) => postService.getPosts(page)
);

const feedSlice = createSlice({
  name: 'feed',
  initialState: { posts: [], loading: false },
  extraReducers: (builder) => {
    builder.addCase(fetchPosts.fulfilled, (state, action) => {
      state.posts = action.payload;
      state.loading = false;
    });
  },
});

export default feedSlice.reducer;
```

### Component Pattern
```typescript
// Example: components/feed/PostCard.tsx
import React from 'react';
import { useDispatch } from 'react-redux';
import { likePost } from '../../redux/slices/feed.slice';

interface PostCardProps {
  post: Post;
  onCommentClick: (postId: string) => void;
}

const PostCard: React.FC<PostCardProps> = ({ post, onCommentClick }) => {
  const dispatch = useDispatch();
  
  return (
    <div className="p-4 border rounded-lg">
      {/* Component JSX */}
    </div>
  );
};

export default PostCard;
```

---

## Testing

### Unit Tests (Critical paths only)
- Login/register flows
- Redux reducers
- Utility functions

### Integration Tests
- API services
- Form validation
- WebSocket connection

### E2E Tests
- Complete user registration
- Post creation and interaction
- Event registration
- Payment flow

---

## Performance Tips

1. Lazy load images
2. Code-split by route
3. Virtual scroll long lists
4. Debounce search input
5. Use React Query for server state (optional)
6. Memoize expensive components
7. Keep bundle < 300KB

---

## Start Building!

### First Steps
1. Create React app with Vite + TypeScript
2. Install dependencies (React Router, Redux Toolkit, Axios, Tailwind, Socket.io)
3. Setup Tailwind CSS
4. Create folder structure
5. Build authentication pages and services
6. Build main layout (Header + Sidebar)
7. Build feed page with post creation
8. Add Redux state management
9. Add real-time WebSocket notifications
10. Build remaining features in order

### Environment Setup
```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080
STRIPE_PUBLIC_KEY=pk_test_xxx
RAZORPAY_KEY_ID=xxx
```

---

## Backend API Documentation

Full API documentation available at: `http://localhost:8080/api/docs`

All responses are JSON. Auth required on all endpoints except `/auth/*`.

**Error Handling:**
- 400: Bad Request (validation errors)
- 401: Unauthorized (invalid/expired token)
- 403: Forbidden (no permission)
- 404: Not Found
- 500: Server Error

---

## Success Checklist

- [ ] Authentication (register, login, email verification)
- [ ] User profiles (view, edit, search)
- [ ] Feed (create post, like, comment, scroll)
- [ ] Real-time notifications
- [ ] Events (browse, create, register)
- [ ] Payments (checkout, order history)
- [ ] Analytics dashboard
- [ ] Search functionality
- [ ] Responsive design
- [ ] Error handling
- [ ] Loading states
- [ ] Tests written

---

## Questions?

Refer to the full backend API specification in `V0DEV_FRONTEND_PROMPT.md` for detailed API documentation, design specifications, and architecture decisions.

Good luck building CampusSync! 🚀
