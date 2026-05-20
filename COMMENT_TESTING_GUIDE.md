# 🧪 CampusSync - Comment System Testing Guide

## 📋 Overview

Test the **Threaded Comment System** with hierarchical replies, permissions, and real-time integration.

**Collection File:** `CampusSync_Comment_Testing.postman_collection.json`

---

## 🚀 Quick Test Flow

### **1. Setup**
```
Register Society + Student Users → Verify → Login → Create Post
```

### **2. Test Comments**
```
POST /posts/{id}/comments → Add comment
POST /posts/{id}/comments → Add another comment
GET /posts/{id}/comments → View threaded structure
```

### **3. Test Replies**
```
POST /comments/{id}/replies → Add reply to first comment
POST /comments/{id}/replies → Add another reply
GET /comments/{id}/replies → View replies
GET /posts/{id}/comments → View full thread
```

### **4. Test Management**
```
PUT /comments/{id} → Edit comment
DELETE /comments/{replyId} → Delete reply first
DELETE /comments/{id} → Delete comment (fails with replies)
DELETE /comments/{remaining} → Delete remaining replies
DELETE /comments/{id} → Delete comment (now works)
```

---

## ✅ Expected Results

### **Threaded Comment Response**
```json
[
  {
    "id": 1,
    "content": "Great post!",
    "authorName": "Student User",
    "createdAt": "2026-04-02T10:30:00",
    "parentCommentId": null,
    "replies": [
      {
        "id": 2,
        "content": "Thanks!",
        "authorName": "Society User",
        "parentCommentId": 1,
        "replies": [],
        "replyCount": 0
      }
    ],
    "replyCount": 1
  }
]
```

### **Post with Comment Count**
```json
{
  "id": 1,
  "content": "Test post",
  "likeCount": 2,
  "commentCount": 3,  // ← Updated with comment count
  "isLikedByCurrentUser": true
}
```

---

## 🔍 Key Test Cases

| Test Case | Expected Result |
|-----------|-----------------|
| Add comment | Returns CommentResponse |
| Add reply | Returns nested CommentResponse |
| Get threaded | Hierarchical structure |
| Update comment | Content changes |
| Delete reply | Reply removed |
| Delete with replies | Error: "Cannot delete..." |
| Delete after replies | Success |
| Wrong permissions | 403 Forbidden |
| Invalid post/comment | 404 Not Found |

---

## 📊 Comment Hierarchy

### **Level 1: Top-level Comments**
```
Post
├── Comment A (by User 1)
├── Comment B (by User 2)
└── Comment C (by User 3)
```

### **Level 2: Replies**
```
Post
├── Comment A
│   ├── Reply A1 (by User 2)
│   └── Reply A2 (by User 1)
└── Comment B
    └── Reply B1 (by User 3)
```

### **API Structure**
```json
{
  "id": 1,
  "content": "Comment A",
  "replies": [
    {
      "id": 2,
      "content": "Reply A1",
      "replies": []
    }
  ]
}
```

---

## 🎯 Test Commands

```bash
# Add comment
curl -X POST "http://localhost:8080/posts/1/comments" \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -d '{"content": "Great post!"}'

# Add reply
curl -X POST "http://localhost:8080/posts/comments/1/replies" \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -d '{"content": "Thanks!"}'

# Get threaded comments
curl "http://localhost:8080/posts/1/comments"
```

---

## 🔒 Permission Rules

- **Create:** Any authenticated user
- **Read:** Public (no auth required)
- **Update:** Comment owner only
- **Delete:** Comment owner or admin
- **Reply:** Any authenticated user

---

## ⚠️ Deletion Rules

1. **Can delete** replies immediately
2. **Cannot delete** comments with existing replies
3. **Must delete** all replies before deleting parent comment
4. **Owner/admin** can delete their comments

---

## 📈 Integration Points

- **Post Response** - Shows comment count
- **Feed Items** - Include comment counts
- **Engagement Scoring** - Comments boost post visibility
- **Real-time Updates** - Counts update immediately

---

*Test Guide: 2 April 2026*