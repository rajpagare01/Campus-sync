# 🧪 CampusSync - Feed System Testing Guide

## 📋 Overview

Test the **Home Feed Integration** with unified posts and events, intelligent sorting, and pagination.

**Collection File:** `CampusSync_Feed_Testing.postman_collection.json`

---

## 🚀 Quick Test Flow

### **1. Setup Data**
```
Register Society User → Verify → Login → Create Posts → Create Events
```

### **2. Test Feed Endpoints**
```
GET /feed?page=0&size=10&filter=all&sort=date          - Full feed
GET /feed?page=0&size=10&filter=posts&sort=date        - Posts only
GET /feed?page=0&size=10&filter=events&sort=date       - Events only
GET /feed?page=0&size=10&filter=all&sort=engagement    - Engagement sort
GET /feed/stats                                         - Statistics
```

### **3. Test Engagement**
```
Like posts → Check engagement sorting → Verify score changes
```

---

## ✅ Expected Results

### **Full Feed Response**
```json
[
  {
    "type": "POST",
    "id": 1,
    "createdAt": "2026-04-02T10:30:00",
    "title": "Post by Society User",
    "content": "Welcome message...",
    "authorName": "Society Test User",
    "likeCount": 3,
    "commentCount": 0,
    "engagementScore": 11.0
  },
  {
    "type": "EVENT",
    "id": 1,
    "createdAt": "2026-05-15T14:00:00",
    "title": "Tech Workshop 2026",
    "content": "Learn web development...",
    "venue": "Computer Lab A",
    "eventType": "SOCIETY",
    "engagementScore": 6.0
  }
]
```

### **Feed Statistics**
```json
{
  "totalPosts": 5,
  "totalEvents": 3,
  "paidEvents": 1,
  "timestamp": "2026-04-02T10:30:00"
}
```

---

## 🔍 Key Test Cases

| Test Case | Expected Result |
|-----------|-----------------|
| Full feed | Posts + events combined |
| Posts filter | Posts only |
| Events filter | Events only |
| Date sort | Newest first |
| Engagement sort | Highest engagement first |
| Pagination | Correct page/size |
| Statistics | Accurate counts |

---

## 📊 Feed Algorithm Verification

### **Engagement Scores**
- **New posts:** High recency score (8-10)
- **Liked posts:** Base score + like count
- **Placement events:** +2.0 bonus
- **Old content:** Low recency score (1-2)

### **Sorting Behavior**
- **Date sort:** Pure chronological
- **Engagement sort:** Popularity-driven

---

## 🎯 Test Commands

```bash
# Basic feed test
curl "http://localhost:8080/feed?page=0&size=5&filter=all&sort=date"

# Posts only
curl "http://localhost:8080/feed/posts?page=0&size=5&sort=engagement"

# Feed stats
curl "http://localhost:8080/feed/stats"
```

---

## 📈 Performance Notes

- **Pagination:** Max 50 items per page
- **Filtering:** Reduces data processing
- **Sorting:** Efficient for small result sets
- **Caching:** Ready for Redis implementation

---

*Test Guide: 2 April 2026*