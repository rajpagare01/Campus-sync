# Quick Test Reference Guide

## Maven Test Commands

### Basic Test Execution
```bash
# Run all tests
mvn clean test

# Run tests with verbose output
mvn test -X

# Run tests with info logging
mvn test -q
```

### Run Specific Tests
```bash
# Single test class
mvn test -Dtest=FollowServiceTest

# Multiple test classes
mvn test -Dtest=FollowServiceTest,PostServiceTest

# Pattern matching
mvn test -Dtest=*ServiceTest
mvn test -Dtest=*RepositoryTest
mvn test -Dtest=*ControllerTest

# Specific test method
mvn test -Dtest=FollowServiceTest#testFollowUserSuccess
```

### Test Organization
```bash
# Repository tests only
mvn test -Dtest=*RepositoryTest

# Service tests only
mvn test -Dtest=*ServiceTest

# Controller tests only
mvn test -Dtest=*ControllerTest

# Integration tests only
mvn test -Dtest=IntegrationTestWithTestContainers
```

### Performance & Coverage
```bash
# Run tests in parallel (4 threads)
mvn test -DthreadCount=4

# Generate coverage report
mvn clean test jacoco:report

# View coverage report (auto-open in browser)
mvn clean test jacoco:report open target/site/jacoco/index.html

# Skip tests during build
mvn clean install -DskipTests

# Fail build if tests fail
mvn clean test -DfailIfNoTests=true

# Run with specific profile
mvn test -Dspring.profiles.active=test
```

### Debugging Tests
```bash
# Run with debug output
mvn test -X

# Run single test with debugging
mvn test -Dtest=FollowServiceTest -DforkMode=never

# Show test execution summary
mvn test -DconfigurationParameters="-verbose"
```

## IntelliJ IDEA Test Shortcuts

### In Editor
- `Ctrl + Shift + F10` - Run current test
- `Ctrl + Shift + F9` - Run tests in package
- `Ctrl + Alt + Shift + F10` - Run with coverage

### Test Runner
- `Ctrl + Alt + R` - Run tests
- `Ctrl + Shift + R` - Run with coverage
- `Alt + Shift + F10` - Run configuration menu

## Test File Locations

```
src/test/
├── java/com/campussync/backend/
│   ├── Repository/
│   │   ├── UserRepositoryTest.java
│   │   ├── PostRepositoryTest.java
│   │   ├── FollowRepositoryTest.java
│   │   └── LikeRepositoryTest.java
│   ├── Service/
│   │   ├── UserServiceTest.java
│   │   ├── PostServiceTest.java
│   │   ├── FollowServiceTest.java
│   │   ├── CommentServiceTest.java
│   │   └── LikeServiceTest.java
│   ├── Controller/
│   │   ├── UserProfileControllerTest.java
│   │   ├── PostControllerTest.java
│   │   └── FollowControllerTest.java
│   ├── IntegrationTestWithTestContainers.java
│   └── TestConfig.java
└── resources/
    └── application-test.properties
```

## Test Annotations Reference

### JUnit 5
```java
@Test                           // Mark as test method
@DisplayName("Test description") // Custom test name
@BeforeEach                      // Run before each test
@AfterEach                       // Run after each test
@BeforeAll                       // Run once before all tests
@AfterAll                        // Run once after all tests
@ParameterizedTest              // Parameterized testing
@RepeatedTest(5)                // Repeat test 5 times
@Disabled                       // Disable test
@Tag("slow")                    // Tag tests
```

### Spring Boot Test
```java
@SpringBootTest                 // Full context test
@WebMvcTest                     // Web layer only
@DataJpaTest                    // JPA layer only
@AutoConfigureMockMvc          // Enable MockMvc
@MockBean                       // Spring mock
@SpyBean                        // Partial mock
```

### Mockito
```java
@ExtendWith(MockitoExtension.class)  // Enable Mockito
@Mock                                 // Create mock
@InjectMocks                          // Inject mocks
@Spy                                  // Partial mock
```

### TestContainers
```java
@Testcontainers                       // Enable containers
@Container                            // Container definition
```

### Spring Security
```java
@WithMockUser                         // Mock authenticated user
@WithMockUser(roles = "ADMIN")       // With specific role
@WithUserDetails                      // Load user from DB
```

## Assertion Examples

### AssertJ
```java
// Basic assertions
assertThat(value).isEqualTo(expected);
assertThat(value).isNotNull();
assertThat(value).isNull();
assertThat(list).hasSize(3);
assertThat(list).contains(item);
assertThat(list).isEmpty();

// String assertions
assertThat(text).startsWith("Hello");
assertThat(text).endsWith("World");
assertThat(text).contains("test");

// Number assertions
assertThat(number).isGreaterThan(10);
assertThat(number).isLessThan(100);
assertThat(number).isBetween(10, 100);

// Exception assertions
assertThatThrownBy(() -> method())
    .isInstanceOf(RuntimeException.class)
    .hasMessage("Error message");
```

## Common Test Patterns

### Repository Test Pattern
```java
@DataJpaTest
class RepositoryTest {
    @Autowired
    private YourRepository repository;
    
    @BeforeEach
    void setUp() {
        // Initialize test data
    }
    
    @Test
    void testMethod() {
        // Given
        // When
        // Then
    }
}
```

### Service Test Pattern
```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock
    private Repository repository;
    
    @InjectMocks
    private YourService service;
    
    @Test
    void testMethod() {
        // Given
        when(repository.method()).thenReturn(value);
        
        // When
        service.method();
        
        // Then
        verify(repository).method();
    }
}
```

### Controller Test Pattern
```java
@SpringBootTest
@AutoConfigureMockMvc
class ControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private Service service;
    
    @Test
    void testEndpoint() throws Exception {
        // Given
        when(service.method()).thenReturn(value);
        
        // When & Then
        mockMvc.perform(get("/api/endpoint"))
            .andExpect(status().isOk());
    }
}
```

## Troubleshooting

### Test Execution Issues

**Tests not running:**
```bash
# Check Maven is installed
mvn --version

# Ensure test files are in src/test/java
# Check class names end with "Test"

# Rebuild and clear cache
mvn clean install -DskipTests
mvn test
```

**Mock not working:**
```java
// Ensure annotation is present
@ExtendWith(MockitoExtension.class)

// Or use @SpringBootTest with @MockBean
@SpringBootTest
class ControllerTest {
    @MockBean
    private Service service;
}
```

**Database connection error:**
```bash
# For TestContainers tests, ensure Docker is running
docker ps

# Check database properties in application-test.properties
# Verify MySQL container can start
docker run -it mysql:8.0 --help
```

**Port already in use:**
```bash
# Kill process on port 8080 (if needed for tests)
# Or let Spring find a random port
# TestContainers uses random ports automatically
```

## Performance Optimization

### Parallel Test Execution
```bash
mvn test -DthreadCount=4 -DuseUnlimitedThreads=false
```

### Skip Integration Tests (Run unit tests only)
```bash
mvn test -Dtest=*RepositoryTest,*ServiceTest
```

### Cache Docker Images
```bash
# TestContainers caches images automatically
# For first run, download may take time
# Subsequent runs use cached image
```

## Coverage Goals

| Component | Target | Command |
|-----------|--------|---------|
| Overall | 80% | `mvn jacoco:report` |
| Repository | 95% | `mvn test jacoco:report` |
| Service | 90% | `mvn test jacoco:report` |
| Controller | 85% | `mvn test jacoco:report` |

## CI/CD Integration

### GitHub Actions
```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
      - run: mvn clean test jacoco:report
```

### Generate Report
```bash
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

## Test Naming Convention

```
testWhatHappens[When][Then]
testFollowUserSuccessfullyWhenNotAlreadyFollowing
testFollowUserThrowsExceptionWhenUserNotFound
testGetFollowersReturnsEmptyPageWhenNoFollowers
```

## Resources

- **JUnit 5 Docs:** https://junit.org/junit5/docs/current/user-guide/
- **Mockito Docs:** https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
- **AssertJ Docs:** https://assertj.org/docs/
- **Spring Boot Test:** https://spring.io/projects/spring-boot/#learn
- **TestContainers:** https://www.testcontainers.org/

## Quick Links

- Test Suite Summary: `TEST_SUITE_SUMMARY.md`
- Testing Guide: `TESTING_GUIDE.md`
- Test Configuration: `src/test/resources/application-test.properties`
- Test Code: `src/test/java/com/campussync/backend/`
