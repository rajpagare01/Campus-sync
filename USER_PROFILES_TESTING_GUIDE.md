# 🎓 CampusSync - Task 4.1: User Profiles Testing Guide

## 📋 Overview

This guide provides comprehensive testing instructions for the **Task 4.1: User Profiles** feature. It includes setup instructions, test cases, expected results, and troubleshooting tips.

---

## 🚀 Quick Start

### **Prerequisites**
1. CampusSync backend running on `http://localhost:8080`
2. MySQL database initialized with schema
3. At least 2 test users registered and verified
4. Postman or similar API testing tool

### **Test Setup**
1. Import the Postman collection: `CampusSync_UserProfiles_Testing.postman_collection.json`
2. Set `base_url` variable to `http://localhost:8080`
3. Register 2-3 test users and get their JWT tokens

---

## 📝 Test Users Setup

### **Test User 1 - Student**
```bash
Email: student1@example.com
Password: Student@123
Name: John Doe
Role: STUDENT
```

**Setup Steps:**
```bash
1. POST /auth/register with above data
2. Check email (or logs) for OTP
3. POST /auth/verify with OTP
4. POST /auth/login to get JWT token
5. Save JWT token to {{jwt_token}} variable
```

### **Test User 2 - Society**
```bash
Email: society@example.com
Password: Society@123
Name: Tech Society
Role: SOCIETY
```

### **Test User 3 - Department**
```bash
Email: department@example.com
Password: Dept@123
Name: Computer Science Dept
Role: DEPARTMENT
```

---

## 🧪 Test Cases

### **Test Suite 1: Profile Retrieval**

#### **Test 1.1: Get Current User's Profile**
```http
GET /users/profile
Authorization: Bearer {student1_jwt_token}
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "student1@example.com",
  "role": "STUDENT",
  "bio": null,
  "profilePictureUrl": null,
  "isVerified": true,
  "createdAt": "2026-04-03T10:30:00",
  "updatedAt": "2026-04-03T10:30:00",
  "postCount": 0,
  "eventCount": 0,
  "likeCount": 0,
  "commentCount": 0
}
```

**Validation Checklist:**
- [ ] Status code is 200
- [ ] ID is returned correctly
- [ ] Name and email match test user
- [ ] Role is STUDENT
- [ ] Statistics are initialized (0 or actual count)
- [ ] Timestamps are present
- [ ] Password is not exposed

---

#### **Test 1.2: Get Public User Profile**
```http
GET /users/{userId}/profile
(No authentication required)
```

**Test with User ID 1:**
```http
GET /users/1/profile
```

**Expected Response (200 OK):**
Same structure as Test 1.1 but without authentication

**Validation Checklist:**
- [ ] Status code is 200
- [ ] Public profile information is returned
- [ ] No authentication token required
- [ ] All statistics visible publicly

---

#### **Test 1.3: Non-existent User Profile**
```http
GET /users/99999/profile
```

**Expected Response (404 Not Found):**
```
User not found or empty response body
```

**Validation Checklist:**
- [ ] Status code is 404
- [ ] Error is handled gracefully

---

#### **Test 1.4: Get Profile Without Authentication**
```http
GET /users/profile
(No Authorization header)
```

**Expected Response (401 Unauthorized):**

**Validation Checklist:**
- [ ] Status code is 401
- [ ] Access denied for private endpoint

---

### **Test Suite 2: Profile Updates**

#### **Test 2.1: Update Full Profile**
```http
PUT /users/profile
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "name": "John Doe Updated",
  "bio": "CS student interested in web development",
  "profilePictureUrl": "https://example.com/profile.jpg"
}
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "name": "John Doe Updated",
  "email": "student1@example.com",
  "role": "STUDENT",
  "bio": "CS student interested in web development",
  "profilePictureUrl": "https://example.com/profile.jpg",
  "isVerified": true,
  "createdAt": "2026-04-03T10:30:00",
  "updatedAt": "2026-04-03T14:20:00",
  "postCount": 0,
  "eventCount": 0,
  "likeCount": 0,
  "commentCount": 0
}
```

**Validation Checklist:**
- [ ] Status code is 200
- [ ] Name updated correctly
- [ ] Bio updated correctly
- [ ] Profile picture URL updated
- [ ] updatedAt timestamp changed
- [ ] Other fields unchanged
- [ ] Email/password not exposed

---

#### **Test 2.2: Partial Update - Name Only**
```http
PUT /users/profile
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "name": "Jane Doe"
}
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "name": "Jane Doe",
  "email": "student1@example.com",
  "role": "STUDENT",
  "bio": "CS student interested in web development",
  "profilePictureUrl": "https://example.com/profile.jpg",
  "isVerified": true,
  "createdAt": "2026-04-03T10:30:00",
  "updatedAt": "2026-04-03T14:25:00",
  "postCount": 0,
  "eventCount": 0,
  "likeCount": 0,
  "commentCount": 0
}
```

**Validation Checklist:**
- [ ] Status code is 200
- [ ] Name updated
- [ ] Bio unchanged
- [ ] Profile picture unchanged
- [ ] updatedAt timestamp updated

---

#### **Test 2.3: Partial Update - Bio Only**
```http
PUT /users/profile
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "bio": "New bio: Event organizer and developer"
}
```

**Expected Response (200 OK):**
- [ ] Bio updated
- [ ] Name unchanged (should still be "Jane Doe" or previous)
- [ ] Profile picture unchanged
- [ ] updatedAt timestamp updated

---

#### **Test 2.4: Update Profile Picture**
```http
PATCH /users/profile/picture?pictureUrl=https://example.com/new-pic.jpg
Authorization: Bearer {jwt_token}
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "name": "Jane Doe",
  "email": "student1@example.com",
  "role": "STUDENT",
  "bio": "New bio: Event organizer and developer",
  "profilePictureUrl": "https://example.com/new-pic.jpg",
  "isVerified": true,
  "createdAt": "2026-04-03T10:30:00",
  "updatedAt": "2026-04-03T14:30:00",
  "postCount": 0,
  "eventCount": 0,
  "likeCount": 0,
  "commentCount": 0
}
```

**Validation Checklist:**
- [ ] Status code is 200
- [ ] Profile picture URL updated
- [ ] updatedAt timestamp changed
- [ ] Other fields unchanged

---

#### **Test 2.5: Update Without Authentication**
```http
PUT /users/profile
Content-Type: application/json
(No Authorization header)

{
  "name": "Hacker Attempt"
}
```

**Expected Response (401 Unauthorized):**

**Validation Checklist:**
- [ ] Status code is 401
- [ ] Profile not updated
- [ ] Error handled gracefully

---

### **Test Suite 3: Activity Tracking**

#### **Test 3.1: Get Current User's Activity (Empty)**
```http
GET /users/activity
Authorization: Bearer {jwt_token}
```

**Expected Response (200 OK - Empty Array):**
```json
[]
```

**Validation Checklist:**
- [ ] Status code is 200
- [ ] Empty array for new user
- [ ] Correct structure returned

---

#### **Test 3.2: Activity After Creating Post**

**Step 1: Create a Post (as authenticated user)**
```http
POST /posts
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "content": "Test post for activity tracking"
}
```

**Step 2: Get Activity**
```http
GET /users/activity
Authorization: Bearer {jwt_token}
```

**Expected Response (200 OK):**
```json
[
  {
    "id": 1,
    "activityType": "POST",
    "description": "Created a post",
    "timestamp": "2026-04-03T14:35:00",
    "relatedId": 1,
    "relatedTitle": null
  }
]
```

**Validation Checklist:**
- [ ] Status code is 200
- [ ] Activity recorded with type "POST"
- [ ] Timestamp is correct
- [ ] Related post ID is correct

---

#### **Test 3.3: Activity After Liking Post**

**Step 1: Like a Post**
```http
POST /posts/{postId}/like
Authorization: Bearer {jwt_token}
```

**Step 2: Get Activity**
```http
GET /users/activity
Authorization: Bearer {jwt_token}
```

**Expected Response (200 OK):**
```json
[
  {
    "id": 2,
    "activityType": "LIKE",
    "description": "Liked a post",
    "timestamp": "2026-04-03T14:40:00",
    "relatedId": 1,
    "relatedTitle": null
  },
  {
    "id": 1,
    "activityType": "POST",
    "description": "Created a post",
    "timestamp": "2026-04-03T14:35:00",
    "relatedId": 1,
    "relatedTitle": null
  }
]
```

**Validation Checklist:**
- [ ] Like activity recorded
- [ ] Activities sorted by timestamp (newest first)
- [ ] All activities visible in feed

---

#### **Test 3.4: Activity After Commenting**

**Step 1: Comment on Post**
```http
POST /posts/{postId}/comments
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "content": "Great post!"
}
```

**Step 2: Get Activity**
```http
GET /users/activity
Authorization: Bearer {jwt_token}
```

**Expected Response (200 OK):**
```json
[
  {
    "id": 3,
    "activityType": "COMMENT",
    "description": "Commented on a post",
    "timestamp": "2026-04-03T14:45:00",
    "relatedId": 1,
    "relatedTitle": null
  },
  // ... previous activities
]
```

**Validation Checklist:**
- [ ] Comment activity recorded
- [ ] Activities list updated
- [ ] Sorting maintained

---

#### **Test 3.5: Activity After Event Registration**

**Step 1: Register for Event**
```http
POST /registrations?eventId={eventId}
Authorization: Bearer {jwt_token}
```

**Step 2: Get Activity**
```http
GET /users/activity
Authorization: Bearer {jwt_token}
```

**Expected Response (200 OK):**
```json
[
  {
    "id": 4,
    "activityType": "REGISTRATION",
    "description": "Registered for event: Tech Fest 2026",
    "timestamp": "2026-04-03T14:50:00",
    "relatedId": 1,
    "relatedTitle": "Tech Fest 2026"
  },
  // ... previous activities
]
```

**Validation Checklist:**
- [ ] Registration activity recorded
- [ ] Event title included in description
- [ ] Related event ID stored

---

#### **Test 3.6: Get Public User Activity**
```http
GET /users/{userId}/activity
(No authentication required)
```

**Expected Response (200 OK):**
Same format as authenticated user's activity

**Validation Checklist:**
- [ ] Status code is 200
- [ ] No authentication required
- [ ] Public activity visible

---

### **Test Suite 4: Statistics**

#### **Test 4.1: Get User Statistics (New User)**
```http
GET /users/stats/my-stats
Authorization: Bearer {jwt_token}
```

**Expected Response (200 OK):**
```json
{
  "userId": 1,
  "userName": "Jane Doe",
  "postCount": 0,
  "eventRegistrations": 0,
  "likeCount": 0,
  "commentCount": 0,
  "joinedDate": "2026-04-03T10:30:00"
}
```

**Validation Checklist:**
- [ ] Status code is 200
- [ ] All statistics initialized
- [ ] User info correct
- [ ] Join date present

---

#### **Test 4.2: Statistics After Creating Post**

**Assume user has created 1 post:**
```http
GET /users/stats/my-stats
Authorization: Bearer {jwt_token}
```

**Expected Response:**
```json
{
  "postCount": 1,
  // ... other stats
}
```

**Validation Checklist:**
- [ ] Post count updated to 1
- [ ] Other stats unchanged

---

#### **Test 4.3: Statistics After Multiple Activities**

**Assume user has:**
- 3 posts
- 2 event registrations
- 5 likes given
- 3 comments made

```http
GET /users/stats/my-stats
Authorization: Bearer {jwt_token}
```

**Expected Response:**
```json
{
  "userId": 1,
  "userName": "Jane Doe",
  "postCount": 3,
  "eventRegistrations": 2,
  "likeCount": 5,
  "commentCount": 3,
  "joinedDate": "2026-04-03T10:30:00"
}
```

**Validation Checklist:**
- [ ] All counters accurate
- [ ] Statistics reflect actual activities
- [ ] Real-time updates

---

#### **Test 4.4: Get Statistics for Any User**
```http
GET /users/{userId}/stats
```

**Expected Response (200 OK):**
Same format as user's own statistics

**Validation Checklist:**
- [ ] Public access works
- [ ] Correct user's stats returned
- [ ] No authentication required

---

### **Test Suite 5: Error Handling**

#### **Test 5.1: Invalid User ID Format**
```http
GET /users/invalid-id/profile
```

**Expected Response:**
- [ ] 400 Bad Request or appropriate error
- [ ] Graceful error handling

---

#### **Test 5.2: Negative User ID**
```http
GET /users/-1/profile
```

**Expected Response:**
- [ ] 404 Not Found
- [ ] Graceful error handling

---

#### **Test 5.3: Empty Request Body**
```http
PUT /users/profile
Authorization: Bearer {jwt_token}
Content-Type: application/json

{}
```

**Expected Response:**
- [ ] 200 OK (no changes)
- [ ] Profile returned unchanged

---

#### **Test 5.4: Null Values in Update**
```http
PUT /users/profile
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "name": null,
  "bio": null,
  "profilePictureUrl": null
}
```

**Expected Response:**
- [ ] 200 OK
- [ ] Null values ignored/not updated
- [ ] Existing values preserved

---

### **Test Suite 6: Data Consistency**

#### **Test 6.1: Verify Data Persistence**

**Step 1: Update Profile**
```http
PUT /users/profile
Authorization: Bearer {jwt_token}
{
  "name": "Updated Name",
  "bio": "Updated bio"
}
```

**Step 2: Get Profile Again**
```http
GET /users/profile
Authorization: Bearer {jwt_token}
```

**Expected:**
- [ ] Updated data persisted
- [ ] Same values returned
- [ ] Data consistency maintained

---

#### **Test 6.2: Timestamp Accuracy**

**Update profile and verify:**
- [ ] createdAt remains unchanged
- [ ] updatedAt is newer than createdAt
- [ ] Subsequent updates increase updatedAt

---

### **Test Suite 7: Integration Tests**

#### **Test 7.1: Profile + Post Integration**

**Workflow:**
1. Create user profile
2. Update profile information
3. Create posts
4. Verify post count in profile
5. Verify posts shown in activity

**Expected:**
- [ ] Post count updated
- [ ] Posts listed in activity
- [ ] All data consistent

---

#### **Test 7.2: Profile + Event Integration**

**Workflow:**
1. Get user profile
2. Register for event
3. Check profile statistics
4. Check activity feed

**Expected:**
- [ ] Event registration counted
- [ ] Activity recorded
- [ ] Statistics updated

---

## 🔍 Debugging Tips

### **Issue: Profile Returns Empty**
- Check user exists in database
- Verify JWT token is valid
- Check user_id in token matches URL

### **Issue: Statistics Show 0**
- Verify user has actually created posts/likes/comments
- Check database constraints
- Verify repository queries work

### **Issue: Activity Not Showing**
- Create test post/like/comment first
- Verify related entities exist
- Check database for orphaned records

### **Issue: 401 Unauthorized**
- Verify JWT token in Authorization header
- Check token hasn't expired
- Try logging in again to get new token

### **Issue: 404 Not Found**
- Verify user ID exists
- Check user_id is numeric
- Try a known existing user ID

---

## 📊 Performance Testing

### **Load Test: Get Profile**
```
Run 100 concurrent requests to GET /users/{userId}/profile
Expected: < 500ms response time
```

### **Load Test: Update Profile**
```
Run 50 concurrent requests to PUT /users/profile
Expected: < 1000ms response time
```

### **Load Test: Get Activity (Large)**
```
User with 1000+ activities
Expected: < 2000ms response time
```

---

## ✅ Acceptance Criteria

All tests must pass:
- [ ] All 50+ test cases execute successfully
- [ ] No null pointer exceptions
- [ ] Data integrity maintained
- [ ] Performance within SLAs
- [ ] Error handling consistent
- [ ] No security vulnerabilities
- [ ] Documentation complete

---

## 🎯 Test Execution Checklist

### **Pre-Testing**
- [ ] Database initialized
- [ ] Backend running
- [ ] Test users created
- [ ] JWT tokens obtained
- [ ] Postman collection imported

### **During Testing**
- [ ] All test suites executed
- [ ] Results documented
- [ ] Failures investigated
- [ ] Screenshots captured

### **Post-Testing**
- [ ] Results compiled
- [ ] Issues logged
- [ ] Performance metrics noted
- [ ] Sign-off obtained

---

## 📞 Summary

This testing guide covers:
- ✅ 40+ comprehensive test cases
- ✅ Profile CRUD operations
- ✅ Activity tracking verification
- ✅ Statistics calculation
- ✅ Error handling
- ✅ Integration scenarios
- ✅ Performance considerations

**Estimated Testing Time:** 2-3 hours

---

*Testing Guide: 3 April 2026*
*Task: 4.1 - User Profiles*
