# CampusSync Frontend Development Guide — Part 1: Foundation

## 1. Project Overview

CampusSync is a campus social platform with these core features:
- **Authentication** — Register (OTP email verify), Login, JWT + Refresh tokens, Forgot/Reset password
- **Social Feed** — Posts, Likes, Comments (threaded), personalized & trending feed
- **Events** — Create, browse, register (free & paid via Razorpay), analytics
- **Follow System** — Follow/Unfollow users, recommendations, mutual follows
- **Notifications** — Real-time via WebSocket (STOMP/SockJS), mark read, delete
- **Search & Discovery** — Search posts/events/users/comments, discover endpoint
- **User Profiles** — View/edit profile, activity timeline, follow stats
- **Payments** — Razorpay integration for paid events
- **Admin** — Moderation, dashboard, role management

---

## 2. Tech Stack Recommendation

```
Framework    : Next.js 14 (App Router)
Language     : TypeScript
Styling      : Tailwind CSS + shadcn/ui components
State        : Zustand (global) + TanStack Query v5 (server state)
HTTP Client  : Axios with interceptors
WebSocket    : @stomp/stompjs + sockjs-client
Payments     : Razorpay JS SDK
Forms        : React Hook Form + Zod validation
Icons        : Lucide React
Toasts       : react-hot-toast
Date         : date-fns
```

---

## 3. Project Structure

```
src/
├── app/                        # Next.js App Router pages
│   ├── (auth)/
│   │   ├── login/page.tsx
│   │   ├── register/page.tsx
│   │   ├── verify/page.tsx
│   │   └── forgot-password/page.tsx
│   ├── (main)/
│   │   ├── feed/page.tsx
│   │   ├── events/
│   │   │   ├── page.tsx
│   │   │   └── [id]/page.tsx
│   │   ├── posts/
│   │   │   └── [id]/page.tsx
│   │   ├── profile/
│   │   │   ├── page.tsx          # Own profile
│   │   │   └── [userId]/page.tsx # Public profile
│   │   ├── search/page.tsx
│   │   └── notifications/page.tsx
│   └── admin/
│       └── dashboard/page.tsx
├── components/
│   ├── auth/
│   ├── feed/
│   ├── events/
│   ├── posts/
│   ├── profile/
│   ├── notifications/
│   ├── search/
│   └── ui/                     # shadcn/ui base components
├── lib/
│   ├── api/                    # All API functions grouped by domain
│   │   ├── auth.ts
│   │   ├── posts.ts
│   │   ├── events.ts
│   │   ├── feed.ts
│   │   ├── follow.ts
│   │   ├── notifications.ts
│   │   ├── search.ts
│   │   ├── users.ts
│   │   └── payments.ts
│   ├── axios.ts                # Axios instance + interceptors
│   ├── websocket.ts            # STOMP client setup
│   └── utils.ts
├── store/
│   ├── authStore.ts            # Zustand: current user, tokens
│   └── notificationStore.ts   # Zustand: unread count, WS state
├── hooks/
│   ├── useAuth.ts
│   ├── useFeed.ts
│   ├── useNotifications.ts
│   └── useWebSocket.ts
└── types/
    └── index.ts                # All TypeScript interfaces
```

---

## 4. Environment Variables

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_WS_URL=http://localhost:8080/ws/notifications
NEXT_PUBLIC_RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxx
```

---

## 5. TypeScript Types

```typescript
// types/index.ts

export type Role = 'STUDENT' | 'SOCIETY' | 'DEPARTMENT' | 'ADMIN';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  bio?: string;
  profilePictureUrl?: string;
  verified: boolean;
  createdAt: string;
}

export interface UserProfile extends User {
  postCount: number;
  eventCount: number;
  likeCount: number;
  commentCount: number;
  followersCount: number;
  followingCount: number;
  following: boolean;   // current viewer follows this user
  followedBy: boolean;  // this user follows current viewer
  mutual: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    email: string;
    role: string;
    firstName: string;
    lastName: string;
    bio?: string;
    profilePicture?: string;
  };
}

export interface Post {
  id: number;
  content: string;
  mediaUrl?: string;
  authorId: number;
  authorName: string;
  createdAt: string;
  updatedAt: string;
  likeCount: number;
  commentCount: number;
  likedByCurrentUser: boolean;
  eventId?: number;
  eventTitle?: string;
}

export type EventType = 'TECHNICAL' | 'CULTURAL' | 'SPORTS' | 'PLACEMENT' | 'WORKSHOP' | 'OTHER';
export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED';

export interface Event {
  id: number;
  title: string;
  description: string;
  venue: string;
  date: string;
  type: EventType;
  status: EventStatus;
  paid: boolean;
  price: number;
  imageUrl?: string;
  viewsCount: number;
  createdBy?: User;
  createdAt: string;
}

export interface FeedItem {
  type: 'POST' | 'EVENT';
  id: number;
  createdAt: string;
  title: string;
  content: string;
  authorName: string;
  authorId?: number;
  mediaUrl?: string;
  imageUrl?: string;
  likeCount?: number;
  commentCount?: number;
  isLikedByCurrentUser?: boolean;
  engagementScore: number;
  // Event-specific
  venue?: string;
  eventDate?: string;
  eventType?: string;
  paid?: boolean;
  price?: number;
}

export interface Comment {
  id: number;
  content: string;
  authorId: number;
  authorName: string;
  postId: number;
  parentCommentId?: number;
  replyCount: number;
  replies: Comment[];
  createdAt: string;
  updatedAt: string;
}

export interface Notification {
  id: number;
  userId: number;
  actorId: number;
  type: 'LIKE' | 'COMMENT' | 'FOLLOW' | 'EVENT_UPDATE' | 'POST_MENTION' | 'REPLY';
  relatedId: number;
  message: string;
  isRead: boolean;
  createdAt: string;
  readAt?: string;
}

export interface FollowStats {
  followersCount: number;
  followingCount: number;
  isFollowing: boolean;
  isFollowedBy: boolean;
  isMutual: boolean;
}

export interface RegistrationResponse {
  id: number;
  userName: string;
  eventTitle: string;
  status: string;
  paymentStatus: string;
  paymentRequired: boolean;
  // Payment fields (only for paid events)
  paymentOrderId?: number;
  providerOrderId?: string;
  razorpayKeyId?: string;
  amountInMinorUnits?: number;
  currency?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
```

---

## 6. Axios Setup with JWT Interceptors

```typescript
// lib/axios.ts
import axios from 'axios';

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Attach access token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Auto-refresh on 401
let isRefreshing = false;
let failedQueue: any[] = [];

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          original.headers.Authorization = `Bearer ${token}`;
          return api(original);
        });
      }

      isRefreshing = true;
      const refreshToken = localStorage.getItem('refreshToken');
      try {
        const { data } = await axios.post(
          `${process.env.NEXT_PUBLIC_API_BASE_URL}/auth/refresh-token`,
          { refreshToken }
        );
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        failedQueue.forEach((p) => p.resolve(data.accessToken));
        failedQueue = [];
        original.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(original);
      } catch {
        failedQueue.forEach((p) => p.reject(error));
        failedQueue = [];
        localStorage.clear();
        window.location.href = '/login';
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(error);
  }
);

export default api;
```

---

## 7. Auth Store (Zustand)

```typescript
// store/authStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: any | null;
  setAuth: (accessToken: string, refreshToken: string, user: any) => void;
  clearAuth: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setAuth: (accessToken, refreshToken, user) =>
        set({ accessToken, refreshToken, user }),
      clearAuth: () => set({ accessToken: null, refreshToken: null, user: null }),
      isAuthenticated: () => !!get().accessToken,
    }),
    { name: 'campussync-auth' }
  )
);
```

---

## 8. Authentication API

```typescript
// lib/api/auth.ts
import api from '../axios';

export const authApi = {
  register: (data: { name: string; email: string; password: string; role: string }) =>
    api.post('/auth/register', data).then(r => r.data),

  verify: (data: { email: string; code: string }) =>
    api.post('/auth/verify', data).then(r => r.data),

  login: (data: { email: string; password: string }) =>
    api.post('/auth/login', data).then(r => r.data),

  logout: (refreshToken?: string) =>
    api.post('/auth/logout', { refreshToken }).then(r => r.data),

  resendOtp: (email: string) =>
    api.post(`/auth/resend-otp?email=${email}`).then(r => r.data),

  forgotPassword: (email: string) =>
    api.post(`/auth/forgot-password?email=${email}`).then(r => r.data),

  resetPassword: (email: string, otp: string, newPassword: string) =>
    api.post(`/auth/reset-password?email=${email}&otp=${otp}&newPassword=${newPassword}`)
      .then(r => r.data),
};
```
