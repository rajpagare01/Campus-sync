# 🚀 Phase 5: Advanced Features - Design & Implementation Plan

**Date:** 5 April 2026  
**Version:** 1.0  
**Status:** Design Phase - Ready for Implementation  
**Target Duration:** 3-4 weeks

---

## 🎯 Phase 5 Overview

### Mission
Transform CampusSync from a feature-complete social platform to an **enterprise-grade system** with advanced capabilities:
- Real-time notifications via WebSocket
- Powerful full-text search with Elasticsearch
- Comprehensive analytics dashboard
- Monetization through event ticket payments
- Mobile-first optimization

### Current State (Phases 1-4)
- ✅ 62.5% project completion
- ✅ 5500+ lines of production code
- ✅ 63 working tests
- ✅ 85-90% performance improvement
- ✅ Production-grade security

### Phase 5 Target
- 75% project completion
- Advanced feature integration
- Enterprise-ready system
- Mobile app support

---

## 📋 Feature 1: Real-time Notifications (WebSocket)

### Overview
Enable live notifications for:
- Post likes and comments
- Event updates
- User follows
- Direct messages (future)

### Architecture

```
┌─────────────────┐
│  Frontend (Web) │
│   (WebSocket)   │
└────────┬────────┘
         │
    ┌────▼────────────────────┐
    │  WebSocket Broker       │
    │  (Spring WebSocket)     │
    │  - Stomp Protocol       │
    │  - Message Handler      │
    └────┬───────────────────┬┘
         │                   │
    ┌────▼──────┐      ┌─────▼──────┐
    │  Notif    │      │   Event    │
    │  Service  │      │   Service  │
    └───────────┘      └────────────┘
         │
    ┌────▼───────────────────┐
    │  Redis Pub/Sub          │
    │  (Message Broadcasting) │
    └────────────────────────┘
         │
    ┌────▼───────────────────┐
    │  Database              │
    │  (Notification Log)    │
    └────────────────────────┘
```

### Implementation Components

#### 1. WebSocketConfig
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    - registerStompEndpoints("/ws/notifications")
    - enableSimpleBroker("/topic", "/queue")
    - setApplicationDestinationPrefixes("/app")
}
```

#### 2. NotificationController
```java
@Controller
@MessageMapping("/notifications")
public class NotificationController {
    - @SendTo("/topic/notifications") broadcastNotification()
    - @SendToUser("/queue/notifications") sendPersonalNotification()
    - subscribeToEvents()
}
```

#### 3. NotificationService
```java
@Service
public class NotificationService {
    - notifyLike(Post post, User liker)
    - notifyComment(Comment comment)
    - notifyFollow(User follower, User followed)
    - notifyEventUpdate(Event event)
    - getNotifications(User user)
    - markAsRead(Notification notification)
}
```

#### 4. Notification Entity
```java
@Entity
public class Notification {
    Long id
    Long userId (recipient)
    Long actorId (who triggered)
    String type (LIKE, COMMENT, FOLLOW, EVENT_UPDATE)
    Long relatedId (post/event/user id)
    String message
    Boolean isRead
    LocalDateTime createdAt
    LocalDateTime readAt
}
```

#### 5. NotificationRepository
```java
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndIsReadFalse(Long userId);
    long countByUserIdAndIsReadFalse(Long userId);
}
```

### API Endpoints

```
WebSocket Connection:
ws://localhost:8080/ws/notifications

Subscribe Topics:
/topic/notifications        - Broadcast notifications
/topic/post/{postId}       - Post-specific updates
/topic/event/{eventId}     - Event-specific updates
/queue/user/{userId}       - User-specific messages

REST Endpoints:
GET  /api/notifications                    - Get all notifications
GET  /api/notifications/unread             - Get unread count
POST /api/notifications/{id}/read          - Mark as read
POST /api/notifications/read-all           - Mark all as read
DELETE /api/notifications/{id}             - Delete notification
```

### Key Features
- ✅ Real-time delivery
- ✅ User-specific targeting
- ✅ Broadcast to multiple users
- ✅ Persistent notification log
- ✅ Read/Unread status tracking
- ✅ Notification preferences

### Testing Strategy
- Unit tests for NotificationService
- Integration tests with embedded WebSocket
- Load testing (100+ concurrent connections)
- Message ordering verification

---

## 🔍 Feature 2: Full-text Search (Elasticsearch)

### Overview
Enable powerful searching across:
- Posts (content, tags, hashtags)
- Events (title, description, location)
- Users (name, bio, departments)
- Comments (thread content)

### Architecture

```
┌──────────────────┐
│  Client Request  │
│  /api/search     │
└────────┬─────────┘
         │
    ┌────▼──────────────────┐
    │  SearchController      │
    │  - parseQuery()        │
    │  - applyFilters()      │
    └────┬───────────────────┘
         │
    ┌────▼──────────────────┐
    │  SearchService        │
    │  - queryElasticsearch()│
    │  - rankResults()       │
    │  - highlightMatches()  │
    └────┬───────────────────┘
         │
    ┌────▼──────────────────┐
    │  Elasticsearch Cluster │
    │  - Posts Index         │
    │  - Events Index        │
    │  - Users Index         │
    │  - Comments Index      │
    └────┬───────────────────┘
         │
    ┌────▼──────────────────┐
    │  MySQL Database        │
    │  (Data Source)         │
    └────────────────────────┘
```

### Implementation Components

#### 1. Elasticsearch Configuration
```yaml
spring.elasticsearch.uris: http://localhost:9200
spring.elasticsearch.username: elastic
spring.elasticsearch.password: password
```

#### 2. Search Index Mappings

**Posts Index:**
```json
{
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "content": { "type": "text", "analyzer": "standard" },
      "authorId": { "type": "keyword" },
      "tags": { "type": "keyword" },
      "createdAt": { "type": "date" },
      "likes": { "type": "integer" }
    }
  }
}
```

#### 3. SearchDocument Entities
```java
@Document(indexName = "posts")
public class PostSearchDocument {
    @Id String id;
    String content;
    Long authorId;
    String authorName;
    List<String> tags;
    LocalDateTime createdAt;
    Integer likes;
}

@Document(indexName = "events")
public class EventSearchDocument {
    @Id String id;
    String title;
    String description;
    String location;
    LocalDateTime date;
    Long organizerId;
}

@Document(indexName = "users")
public class UserSearchDocument {
    @Id String id;
    String name;
    String email;
    String bio;
    String department;
}
```

#### 4. SearchService
```java
@Service
public class SearchService {
    
    // Full-text search
    Page<PostSearchDocument> searchPosts(String query, Pageable page)
    Page<EventSearchDocument> searchEvents(String query, Pageable page)
    Page<UserSearchDocument> searchUsers(String query, Pageable page)
    
    // Advanced search with filters
    Page<PostSearchDocument> searchPostsAdvanced(
        String query, 
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long authorId,
        Pageable page
    )
    
    // Autocomplete
    List<String> getPostAutocomplete(String prefix)
    List<String> getUserAutocomplete(String prefix)
    
    // Trending
    List<String> getTrendingTags(int limit)
    List<EventSearchDocument> getTrendingEvents(int limit)
}
```

#### 5. SearchController
```java
@RestController
@RequestMapping("/api/search")
public class SearchController {
    
    @GetMapping("/posts")
    Page<PostSearchDTO> searchPosts(
        @RequestParam String q,
        @PageableDefault(size = 20) Pageable page
    )
    
    @GetMapping("/events")
    Page<EventSearchDTO> searchEvents(
        @RequestParam String q,
        @PageableDefault(size = 20) Pageable page
    )
    
    @GetMapping("/users")
    Page<UserSearchDTO> searchUsers(
        @RequestParam String q,
        @PageableDefault(size = 20) Pageable page
    )
    
    @GetMapping("/advanced")
    Page<SearchResultDTO> advancedSearch(
        @RequestParam String q,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        Pageable page
    )
    
    @GetMapping("/autocomplete")
    List<String> autocomplete(@RequestParam String q)
    
    @GetMapping("/trending")
    TrendingDTO getTrending()
}
```

### API Endpoints

```
GET  /api/search/posts?q=java               - Search posts
GET  /api/search/events?q=hackathon         - Search events
GET  /api/search/users?q=john               - Search users
GET  /api/search/advanced?q=&type=post      - Advanced search
GET  /api/search/autocomplete?q=java        - Autocomplete suggestions
GET  /api/search/trending                   - Trending topics/events
```

### Search Features
- ✅ Full-text search across content
- ✅ Fuzzy matching for typo tolerance
- ✅ Advanced filters (date, author, type)
- ✅ Autocomplete suggestions
- ✅ Trending topics/events
- ✅ Result highlighting
- ✅ Faceted search

### Testing Strategy
- Unit tests for SearchService
- Integration tests with embedded Elasticsearch
- Performance tests (10,000+ documents)
- Relevance scoring verification

---

## 📊 Feature 3: Analytics Dashboard

### Overview
Comprehensive analytics for:
- User engagement metrics
- Event popularity trends
- Platform statistics
- Content performance

### Architecture

```
┌──────────────────┐
│  Admin Dashboard │
│  (Charts/Graphs) │
└────────┬─────────┘
         │
    ┌────▼──────────────────┐
    │  AnalyticsController   │
    │  - getMetrics()        │
    │  - getTimeSeries()     │
    └────┬───────────────────┘
         │
    ┌────▼──────────────────┐
    │  AnalyticsService      │
    │  - calculateMetrics()  │
    │  - aggregateData()     │
    │  - generateReports()   │
    └────┬───────────────────┘
         │
    ┌────▼──────────────────┐
    │  Analytics Repository  │
    │  - Complex Queries     │
    └────┬───────────────────┘
         │
    ┌────▼──────────────────┐
    │  Database             │
    │  (Aggregated Data)    │
    └────────────────────────┘
```

### Implementation Components

#### 1. Analytics Entity
```java
@Entity
public class Analytics {
    Long id;
    String type; // USER_ENGAGEMENT, EVENT_STATS, POST_PERFORMANCE
    LocalDate date;
    Long metricValue;
    Map<String, Object> details;
}
```

#### 2. AnalyticsService
```java
@Service
public class AnalyticsService {
    
    // User Engagement Metrics
    UserEngagementMetrics getUserEngagementMetrics(LocalDate startDate, LocalDate endDate)
    List<DailyUserStats> getDailyUserStats(int days)
    UserRetentionMetrics getUserRetention(int days)
    
    // Event Analytics
    EventAnalytics getEventAnalytics(Long eventId)
    List<EventPopularity> getTrendingEvents(int limit)
    EventCategoryStats getCategoryStats()
    
    // Post Analytics
    PostPerformanceMetrics getPostPerformance(Long postId)
    List<ContentTrend> getContentTrends(int days)
    HashtagAnalytics getHashtagStats()
    
    // Platform Statistics
    PlatformStats getPlatformStats()
    List<DailyStats> getDailyStats(int days)
    
    // Report Generation
    AnalyticsReport generateReport(ReportType type, LocalDate start, LocalDate end)
}
```

#### 3. Analytics DTOs
```java
public class UserEngagementMetrics {
    Long totalUsers;
    Long activeUsers;
    Long newUsers;
    Double engagementRate; // activities/total users
    Double postsPerUser;
    Double commentsPerUser;
}

public class EventAnalytics {
    Long eventId;
    String title;
    Long totalRegistrations;
    Long actualAttendees;
    Double attendanceRate;
    Long totalPosts;
    Long totalComments;
    Long totalLikes;
}

public class PostPerformanceMetrics {
    Long postId;
    Long likes;
    Long comments;
    Long shares;
    Double engagementRate; // (likes + comments)/views
    LocalDateTime peakEngagementTime;
}

public class PlatformStats {
    Long totalUsers;
    Long totalPosts;
    Long totalEvents;
    Long totalComments;
    Long totalLikes;
    Double averagePostsPerUser;
    Double averageEngagementRate;
}
```

#### 4. AnalyticsController
```java
@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {
    
    @GetMapping("/platform-stats")
    PlatformStats getPlatformStats()
    
    @GetMapping("/user-engagement")
    UserEngagementMetrics getUserEngagement(
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate
    )
    
    @GetMapping("/daily-stats")
    List<DailyStats> getDailyStats(@RequestParam int days)
    
    @GetMapping("/trending-events")
    List<EventPopularity> getTrendingEvents(@RequestParam(defaultValue = "10") int limit)
    
    @GetMapping("/top-posts")
    List<PostPerformanceMetrics> getTopPosts(@RequestParam(defaultValue = "10") int limit)
    
    @GetMapping("/content-trends")
    List<ContentTrend> getContentTrends(@RequestParam int days)
    
    @GetMapping("/hashtag-stats")
    HashtagAnalytics getHashtagStats()
    
    @PostMapping("/report")
    AnalyticsReport generateReport(
        @RequestParam ReportType type,
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate
    )
}
```

### API Endpoints

```
GET  /api/admin/analytics/platform-stats       - Overall platform metrics
GET  /api/admin/analytics/user-engagement      - User engagement metrics
GET  /api/admin/analytics/daily-stats          - Daily statistics
GET  /api/admin/analytics/trending-events      - Trending events
GET  /api/admin/analytics/top-posts            - Top performing posts
GET  /api/admin/analytics/content-trends       - Content trend analysis
GET  /api/admin/analytics/hashtag-stats        - Hashtag analytics
POST /api/admin/analytics/report               - Generate custom report
```

### Dashboard Metrics
- ✅ Total users, posts, events, comments, likes
- ✅ Daily/weekly/monthly trends
- ✅ User engagement rates
- ✅ Event popularity metrics
- ✅ Post performance analytics
- ✅ Trending content/hashtags
- ✅ Custom report generation

---

## 💳 Feature 4: Payment Integration

### Overview
Enable event monetization through:
- Event ticket sales
- Payment processing (Stripe/Razorpay)
- Invoice generation
- Refund handling

### Architecture

```
┌──────────────────┐
│  Event Creator   │
│  (Ticket Pricing)│
└────────┬─────────┘
         │
    ┌────▼──────────────────┐
    │  PaymentController     │
    │  - checkoutEvent()     │
    │  - confirmPayment()    │
    └────┬───────────────────┘
         │
    ┌────▼──────────────────┐
    │  PaymentService        │
    │  - createPayment()     │
    │  - verifyPayment()     │
    │  - processRefund()     │
    └────┬───────────────────┘
         │
    ┌────▼──────────────────┐
    │  Payment Gateway       │
    │  (Stripe/Razorpay)     │
    └────────────────────────┘
```

### Implementation Components

#### 1. Payment Entities
```java
@Entity
public class EventTicket {
    Long id;
    Long eventId;
    String name; // Gold, Silver, Standard
    Double price;
    Integer totalQuantity;
    Integer soldQuantity;
    LocalDateTime onSaleDate;
    LocalDateTime offSaleDate;
}

@Entity
public class Payment {
    Long id;
    Long userId;
    Long eventId;
    Double amount;
    String currency; // USD, INR
    PaymentStatus status; // PENDING, COMPLETED, FAILED, REFUNDED
    String transactionId;
    LocalDateTime createdAt;
    LocalDateTime completedAt;
}

@Entity
public class Ticket {
    Long id;
    Long userId;
    Long eventTicketId;
    Long paymentId;
    String ticketNumber;
    TicketStatus status; // VALID, USED, REFUNDED
    LocalDateTime generatedAt;
}
```

#### 2. PaymentService
```java
@Service
public class PaymentService {
    
    // Stripe Integration
    StripeCheckoutSession createCheckout(Long eventId, int quantity)
    void confirmStripePayment(String sessionId)
    void refundStripePayment(Long paymentId)
    
    // Razorpay Integration
    RazorpayOrder createOrder(Long eventId, Double amount)
    void verifyRazorpayPayment(String paymentId, String signature)
    void processRazorpayRefund(Long paymentId)
    
    // Payment Management
    Payment getPayment(Long paymentId)
    List<Payment> getUserPayments(Long userId)
    List<Payment> getEventPayments(Long eventId)
    
    // Ticket Generation
    List<Ticket> generateTickets(Long paymentId, int quantity)
    String getTicketPDF(Long ticketId)
    
    // Invoice Generation
    Invoice generateInvoice(Long paymentId)
    void sendInvoiceEmail(Long paymentId)
}
```

#### 3. PaymentController
```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @PostMapping("/checkout/{eventId}")
    CheckoutResponse checkout(
        @PathVariable Long eventId,
        @RequestParam int quantity,
        @RequestParam(required = false) Long ticketTypeId
    )
    
    @PostMapping("/stripe/webhook")
    void handleStripeWebhook(@RequestBody String payload)
    
    @PostMapping("/razorpay/webhook")
    void handleRazorpayWebhook(@RequestBody RazorpayWebhook payload)
    
    @GetMapping("/history")
    List<PaymentDTO> getPaymentHistory(Pageable page)
    
    @GetMapping("/ticket/{ticketId}/pdf")
    ResponseEntity<byte[]> getTicketPDF(@PathVariable Long ticketId)
    
    @PostMapping("/refund/{paymentId}")
    RefundResponse requestRefund(@PathVariable Long paymentId)
}
```

### API Endpoints

```
POST /api/payments/checkout/{eventId}         - Create checkout session
POST /api/payments/stripe/webhook             - Stripe webhook handler
POST /api/payments/razorpay/webhook           - Razorpay webhook handler
GET  /api/payments/history                    - Payment history
GET  /api/payments/ticket/{ticketId}/pdf      - Download ticket PDF
POST /api/payments/refund/{paymentId}         - Request refund
```

### Payment Features
- ✅ Multi-currency support (USD, INR)
- ✅ Stripe & Razorpay integration
- ✅ Ticket generation & delivery
- ✅ Invoice generation
- ✅ Refund processing
- ✅ Payment history tracking
- ✅ Webhook handling

---

## 📱 Feature 5: Mobile Optimization

### Overview
Native mobile app support:
- React Native/Flutter frontend (separate repo)
- Optimized APIs
- Mobile-specific endpoints
- Push notifications

### Mobile API Enhancements

#### 1. Device Registration
```java
@Entity
public class DeviceToken {
    Long id;
    Long userId;
    String token; // FCM or APNs token
    String platform; // iOS, Android
    LocalDateTime registeredAt;
}

@Service
public class PushNotificationService {
    void registerDevice(String token, String platform)
    void sendPushNotification(Long userId, String title, String message)
    void sendBroadcastNotification(String title, String message)
}
```

#### 2. Mobile-specific Endpoints
```java
@GetMapping("/api/v2/feed")  // Optimized for mobile
@GetMapping("/api/v2/events")
@GetMapping("/api/v2/user/profile")
@PostMapping("/api/v2/posts")
```

#### 3. Image Optimization
```java
- Generate thumbnails (200x200, 500x500, 1000x1000)
- WebP format support
- Progressive loading
- CDN integration
```

### Mobile Features
- ✅ Push notifications (FCM/APNs)
- ✅ Optimized payload sizes
- ✅ Image compression
- ✅ Battery-efficient polling
- ✅ Offline support (sync on reconnect)

---

## 🗂️ Implementation Timeline

### Week 1-2: WebSocket Notifications
- [ ] Configure WebSocket server
- [ ] Build Notification entities and repositories
- [ ] Implement NotificationService
- [ ] Create WebSocket controllers
- [ ] Test real-time messaging

### Week 2-3: Elasticsearch Search
- [ ] Setup Elasticsearch cluster
- [ ] Create search indices and mappings
- [ ] Implement SearchService
- [ ] Build SearchController with advanced queries
- [ ] Integration testing

### Week 3: Analytics Dashboard
- [ ] Design analytics schema
- [ ] Implement AnalyticsService
- [ ] Build AnalyticsController
- [ ] Create dashboard DTOs
- [ ] Admin dashboard frontend

### Week 3-4: Payment Integration
- [ ] Integrate Stripe/Razorpay SDKs
- [ ] Build Payment entities and services
- [ ] Implement webhook handlers
- [ ] Ticket generation system
- [ ] Invoice generation

### Week 4: Mobile Optimization
- [ ] Setup FCM/APNs integration
- [ ] Create device token management
- [ ] Build push notification service
- [ ] Optimize API responses
- [ ] Mobile app integration testing

---

## 📚 Database Schema Changes

### New Tables
```sql
-- Notifications
CREATE TABLE notification (...)
CREATE TABLE device_token (...)

-- Payments
CREATE TABLE event_ticket (...)
CREATE TABLE payment (...)
CREATE TABLE ticket (...)
CREATE TABLE invoice (...)

-- Analytics
CREATE TABLE analytics (...)

-- Search (in Elasticsearch, not MySQL)
```

### New Indexes
- Elasticsearch indices for search
- Database indexes for analytics queries
- Device token lookup indexes

---

## 🔐 Security Considerations

### Authentication & Authorization
- ✅ JWT token validation on all endpoints
- ✅ Role-based access control for admin features
- ✅ Rate limiting for payment endpoints
- ✅ CORS configuration for mobile apps

### Payment Security
- ✅ PCI-DSS compliance (use tokenization)
- ✅ HTTPS only
- ✅ Webhook signature verification
- ✅ Encrypted sensitive data

### Data Protection
- ✅ User data encryption
- ✅ Audit logging for payments
- ✅ GDPR compliance for EU users
- ✅ Payment data segregation

---

## 📊 Expected Outcomes

### Performance
- WebSocket: <100ms notification delivery
- Search: <200ms query response (1M documents)
- Analytics: <500ms dashboard load
- Payments: <1s transaction completion

### Scalability
- Support 10,000+ concurrent WebSocket connections
- Index 10M+ searchable documents
- Process 1,000+ transactions/hour
- Support 100,000+ users

### Quality
- 90%+ test coverage
- 99.9% payment success rate
- 99% search accuracy
- <0.1% error rate

---

## ✅ Success Criteria

- [x] Phase 5 design complete
- [ ] WebSocket implementation & testing
- [ ] Elasticsearch integration & testing
- [ ] Analytics dashboard complete
- [ ] Payment system production-ready
- [ ] Mobile API optimizations complete
- [ ] Comprehensive documentation
- [ ] 100+ Phase 5 tests passing
- [ ] Performance benchmarks met
- [ ] Security audit passed

---

## 🚀 Next Steps

1. **Approve Design** - Review and approve Phase 5 architecture
2. **Setup Infrastructure** - Elasticsearch, Redis, Payment accounts
3. **Begin Implementation** - Start with WebSocket (highest impact)
4. **Parallel Development** - Search and Analytics can proceed in parallel
5. **Integration Testing** - Test feature interactions
6. **Performance Testing** - Load testing before production
7. **Security Audit** - Payment and data security review
8. **User Testing** - Beta with subset of users

---

**Phase 5 Design Ready for Implementation**  
*Estimated Completion: Mid-April 2026*  
*Target: 75% overall project completion*

---

*Design Document Version: 1.0*  
*Prepared by: GitHub Copilot*  
*Date: 5 April 2026*
