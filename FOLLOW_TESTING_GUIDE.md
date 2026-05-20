# 🧪 CampusSync - Follow System Testing Guide

## 📋 Overview

Test the **User Following System** with follow/unfollow operations, statistics, recommendations, and mutual follows.

**Collection File:** `CampusSync_Follow_Testing.postman_collection.json`

---

## 🚀 Quick Test Flow

### **1. Setup Data**
```
Register 3+ Users → Verify → Login as different users → Follow operations
```

### **2. Test Follow Operations**
```
Login as User A → Follow User B → Check stats → Unfollow → Check stats
```

### **3. Test Relationships**
```
User A follows User B → User B follows User A → Check mutual follows
```

### **4. Test Recommendations**
```
Create follow network → Get recommendations → Verify logic
```

---

## ✅ Expected Results

### **Follow User Response**
```json
{
  "id": 1,
  "followerId": 1,
  "followerName": "John Doe",
  "followingId": 2,
  "followingName": "Jane Smith",
  "createdAt": "2026-04-04T12:00:00",
  "isMutual": false
}
```

### **Follow Stats Response**
```json
{
  "followersCount": 5,
  "followingCount": 3,
  "isFollowing": true,
  "isFollowedBy": false,
  "isMutual": false
}
```

### **Followers List Response**
```json
{
  "content": [
    {
      "id": 1,
      "followerId": 2,
      "followerName": "Jane Smith",
      "followingId": 1,
      "followingName": "John Doe",
      "createdAt": "2026-04-04T12:00:00",
      "isMutual": true
    }
  ],
  "pageable": {
    "page": 0,
    "size": 20
  },
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "empty": false
}
```

### **Recommended Users Response**
```json
{
  "content": [
    {
      "id": 3,
      "name": "Bob Wilson",
      "email": "bob@example.com",
      "role": "STUDENT"
    }
  ],
  "pageable": {
    "page": 0,
    "size": 20
  },
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "empty": false
}
```

---

## 🧪 Detailed Test Cases

### **TC-001: Follow User**
- **Endpoint:** `POST /api/follow/follow/{userId}`
- **Auth:** Required
- **Input:** Valid target user ID
- **Expected:** 200 OK, FollowResponse
- **Error Cases:**
  - 400: Self-follow attempt
  - 400: Already following
  - 404: User not found

### **TC-002: Unfollow User**
- **Endpoint:** `DELETE /api/follow/unfollow/{userId}`
- **Auth:** Required
- **Input:** Valid target user ID
- **Expected:** 200 OK, Success message
- **Error Cases:**
  - 400: Not following

### **TC-003: Check Following Status**
- **Endpoint:** `GET /api/follow/is-following/{userId}`
- **Auth:** Required
- **Expected:** 200 OK, boolean

### **TC-004: Get Follow Stats**
- **Endpoint:** `GET /api/follow/stats/{userId}`
- **Auth:** Required
- **Expected:** 200 OK, FollowStats

### **TC-005: Get Followers**
- **Endpoint:** `GET /api/follow/followers/{userId}?page=0&size=20`
- **Auth:** Required
- **Expected:** 200 OK, PaginatedResponse<FollowResponse>

### **TC-006: Get Following**
- **Endpoint:** `GET /api/follow/following/{userId}?page=0&size=20`
- **Auth:** Required
- **Expected:** 200 OK, PaginatedResponse<FollowResponse>

### **TC-007: Get Recommendations**
- **Endpoint:** `GET /api/follow/recommendations?page=0&size=20`
- **Auth:** Required
- **Expected:** 200 OK, PaginatedResponse<User>

### **TC-008: Get Mutual Follows**
- **Endpoint:** `GET /api/follow/mutual`
- **Auth:** Required
- **Expected:** 200 OK, List<User>

---

## 🔍 Validation Checks

### **Business Logic**
- ✅ Cannot follow self
- ✅ Cannot follow same user twice
- ✅ Mutual follow detection
- ✅ Recommendation algorithm (users followed by followed users)
- ✅ Pagination works correctly
- ✅ Cache invalidation on follow/unfollow

### **Security**
- ✅ Authentication required for all endpoints
- ✅ Authorization checks (own data access)
- ✅ Input validation
- ✅ Rate limiting applies

### **Performance**
- ✅ Response time < 500ms
- ✅ Pagination reduces data load
- ✅ Caching improves stats retrieval

---

## 🚨 Common Issues & Fixes

### **Issue: Already following error**
```
Fix: Check current follow status before attempting to follow
```

### **Issue: Mutual not detected**
```
Fix: Ensure both users have followed each other, check database
```

### **Issue: Recommendations empty**
```
Fix: Create more users and follow relationships for algorithm to work
```

---

## 📊 Test Data Setup

### **Create Test Users**
```json
POST /api/auth/register
{
  "name": "Test User 1",
  "email": "test1@example.com",
  "password": "password123",
  "role": "STUDENT"
}
```

### **Follow Network Setup**
```
User A → User B
User A → User C
User B → User C
User D → User C
Expected: User D recommended to User A
```

---

## 🎯 Success Criteria

- [ ] All endpoints return 200 OK
- [ ] Follow/unfollow operations work
- [ ] Stats update correctly
- [ ] Pagination works
- [ ] Recommendations are relevant
- [ ] Mutual follows detected
- [ ] No security vulnerabilities
- [ ] Performance acceptable

---

*Generated: 4 April 2026*  
*Test Collection: CampusSync_Follow_Testing.postman_collection.json*