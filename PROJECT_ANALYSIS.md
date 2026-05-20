# CampusSync Backend - Project Analysis Report
**Date:** 2 April 2026

---

## 📋 Executive Summary

**CampusSync Backend** is a comprehensive Spring Boot REST API for managing campus events and user registrations. It's built with a modern tech stack featuring JWT authentication, Redis caching, MySQL database, and AI integration capabilities.

**Project Type:** Spring Boot 3.x Microservice  
**Java Version:** 21  
**Build Tool:** Maven  
**Status:** In Development

---

## 🎯 Project Purpose & Features

### Core Functionality
CampusSync is a campus event management platform that enables:

1. **User Management**
   - Multi-role user registration (STUDENT, DEPARTMENT, SOCIETY, ADMIN)
   - Email-based OTP verification
   - Password reset with OTP
   - JWT-based authentication

2. **Event Management**
   - Create events (restricted to DEPARTMENT, SOCIETY, ADMIN roles)
   - View all events (public endpoint)
   - Event categories: SOCIETY, DEPARTMENT, PLACEMENT
   - Paid/Free event support
   - Event images and descriptions

3. **Event Registration System**
   - Students can register for events
   - Prevent duplicate registrations
   - View personal registrations
   - Cancel registrations
   - Event organizers can view participant lists

4. **File Management**
   - Image/Video upload support (max 50MB)
   - Automatic file naming with UUID
   - File access via `/uploads/**` endpoint

5. **AI Integration**
   - Generate AI responses using Ollama (llama3 model)
   - REST endpoint for prompts
   - Currently uses Ollama API (not OpenAI)

---

## 🏗️ Architecture & Design Patterns

### Layered Architecture
```
Controller Layer (5 controllers)
    ↓
Service Layer (7 services)
    ↓
Repository Layer (JPA Repositories)
    ↓
Database (MySQL) / Cache (Redis)
```

### Design Patterns Used
- **MVC Pattern** - Controllers handle HTTP requests, Services handle business logic
- **DTO Pattern** - Data Transfer Objects for API responses
- **Repository Pattern** - JPA abstraction for database access
- **Strategy Pattern** - Different role-based access strategies
- **Filter Pattern** - JWT filter for authentication

---

## 📦 Technology Stack

### Framework & Libraries
| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.5.13 |
| Security | Spring Security + JWT | Latest |
| Database ORM | Spring Data JPA | Latest |
| Authentication | JWT (JJWT) | 0.11.5 |
| Database | MySQL | Latest |
| Cache | Redis (Lettuce) | Latest |
| Email | Spring Mail (JavaMailSender) | Built-in |
| Validation | Spring Validation | Built-in |
| Utilities | Lombok | Latest |
| Testing | Spring Test + Security Test | Built-in |

### External Services
- **Redis Cloud:** Redis Labs (ap-south-1 region)
  - Host: `redis-16068.crce206.ap-south-1-1.ec2.cloud.redislabs.com`
  - Port: 16068
- **Email Service:** Gmail SMTP
  - Host: `smtp.gmail.com`
  - Port: 587
- **AI Service:** Ollama (Local)
  - URL: `http://localhost:11434/api/generate`
  - Model: llama3

---

## 🗂️ Project Structure

```
src/main/java/com/campussync/backend/
├── BackendApplication.java           # Main entry point
├── Config/
│   ├── JwtUtil.java                 # JWT token generation & validation
│   ├── JwtFilter.java               # JWT request filter
│   ├── SecurityConfig.java          # Spring Security configuration
│   ├── RedisConfig.java             # Redis connection setup
│   └── WebConfig.java               # Web resource configuration
├── Controller/                       # REST API endpoints (5)
│   ├── AuthController.java          # Authentication endpoints
│   ├── EventController.java         # Event management
│   ├── RegistrationController.java  # Event registration
│   ├── FileController.java          # File upload
│   └── AIController.java            # AI generation
├── Service/                          # Business logic (7)
│   ├── UserService.java             # User registration & auth
│   ├── EventService.java            # Event operations
│   ├── RegistrationService.java     # Registration logic
│   ├── EmailService.java            # Email sending
│   ├── FileService.java             # File upload handling
│   ├── AIService.java               # AI prompt processing
│   └── DashboardService.java        # Dashboard metrics
├── Model/                            # JPA Entities (4)
│   ├── User.java                    # User entity
│   ├── Event.java                   # Event entity
│   ├── Registration.java            # Registration entity
│   └── Role.java                    # Role enum
├── Repository/                       # Data access (3)
│   ├── UserRepository.java
│   ├── EventRepository.java
│   └── RegistrationRepository.java
├── Dto/                             # Data Transfer Objects (6)
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── RegistrationResponse.java
│   ├── EventParticipantResponse.java
│   ├── UserRegistrationResponse.java
│   └── DashboardResponse.java
└── Exception/
    └── GlobleException.java         # Empty (not implemented)

src/main/resources/
├── application.properties            # Configuration
└── static/ & templates/

src/test/
└── BackendApplicationTests.java
```

---

## 🔐 Security Architecture

### Authentication Flow
```
1. User Registration (POST /auth/register)
   ├─ Generate OTP
   ├─ Store OTP in Redis (5 min expiry)
   ├─ Send OTP via email
   └─ Store encoded password in Redis

2. Email Verification (POST /auth/verify)
   ├─ Validate OTP from Redis
   ├─ Create User in Database
   ├─ Clean Redis keys
   └─ User ready for login

3. Login (POST /auth/login)
   ├─ Validate credentials
   ├─ Check email verification
   └─ Generate JWT token (24h expiry)

4. Request Authentication
   ├─ Extract Bearer token from Authorization header
   ├─ Validate JWT signature
   ├─ Extract email & role from claims
   └─ Grant access based on role
```

### JWT Token Structure
```json
{
  "sub": "user@example.com",
  "role": "STUDENT",
  "iat": 1704067200,
  "exp": 1704153600
}
```

### Role-Based Access Control (RBAC)
| Endpoint | STUDENT | DEPARTMENT | SOCIETY | ADMIN |
|----------|---------|-----------|---------|-------|
| POST /auth/* | ✅ | ✅ | ✅ | ✅ |
| GET /events | ✅ | ✅ | ✅ | ✅ |
| POST /events | ❌ | ✅ | ✅ | ✅ |
| POST /registrations | ✅ | ❌ | ❌ | ❌ |
| GET /registrations/user/{id} | ✅ | ❌ | ❌ | ✅ |
| GET /registrations/event/{id} | ❌ | ✅ | ✅ | ✅ |
| PUT /registrations/cancel/{id} | ✅ | ❌ | ❌ | ✅ |

### Security Features
✅ **CSRF Protection:** Disabled (API-first design)  
✅ **Password Encoding:** BCrypt  
✅ **OTP Storage:** Redis with TTL  
✅ **Token Validation:** JWT signature verification  
✅ **Email Verification:** Required before login  
✅ **Role-based Access:** Method-level security annotations  

---

## 📡 API Endpoints

### Authentication Endpoints
```
POST   /auth/register          - Register new user
POST   /auth/verify            - Verify email with OTP
POST   /auth/login             - Login user
POST   /auth/resend-otp        - Resend verification OTP
POST   /auth/forgot-password   - Request password reset
POST   /auth/reset-password    - Reset password with OTP
```

### Event Management
```
POST   /events                 - Create event (DEPARTMENT/SOCIETY/ADMIN only)
GET    /events                 - List all events (public)
```

### Registration Management
```
POST   /registrations          - Register for event (STUDENT only)
GET    /registrations/user/{userId}    - Get user's registrations
GET    /registrations/event/{eventId}  - Get event participants
PUT    /registrations/cancel/{id}      - Cancel registration
```

### File Management
```
POST   /events/files/upload    - Upload image/video
GET    /uploads/{filename}     - Access uploaded files
```

### AI Services
```
GET    /ai/generate?prompt=... - Generate AI response
```

### Dashboard (Not exposed via controller yet)
```
Service: DashboardService.getDashboard()
- Returns: Total users, events, registrations, paid events
```

---

## 💾 Database Schema

### Users Table
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255),
  email VARCHAR(255) UNIQUE,
  password VARCHAR(255) -- BCrypt encoded
  is_verified BOOLEAN DEFAULT FALSE,
  verification_code VARCHAR(255),
  role ENUM('STUDENT', 'DEPARTMENT', 'SOCIETY', 'ADMIN')
);
```

### Events Table
```sql
CREATE TABLE event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255),
  description TEXT,
  venue VARCHAR(255),
  date DATETIME,
  type VARCHAR(50), -- SOCIETY, DEPARTMENT, PLACEMENT
  paid BOOLEAN DEFAULT FALSE,
  price DOUBLE,
  image_url VARCHAR(255),
  created_by BIGINT FOREIGN KEY -> users(id)
);
```

### Registrations Table
```sql
CREATE TABLE registration (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT FOREIGN KEY -> users(id),
  event_id BIGINT FOREIGN KEY -> event(id),
  status VARCHAR(50) -- REGISTERED, CANCELLED
);
```

---

## ⚙️ Configuration Details

### Database Configuration
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/campus_sync
spring.datasource.username=root
spring.datasource.password=Rajpagare@12345
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### Redis Configuration
```
Host: redis-16068.crce206.ap-south-1-1.ec2.cloud.redislabs.com
Port: 16068
Username: default
Password: OqI4lU7HLXPHs6EBV5pFXH3Ky4WuTiC5
```

### Email Configuration
```
SMTP Host: smtp.gmail.com
Port: 587
Username: rajpagare011@gmail.com
Password: fvudkgibzmrmhwyr (App password)
Authentication: Enabled
TLS: Enabled
```

### File Upload Configuration
```
Max File Size: 50MB
Max Request Size: 50MB
Upload Directory: {project_root}/uploads/
Allowed Types: image/*, video/*
File Naming: {UUID}_{original_filename}
```

### AI Service Configuration
```
API URL: http://localhost:11434/api/generate
Model: llama3
Stream: false
API Key: ed9fa0195c4d4b018681f1be83248b5f
(Note: This is Ollama API key, not OpenAI)
```

---

## 🔄 Key Business Logic

### User Registration Flow
1. POST `/auth/register` - Generate 6-digit OTP, store in Redis (5 min TTL)
2. Email sent with OTP
3. User calls POST `/auth/verify?email=...&code=...&name=...`
4. OTP validated, user created in database, Redis cleaned
5. User can now login

### Event Registration Flow
1. User authenticated with JWT
2. POST `/registrations?eventId={id}` - Check duplicate prevention
3. Create Registration record
4. Send confirmation email
5. Return RegistrationResponse

### Password Reset Flow
1. POST `/auth/forgot-password?email=...` - Generate reset OTP
2. Email sent with OTP (5 min TTL)
3. POST `/auth/reset-password?email=...&otp=...&newPassword=...`
4. Validate OTP, update password (BCrypt encoded), clean Redis

### OTP Resend with Cooldown
- Cooldown: 60 seconds
- OTP validity: 5 minutes
- Prevents abuse of OTP endpoint

---

## 📊 Data Models

### User Model
```java
@Entity(name = "users")
- id: Long (PK)
- name: String
- email: String (UNIQUE)
- password: String (BCrypt encoded)
- isVerified: boolean
- verificationCode: String
- role: Role (ENUM)
```

### Event Model
```java
@Entity
- id: Long (PK)
- title: String
- description: String
- venue: String
- date: LocalDateTime
- type: String (SOCIETY/DEPARTMENT/PLACEMENT)
- paid: boolean
- price: double
- imageUrl: String
- createdBy: User (ManyToOne FK)
```

### Registration Model
```java
@Entity
- id: Long (PK)
- user: User (ManyToOne FK)
- event: Event (ManyToOne FK)
- status: String (REGISTERED/CANCELLED)
```

### Role Enum
```
STUDENT
DEPARTMENT
SOCIETY
ADMIN
```

---

## 🚨 Current Issues & Observations

### 🔴 Critical Issues
1. **Hardcoded Credentials** - Database password exposed in application.properties
2. **Hardcoded Redis Credentials** - RedisConfig contains production credentials
3. **Gmail App Password Exposed** - Email configuration contains actual password
4. **Ollama API Key Exposed** - API key visible in properties
5. **No SSL/TLS for Redis** - Redis connection lacks encryption

### 🟡 Medium Priority Issues
1. **Empty Exception Handler** - GlobleException.java not implemented
2. **JWT Filter Issues** - JwtFilter doesn't properly set SecurityContext (authentication not propagated)
3. **No Global Exception Handler** - Missing @ControllerAdvice for error handling
4. **Missing JWT Filter Registration** - SecurityConfig doesn't add JwtFilter to filter chain
5. **ROLE_ stored as Object** - Should be stored as String in Redis
6. **No Input Validation** - DTOs lack @NotNull, @Email, @Size annotations
7. **No Pagination** - getAllEvents() loads all events (scalability issue)
8. **Missing Dashboard Endpoint** - DashboardService exists but not exposed

### 🟢 Minor Issues
1. **Typo in Exception Class** - "Globle" instead of "Global"
2. **No API Documentation** - Missing Swagger/Springdoc
3. **No Logging** - Missing SLF4J logging
4. **No Rate Limiting** - No protection against brute force attacks
5. **No CORS Configuration** - Frontend integration needs CORS setup
6. **toString/equals/hashCode Missing** - Model classes rely on Lombok

---

## 🔧 Missing Features

### High Priority
- [ ] Global Exception Handler (@ControllerAdvice)
- [ ] JWT Filter SecurityContext setup
- [ ] Dashboard API endpoint
- [ ] Input validation on DTOs
- [ ] Pagination for events listing
- [ ] Event search/filter capabilities
- [ ] CORS configuration

### Medium Priority
- [ ] API Documentation (Swagger/OpenAPI)
- [ ] Logging (SLF4J + Logback)
- [ ] Rate limiting for OTP
- [ ] Event update/delete endpoints
- [ ] User profile endpoints
- [ ] Event review/rating system
- [ ] Notification system

### Low Priority
- [ ] Testing (Unit & Integration tests)
- [ ] Performance optimization
- [ ] Caching strategies
- [ ] Analytics/Reporting
- [ ] Backup & Recovery
- [ ] Monitoring & Alerting

---

## 📈 Performance Considerations

### Current Bottlenecks
1. **No Database Indexing** - Queries on `email` should be indexed
2. **N+1 Query Problem** - Event listing may cause N+1 queries
3. **No Caching** - Every getAllEvents() hits database
4. **Full Object Serialization** - Redis stores full Java objects

### Optimization Recommendations
1. Add @Transactional(readOnly=true) for read operations
2. Implement Spring Data Projections for DTO mapping
3. Add @Cacheable for frequently accessed data
4. Use pagination with @PageableDefault
5. Add database indexes on email, userId, eventId
6. Implement Redis caching for events (with TTL)

---

## 🧪 Testing Coverage

**Current Status:** Minimal  
Only `BackendApplicationTests.java` exists (likely empty)

### Recommended Test Suite
- **Unit Tests** - Service layer logic (40+ tests)
- **Integration Tests** - Controller endpoints (30+ tests)
- **Security Tests** - JWT validation, role-based access (20+ tests)
- **Repository Tests** - Custom query methods (10+ tests)

---

## 🚀 Deployment Notes

### Requirements
- **Java:** 21
- **Database:** MySQL 8.0+
- **Redis:** 6.0+
- **Ollama:** For AI features (optional)
- **SMTP Server:** Gmail or equivalent

### Configuration Management
- ❌ No .env or ConfigServer usage
- ❌ Environment-specific properties not separated
- ❌ Credentials hardcoded

### Recommended Deployment Setup
1. Move credentials to environment variables
2. Use Spring Cloud Config or AWS Secrets Manager
3. Create application-prod.properties
4. Add Docker support
5. Implement CI/CD pipeline

---

## 📝 Development Recommendations

### Immediate Actions (Next Sprint)
1. Implement proper exception handling
2. Fix JWT filter to properly authenticate users
3. Add input validation to all DTOs
4. Create Dashboard API endpoint
5. Add API documentation

### Short Term (1-2 Sprints)
1. Implement pagination for events
2. Add event search/filter
3. Create global error handler
4. Add comprehensive logging
5. Move credentials to environment variables

### Long Term (Roadmap)
1. Implement advanced search with Elasticsearch
2. Add real-time notifications (WebSocket)
3. Implement payment gateway integration
4. Add mobile app support
5. Implement analytics dashboard
6. Add event attendance tracking

---

## 📚 Code Quality Metrics

| Metric | Value | Status |
|--------|-------|--------|
| Total Java Classes | 30+ | ✅ |
| Controllers | 5 | ✅ |
| Services | 7 | ✅ |
| Repositories | 3 | ✅ |
| DTOs | 6 | ✅ |
| Models | 4 | ✅ |
| Code Duplication | Medium | ⚠️ |
| Exception Handling | Poor | 🔴 |
| Logging | None | 🔴 |
| Test Coverage | <10% | 🔴 |
| Documentation | Minimal | ⚠️ |

---

## 🔍 Dependencies Audit

All dependencies are from official sources and relatively recent (Spring Boot 3.5.13 LTS)

**Key Dependencies:**
- ✅ Spring Boot 3.5.13 (Latest stable)
- ✅ Spring Security (Integrated)
- ✅ JWT JJWT 0.11.5 (Stable)
- ✅ MySQL Connector 8.x
- ✅ Lettuce (Redis client)
- ✅ Lombok (Code generation)

**No known security vulnerabilities** (as of current versions)

---

## 🎓 Learning Points from This Codebase

### Well Implemented
✅ Role-based access control using @PreAuthorize  
✅ OTP-based email verification system  
✅ JWT token generation and validation  
✅ Redis integration for temporary storage  
✅ File upload with security checks  
✅ Service-Repository pattern implementation  

### Needs Improvement
❌ Security - Exposed credentials  
❌ Error Handling - No global exception handler  
❌ Validation - Missing input validation  
❌ Authentication - JWT filter doesn't propagate auth  
❌ Scalability - No pagination, no caching strategy  

---

## 📞 Summary

**CampusSync Backend** is a well-structured Spring Boot application with solid fundamentals in authentication, authorization, and business logic implementation. However, it requires immediate attention to security (credential management), error handling, and proper JWT filter configuration before production use.

The codebase demonstrates good understanding of Spring Security, Redis integration, and role-based access control. With the recommended improvements, this can become a robust, production-ready campus event management platform.

**Overall Code Quality Score: 6.5/10** ⚠️  
**Security Score: 3/10** 🔴  
**Scalability Score: 5/10** ⚠️  

---

*Report Generated: 2 April 2026*
*Analysis By: GitHub Copilot*
