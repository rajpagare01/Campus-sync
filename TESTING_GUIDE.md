# Comprehensive Testing Guide for CampusSync Backend

## Overview
This document provides a comprehensive guide to running, understanding, and maintaining the test suite for the CampusSync backend application.

## Test Structure

### Test Hierarchy

```
src/test/java/com/campussync/backend/
├── Repository/
│   ├── UserRepositoryTest.java          # User CRUD and queries
│   ├── PostRepositoryTest.java          # Post CRUD and queries
│   ├── FollowRepositoryTest.java        # Follow relationship queries
│   └── LikeRepositoryTest.java          # Like/Dislike operations
├── Service/
│   ├── UserServiceTest.java             # User business logic (mocked)
│   ├── PostServiceTest.java             # Post management logic (mocked)
│   ├── FollowServiceTest.java           # Follow/Unfollow logic (mocked)
│   ├── CommentServiceTest.java          # Comment management (mocked)
│   └── LikeServiceTest.java             # Like/Dislike logic (mocked)
├── Controller/
│   ├── UserProfileControllerTest.java   # User API endpoints (integration)
│   ├── PostControllerTest.java          # Post API endpoints (integration)
│   └── FollowControllerTest.java        # Follow API endpoints (integration)
├── IntegrationTestWithTestContainers.java  # Full integration with MySQL container
└── TestConfig.java                      # Test configuration and setup
```

## Test Types

### 1. Unit Tests - Repository Layer (@DataJpaTest)
**Location:** `src/test/java/com/campussync/backend/Repository/`

Tests the data access layer with an in-memory or real database.

**Features:**
- Tests all CRUD operations
- Tests custom query methods
- Validates database constraints
- No mocking of repository methods

**Example:**
```bash
# Run all repository tests
mvn test -Dtest=*RepositoryTest

# Run specific repository test
mvn test -Dtest=UserRepositoryTest
```

### 2. Unit Tests - Service Layer (@ExtendWith(MockitoExtension.class))
**Location:** `src/test/java/com/campussync/backend/Service/`

Tests business logic with mocked dependencies.

**Features:**
- Mocked repositories
- Mocked external services
- Tests logic in isolation
- Tests exception handling
- Tests edge cases

**Example:**
```bash
# Run all service tests
mvn test -Dtest=*ServiceTest

# Run specific service test
mvn test -Dtest=FollowServiceTest
```

### 3. Integration Tests - Controller Layer (@SpringBootTest with @AutoConfigureMockMvc)
**Location:** `src/test/java/com/campussync/backend/Controller/`

Tests REST API endpoints with MockMvc.

**Features:**
- Tests HTTP request/response
- Tests security annotations
- Tests path parameters and query strings
- Tests JSON serialization/deserialization
- Tests HTTP status codes

**Example:**
```bash
# Run all controller tests
mvn test -Dtest=*ControllerTest

# Run specific controller test
mvn test -Dtest=FollowControllerTest
```

### 4. Integration Tests - Full Stack (@Testcontainers)
**Location:** `src/test/java/com/campussync/backend/IntegrationTestWithTestContainers.java`

Tests with real MySQL database in a Docker container.

**Features:**
- Real database testing
- Multi-entity interactions
- Database constraints validation
- Transaction behavior
- Concurrent operations

**Requirements:**
- Docker must be installed and running
- First run may take longer due to image download

## Running Tests

### Run All Tests
```bash
mvn clean test
```

### Run Specific Test Class
```bash
mvn test -Dtest=UserRepositoryTest
```

### Run Tests Matching Pattern
```bash
mvn test -Dtest=*RepositoryTest
```

### Run Tests with Coverage Report
```bash
mvn clean test jacoco:report
# Report available at: target/site/jacoco/index.html
```

### Run Tests in Parallel
```bash
mvn test -DthreadCount=4
```

### Skip Tests During Build
```bash
mvn clean install -DskipTests
```

## Test Coverage

### Coverage Targets
- **Controllers:** 80%+ coverage
- **Services:** 85%+ coverage
- **Repositories:** 90%+ coverage
- **Overall:** 80%+ coverage

### View Coverage Report
```bash
mvn jacoco:report
open target/site/jacoco/index.html  # On Mac/Linux
start target/site/jacoco/index.html # On Windows
```

## Test Profiles

### Available Profiles
- **test:** Default profile for unit and integration tests
- **dev:** Development profile with mock data
- **prod:** Production profile with real configurations

### Activate Profile
```bash
mvn test -Dspring.profiles.active=test
```

## Test Data Setup

### Using @BeforeEach
Each test class includes a `setUp()` method that initializes test data:

```java
@BeforeEach
void setUp() {
    // Create test users, posts, etc.
    testUser = new User();
    testUser.setName("Test User");
    testUser.setEmail("test@example.com");
    // ...
}
```

### Database Reset Between Tests
Tests use `@DataJpaTest` which automatically rolls back changes after each test.

## Mocking Strategy

### Mock Annotations
- `@Mock` - Create a mock object
- `@InjectMocks` - Create instance with injected mocks
- `@MockBean` - Spring mock for integration tests

### Mock Verification
```java
// Verify method was called
verify(repository, times(1)).save(any());

// Verify method was NOT called
verify(repository, never()).delete(any());

// Verify method arguments
verify(repository).save(argThat(user -> user.getEmail().equals("test@example.com")));
```

## Security Testing

### @WithMockUser Annotation
Tests endpoints with authenticated user:
```java
@WithMockUser(username = "test@example.com", roles = "STUDENT")
void testAuthenticatedEndpoint() {
    // User is authenticated as STUDENT
}
```

### CSRF Protection
Controller tests include CSRF token:
```java
mockMvc.perform(post("/api/endpoint")
    .with(csrf())
    .contentType(MediaType.APPLICATION_JSON))
```

## Common Test Scenarios

### Testing User Registration
```java
// Check valid registration
// Check duplicate email handling
// Check invalid email format
// Check password validation
```

### Testing Follow/Unfollow
```java
// Can't follow self
// Can't follow twice
// Successful follow
// Successful unfollow
```

### Testing Post Management
```java
// Only authorized roles can create
// Author can update own post
// Author can delete own post
// Can't update other's post
```

### Testing Pagination
```java
// Empty results return empty page
// Correct page size
// Correct total elements
// Correct page number
```

## Debugging Tests

### Enable Debug Logging
```properties
logging.level.com.campussync.backend=DEBUG
logging.level.org.springframework.test=DEBUG
```

### Use @DisplayName for Clarity
```java
@DisplayName("Should follow user successfully")
void testFollowUserSuccess() { ... }
```

### Print Mock Call Details
```java
ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
verify(repository).save(captor.capture());
User savedUser = captor.getValue();
```

## Best Practices

### 1. Test Naming Convention
```
test[What][When][Then]
testFollowUserSuccess
testFollowUserWhenAlreadyFollowing
testFollowUserThrowsExceptionWhenUserNotFound
```

### 2. Use Descriptive Assertions
```java
// Good
assertThat(response.getFollowerId()).isEqualTo(1L);

// Not as good
assert response != null;
```

### 3. Test One Thing Per Test
```java
// Good - Tests single behavior
@Test
void testFollowUserSuccess() { ... }

// Avoid - Tests multiple behaviors
@Test
void testFollowAndUnfollowAndGetStats() { ... }
```

### 4. Mock External Dependencies Only
```java
// Good - Mock external service
@Mock
private EmailService emailService;

// Avoid - Mock internal repository
@Mock
private UserRepository userRepository;  // In service test
```

### 5. Use TestContainers for Integration
- Use `@Testcontainers` for full database integration
- Use `@DataJpaTest` for repository unit tests
- Use `@SpringBootTest` for controller/API tests

## Troubleshooting

### TestContainer Issues

**Problem:** Docker daemon not running
```
Solution: Start Docker Desktop or Docker daemon
```

**Problem:** MySQL container fails to start
```
Solution: Check Docker has enough resources
         Increase memory allocation in Docker settings
```

**Problem:** Port 3306 already in use
```
Solution: Use random port with Testcontainers
         Or stop existing MySQL container
```

### Flaky Tests

**Problem:** Tests pass/fail randomly
```
Solution: 
- Check for hardcoded delays or race conditions
- Verify database state between tests
- Use @Transactional to isolate tests
- Check for concurrent modifications
```

### Mock Not Working

**Problem:** Mock is null or not injected
```
Solution:
- Ensure @ExtendWith(MockitoExtension.class) is present
- Check @InjectMocks annotation is used
- Verify constructor injection is used
```

## Continuous Integration

### GitHub Actions Setup
Tests run automatically on:
- Push to main/develop branches
- Pull requests
- Scheduled daily builds

### Test Report
Coverage reports are generated and available in:
- GitHub Actions artifacts
- Pull request comments
- Release notes

## Performance Considerations

### Test Execution Time
- Unit tests: ~1-2 seconds each
- Integration tests: ~3-5 seconds each
- TestContainer tests: ~10-15 seconds (includes DB startup)

### Optimization Tips
1. Run unit tests first (faster feedback)
2. Run integration tests in parallel
3. Use @Transactional to speed up data setup
4. Cache Docker images for TestContainers

## Resources

### Testing Libraries
- JUnit 5: https://junit.org/junit5/
- Mockito: https://site.mockito.org/
- AssertJ: https://assertj.org/
- TestContainers: https://www.testcontainers.org/

### Documentation
- Spring Boot Testing: https://spring.io/guides/gs/testing-web/
- Spring Security Testing: https://spring.io/projects/spring-security

## Contact & Support

For issues or questions about tests:
1. Check this guide first
2. Review existing test examples
3. Check test execution logs
4. Consult team documentation
