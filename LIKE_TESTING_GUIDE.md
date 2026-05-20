# 🧪 CampusSync - Like System Testing Guide

## 📋 Overview

Test the complete **Like System** implementation with toggle functionality, like counts, and user status tracking.

**Collection File:** `CampusSync_Like_Testing.postman_collection.json`

---

## 🚀 Quick Test Flow

### **1. Setup Users**
```
POST /auth/register → Society User
POST /auth/verify → Verify Society
POST /auth/login → Login Society (get JWT)
POST /auth/register → Student User
POST /auth/verify → Verify Student
```

### **2. Create Test Post**
```
POST /posts → Create post (as Society)
Response: Post ID saved automatically
```

### **3. Test Like System**
```
POST /posts/{id}/like → Like post (as Society)
GET /posts/{id} → Check likeCount: 1, isLikedByCurrentUser: true
GET /posts/{id}/likes → See like details
POST /posts/{id}/like → Unlike post (as Society)
GET /posts/{id} → Check likeCount: 0, isLikedByCurrentUser: false
```

### **4. Multi-User Testing**
```
POST /auth/login → Login as Student
POST /posts/{id}/like → Student likes post
GET /posts/{id}/likes → See both users' likes
GET /posts/likes/user → See student's liked posts
```

---

## ✅ Expected Results

### **Like Response (First Time)**
```json
{
  "id": 1,
  "userId": 1,
  "userName": "Society Test User",
  "userEmail": "society@example.com",
  "postId": 1,
  "createdAt": "2026-04-02T10:30:00"
}
```

### **Unlike Response (Second Time)**
```json
null
```

### **Post with Like Data**
```json
{
  "id": 1,
  "content": "Test post",
  "likeCount": 1,
  "commentCount": 0,
  "isLikedByCurrentUser": true
}
```

---

## 🔍 Key Test Cases

| Test Case | Expected Result |
|-----------|-----------------|
| Like post | Returns LikeResponse |
| Like again | Returns null (unliked) |
| Check post | likeCount increases/decreases |
| Multiple users | Each can like independently |
| No auth | 401 Unauthorized |
| Invalid post | 400 Bad Request |

---

## 🎯 Test Commands

```bash
# Quick test with cURL
curl -X POST "http://localhost:8080/posts/1/like" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 📊 Test Coverage

- ✅ **Toggle Logic** - Like/unlike in one endpoint
- ✅ **Count Updates** - Real-time like counts
- ✅ **User Status** - Shows if user liked post
- ✅ **Multi-User** - Multiple users can like
- ✅ **Security** - Authentication required
- ✅ **Errors** - Proper error handling

---

*Test Guide: 2 April 2026*