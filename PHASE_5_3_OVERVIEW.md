# Phase 5.3: Analytics Dashboard - Feature Overview

**Status:** Designed & Ready for Implementation  
**Date:** 6 April 2026  
**Complexity:** Medium (8-12 hours)

---

## 🎯 What is Phase 5.3?

Phase 5.3 is an **Analytics Dashboard** that provides comprehensive insights into:
- User engagement and activity
- Event popularity and attendance
- Post performance and content trends
- Platform-wide statistics
- User retention and growth metrics

---

## 📊 Core Features

### 1. User Engagement Analytics
- **Total Users** - Active vs inactive breakdown
- **New Users** - Daily/weekly/monthly new registrations
- **Engagement Rate** - % of users creating posts/comments/likes
- **Activity Metrics** - Posts per user, comments per user, daily active users
- **Retention Metrics** - Day 7, 30, 90 user retention rates
- **Daily Stats** - Timeline of user activity

### 2. Event Analytics
- **Event Popularity** - Trending events ranked by engagement
- **Attendance Metrics** - Registrations, actual attendees, attendance rate
- **Category Stats** - Events grouped by category with engagement
- **Event Engagement** - Posts, comments, likes per event
- **Time Series** - Attendance trends over time

### 3. Content Analytics
- **Post Performance** - Likes, comments, engagement rate per post
- **Content Trends** - Trending topics/hashtags over time
- **Peak Times** - When posts get most engagement
- **Content Categories** - Performance by content type
- **Share Metrics** - Post sharing and reach

### 4. Platform Statistics
- **Overall Metrics**
  - Total users, posts, events, comments, likes
  - Average posts per user
  - Average engagement rate
  - Platform growth rate

- **Daily Snapshots**
  - Daily user count
  - Daily posts created
  - Daily comments
  - Daily likes
  - Daily active users

---

## 🏗️ Architecture

### System Flow
```
Admin Dashboard (Frontend)
        ↓
AnalyticsController (/api/analytics/*)
        ↓
AnalyticsService (Aggregation logic)
        ↓
Custom Queries (JPA Specification/Native SQL)
        ↓
MySQL Database
        ↓
JSON Response (Charts data)
```

### Data Models

#### Analytics Entity
```java
@Entity
public class Analytics {
    Long id;
    String type;               // USER_ENGAGEMENT, EVENT_STATS, POST_PERF
    LocalDate date;
    Long metricValue;
    Map<String, Object> details;
    LocalDateTime createdAt;
}
```

#### DTOs (Data Transfer Objects)
- UserEngagementMetrics
- EventAnalytics
- PostPerformanceMetrics
- PlatformStats
- DailyUserStats
- UserRetentionMetrics
- EventPopularity
- ContentTrend

---

## 🔌 API Endpoints (Planned)

### User Engagement
```
GET /api/analytics/users/engagement         - Get engagement metrics
GET /api/analytics/users/daily-stats?days=30  - Get daily user stats
GET /api/analytics/users/retention?days=30    - Get user retention
GET /api/analytics/users/growth              - Get user growth stats
```

### Events
```
GET /api/analytics/events/trending?limit=10   - Get trending events
GET /api/analytics/events/{id}/stats          - Get event analytics
GET /api/analytics/events/category-stats      - Get stats by category
GET /api/analytics/events/attendance-rate     - Get attendance rates
```

### Content
```
GET /api/analytics/posts/{id}/performance    - Get post metrics
GET /api/analytics/content/trends?days=30    - Get content trends
GET /api/analytics/hashtags/trending         - Get trending hashtags
GET /api/analytics/content/peak-times        - Get peak engagement times
```

### Platform
```
GET /api/analytics/platform/stats            - Get platform statistics
GET /api/analytics/platform/daily?days=30    - Get daily platform stats
GET /api/analytics/platform/growth           - Get growth metrics
GET /api/analytics/platform/summary          - Get summary dashboard
```

### Reports
```
GET /api/analytics/reports?type=user&start=2026-01-01&end=2026-04-06
  - Generate analytics report

GET /api/analytics/export?format=csv&report=engagement
  - Export analytics as CSV
```

---

## 📈 Key Metrics

### User Metrics
- **Total Users** - Count of all users
- **Active Users** - Users with activity in last 7/30/90 days
- **New Users** - New registrations per day/week/month
- **Engagement Rate** - (Users creating posts/comments) / Total Users
- **Average Posts/User** - Total posts / Total users
- **Retention Rate** - Day 7/30/90 users returning

### Event Metrics
- **Total Events** - Count of all events
- **Attendee Count** - Registration vs actual attendance
- **Attendance Rate** - Actual / Registered ratio
- **Event Engagement** - Posts + Comments + Likes per event
- **Category Distribution** - Events by category

### Content Metrics
- **Total Posts** - Count of all posts
- **Engagement Rate** - (Likes + Comments) / Views
- **Peak Time** - Time of day with most engagement
- **Trending Hashtags** - Most used tags
- **Content Distribution** - Posts by category/type

### Platform Metrics
- **Monthly Active Users** - Users active in last 30 days
- **Daily Active Users** - Users active today
- **Daily Posts** - Average posts per day
- **Daily Engagement** - Average likes/comments per day
- **Growth Rate** - User growth % month-over-month

---

## 💻 Implementation Components

### Files to Create

#### Service (1 file)
- `Service/AnalyticsService.java` (200-300 lines)
  - All metric calculation methods
  - Data aggregation logic
  - Report generation

#### Repository (1 file)
- `Repository/AnalyticsRepository.java`
  - Custom query methods
  - Aggregation queries
  - Date-range queries

#### Controller (1 file)
- `Controller/AnalyticsController.java` (150-200 lines)
  - 12+ REST endpoints
  - Parameter validation
  - Response formatting

#### DTOs (8+ files)
- `UserEngagementMetrics.java`
- `EventAnalytics.java`
- `PostPerformanceMetrics.java`
- `PlatformStats.java`
- `DailyUserStats.java`
- `UserRetentionMetrics.java`
- `ContentTrend.java`
- `AnalyticsReport.java`

#### Tests (2 files)
- `AnalyticsServiceTest.java` (10+ test cases)
- `AnalyticsRepositoryTest.java` (10+ test cases)

---

## 🎯 Expected Metrics Output

### User Engagement Response
```json
{
  "totalUsers": 1000,
  "activeUsers": 450,
  "newUsers": 25,
  "engagementRate": 0.45,
  "postsPerUser": 3.2,
  "commentsPerUser": 1.8,
  "period": "2026-03-01 to 2026-04-06"
}
```

### Event Analytics Response
```json
{
  "eventId": 1,
  "title": "Tech Conference 2026",
  "totalRegistrations": 500,
  "actualAttendees": 425,
  "attendanceRate": 0.85,
  "totalPosts": 156,
  "totalComments": 489,
  "totalLikes": 2340,
  "engagementRate": 0.58
}
```

### Platform Stats Response
```json
{
  "totalUsers": 1000,
  "totalPosts": 3200,
  "totalEvents": 45,
  "totalComments": 5800,
  "totalLikes": 28900,
  "averagePostsPerUser": 3.2,
  "averageEngagementRate": 0.42,
  "dailyActiveUsers": 234,
  "monthlyActiveUsers": 850,
  "lastUpdated": "2026-04-06T09:00:00"
}
```

---

## 🔧 Implementation Approach

### Step 1: DTOs & Models
- Create data transfer objects for each metric type
- Define data structure for responses

### Step 2: Repository Layer
- Implement custom query methods
- Optimize with indexes for performance
- Use JPA Specification or native queries

### Step 3: Service Layer
- Implement metric calculation methods
- Handle date range filtering
- Implement caching for performance

### Step 4: Controller Layer
- Create REST endpoints
- Add parameter validation
- Format responses

### Step 5: Testing
- Unit tests with mocks
- Integration tests with real data
- Performance tests for large datasets

### Step 6: Documentation
- API documentation
- Query optimization notes
- Integration guide

---

## ⏱️ Estimated Timeline

| Phase | Task | Duration |
|-------|------|----------|
| 1 | DTOs & Data Models | 1 hour |
| 2 | Repository Implementation | 1.5 hours |
| 3 | Service Implementation | 2 hours |
| 4 | Controller Implementation | 1.5 hours |
| 5 | Testing | 1.5 hours |
| 6 | Documentation | 1 hour |
| **Total** | | **8-10 hours** |

---

## 🔍 Key Queries

### User Engagement Query
```sql
SELECT 
  COUNT(DISTINCT u.id) as totalUsers,
  COUNT(DISTINCT CASE WHEN u.updated_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN u.id END) as activeUsers,
  COUNT(DISTINCT CASE WHEN u.created_at >= CURDATE() THEN u.id END) as newUsers,
  COUNT(DISTINCT p.id) as totalPosts,
  COUNT(DISTINCT c.id) as totalComments,
  COUNT(DISTINCT l.id) as totalLikes
FROM users u
LEFT JOIN posts p ON u.id = p.user_id
LEFT JOIN comments c ON u.id = c.user_id  
LEFT JOIN likes l ON u.id = l.user_id;
```

### Event Analytics Query
```sql
SELECT 
  e.id,
  e.title,
  COUNT(DISTINCT ea.user_id) as registrations,
  COUNT(DISTINCT CASE WHEN ea.attended = true THEN ea.user_id END) as attendees,
  COUNT(DISTINCT p.id) as posts,
  COUNT(DISTINCT c.id) as comments,
  COUNT(DISTINCT l.id) as likes
FROM events e
LEFT JOIN event_attendees ea ON e.id = ea.event_id
LEFT JOIN posts p ON e.id = p.event_id
LEFT JOIN comments c ON p.id = c.post_id
LEFT JOIN likes l ON p.id = l.post_id
GROUP BY e.id;
```

### Daily Stats Query
```sql
SELECT 
  DATE(u.created_at) as date,
  COUNT(DISTINCT u.id) as newUsers,
  COUNT(DISTINCT p.id) as newPosts,
  COUNT(DISTINCT c.id) as newComments,
  COUNT(DISTINCT l.id) as newLikes
FROM users u
LEFT JOIN posts p ON u.id = p.user_id AND DATE(p.created_at) = DATE(u.created_at)
LEFT JOIN comments c ON DATE(c.created_at) = DATE(u.created_at)
LEFT JOIN likes l ON DATE(l.created_at) = DATE(u.created_at)
GROUP BY DATE(u.created_at)
ORDER BY date DESC;
```

---

## 📊 Dashboard Features (Frontend)

Once API is ready, frontend can display:

### User Engagement Card
- Total users, active users, new users
- Engagement rate (gauge)
- Posts/comments per user
- Trend line

### Event Analytics Card
- Top 10 trending events
- Attendance rate by event
- Category distribution (pie chart)
- Event timeline

### Content Performance Card
- Top 10 posts
- Trending hashtags
- Content distribution
- Peak engagement times

### Platform Overview Card
- Key metrics (cards)
- Daily active users (line chart)
- Growth rate (line chart)
- Period comparison

---

## 🎓 Learning Outcomes

- Complex aggregation queries
- Database optimization for analytics
- Time series data handling
- Caching strategies
- Report generation
- Data visualization preparation

---

## ✅ Success Criteria

- [x] Design complete
- [ ] All DTOs created
- [ ] Repository with query methods
- [ ] Service with all metric calculations
- [ ] Controller with API endpoints
- [ ] Unit tests passing
- [ ] Integration tests passing
- [ ] Documentation complete

---

## 🚀 Next Phase (5.4)

After Phase 5.3, Phase 5.4 will add:
- Payment processing (Stripe/Razorpay)
- Event ticket sales
- Subscription management
- Financial reporting

---

## 📚 Reference

See `PHASE_5_DESIGN.md` Feature 3 section for complete specifications.

---

**Status:** ✅ Design Complete | Ready for Implementation  
**Complexity:** Medium | **Estimated Time:** 8-10 hours  
**Impact:** High - Provides crucial business insights

Ready to implement Phase 5.3? 🚀
