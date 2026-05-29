/**
 * K6 Load Test — README
 * =====================
 * CampusSync API Load Testing with k6
 */

# CampusSync — k6 Load Tests

## Installation
```bash
# Windows (via Chocolatey)
choco install k6

# macOS
brew install k6

# Or download from: https://k6.io/docs/getting-started/installation/
```

---

## Testing Priority (Run in this order)

### Why this order?
```
Auth → Public Reads → Authenticated Writes → Registration → Search
```
- **Auth must work** before anything else can be tested
- **Public reads** have the most traffic → find DB/caching bottlenecks first
- **Writes** stress the DB under concurrency
- **Registration + Search** are heavier compound operations

---

## Test 1: Auth APIs (MUST RUN FIRST)
```bash
k6 run -e BASE_URL=https://campus-sync-9luf.onrender.com k6/01_auth_test.js
```
**What it tests:**
- `POST /api/v1/auth/login` — BCrypt password check + JWT generation
- `POST /api/v1/auth/refresh` — Token rotation

**Load:** Ramp from 10 → 50 → 100 users over 4 minutes

**Fix issues if:** Login P95 > 2s (tune BCrypt strength or add caching)

---

## Test 2: Public Read APIs (Events + Feed)
```bash
# Anonymous (no auth needed)
k6 run -e BASE_URL=https://campus-sync-9luf.onrender.com k6/02_public_read_test.js

# With a JWT token for authenticated feed tests
k6 run -e BASE_URL=https://campus-sync-9luf.onrender.com \
        -e TOKEN=<your_jwt_token> \
        k6/02_public_read_test.js
```
**What it tests:**
- `GET /api/v1/events?page=0&size=20`
- `GET /api/v1/events/search?keyword=tech`
- `GET /api/v1/feed?page=0&size=20`
- `GET /api/v1/feed/stats`
- `GET /api/v1/feed/trending`

**Load:** 20 → 150 anonymous users + 30 concurrent authenticated users

**Fix issues if:** Feed P95 > 3s (add caching or fix N+1 queries)

---

## Test 3: Authenticated Write Actions
```bash
# Get a token first (login once manually or use Test 1 output)
k6 run -e BASE_URL=https://campus-sync-9luf.onrender.com \
        -e TOKEN=<your_jwt_token> \
        k6/03_authenticated_actions_test.js
```
**What it tests:**
- `GET /api/v1/users/profile`
- `POST /api/v1/posts`
- `GET /api/v1/posts`
- `POST /api/v1/posts/:id/like`
- `POST /api/v1/comments`
- `GET /api/v1/events/search`

**Load:** Ramp from 5 → 50 authenticated writers over 4 minutes

**Fix issues if:** Write failures > 10% (DB connection pool exhaustion)

---

## Getting a JWT Token for Tests
```bash
# Login once and copy the accessToken
curl -X POST https://campus-sync-9luf.onrender.com/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"rajpagare011@gmail.com","password":"your_password"}'
```

---

## Thresholds (Pass/Fail Criteria)

| Test | Metric | Threshold |
|------|--------|-----------|
| Auth | Login P95 | < 1500ms |
| Auth | Login success rate | > 95% |
| Events | P95 response | < 2000ms |
| Feed | P95 response | < 3000ms |
| Posts write | P95 response | < 3000ms |
| All | HTTP failure rate | < 5% |

---

## Reading Results
After each run look for:
- `✓` = threshold passed
- `✗` = threshold failed (bottleneck found!)
- `http_req_duration{p(95)}` — 95th percentile response time
- `http_reqs` — total requests and req/s throughput

## Common Issues & Fixes
| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| Login P95 > 3s | BCrypt too strong | Lower `BCryptPasswordEncoder` strength from default 10 to 8 |
| Feed P95 > 5s | N+1 queries | Add `@EntityGraph` or use joins in repository |
| Write failures > 10% | DB pool exhausted | Increase `hikari.maximum-pool-size` in properties |
| 429 errors | Rate limiting | Increase rate limit or use multiple test accounts |
