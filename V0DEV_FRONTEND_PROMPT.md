# CampusSync Frontend - Comprehensive v0dev Prompt

## Project Overview

You are building a modern, full-featured social networking platform for college campuses called **CampusSync**. This is a comprehensive social and event management platform with real-time features, analytics, and payment processing.

**Project Goal:** Create a responsive, production-ready frontend that connects to a fully functional backend API serving 85% of the platform's core features.

**Timeline:** Build incrementally, starting with authentication and core features, then adding advanced features.

---

## Technology Stack (Recommended)

- **Framework:** React 18+ with TypeScript
- **State Management:** Redux Toolkit or Zustand (for simplicity)
- **HTTP Client:** Axios with interceptors for auth
- **Real-time:** Socket.io-client for WebSocket notifications
- **UI Framework:** Tailwind CSS with shadcn/ui components
- **Forms:** React Hook Form + Zod validation
- **Routing:** React Router v6
- **Build Tool:** Vite (for speed and modern tooling)
- **Testing:** Vitest + React Testing Library
- **E2E Testing:** Cypress or Playwright
- **Payment Integration:** Stripe.js and Razorpay SDK
- **PDF Generation:** React-to-PDF or similar

---

## Backend API Information

### Base URL
```
http://localhost:8080/api
```

### Authentication
- **Method:** JWT Tokens (Bearer token in Authorization header)
- **Flow:** Login → Get token → Store in localStorage/secure cookie → Attach to all requests
- **Token Refresh:** Implement token refresh on 401 responses

### Core API Endpoints

#### Authentication
```
POST   /auth/register         - User registration
POST   /auth/login            - User login
POST   /auth/logout           - User logout
POST   /auth/refresh-token    - Refresh JWT token
POST   /auth/verify-email     - Verify email with code
```

#### User Management
```
GET    /users/{userId}        - Get user profile
PUT    /users/{userId}        - Update user profile
GET    /users/search          - Search users
GET    /users/{userId}/followers - Get user followers
GET    /users/{userId}/following - Get user following
```

#### Posts & Social
```
POST   /posts                 - Create post
GET    /posts                 - Get feed (paginated)
GET    /posts/{postId}        - Get single post
PUT    /posts/{postId}        - Edit post
DELETE /posts/{postId}        - Delete post
POST   /posts/{postId}/like   - Like post
DELETE /posts/{postId}/like   - Unlike post
POST   /posts/{postId}/comments - Create comment
GET    /posts/{postId}/comments - Get comments
```

#### Follow System
```
POST   /follows               - Follow user
DELETE /follows/{followId}    - Unfollow user
GET    /users/{userId}/followers - Get followers
GET    /users/{userId}/following - Get following
GET    /follows/recommended   - Get recommended users
```

#### Events
```
POST   /events                - Create event
GET    /events                - List events (with filters)
GET    /events/{eventId}      - Get event details
PUT    /events/{eventId}      - Update event
DELETE /events/{eventId}      - Delete event
POST   /events/{eventId}/register - Register for event
DELETE /events/{eventId}/register - Cancel registration
GET    /events/{eventId}/attendees - Get attendees
```

#### Notifications (WebSocket + REST)
```
GET    /notifications         - Get notifications (paginated)
PUT    /notifications/{id}/read - Mark as read
DELETE /notifications/{id}    - Delete notification
WS     /ws/notifications      - WebSocket endpoint for real-time updates
```

#### Search (Elasticsearch)
```
GET    /search/posts          - Search posts (full-text)
GET    /search/users          - Search users
GET    /search/events         - Search events
GET    /search/hashtags       - Search hashtags
```

#### Analytics
```
GET    /analytics/users/engagement - User engagement metrics
GET    /analytics/posts/{postId}/performance - Post performance
GET    /analytics/events/trending - Trending events
GET    /analytics/platform/stats - Platform statistics
```

#### Payments
```
POST   /payments/create       - Initiate payment
POST   /payments/webhook/stripe - Stripe webhook
POST   /payments/webhook/razorpay - Razorpay webhook
GET    /orders                - Get user's orders
GET    /orders/{orderId}      - Get order details
POST   /orders/{orderId}/refund - Request refund
GET    /receipts/{orderId}    - Download receipt PDF
```

---

## Frontend Feature Breakdown

### Phase 1: Core Authentication & User Management (Week 1)
**Priority: CRITICAL**

#### Features:
1. **Login/Register Page**
   - Email/password registration
   - Email verification flow
   - Login with "Remember me"
   - Password reset link
   - Form validation (Zod schema)
   - Error messaging

2. **User Profile Pages**
   - View own profile
   - Edit profile (name, bio, profile picture, cover image)
   - View other users' profiles
   - Profile picture upload with preview
   - Display follower/following counts

3. **User Discovery**
   - Search users functionality
   - User cards with profile info
   - Follow/Unfollow buttons
   - Recommended users section

#### Components:
- LoginForm
- RegisterForm
- VerifyEmailModal
- UserProfile
- UserCard
- UserSearchInput
- FollowButton

### Phase 2: Social Features (Week 2)
**Priority: CRITICAL**

#### Features:
1. **Feed & Timeline**
   - Display posts in chronological order
   - Infinite scroll pagination
   - Real-time post updates (WebSocket)
   - Filter by followed users

2. **Post Creation**
   - Text post creation
   - Image upload support
   - Hashtag support
   - Post preview
   - Character counter
   - Error handling

3. **Post Interactions**
   - Like/Unlike posts
   - Comment on posts
   - Comment pagination
   - Delete own comments
   - Real-time like count updates

4. **Comments Section**
   - Nested comments display (optional for Phase 1)
   - Comment editing
   - Comment deletion
   - Reply to comments

#### Components:
- Feed
- PostCard
- PostCreationModal
- PostComments
- CommentForm
- CommentItem
- LikeButton

### Phase 3: Event Management (Week 3)
**Priority: HIGH**

#### Features:
1. **Event Discovery**
   - Browse events
   - Filter events (category, date, location)
   - Search events
   - Event cards with key info

2. **Event Details**
   - Full event details page
   - Event description
   - Attendee list
   - Event location/date/time
   - Registration button
   - Share event button

3. **Event Creation**
   - Create event form
   - Upload event banner/image
   - Set event details (date, time, location, capacity)
   - Add event description
   - Categorize event
   - Event preview

4. **Event Registration**
   - Register for event
   - View registered events
   - Cancel registration
   - Attendee list for event creators

#### Components:
- EventBrowser
- EventCard
- EventDetailsPage
- EventCreationModal
- EventFilter
- AttendeesList
- RegisterButton

### Phase 4: Notifications & Real-time (Week 4)
**Priority: HIGH**

#### Features:
1. **Notification Center**
   - Display notifications list
   - Mark as read
   - Delete notifications
   - Filter notifications by type (like, comment, follow, event update)

2. **Real-time Updates**
   - WebSocket connection management
   - Real-time notification bell badge
   - Toast notifications for important events
   - Desktop notifications (optional)

3. **Notification Types**
   - When someone likes your post
   - When someone comments on your post
   - When someone follows you
   - Event registration confirmation
   - Event updates (postponed, cancelled, etc.)
   - Payment confirmation

#### Components:
- NotificationCenter
- NotificationBell
- NotificationItem
- NotificationToast
- WebSocket hooks

### Phase 5: Payment Integration (Week 5)
**Priority: MEDIUM**

#### Features:
1. **Payment Flow**
   - Event ticket purchase page
   - Stripe payment form
   - Razorpay payment form
   - Payment method selection
   - Quantity selector

2. **Order Management**
   - View order history
   - Download receipt/invoice
   - Request refund
   - Refund status tracking

3. **Checkout Page**
   - Cart summary
   - Price breakdown
   - Apply promo codes (optional)
   - Payment gateway integration

#### Components:
- EventTicketPurchase
- PaymentForm
- PaymentMethodSelector
- OrderHistory
- OrderDetails
- ReceiptDownloader

### Phase 6: Analytics Dashboard (Week 6)
**Priority: MEDIUM**

#### Features:
1. **User Analytics**
   - Personal engagement metrics
   - Post performance
   - Follower growth charts

2. **Event Analytics (for creators)**
   - Event registration tracking
   - Attendance metrics
   - Revenue analytics (if paid events)

3. **Platform Analytics (admin)**
   - User statistics
   - Platform health metrics
   - Trending content
   - Activity charts

#### Components:
- AnalyticsDashboard
- MetricCard
- ChartComponent (using Chart.js or Recharts)
- TrendingContent

### Phase 7: Search & Discovery (Week 7)
**Priority: LOW**

#### Features:
1. **Full-text Search**
   - Search posts, users, events
   - Search filters
   - Autocomplete suggestions
   - Search history

2. **Hashtag Support**
   - Click hashtag to see related posts
   - Trending hashtags
   - Hashtag feed

#### Components:
- SearchBar
- SearchResults
- HashtagFeed
- TrendingHashtags

---

## UI/UX Design Guidelines

### Design System
- **Color Palette:**
  - Primary: Campus blue (#0066CC)
  - Secondary: Campus green (#00AA44)
  - Accent: Campus orange (#FF9900)
  - Neutral: Gray scale (#000, #333, #666, #999, #CCC, #EEE, #FFF)

- **Typography:**
  - Headings: Bold, 24-32px
  - Body: Regular, 14-16px
  - Small text: 12-13px

- **Spacing:** 8px, 16px, 24px, 32px (base unit: 8px)

- **Shadows:** Subtle shadows for depth (only on cards and modals)

### Layout
- **Desktop:** 1200px max-width container
- **Tablet:** Full width with padding
- **Mobile:** Full width, optimized touch targets (48px minimum)

### Navigation
- **Header:** Logo, search bar, notifications, user menu
- **Sidebar:** Navigation menu (collapsible on mobile)
- **Footer:** Links, copyright, social media

### Responsiveness
- Mobile-first approach
- Breakpoints: 640px (sm), 1024px (md), 1280px (lg)
- Touch-friendly buttons (48px × 48px minimum)

---

## Key Integration Points

### Authentication Flow
1. User logs in → API returns JWT token
2. Store token in localStorage (or secure HttpOnly cookie)
3. Configure Axios interceptor to attach token to all requests
4. On 401 response, attempt token refresh
5. If refresh fails, redirect to login

### WebSocket Connection
1. Connect to `/ws/notifications` after authentication
2. Listen for notification events
3. Update Redux store with new notifications
4. Show toast notification to user
5. Update notification bell badge count

### Payment Integration
1. User clicks "Buy Tickets" button
2. Show payment method selection (Stripe or Razorpay)
3. Redirect to payment form
4. After successful payment, webhook from backend updates order status
5. Redirect to confirmation page with receipt
6. Send confirmation email

### State Management Architecture
```
Redux Store Structure:
├─ auth
│  ├─ user (current user info)
│  ├─ token (JWT token)
│  ├─ isAuthenticated (boolean)
│  └─ loading (boolean)
├─ feed
│  ├─ posts (array of posts)
│  ├─ loading (boolean)
│  ├─ hasMore (boolean for pagination)
│  └─ currentPage (number)
├─ events
│  ├─ events (array)
│  ├─ selectedEvent (current event details)
│  ├─ filters (event filters)
│  └─ loading (boolean)
├─ notifications
│  ├─ list (array of notifications)
│  ├─ unreadCount (number)
│  └─ connected (WebSocket status)
├─ orders
│  ├─ list (user's orders)
│  ├─ selectedOrder (current order details)
│  └─ loading (boolean)
└─ ui
   ├─ sidebarOpen (boolean)
   ├─ theme (dark/light)
   └─ toast (current toast message)
```

---

## API Integration Checklist

### Auth Service
- [ ] POST /auth/register
- [ ] POST /auth/login
- [ ] POST /auth/logout
- [ ] POST /auth/refresh-token
- [ ] POST /auth/verify-email

### User Service
- [ ] GET /users/{userId}
- [ ] PUT /users/{userId}
- [ ] GET /users/search
- [ ] GET /users/{userId}/followers
- [ ] GET /users/{userId}/following

### Feed Service
- [ ] POST /posts
- [ ] GET /posts (with pagination)
- [ ] GET /posts/{postId}
- [ ] PUT /posts/{postId}
- [ ] DELETE /posts/{postId}
- [ ] POST /posts/{postId}/like
- [ ] DELETE /posts/{postId}/like
- [ ] POST /posts/{postId}/comments
- [ ] GET /posts/{postId}/comments

### Follow Service
- [ ] POST /follows
- [ ] DELETE /follows/{followId}
- [ ] GET /follows/recommended

### Event Service
- [ ] POST /events
- [ ] GET /events (with filters)
- [ ] GET /events/{eventId}
- [ ] PUT /events/{eventId}
- [ ] DELETE /events/{eventId}
- [ ] POST /events/{eventId}/register
- [ ] DELETE /events/{eventId}/register
- [ ] GET /events/{eventId}/attendees

### Notification Service
- [ ] GET /notifications
- [ ] PUT /notifications/{id}/read
- [ ] DELETE /notifications/{id}
- [ ] WebSocket: /ws/notifications

### Search Service
- [ ] GET /search/posts
- [ ] GET /search/users
- [ ] GET /search/events
- [ ] GET /search/hashtags

### Payment Service
- [ ] POST /payments/create
- [ ] GET /orders
- [ ] GET /orders/{orderId}
- [ ] POST /orders/{orderId}/refund

### Analytics Service
- [ ] GET /analytics/users/engagement
- [ ] GET /analytics/posts/{postId}/performance
- [ ] GET /analytics/events/trending
- [ ] GET /analytics/platform/stats

---

## Error Handling Strategy

### HTTP Error Codes
- **400:** Bad Request → Show validation errors
- **401:** Unauthorized → Redirect to login
- **403:** Forbidden → Show permission error
- **404:** Not Found → Show 404 page
- **429:** Rate Limited → Show retry message
- **500:** Server Error → Show generic error message

### Error Display
- Toast notifications for transient errors
- Modal dialogs for critical errors
- Form field errors for validation
- Error boundary component for crashes

---

## Performance Considerations

### Optimization Strategies
- [ ] Lazy load images
- [ ] Code splitting by route
- [ ] Virtual scrolling for long lists
- [ ] Debounce search input
- [ ] Cache API responses (React Query)
- [ ] Memoize expensive computations
- [ ] Optimize bundle size

### Caching Strategy
- Cache user profile data for 5 minutes
- Cache events list for 2 minutes
- Don't cache real-time data (feed, notifications)
- Invalidate cache on user actions

---

## Security Considerations

### Best Practices
- [ ] HTTPS only
- [ ] JWT token in HttpOnly cookie (preferred) or localStorage
- [ ] CSRF protection (include CSRF token in POST/PUT/DELETE)
- [ ] Input validation (Zod schemas)
- [ ] XSS prevention (sanitize user input)
- [ ] Rate limiting on client side
- [ ] Secure password reset flow
- [ ] No sensitive data in Redux store

---

## Testing Strategy

### Unit Tests
- Test utility functions
- Test Redux reducers
- Test custom hooks

### Integration Tests
- Test API services
- Test authentication flow
- Test form submissions

### E2E Tests (Priority: Medium)
- User registration flow
- Login flow
- Create and post a post
- Like a post
- Comment on a post
- Register for an event
- Make a payment

---

## Project Structure

```
campussync-frontend/
├─ public/
│  ├─ index.html
│  └─ favicon.ico
├─ src/
│  ├─ components/
│  │  ├─ auth/
│  │  │  ├─ LoginForm.tsx
│  │  │  ├─ RegisterForm.tsx
│  │  │  └─ VerifyEmail.tsx
│  │  ├─ feed/
│  │  │  ├─ Feed.tsx
│  │  │  ├─ PostCard.tsx
│  │  │  ├─ PostCreation.tsx
│  │  │  └─ Comments.tsx
│  │  ├─ events/
│  │  │  ├─ EventBrowser.tsx
│  │  │  ├─ EventCard.tsx
│  │  │  ├─ EventDetails.tsx
│  │  │  └─ EventCreation.tsx
│  │  ├─ notifications/
│  │  │  ├─ NotificationCenter.tsx
│  │  │  ├─ NotificationBell.tsx
│  │  │  └─ NotificationItem.tsx
│  │  ├─ user/
│  │  │  ├─ UserProfile.tsx
│  │  │  ├─ UserCard.tsx
│  │  │  └─ UserSearch.tsx
│  │  ├─ payments/
│  │  │  ├─ PaymentForm.tsx
│  │  │  ├─ OrderHistory.tsx
│  │  │  └─ Receipt.tsx
│  │  ├─ analytics/
│  │  │  ├─ Dashboard.tsx
│  │  │  └─ MetricCard.tsx
│  │  ├─ common/
│  │  │  ├─ Header.tsx
│  │  │  ├─ Sidebar.tsx
│  │  │  ├─ Footer.tsx
│  │  │  ├─ Modal.tsx
│  │  │  └─ Toast.tsx
│  │  └─ layout/
│  │     ├─ MainLayout.tsx
│  │     └─ AuthLayout.tsx
│  ├─ pages/
│  │  ├─ LoginPage.tsx
│  │  ├─ RegisterPage.tsx
│  │  ├─ FeedPage.tsx
│  │  ├─ ProfilePage.tsx
│  │  ├─ EventsPage.tsx
│  │  ├─ EventDetailsPage.tsx
│  │  ├─ PaymentPage.tsx
│  │  ├─ NotificationsPage.tsx
│  │  ├─ AnalyticsPage.tsx
│  │  └─ 404Page.tsx
│  ├─ services/
│  │  ├─ api.ts (Axios instance)
│  │  ├─ auth.service.ts
│  │  ├─ user.service.ts
│  │  ├─ post.service.ts
│  │  ├─ event.service.ts
│  │  ├─ notification.service.ts
│  │  ├─ payment.service.ts
│  │  ├─ search.service.ts
│  │  └─ analytics.service.ts
│  ├─ redux/
│  │  ├─ store.ts
│  │  ├─ slices/
│  │  │  ├─ auth.slice.ts
│  │  │  ├─ feed.slice.ts
│  │  │  ├─ events.slice.ts
│  │  │  ├─ notifications.slice.ts
│  │  │  ├─ orders.slice.ts
│  │  │  └─ ui.slice.ts
│  │  └─ hooks.ts
│  ├─ hooks/
│  │  ├─ useWebSocket.ts
│  │  ├─ useInfiniteScroll.ts
│  │  ├─ useAuth.ts
│  │  └─ useNotifications.ts
│  ├─ types/
│  │  ├─ auth.types.ts
│  │  ├─ user.types.ts
│  │  ├─ post.types.ts
│  │  ├─ event.types.ts
│  │  ├─ notification.types.ts
│  │  ├─ payment.types.ts
│  │  └─ api.types.ts
│  ├─ utils/
│  │  ├─ validators.ts (Zod schemas)
│  │  ├─ formatters.ts
│  │  ├─ constants.ts
│  │  └─ helpers.ts
│  ├─ styles/
│  │  ├─ globals.css
│  │  └─ variables.css
│  ├─ App.tsx
│  ├─ main.tsx
│  └─ config.ts (API base URL, constants)
├─ tests/
│  ├─ unit/
│  ├─ integration/
│  └─ e2e/
├─ .env.example
├─ .env.local
├─ vite.config.ts
├─ tsconfig.json
├─ tailwind.config.js
├─ package.json
└─ README.md
```

---

## Development Workflow

### Setup Instructions
```bash
# Install dependencies
npm install

# Setup environment variables
cp .env.example .env.local
# Edit .env.local with your API base URL

# Start development server
npm run dev

# Run tests
npm run test

# Build for production
npm run build
```

### Environment Variables
```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080
STRIPE_PUBLIC_KEY=pk_test_...
RAZORPAY_KEY_ID=...
```

---

## Success Criteria

### Phase 1 (Auth & Profiles)
- [ ] Users can register and verify email
- [ ] Users can log in and stay logged in
- [ ] Users can view and edit their profile
- [ ] Users can search and view other profiles
- [ ] Follow/unfollow functionality works

### Phase 2 (Social Features)
- [ ] Users can see their feed
- [ ] Users can create posts with text and images
- [ ] Users can like and comment on posts
- [ ] Real-time updates via WebSocket
- [ ] Infinite scroll pagination works

### Phase 3 (Events)
- [ ] Users can browse events
- [ ] Users can create events
- [ ] Users can register for events
- [ ] Event details display correctly
- [ ] Attendee list shows correctly

### Phase 4 (Notifications)
- [ ] Notification center displays notifications
- [ ] Real-time notifications appear
- [ ] Users can mark notifications as read
- [ ] Notification bell badge updates

### Phase 5 (Payments)
- [ ] Payment form displays correctly
- [ ] Stripe payment processing works
- [ ] Razorpay payment processing works
- [ ] Order history shows correctly
- [ ] Receipts can be downloaded

### Phase 6 (Analytics)
- [ ] Analytics dashboard displays metrics
- [ ] Charts render correctly
- [ ] User can see personal analytics
- [ ] Event creators can see event analytics

### Phase 7 (Search)
- [ ] Full-text search works for posts, users, events
- [ ] Hashtag search works
- [ ] Search results display correctly

---

## Notes for v0dev

### Starting Point
Please start with:
1. **Project Setup:** Create React + TypeScript project with Vite
2. **Authentication:** Build login/register pages and auth service
3. **Basic Styling:** Implement Tailwind CSS with design system
4. **Redux Store:** Setup Redux Toolkit with initial slices
5. **Main Layout:** Create header, sidebar, and layout components

### Code Style
- Use TypeScript strictly (no `any` types)
- Use functional components with hooks
- Use React Query for server state management (optional but recommended)
- Follow React best practices
- Use meaningful variable and function names
- Add comments for complex logic

### Component Guidelines
- One component per file
- Prop interfaces/types at top of file
- Reusable components in `components/common`
- Page-specific components in `components/pages`

### API Integration
- Create service files for each domain (auth, user, post, etc.)
- Use Axios with interceptors for auth
- Handle errors gracefully
- Show loading states
- Add request/response logging in development

### Testing
- Write tests for critical paths
- Mock API calls with Vitest
- Use React Testing Library for component tests
- Aim for 70%+ code coverage

---

## Success Metrics

✅ **Must Have:**
- Fully responsive design (mobile, tablet, desktop)
- Authentication working
- Feed displaying posts
- Real-time notifications
- Payment integration
- 95%+ code coverage for critical flows
- < 3s initial load time

✅ **Nice to Have:**
- Dark mode support
- Offline support
- Push notifications
- Image optimization
- Analytics dashboard
- Admin panel

---

## Final Notes

**Backend API Status:** 85% complete and production-ready
- All core endpoints working
- Real-time WebSocket support
- Elasticsearch search integrated
- Payment processing ready
- Analytics dashboard live

**Your Mission:** Build a beautiful, performant frontend that leverages all these backend features to create an amazing user experience for college students.

Good luck! 🚀
