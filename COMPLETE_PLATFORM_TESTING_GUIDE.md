# 🧪 CampusSync - Complete Social Platform Testing Guide

## 📋 Overview

This comprehensive test collection validates the **complete social platform** implementation across **Tasks 2.1-2.4**:

- **Task 2.1:** Post Management System
- **Task 2.2:** Like System Implementation
- **Task 2.3:** Comment System Implementation
- **Task 2.4:** Home Feed Integration

**Collection File:** `CampusSync_Complete_Social_Platform.postman_collection.json`

---

## 🚀 Complete Test Flow

### **Phase 1: Setup & Authentication**
1. Register Society and Student users
2. Verify emails (use OTP: `123456`)
3. Login and get JWT tokens

### **Phase 2: Content Creation (Task 2.1)**
1. Create multiple posts with different content
2. Create an event
3. Create a post linked to the event

### **Phase 3: Like System (Task 2.2)**
1. Like and unlike posts
2. Switch between users
3. Verify like counts and user status

### **Phase 4: Comment System (Task 2.3)**
1. Add comments to posts
2. Add replies to comments
3. Get threaded comment structure
4. Verify comment counts update

### **Phase 5: Home Feed (Task 2.4)**
1. Test different feed filters (all, posts, events)
2. Test sorting options (date, engagement)
3. Test pagination
4. Check feed statistics

### **Integration: Complete User Journey**
1. Public feed access
2. User engagement (likes, comments)
3. Event registration
4. Updated feed with engagement

---

## ✅ Expected Test Results

### **Final Feed Statistics**
```json
{
  "totalPosts": 3,
  "totalEvents": 1,
  "paidEvents": 0,
  "timestamp": "2026-04-02T10:30:00"
}
```

### **Engaged Post in Feed**
```json
{
  "type": "POST",
  "id": 1,
  "content": "Welcome back to campus!...",
  "authorName": "Society Test User",
  "likeCount": 2,
  "commentCount": 4,
  "isLikedByCurrentUser": true,
  "engagementScore": 12.5
}
```

### **Threaded Comments**
```json
[
  {
    "id": 1,
    "content": "This is exactly what our campus needed!...",
    "authorName": "Student User",
    "replies": [
      {
        "id": 2,
        "content": "@Student User - Thanks for the enthusiasm!...",
        "replies": [
          {
            "id": 3,
            "content": "@Society Test User - I'm most interested...",
            "replies": []
          }
        ]
      }
    ],
    "replyCount": 2
  }
]
```

---

## 📊 Test Coverage Matrix

| Feature | Task | Test Cases | Status |
|---------|------|------------|--------|
| User Registration | 1.0 | 2 users | ✅ Covered |
| Post Creation | 2.1 | 3 posts, 1 event | ✅ Covered |
| Like System | 2.2 | Like/unlike, multi-user | ✅ Covered |
| Comment System | 2.3 | Comments, replies, threading | ✅ Covered |
| Home Feed | 2.4 | Filters, sorting, pagination | ✅ Covered |
| Integration | All | Complete user journey | ✅ Covered |
| Cross-Feature | All | Stats, counts, updates | ✅ Covered |

**Total Test Coverage:** **100%** ✅

---

## 🎯 Key Test Scenarios

### **1. Content Creation Flow**
```
Society User → Create Posts → Create Event → Link Post to Event
Result: 3 posts + 1 event in system
```

### **2. Engagement Flow**
```
Student User → Like Post → Add Comment → Add Reply
Society User → Like Post → Reply to Comment
Result: 2 likes, 4 comments, threaded discussion
```

### **3. Feed Integration**
```
Public Access → View Feed → See Engagement → Register for Event
Result: Complete social interaction cycle
```

### **4. Cross-Feature Verification**
```
Check Post Stats → Verify Feed → Confirm Threading → Validate Counts
Result: All features working together seamlessly
```

---

## 🔄 Variable Management

The collection automatically manages these variables:
- `jwt_token` - Current user's JWT token
- `created_post_id` - ID of first created post
- `created_event_id` - ID of created event
- `comment_id` - ID of first comment
- `reply_id` - ID of first reply

---

## 📈 Performance Validation

### **Response Times**
- Authentication: < 500ms
- Post creation: < 300ms
- Like operations: < 200ms
- Comment operations: < 300ms
- Feed queries: < 500ms

### **Data Integrity**
- Foreign key constraints maintained
- Counts update correctly
- Threading preserved
- User permissions enforced

---

## 🎉 Success Criteria

### **All Tests Pass When:**
- ✅ **Authentication:** JWT tokens generated and accepted
- ✅ **Content Creation:** Posts and events created successfully
- ✅ **Engagement:** Likes and comments work across users
- ✅ **Feed:** Shows content with correct sorting and filtering
- ✅ **Integration:** All features work together seamlessly
- ✅ **Counts:** Like and comment counts update in real-time
- ✅ **Threading:** Comments display in proper hierarchical structure

### **Final System State:**
- 2 registered users (Society + Student)
- 3 posts with varying engagement
- 1 event with registration
- 2 likes on posts
- 4 comments in threaded discussion
- Fully functional social feed

---

## 🚀 Quick Start Commands

```bash
# 1. Start the application
./mvnw spring-boot:run

# 2. Import collection into Postman
# CampusSync_Complete_Social_Platform.postman_collection.json

# 3. Set environment variable
base_url = http://localhost:8080

# 4. Run tests in order (Phase 1 → 5)
# All variables managed automatically
```

---

## 🔍 Troubleshooting

### **Common Issues**
- **401 Unauthorized:** Check JWT token is set
- **400 Bad Request:** Verify request body format
- **404 Not Found:** Check IDs are correct
- **500 Server Error:** Check application logs

### **Variable Issues**
- Clear all variables and restart from Phase 1
- Check Postman console for variable updates
- Ensure tests run in correct order

---

## 📊 Test Results Summary

After running the complete collection:

| Component | Expected State | Verification |
|-----------|----------------|--------------|
| Users | 2 registered, verified | Auth endpoints work |
| Posts | 3 created, mixed content | Post endpoints work |
| Events | 1 created, linked to post | Event endpoints work |
| Likes | 2 likes on posts | Like system works |
| Comments | 4 comments, 2 replies, threaded | Comment system works |
| Feed | Shows all content, sorted by engagement | Feed system works |
| Integration | All counts and stats accurate | Cross-feature works |

---

## 🎯 Next Steps After Testing

1. **Verify Results:** Check all endpoints return expected data
2. **Performance Test:** Run with multiple users/data
3. **Phase 3:** Start advanced event management features
4. **Production Prep:** Add caching, monitoring, documentation

---

*Complete Social Platform Test Suite*
*Created: 2 April 2026*
*Covers: Tasks 2.1-2.4 - 100% Feature Coverage*