# CampusSync Frontend Development Guide — Part 3: Pages & Components

## 13. Page-by-Page Implementation Guide

---

### 13.1 Register Page (`/register`)

**Flow:** Fill form → `POST /auth/register` → OTP sent → redirect to `/verify?email=x`

```typescript
// app/(auth)/register/page.tsx
'use client';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { authApi } from '@/lib/api/auth';
import { useRouter } from 'next/navigation';

const schema = z.object({
  name: z.string().min(2),
  email: z.string().email(),
  password: z.string().min(8),
  role: z.enum(['STUDENT', 'SOCIETY', 'DEPARTMENT']),
});

export default function RegisterPage() {
  const router = useRouter();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { role: 'STUDENT' },
  });

  const onSubmit = async (data: any) => {
    await authApi.register(data);
    router.push(`/verify?email=${encodeURIComponent(data.email)}`);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('name')} placeholder="Full Name" />
      <input {...register('email')} type="email" placeholder="Email" />
      <input {...register('password')} type="password" placeholder="Password" />
      <select {...register('role')}>
        <option value="STUDENT">Student</option>
        <option value="SOCIETY">Society</option>
        <option value="DEPARTMENT">Department</option>
      </select>
      <button type="submit" disabled={isSubmitting}>Register</button>
    </form>
  );
}
```

---

### 13.2 OTP Verify Page (`/verify`)

**Flow:** Enter 6-digit OTP → `POST /auth/verify` → save tokens → redirect to `/feed`

```typescript
'use client';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/store/authStore';

// Key logic:
const onSubmit = async ({ code }: { code: string }) => {
  const res = await authApi.verify({ email, code });
  useAuthStore.getState().setAuth(res.accessToken, res.refreshToken, res.user);
  router.push('/feed');
};
// Also: resend OTP button with 60s cooldown calling authApi.resendOtp(email)
```

---

### 13.3 Login Page (`/login`)

```typescript
const onSubmit = async (data: any) => {
  const res = await authApi.login(data);
  useAuthStore.getState().setAuth(res.accessToken, res.refreshToken, res.user);
  router.push('/feed');
};
```

---

### 13.4 Feed Page (`/feed`) — Main Feature

**Components needed:**
- `FeedFilter` — tab buttons: All / Posts / Events
- `FeedSort` — toggle: Latest / Trending
- `FeedCard` — renders either PostCard or EventCard based on `item.type`
- `PostCard` — content, author, like button, comment count, media
- `EventCard` — title, date, venue, paid badge, register button
- `InfiniteScrollContainer` — triggers next page load

```typescript
'use client';
import { useFeed } from '@/hooks/useFeed';
import { useInView } from 'react-intersection-observer';

export default function FeedPage() {
  const [filter, setFilter] = useState('all');
  const [sort, setSort] = useState('date');
  const { data, fetchNextPage, hasNextPage, isLoading } = useFeed(filter, sort);
  const { ref, inView } = useInView();

  useEffect(() => {
    if (inView && hasNextPage) fetchNextPage();
  }, [inView]);

  const items = data?.pages.flatMap(p => p) ?? [];

  return (
    <div>
      <FeedFilter value={filter} onChange={setFilter} />
      <FeedSort value={sort} onChange={setSort} />
      {items.map(item => (
        <FeedCard key={`${item.type}-${item.id}`} item={item} />
      ))}
      <div ref={ref} /> {/* infinite scroll sentinel */}
    </div>
  );
}
```

**FeedCard component:**
```typescript
function FeedCard({ item }: { item: FeedItem }) {
  if (item.type === 'POST') return <PostCard post={item} />;
  if (item.type === 'EVENT') return <EventCard event={item} />;
  return null;
}
```

---

### 13.5 PostCard Component

```typescript
function PostCard({ post }: { post: FeedItem }) {
  const [liked, setLiked] = useState(post.isLikedByCurrentUser);
  const [likeCount, setLikeCount] = useState(post.likeCount ?? 0);
  const [showComments, setShowComments] = useState(false);

  const handleLike = async () => {
    const res = await postsApi.toggleLike(post.id);
    setLiked(res.liked);
    setLikeCount(res.likeCount);
  };

  return (
    <div className="card">
      <div className="author-row">
        <Avatar name={post.authorName} />
        <span>{post.authorName}</span>
        <span className="text-muted">{formatDate(post.createdAt)}</span>
      </div>
      <p>{post.content}</p>
      {post.mediaUrl && <img src={post.mediaUrl} alt="media" />}
      <div className="actions">
        <button onClick={handleLike}>
          {liked ? '❤️' : '🤍'} {likeCount}
        </button>
        <button onClick={() => setShowComments(!showComments)}>
          💬 {post.commentCount}
        </button>
      </div>
      {showComments && <CommentSection postId={post.id} />}
    </div>
  );
}
```

---

### 13.6 CommentSection Component

```typescript
function CommentSection({ postId }: { postId: number }) {
  const { data: comments } = useQuery({
    queryKey: ['comments', postId],
    queryFn: () => postsApi.getComments(postId),
  });
  const [text, setText] = useState('');
  const queryClient = useQueryClient();

  const submit = async () => {
    await postsApi.addComment(postId, text);
    setText('');
    queryClient.invalidateQueries({ queryKey: ['comments', postId] });
  };

  return (
    <div>
      {comments?.map(c => <CommentItem key={c.id} comment={c} postId={postId} />)}
      <input value={text} onChange={e => setText(e.target.value)} placeholder="Add a comment..." />
      <button onClick={submit}>Send</button>
    </div>
  );
}

function CommentItem({ comment, postId }: { comment: Comment; postId: number }) {
  const [showReplies, setShowReplies] = useState(false);
  const [replyText, setReplyText] = useState('');

  const submitReply = async () => {
    await postsApi.addReply(comment.id, replyText);
    setReplyText('');
  };

  return (
    <div className="comment">
      <strong>{comment.authorName}</strong>
      <p>{comment.content}</p>
      <button onClick={() => setShowReplies(!showReplies)}>
        Replies ({comment.replyCount})
      </button>
      {showReplies && (
        <div className="replies">
          {comment.replies.map(r => (
            <div key={r.id} className="reply">
              <strong>{r.authorName}</strong>: {r.content}
            </div>
          ))}
          <input value={replyText} onChange={e => setReplyText(e.target.value)} placeholder="Reply..." />
          <button onClick={submitReply}>Reply</button>
        </div>
      )}
    </div>
  );
}
```

---

### 13.7 EventCard & Registration

```typescript
function EventCard({ event }: { event: FeedItem | Event }) {
  const { register, loading } = useEventRegistration();

  return (
    <div className="card">
      {event.imageUrl && <img src={event.imageUrl} alt={event.title} />}
      <div className="badges">
        <span>{event.eventType}</span>
        {event.paid && <span className="badge-paid">Paid ₹{event.price}</span>}
      </div>
      <h3>{event.title}</h3>
      <p>{event.content}</p>
      <div>📍 {event.venue}</div>
      <div>📅 {formatDate(event.eventDate ?? event.date)}</div>
      <button
        onClick={() => register(event.id, event.title)}
        disabled={loading}
      >
        {loading ? 'Processing...' : event.paid ? `Register — ₹${event.price}` : 'Register Free'}
      </button>
    </div>
  );
}
```

---

### 13.8 Events Page (`/events`)

**Features:** Paginated list, search bar, filter by type, filter by paid/free, search by location

```typescript
export default function EventsPage() {
  const [keyword, setKeyword] = useState('');
  const [type, setType] = useState('');
  const [page, setPage] = useState(0);

  const { data } = useQuery({
    queryKey: ['events', keyword, type, page],
    queryFn: () => eventsApi.search({ keyword: keyword || undefined, type: type || undefined, page, size: 20 }),
  });

  return (
    <div>
      <SearchBar value={keyword} onChange={setKeyword} />
      <EventTypeFilter value={type} onChange={setType} />
      <div className="events-grid">
        {data?.content.map(event => <EventCard key={event.id} event={event} />)}
      </div>
      <Pagination
        current={page}
        total={data?.totalPages ?? 0}
        onChange={setPage}
      />
    </div>
  );
}
```

---

### 13.9 Event Detail Page (`/events/[id]`)

```typescript
export default function EventDetailPage({ params }: { params: { id: string } }) {
  const { data: event } = useQuery({
    queryKey: ['event', params.id],
    queryFn: () => eventsApi.getById(Number(params.id)),
  });
  const { data: posts } = useQuery({
    queryKey: ['event-posts', params.id],
    queryFn: () => postsApi.getByEvent(Number(params.id)),
  });
  const { register, loading } = useEventRegistration();

  return (
    <div>
      {event?.imageUrl && <img src={event.imageUrl} className="hero-image" />}
      <h1>{event?.title}</h1>
      <p>{event?.description}</p>
      <div>📍 {event?.venue} | 📅 {formatDate(event?.date)}</div>
      {event?.paid && <div>💰 ₹{event.price}</div>}
      <button onClick={() => register(event!.id, event!.title)} disabled={loading}>
        {event?.paid ? `Pay & Register — ₹${event.price}` : 'Register Free'}
      </button>

      <h2>Related Posts</h2>
      {posts?.content.map(post => <PostCard key={post.id} post={post} />)}
    </div>
  );
}
```

---

### 13.10 User Profile Page (`/profile/[userId]`)

```typescript
export default function ProfilePage({ params }: { params: { userId: string } }) {
  const myUserId = useAuthStore(s => s.user?.id);
  const isOwnProfile = String(myUserId) === params.userId;

  const { data: profile } = useQuery({
    queryKey: ['profile', params.userId],
    queryFn: () => usersApi.getProfile(Number(params.userId)),
  });
  const { data: posts } = useQuery({
    queryKey: ['user-posts', params.userId],
    queryFn: () => postsApi.getByAuthor(Number(params.userId)),
  });

  const handleFollow = async () => {
    if (profile?.following) await followApi.unfollow(profile.id);
    else await followApi.follow(profile!.id);
    queryClient.invalidateQueries({ queryKey: ['profile', params.userId] });
  };

  return (
    <div>
      <div className="profile-header">
        <img src={profile?.profilePictureUrl || '/default-avatar.png'} className="avatar-lg" />
        <div>
          <h1>{profile?.name}</h1>
          <p>{profile?.bio}</p>
          <div className="stats">
            <span>{profile?.followersCount} Followers</span>
            <span>{profile?.followingCount} Following</span>
            <span>{profile?.postCount} Posts</span>
          </div>
          {!isOwnProfile && (
            <button onClick={handleFollow}>
              {profile?.following ? 'Unfollow' : profile?.followedBy ? 'Follow Back' : 'Follow'}
              {profile?.mutual && ' (Mutual)'}
            </button>
          )}
          {isOwnProfile && <EditProfileButton />}
        </div>
      </div>

      <div className="profile-tabs">
        {/* Posts tab, Activity tab */}
        {posts?.content.map(post => <PostCard key={post.id} post={post} />)}
      </div>
    </div>
  );
}
```

---

### 13.11 Notifications Page + Bell Component

```typescript
// Notification bell in navbar
function NotificationBell() {
  const { data } = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: notificationsApi.getUnread,
    refetchInterval: 30_000,
  });
  const unreadCount = data?.count ?? 0;

  return (
    <button className="relative">
      🔔
      {unreadCount > 0 && (
        <span className="badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
      )}
    </button>
  );
}
```

```typescript
// Notification item renderer
function NotificationItem({ notification, onRead }: { notification: Notification; onRead: () => void }) {
  const icons = {
    LIKE: '❤️',
    COMMENT: '💬',
    FOLLOW: '👤',
    EVENT_UPDATE: '📅',
    REPLY: '↩️',
    POST_MENTION: '@',
  };

  return (
    <div
      className={`notification-item ${!notification.isRead ? 'unread' : ''}`}
      onClick={onRead}
    >
      <span>{icons[notification.type]}</span>
      <div>
        <p>{notification.message}</p>
        <span className="text-muted">{formatDate(notification.createdAt)}</span>
      </div>
    </div>
  );
}
```

---

### 13.12 Search Page (`/search`)

```typescript
export default function SearchPage() {
  const [query, setQuery] = useState('');
  const [tab, setTab] = useState<'all' | 'posts' | 'events' | 'users'>('all');
  const debouncedQuery = useDebounce(query, 400);

  const { data } = useQuery({
    queryKey: ['search', 'discover', debouncedQuery],
    queryFn: () => searchApi.discover(debouncedQuery, 5),
    enabled: debouncedQuery.length > 1,
  });

  return (
    <div>
      <input
        value={query}
        onChange={e => setQuery(e.target.value)}
        placeholder="Search posts, events, people..."
        autoFocus
      />
      <div className="tabs">
        {['all', 'posts', 'events', 'users'].map(t => (
          <button key={t} onClick={() => setTab(t as any)}>{t}</button>
        ))}
      </div>

      {tab === 'all' && data && (
        <div>
          <Section title="Posts" items={data.posts} render={(p: Post) => <PostCard post={p} />} />
          <Section title="Events" items={data.events} render={(e: Event) => <EventCard event={e} />} />
          <Section title="People" items={data.users} render={(u: User) => <UserRow user={u} />} />
        </div>
      )}
    </div>
  );
}
```

---

## 14. Create Event Form

```typescript
// Only for SOCIETY / DEPARTMENT / ADMIN roles
const EventTypes = ['TECHNICAL', 'CULTURAL', 'SPORTS', 'PLACEMENT', 'WORKSHOP', 'OTHER'];

const schema = z.object({
  title: z.string().min(3).max(200),
  description: z.string().min(10),
  venue: z.string().min(2),
  date: z.string(), // ISO datetime string
  type: z.enum(['TECHNICAL', 'CULTURAL', 'SPORTS', 'PLACEMENT', 'WORKSHOP', 'OTHER']),
  paid: z.boolean(),
  price: z.number().min(0),
  imageUrl: z.string().url().optional(),
});

const onSubmit = async (data: any) => {
  if (!data.paid) data.price = 0;
  const event = await eventsApi.create(data);
  router.push(`/events/${event.id}`);
};
```

---

## 15. Create Post Form

```typescript
const schema = z.object({
  content: z.string().min(1).max(2000),
  mediaUrl: z.string().url().optional().or(z.literal('')),
  eventId: z.number().optional(),
});

const onSubmit = async (data: any) => {
  await postsApi.create({
    content: data.content,
    mediaUrl: data.mediaUrl || undefined,
    eventId: data.eventId || undefined,
  });
  queryClient.invalidateQueries({ queryKey: ['feed'] });
  toast.success('Post created!');
};
```

---

## 16. Role-Based Access Control

```typescript
// hooks/useRole.ts
export function useRole() {
  const role = useAuthStore(s => s.user?.role);
  return {
    isStudent: role === 'STUDENT',
    isSociety: role === 'SOCIETY',
    isDepartment: role === 'DEPARTMENT',
    isAdmin: role === 'ADMIN',
    canCreateEvent: ['SOCIETY', 'DEPARTMENT', 'ADMIN'].includes(role ?? ''),
    canCreatePost: !!role,
    canModerate: role === 'ADMIN',
  };
}
```

```typescript
// In components:
const { canCreateEvent } = useRole();
{canCreateEvent && <CreateEventButton />}
```

---

## 17. Recommended Users (Sidebar)

```typescript
function RecommendedUsers() {
  const { data } = useQuery({
    queryKey: ['recommendations'],
    queryFn: () => followApi.getRecommendations(0, 5),
  });

  return (
    <div className="sidebar-card">
      <h3>People to Follow</h3>
      {data?.content.map(user => (
        <UserRow key={user.id} user={user} showFollow />
      ))}
    </div>
  );
}
```

---

## 18. Utility Helpers

```typescript
// lib/utils.ts
import { formatDistanceToNow, format } from 'date-fns';

export function formatDate(dateStr?: string): string {
  if (!dateStr) return '';
  return formatDistanceToNow(new Date(dateStr), { addSuffix: true });
}

export function formatFullDate(dateStr?: string): string {
  if (!dateStr) return '';
  return format(new Date(dateStr), 'PPP p');
}

export function formatCurrency(amountInPaise: number, currency = 'INR'): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency,
  }).format(amountInPaise / 100);
}

export function useDebounce<T>(value: T, ms = 400): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return debounced;
}
```

---

## 19. App Layout with WebSocket + Navbar

```typescript
// app/(main)/layout.tsx
'use client';
import { useWebSocket } from '@/hooks/useWebSocket';

export default function MainLayout({ children }: { children: React.ReactNode }) {
  useWebSocket(); // Connects WS once and stays alive

  return (
    <div>
      <Navbar />
      <main>{children}</main>
    </div>
  );
}
```

```typescript
// Navbar items:
// / feed (Home), /events, /search, /profile/[myId], /notifications
// + Create Post / Create Event buttons based on role
```

---

## 20. Error Handling Pattern

```typescript
// Global error boundary for TanStack Query
const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error: any) => {
      if (error.response?.status === 403) {
        toast.error('You do not have permission to do that.');
      } else if (error.response?.status >= 500) {
        toast.error('Server error. Please try again later.');
      }
    },
  }),
});
```

---

## 21. Key Behavioral Notes for the Frontend

| Behaviour | Detail |
|---|---|
| **JWT expiry** | Interceptor auto-refreshes silently using `/auth/refresh-token` |
| **Paid event register** | Single `POST /registrations?eventId=X` call; if `paymentRequired=true` open Razorpay modal with `providerOrderId` + `razorpayKeyId` from response |
| **Feed pagination** | Response is a flat `FeedItem[]`, NOT a `PaginatedResponse`. Use `page` + `size` params |
| **Event feed items** | `createdAt` = when event was published; `eventDate` = when it occurs |
| **Like toggle** | Returns `{ liked: boolean, likeCount: number }` — update UI optimistically |
| **Comments** | Threaded — top-level comments have `replies[]` nested inside them |
| **Notifications** | Real-time via WS + polling `/api/notifications/unread` every 30s as fallback |
| **Self-follow guard** | Backend rejects self-follows; hide Follow button on own profile |
| **Self-notification** | Backend skips; no need to filter on frontend |
| **Auth base path** | `/auth/**` — no `/api` prefix |
| **Events base path** | `/events/**` — no `/api` prefix |
| **Posts base path** | `/api/posts/**` |
| **Feed base path** | `/feed/**` — no `/api` prefix |
| **WS endpoint** | `http://localhost:8080/ws/notifications` (SockJS) |
| **Token header** | `Authorization: Bearer <accessToken>` on every request |
| **Token in WS** | Pass in STOMP CONNECT header: `Authorization: Bearer <token>` |
