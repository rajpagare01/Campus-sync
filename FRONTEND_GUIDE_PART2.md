# CampusSync Frontend Development Guide — Part 2: API Layer & Core Features

## 9. All API Functions

### Posts API
```typescript
// lib/api/posts.ts
import api from '../axios';

export const postsApi = {
  getAll: (page = 0, size = 20) =>
    api.get(`/api/posts?page=${page}&size=${size}`).then(r => r.data),

  getById: (id: number) =>
    api.get(`/api/posts/${id}`).then(r => r.data),

  create: (data: { content: string; mediaUrl?: string; eventId?: number }) =>
    api.post('/api/posts', data).then(r => r.data),

  update: (id: number, data: { content: string; mediaUrl?: string; eventId?: number }) =>
    api.put(`/api/posts/${id}`, data).then(r => r.data),

  delete: (id: number) =>
    api.delete(`/api/posts/${id}`).then(r => r.data),

  getByAuthor: (authorId: number, page = 0, size = 20) =>
    api.get(`/api/posts/author/${authorId}?page=${page}&size=${size}`).then(r => r.data),

  getByEvent: (eventId: number, page = 0, size = 20) =>
    api.get(`/api/posts/event/${eventId}?page=${page}&size=${size}`).then(r => r.data),

  getWithMedia: (page = 0, size = 20) =>
    api.get(`/api/posts/media?page=${page}&size=${size}`).then(r => r.data),

  // Likes
  toggleLike: (postId: number) =>
    api.post(`/posts/${postId}/like`).then(r => r.data),

  getLikes: (postId: number) =>
    api.get(`/posts/${postId}/likes`).then(r => r.data),

  getUserLikes: () =>
    api.get('/posts/likes/user').then(r => r.data),

  // Comments
  getComments: (postId: number) =>
    api.get(`/posts/${postId}/comments`).then(r => r.data),

  addComment: (postId: number, content: string) =>
    api.post(`/posts/${postId}/comments`, { content }).then(r => r.data),

  addReply: (commentId: number, content: string) =>
    api.post(`/posts/comments/${commentId}/replies`, { content }).then(r => r.data),

  updateComment: (commentId: number, content: string) =>
    api.put(`/posts/comments/${commentId}`, { content }).then(r => r.data),

  deleteComment: (commentId: number) =>
    api.delete(`/posts/comments/${commentId}`).then(r => r.data),
};
```

### Events API
```typescript
// lib/api/events.ts
import api from '../axios';

export const eventsApi = {
  getAll: (page = 0, size = 20) =>
    api.get(`/events?page=${page}&size=${size}`).then(r => r.data),

  getById: (id: number) =>
    api.get(`/events/${id}`).then(r => r.data),

  create: (data: Partial<Event>) =>
    api.post('/events', data).then(r => r.data),

  update: (id: number, data: Partial<Event>) =>
    api.put(`/events/${id}`, data).then(r => r.data),

  delete: (id: number) =>
    api.delete(`/events/${id}`).then(r => r.data),

  updateStatus: (id: number, status: string) =>
    api.patch(`/events/${id}/status?status=${status}`).then(r => r.data),

  search: (params: { keyword?: string; type?: string; status?: string; page?: number; size?: number }) =>
    api.get('/events/search', { params }).then(r => r.data),

  getByCreator: (creatorId: number, page = 0, size = 20) =>
    api.get(`/events/creator/${creatorId}?page=${page}&size=${size}`).then(r => r.data),

  getAnalytics: (id: number) =>
    api.get(`/events/${id}/analytics`).then(r => r.data),
};
```

### Feed API
```typescript
// lib/api/feed.ts
import api from '../axios';

export const feedApi = {
  getHome: (params?: { page?: number; size?: number; filter?: string; sort?: string }) =>
    api.get('/feed', { params }).then(r => r.data),

  getPosts: (params?: { page?: number; size?: number; sort?: string }) =>
    api.get('/feed/posts', { params }).then(r => r.data),

  getEvents: (params?: { page?: number; size?: number; sort?: string }) =>
    api.get('/feed/events', { params }).then(r => r.data),

  getTrending: (params?: { page?: number; size?: number; filter?: string }) =>
    api.get('/feed/trending', { params }).then(r => r.data),

  getStats: () =>
    api.get('/feed/stats').then(r => r.data),
};
```

### Follow API
```typescript
// lib/api/follow.ts
import api from '../axios';

export const followApi = {
  follow: (userId: number) =>
    api.post(`/api/follow/follow/${userId}`).then(r => r.data),

  unfollow: (userId: number) =>
    api.delete(`/api/follow/unfollow/${userId}`).then(r => r.data),

  isFollowing: (userId: number) =>
    api.get(`/api/follow/is-following/${userId}`).then(r => r.data),

  getStats: (userId: number) =>
    api.get(`/api/follow/stats/${userId}`).then(r => r.data),

  getFollowers: (userId: number, page = 0, size = 20) =>
    api.get(`/api/follow/followers/${userId}?page=${page}&size=${size}`).then(r => r.data),

  getFollowing: (userId: number, page = 0, size = 20) =>
    api.get(`/api/follow/following/${userId}?page=${page}&size=${size}`).then(r => r.data),

  getRecommendations: (page = 0, size = 10) =>
    api.get(`/api/follow/recommendations?page=${page}&size=${size}`).then(r => r.data),

  getMutual: () =>
    api.get('/api/follow/mutual').then(r => r.data),
};
```

### Users API
```typescript
// lib/api/users.ts
import api from '../axios';

export const usersApi = {
  getMyProfile: () =>
    api.get('/users/profile').then(r => r.data),

  getProfile: (userId: number) =>
    api.get(`/users/${userId}/profile`).then(r => r.data),

  updateProfile: (data: { name?: string; bio?: string; profilePictureUrl?: string }) =>
    api.put('/users/profile', data).then(r => r.data),

  updatePicture: (pictureUrl: string) =>
    api.patch(`/users/profile/picture?pictureUrl=${encodeURIComponent(pictureUrl)}`).then(r => r.data),

  getMyActivity: () =>
    api.get('/users/activity').then(r => r.data),

  getActivity: (userId: number) =>
    api.get(`/users/${userId}/activity`).then(r => r.data),

  getStats: (userId: number) =>
    api.get(`/users/${userId}/stats`).then(r => r.data),

  getMyStats: () =>
    api.get('/users/stats/my-stats').then(r => r.data),
};
```

### Notifications API
```typescript
// lib/api/notifications.ts
import api from '../axios';

export const notificationsApi = {
  getAll: () =>
    api.get('/api/notifications').then(r => r.data),

  getUnread: () =>
    api.get('/api/notifications/unread').then(r => r.data),

  markAsRead: (id: number) =>
    api.post(`/api/notifications/${id}/read`).then(r => r.data),

  markAllAsRead: () =>
    api.post('/api/notifications/read-all').then(r => r.data),

  delete: (id: number) =>
    api.delete(`/api/notifications/${id}`).then(r => r.data),
};
```

### Registrations & Payments API
```typescript
// lib/api/payments.ts
import api from '../axios';

export const registrationsApi = {
  // Unified: handles both free and paid events
  register: (eventId: number) =>
    api.post(`/registrations?eventId=${eventId}`).then(r => r.data),

  getMyRegistrations: () =>
    api.get('/registrations/user/me').then(r => r.data),

  getEventParticipants: (eventId: number) =>
    api.get(`/registrations/event/${eventId}`).then(r => r.data),

  cancel: (id: number) =>
    api.put(`/registrations/cancel/${id}`).then(r => r.data),
};

export const paymentsApi = {
  // For retrying a failed payment
  createCheckout: (eventId: number, successUrl?: string, cancelUrl?: string) =>
    api.post('/api/payments/checkout', { eventId, successUrl, cancelUrl }).then(r => r.data),

  getOrder: (orderId: number) =>
    api.get(`/api/payments/orders/${orderId}`).then(r => r.data),

  refund: (orderId: number, reason?: string) =>
    api.post(`/api/payments/orders/${orderId}/refund`, { reason }).then(r => r.data),
};
```

### Search API
```typescript
// lib/api/search.ts
import api from '../axios';

export const searchApi = {
  posts: (q: string, page = 0, size = 20) =>
    api.get(`/api/search/posts?q=${encodeURIComponent(q)}&page=${page}&size=${size}`).then(r => r.data),

  events: (q: string, page = 0, size = 20) =>
    api.get(`/api/search/events?q=${encodeURIComponent(q)}&page=${page}&size=${size}`).then(r => r.data),

  users: (q: string, page = 0, size = 20) =>
    api.get(`/api/search/users?q=${encodeURIComponent(q)}&page=${page}&size=${size}`).then(r => r.data),

  // Returns posts + events + users + comments in one call
  discover: (q: string, sizePerSection = 5) =>
    api.get(`/api/search/discover?q=${encodeURIComponent(q)}&sizePerSection=${sizePerSection}`).then(r => r.data),

  eventsByLocation: (location: string, page = 0, size = 20) =>
    api.get(`/api/search/events/by-location?location=${encodeURIComponent(location)}&page=${page}&size=${size}`).then(r => r.data),

  eventsByCategory: (category: string, page = 0, size = 20) =>
    api.get(`/api/search/events/by-category?category=${encodeURIComponent(category)}&page=${page}&size=${size}`).then(r => r.data),

  topUsers: (page = 0, size = 20) =>
    api.get(`/api/search/users/top?page=${page}&size=${size}`).then(r => r.data),
};
```

---

## 10. WebSocket — Real-time Notifications

```typescript
// lib/websocket.ts
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let stompClient: Client | null = null;

export function connectWebSocket(
  accessToken: string,
  onNotification: (notification: any) => void
) {
  stompClient = new Client({
    webSocketFactory: () =>
      new SockJS(process.env.NEXT_PUBLIC_WS_URL!),
    connectHeaders: {
      Authorization: `Bearer ${accessToken}`,
    },
    reconnectDelay: 5000,
    onConnect: () => {
      // Subscribe to user-specific notification queue
      stompClient!.subscribe('/user/queue/notifications', (message) => {
        const notification = JSON.parse(message.body);
        onNotification(notification);
      });

      // Optional: subscribe to user topic as well
      stompClient!.subscribe('/app/notifications/subscribe', () => {});
    },
    onStompError: (frame) => {
      console.error('WebSocket error:', frame);
    },
  });

  stompClient.activate();
  return stompClient;
}

export function disconnectWebSocket() {
  stompClient?.deactivate();
  stompClient = null;
}

export function ackNotification(notificationId: number) {
  stompClient?.publish({
    destination: '/app/notifications/ack',
    body: JSON.stringify({ notificationId }),
  });
}
```

```typescript
// hooks/useWebSocket.ts
'use client';
import { useEffect } from 'react';
import { useAuthStore } from '@/store/authStore';
import { useNotificationStore } from '@/store/notificationStore';
import { connectWebSocket, disconnectWebSocket } from '@/lib/websocket';

export function useWebSocket() {
  const { accessToken, isAuthenticated } = useAuthStore();
  const { addNotification, incrementUnread } = useNotificationStore();

  useEffect(() => {
    if (!isAuthenticated() || !accessToken) return;

    const client = connectWebSocket(accessToken, (notification) => {
      addNotification(notification);
      if (!notification.isRead) incrementUnread();
    });

    return () => disconnectWebSocket();
  }, [accessToken]);
}
```

---

## 11. Razorpay Payment Flow

```typescript
// hooks/useEventRegistration.ts
'use client';
import { useState } from 'react';
import { registrationsApi } from '@/lib/api/payments';
import toast from 'react-hot-toast';

declare global {
  interface Window { Razorpay: any; }
}

export function useEventRegistration() {
  const [loading, setLoading] = useState(false);

  const register = async (eventId: number, eventTitle: string) => {
    setLoading(true);
    try {
      const res = await registrationsApi.register(eventId);

      if (res.paymentRequired && res.providerOrderId) {
        // Paid event — open Razorpay modal
        await openRazorpay(res, eventTitle);
      } else {
        // Free event — done
        toast.success(`Registered for ${eventTitle}!`);
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  const openRazorpay = (registration: any, eventTitle: string) => {
    return new Promise<void>((resolve, reject) => {
      const options = {
        key: registration.razorpayKeyId,
        amount: registration.amountInMinorUnits,
        currency: registration.currency || 'INR',
        name: 'CampusSync',
        description: `Registration: ${eventTitle}`,
        order_id: registration.providerOrderId,
        handler: () => {
          toast.success('Payment successful! Registration confirmed.');
          resolve();
        },
        modal: {
          ondismiss: () => {
            toast.error('Payment cancelled.');
            reject(new Error('Payment cancelled'));
          },
        },
        theme: { color: '#6366f1' },
      };

      const rzp = new window.Razorpay(options);
      rzp.open();
    });
  };

  return { register, loading };
}
```

Add to `app/layout.tsx`:
```html
<Script src="https://checkout.razorpay.com/v1/checkout.js" strategy="lazyOnload" />
```

---

## 12. TanStack Query Setup

```typescript
// app/providers.tsx
'use client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 1000 * 60 * 2, // 2 minutes
        retry: 1,
      },
    },
  }));
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
```

### Query Hook Examples

```typescript
// Feed
export function useFeed(filter = 'all', sort = 'date') {
  return useInfiniteQuery({
    queryKey: ['feed', filter, sort],
    queryFn: ({ pageParam = 0 }) => feedApi.getHome({ page: pageParam, size: 20, filter, sort }),
    getNextPageParam: (last) => last.last ? undefined : last.pageNumber + 1,
    initialPageParam: 0,
  });
}

// Events
export function useEvents(page = 0) {
  return useQuery({
    queryKey: ['events', page],
    queryFn: () => eventsApi.getAll(page),
  });
}

// User profile
export function useProfile(userId: number) {
  return useQuery({
    queryKey: ['profile', userId],
    queryFn: () => usersApi.getProfile(userId),
  });
}

// Notifications (poll every 30s)
export function useNotifications() {
  return useQuery({
    queryKey: ['notifications'],
    queryFn: notificationsApi.getAll,
    refetchInterval: 30_000,
  });
}
```
