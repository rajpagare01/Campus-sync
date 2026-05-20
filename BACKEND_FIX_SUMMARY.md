# Backend Fix Summary - PostRepository Query Issue

## Problem
The Spring Boot backend failed to start with the following error:
```
Could not create query for public abstract long com.campussync.backend.Repository.PostRepository.countByEvent_Id(java.lang.Long); 
No property 'event' found for type 'Post'
```

## Root Cause
The `PostRepository` interface had a method named `countByEvent_Id()`, but the `Post` entity doesn't have an `event` property. Instead, the Post entity has:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "event_id")
private Event linkedEvent;  // ← Actual property name
```

Spring Data JPA derives query methods from **property names**, not database column names. The method tried to find a property called `event`, which didn't exist.

## Solution
### 1. Fixed PostRepository.java
**Changed:**
```java
long countByEvent_Id(Long eventId);
```
**To:**
```java
long countByLinkedEvent_Id(Long eventId);
```

### 2. Fixed AnalyticsService.java
**Changed:**
```java
long postsCount = postRepository.countByEvent_Id(eventId);
```
**To:**
```java
long postsCount = postRepository.countByLinkedEvent_Id(eventId);
```

## Files Modified
- `src/main/java/com/campussync/backend/Repository/PostRepository.java`
- `src/main/java/com/campussync/backend/Service/AnalyticsService.java`

## Compilation Status
✅ **Successfully Compiled** - No errors after applying the fix

## Next Steps
The backend can now be started without the repository initialization error. The fixed method now correctly references the `linkedEvent` property, which maps to the `event_id` foreign key column in the database.